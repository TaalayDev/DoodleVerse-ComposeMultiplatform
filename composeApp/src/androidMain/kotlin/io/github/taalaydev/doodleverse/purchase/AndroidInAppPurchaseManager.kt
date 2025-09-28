package io.github.taalaydev.doodleverse.purchase

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import io.github.taalaydev.doodleverse.purchase.models.*
import io.github.taalaydev.doodleverse.purchase.models.Purchase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume

class AndroidInAppPurchaseManager(
    private val context: Context
) : InAppPurchaseManager, PurchasesUpdatedListener, BillingClientStateListener {

    private lateinit var billingClient: BillingClient

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    override val purchaseUpdates: Flow<Purchase> = callbackFlow {
        val listener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases?.forEach { purchase ->
                    trySend(purchase.toPurchase())
                }
            }
        }

        // This will be set when billing client is initialized
        awaitClose { }
    }

    override suspend fun initialize(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            val params = PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(params)
                .build()

            _connectionState.value = BillingConnectionState.CONNECTING
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _connectionState.value = BillingConnectionState.CONNECTED
                        _isInitialized.value = true
                        continuation.resume(Result.success(Unit))
                    } else {
                        _connectionState.value = BillingConnectionState.DISCONNECTED
                        continuation.resume(Result.failure(Exception("Billing setup failed: ${billingResult.debugMessage}")))
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _connectionState.value = BillingConnectionState.DISCONNECTED
                    _isInitialized.value = false
                }
            })
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    override suspend fun getProducts(productIds: List<String>): Result<List<Product>> =
        suspendCancellableCoroutine { continuation ->
            val productList = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val products = productDetailsList.productDetailsList.map { it.toProduct() }
                    continuation.resume(Result.success(products))
                } else {
                    continuation.resume(Result.failure(Exception("Failed to get products: ${billingResult.debugMessage}")))
                }
            }
        }

    override suspend fun launchPurchaseFlow(productId: String): PurchaseResult =
        suspendCancellableCoroutine { continuation ->
            // First get product details
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                    productDetailsList.productDetailsList.isNotEmpty()) {

                    val productDetails = productDetailsList.productDetailsList.first()
                    val purchaseParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .build()
                            )
                        )
                        .build()

                    // Launch billing flow (requires Activity)
                    val activity = context as? Activity
                        ?: throw IllegalStateException("Context must be an Activity for purchase flow")

                    val purchaseResult = billingClient.launchBillingFlow(activity, purchaseParams)

                    when (purchaseResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            // Purchase flow launched successfully
                            // Result will come through PurchasesUpdatedListener
                        }
                        BillingClient.BillingResponseCode.USER_CANCELED -> {
                            continuation.resume(PurchaseResult.Canceled)
                        }
                        else -> {
                            continuation.resume(
                                PurchaseResult.Error(
                                    PurchaseError.Unknown(purchaseResult.debugMessage)
                                )
                            )
                        }
                    }
                } else {
                    continuation.resume(
                        PurchaseResult.Error(
                            PurchaseError.ProductNotFound(productId)
                        )
                    )
                }
            }
        }

    override suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(Exception("Failed to acknowledge purchase: ${billingResult.debugMessage}")))
                }
            }
        }

    override suspend fun consumePurchase(purchaseToken: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            billingClient.consumeAsync(params) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(Exception("Failed to consume purchase: ${billingResult.debugMessage}")))
                }
            }
        }

    override suspend fun restorePurchases(): Result<List<Purchase>> =
        suspendCancellableCoroutine { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val purchases = purchasesList.map { it.toPurchase() }
                    continuation.resume(Result.success(purchases))
                } else {
                    continuation.resume(Result.failure(Exception("Failed to restore purchases: ${billingResult.debugMessage}")))
                }
            }
        }

    override suspend fun getPurchases(): Result<List<Purchase>> = restorePurchases()

    override fun dispose() {
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
        _connectionState.value = BillingConnectionState.CLOSED
        _isInitialized.value = false
    }

    // PurchasesUpdatedListener implementation
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<com.android.billingclient.api.Purchase>?) {
        // This is handled in the purchaseUpdates flow
    }

    // BillingClientStateListener implementation
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        // Handled in initialize()
    }

    override fun onBillingServiceDisconnected() {
        _connectionState.value = BillingConnectionState.DISCONNECTED
        _isInitialized.value = false
    }
}

// Extension functions for mapping
private fun ProductDetails.toProduct(): Product {
    val oneTimePurchaseOffer = oneTimePurchaseOfferDetails
    return Product(
        id = productId,
        title = title,
        description = description,
        price = oneTimePurchaseOffer?.formattedPrice ?: "",
        priceAmountMicros = oneTimePurchaseOffer?.priceAmountMicros ?: 0L,
        priceCurrencyCode = oneTimePurchaseOffer?.priceCurrencyCode ?: "",
        type = ProductType.NON_CONSUMABLE // Adjust based on your needs
    )
}

private fun com.android.billingclient.api.Purchase.toPurchase(): Purchase {
    return Purchase(
        productId = products.first(),
        purchaseToken = purchaseToken,
        orderId = orderId,
        purchaseTime = purchaseTime,
        purchaseState = when (purchaseState) {
            com.android.billingclient.api.Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
            com.android.billingclient.api.Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
            else -> PurchaseState.UNSPECIFIED
        },
        isAcknowledged = isAcknowledged,
        originalJson = originalJson,
        signature = signature
    )
}

