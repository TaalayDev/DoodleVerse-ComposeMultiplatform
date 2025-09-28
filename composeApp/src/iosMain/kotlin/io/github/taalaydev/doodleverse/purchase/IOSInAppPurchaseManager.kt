package io.github.taalaydev.doodleverse.purchase

import io.github.taalaydev.doodleverse.purchase.models.Product
import io.github.taalaydev.doodleverse.purchase.models.ProductType
import io.github.taalaydev.doodleverse.purchase.models.Purchase
import io.github.taalaydev.doodleverse.purchase.models.PurchaseError
import io.github.taalaydev.doodleverse.purchase.models.PurchaseResult
import io.github.taalaydev.doodleverse.purchase.models.PurchaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterBehavior10_4
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSSet
import platform.Foundation.currencyCode
import platform.Foundation.setWithArray
import platform.Foundation.timeIntervalSince1970
import platform.StoreKit.SKErrorCode
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKPaymentTransaction
import platform.StoreKit.SKPaymentTransactionObserverProtocol
import platform.StoreKit.SKPaymentTransactionState
import platform.StoreKit.SKProduct
import platform.StoreKit.SKProductsRequest
import platform.StoreKit.SKProductsRequestDelegateProtocol
import platform.StoreKit.SKProductsResponse
import platform.StoreKit.SKRequest
import platform.darwin.NSObject
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.filterIsInstance
import kotlin.collections.find
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.coroutines.resume

class IOSInAppPurchaseManager : InAppPurchaseManager {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private var productsRequest: SKProductsRequest? = null
    private var availableProducts: List<SKProduct> = emptyList()
    private var transactionObserver: TransactionObserver? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _purchaseUpdates = MutableSharedFlow<Purchase>()
    override val purchaseUpdates: Flow<Purchase> = _purchaseUpdates.asSharedFlow()

    // Pending purchase continuations
    private val pendingPurchases = mutableMapOf<String, kotlin.coroutines.Continuation<PurchaseResult>>()
    private val pendingRestores = mutableListOf<kotlin.coroutines.Continuation<Result<List<Purchase>>>>()

    override suspend fun initialize(): Result<Unit> {
        return try {
            _connectionState.value = BillingConnectionState.CONNECTING

            // Check if payments are allowed
            if (!SKPaymentQueue.canMakePayments()) {
                _connectionState.value = BillingConnectionState.DISCONNECTED
                return Result.failure(Exception("In-app purchases are not allowed on this device"))
            }

            // Create and add transaction observer
            transactionObserver = TransactionObserver { transactions ->
                handleTransactionUpdates(transactions)
            }

            SKPaymentQueue.defaultQueue().addTransactionObserver(transactionObserver!!)

            _connectionState.value = BillingConnectionState.CONNECTED
            _isInitialized.value = true

            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = BillingConnectionState.DISCONNECTED
            Result.failure(e)
        }
    }

    override suspend fun getProducts(productIds: List<String>): Result<List<Product>> =
        suspendCancellableCoroutine { continuation ->
            val productIdentifiers = NSSet.setWithArray(productIds)

            val delegate = ProductRequestDelegate(
                onSuccess = { products ->
                    println("Received products: ${products.map { it.productIdentifier }}")
                    availableProducts = products
                    val mappedProducts = products.map { it.toProduct() }
                    continuation.resume(Result.success(mappedProducts))
                },
                onFailure = { error ->
                    continuation.resume(Result.failure(Exception("Failed to get products: ${error.localizedDescription}")))
                }
            )

            productsRequest = SKProductsRequest(productIdentifiers).apply {
                setDelegate(delegate)
                start()
            }

            continuation.invokeOnCancellation {
                productsRequest?.cancel()
            }
        }

    override suspend fun launchPurchaseFlow(productId: String): PurchaseResult =
        suspendCancellableCoroutine { continuation ->
            val product = availableProducts.find { it.productIdentifier == productId }

            if (product == null) {
                continuation.resume(PurchaseResult.Error(PurchaseError.ProductNotFound(productId)))
                return@suspendCancellableCoroutine
            }

            pendingPurchases[productId] = continuation

            val payment = SKPayment.paymentWithProduct(product)
            SKPaymentQueue.defaultQueue().addPayment(payment)

            continuation.invokeOnCancellation {
                pendingPurchases.remove(productId)
            }
        }

    override suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit> {
        // iOS automatically acknowledges purchases when you call finishTransaction
        return Result.success(Unit)
    }

    override suspend fun consumePurchase(purchaseToken: String): Result<Unit> {
        // iOS doesn't have a concept of consuming purchases like Android
        // For consumable products, you manage state in your app
        return Result.success(Unit)
    }

    override suspend fun restorePurchases(): Result<List<Purchase>> =
        suspendCancellableCoroutine { continuation ->
            pendingRestores.add(continuation)
            SKPaymentQueue.defaultQueue().restoreCompletedTransactions()

            continuation.invokeOnCancellation {
                pendingRestores.remove(continuation)
            }
        }

    override suspend fun getPurchases(): Result<List<Purchase>> {
        // iOS doesn't provide a direct way to get current purchases
        // You typically need to store them locally or restore them
        return restorePurchases()
    }

    override fun dispose() {
        transactionObserver?.let { observer ->
            SKPaymentQueue.defaultQueue().removeTransactionObserver(observer)
        }
        transactionObserver = null
        productsRequest?.cancel()
        productsRequest = null
        _connectionState.value = BillingConnectionState.CLOSED
        _isInitialized.value = false

        // Cancel pending operations
        pendingPurchases.values.forEach { continuation ->
            continuation.resume(PurchaseResult.Error(PurchaseError.ServiceDisconnected()))
        }
        pendingPurchases.clear()

        pendingRestores.forEach { continuation ->
            continuation.resume(Result.failure(Exception("Service disconnected")))
        }
        pendingRestores.clear()
    }

    private fun handleTransactionUpdates(transactions: List<SKPaymentTransaction>) {
        val restoredPurchases = mutableListOf<Purchase>()

        transactions.forEach { transaction ->
            when (transaction.transactionState) {
                SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
                    val purchase = transaction.toPurchase()
                    scope.launch {
                        _purchaseUpdates.emit(purchase)
                    }

                    // Complete pending purchase
                    val productId = transaction.payment.productIdentifier
                    pendingPurchases.remove(productId)?.resume(PurchaseResult.Success(purchase))

                    SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                }

                SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                    val productId = transaction.payment.productIdentifier
                    val error = transaction.error

                    val purchaseResult = when (error?.code) {
                        SKErrorCode.SKErrorPaymentCancelled.value -> PurchaseResult.Canceled
                        else -> PurchaseResult.Error(
                            PurchaseError.PaymentFailed(error?.localizedDescription ?: "Payment failed")
                        )
                    }

                    pendingPurchases.remove(productId)?.resume(purchaseResult)
                    SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                }

                SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
                    val purchase = transaction.toPurchase()
                    restoredPurchases.add(purchase)

                    scope.launch {
                        _purchaseUpdates.emit(purchase)
                    }

                    SKPaymentQueue.defaultQueue().finishTransaction(transaction)
                }

                SKPaymentTransactionState.SKPaymentTransactionStatePurchasing -> {
                    // Transaction is in progress, do nothing
                }

                SKPaymentTransactionState.SKPaymentTransactionStateDeferred -> {
                    val productId = transaction.payment.productIdentifier
                    pendingPurchases.remove(productId)?.resume(PurchaseResult.Pending)
                }

                else -> {
                    // Handle any other states if necessary
                }
            }
        }

        // Complete restore operations if we have any pending
        if (restoredPurchases.isNotEmpty() || pendingRestores.isNotEmpty()) {
            pendingRestores.forEach { continuation ->
                continuation.resume(Result.success(restoredPurchases))
            }
            pendingRestores.clear()
        }
    }
}

// Separate classes for handling delegates to avoid protocol implementation issues
private class TransactionObserver(
    private val onTransactionUpdate: (List<SKPaymentTransaction>) -> Unit
) : NSObject(), SKPaymentTransactionObserverProtocol {

    override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
        val transactions = updatedTransactions.filterIsInstance<SKPaymentTransaction>()
        onTransactionUpdate(transactions)
    }

    override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
        // Restore completed successfully
    }

    override fun paymentQueue(queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError: NSError) {
        // Handle restore failure if needed
    }
}

private class ProductRequestDelegate(
    private val onSuccess: (List<SKProduct>) -> Unit,
    private val onFailure: (NSError) -> Unit
) : NSObject(), SKProductsRequestDelegateProtocol {

    override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
        val products = didReceiveResponse.products.map { it as SKProduct }
        onSuccess(products)
    }

    override fun request(request: SKRequest, didFailWithError: NSError) {
        onFailure(didFailWithError)
    }
}

// Extension functions for mapping
private fun SKProduct.toProduct(): Product {
    val formatter = NSNumberFormatter().apply {
        formatterBehavior = NSNumberFormatterBehavior10_4
        numberStyle = NSNumberFormatterCurrencyStyle
        locale = priceLocale
    }

    return Product(
        id = productIdentifier,
        title = localizedTitle,
        description = localizedDescription,
        price = formatter.stringFromNumber(price) ?: "$${price}",
        priceAmountMicros = (price.doubleValue * 1_000_000).toLong(),
        priceCurrencyCode = priceLocale.currencyCode ?: "USD",
        type = ProductType.NON_CONSUMABLE // You might want to determine this based on your product configuration
    )
}

private fun SKPaymentTransaction.toPurchase(): Purchase {
    return Purchase(
        productId = payment.productIdentifier,
        purchaseToken = transactionIdentifier ?: "",
        orderId = transactionIdentifier,
        purchaseTime = (transactionDate?.timeIntervalSince1970?.toLong() ?: 0L) * 1000,
        purchaseState = when (transactionState) {
            SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> PurchaseState.PURCHASED
            SKPaymentTransactionState.SKPaymentTransactionStateFailed -> PurchaseState.FAILED
            SKPaymentTransactionState.SKPaymentTransactionStateRestored -> PurchaseState.PURCHASED
            SKPaymentTransactionState.SKPaymentTransactionStatePurchasing -> PurchaseState.PENDING
            SKPaymentTransactionState.SKPaymentTransactionStateDeferred -> PurchaseState.DEFERRED
            else -> PurchaseState.UNSPECIFIED
        },
        isAcknowledged = transactionState == SKPaymentTransactionState.SKPaymentTransactionStatePurchased ||
                transactionState == SKPaymentTransactionState.SKPaymentTransactionStateRestored
    )
}