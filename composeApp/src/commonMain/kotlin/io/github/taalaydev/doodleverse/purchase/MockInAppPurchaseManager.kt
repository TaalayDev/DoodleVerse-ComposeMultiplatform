package io.github.taalaydev.doodleverse.purchase

import io.github.taalaydev.doodleverse.purchase.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

class MockInAppPurchaseManager : InAppPurchaseManager {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized = _isInitialized.asStateFlow()

    private val _purchaseUpdates = MutableSharedFlow<Purchase>()
    override val purchaseUpdates = _purchaseUpdates.asSharedFlow()

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    override val connectionState = _connectionState.asStateFlow()

    // Use a coroutine scope for emitting purchase updates
    private val scope = CoroutineScope(Dispatchers.Main)

    // Simulate local purchase storage
    private val _purchases = mutableSetOf<Purchase>()

    // Mock configuration for testing different scenarios
    var simulateErrors: Boolean = false
    var simulateSlowNetwork: Boolean = false
    var failureRate: Float = 0.1f // 10% failure rate by default

    // Single premium product that unlocks everything
    private val mockProducts = listOf(
        Product(
            id = PurchaseRepository.DOODLEVERSE_PREMIUM,
            title = "DoodleVerse Premium",
            description = "Unlock everything: All lessons, brushes, tools, textures, stickers, and future content updates",
            price = "$1.99",
            priceAmountMicros = 9990000,
            priceCurrencyCode = "USD",
            type = ProductType.NON_CONSUMABLE,

        )
    )

    override suspend fun initialize(): Result<Unit> {
        if (simulateSlowNetwork) {
            delay(2000) // Simulate slow initialization
        } else {
            delay(500) // Normal initialization delay
        }

        _connectionState.value = BillingConnectionState.CONNECTING
        delay(200)

        if (simulateErrors && Random.nextFloat() < failureRate) {
            _connectionState.value = BillingConnectionState.DISCONNECTED
            return Result.failure(Exception("Failed to connect to billing service"))
        }

        _connectionState.value = BillingConnectionState.CONNECTED
        _isInitialized.value = true

        return Result.success(Unit)
    }

    override suspend fun getProducts(productIds: List<String>): Result<List<Product>> {
        if (!_isInitialized.value) {
            return Result.failure(Exception("Billing service not initialized"))
        }

        if (simulateSlowNetwork) {
            delay(1500)
        } else {
            delay(300)
        }

        if (simulateErrors && Random.nextFloat() < failureRate) {
            return Result.failure(Exception("Failed to retrieve product information"))
        }

        val filteredProducts = mockProducts.filter { it.id in productIds }
        return Result.success(filteredProducts)
    }

    override suspend fun launchPurchaseFlow(productId: String): PurchaseResult {
        if (!_isInitialized.value) {
            return PurchaseResult.Error(PurchaseError.ServiceDisconnected())
        }

        val product = mockProducts.find { it.id == productId }
            ?: return PurchaseResult.Error(PurchaseError.ProductNotFound(productId))

        // Check if already purchased
        if (_purchases.any { it.productId == productId && it.purchaseState == PurchaseState.PURCHASED }) {
            return PurchaseResult.Error(PurchaseError.ItemAlreadyOwned(productId))
        }

        // Simulate purchase flow delay
        if (simulateSlowNetwork) {
            delay(3000)
        } else {
            delay(1000)
        }

        // Simulate different purchase outcomes
        val outcome = when {
            simulateErrors && Random.nextFloat() < failureRate * 0.5f -> "error"
            Random.nextFloat() < 0.1f -> "canceled" // 10% cancel rate
            Random.nextFloat() < 0.05f -> "pending" // 5% pending rate
            else -> "success"
        }

        return when (outcome) {
            "error" -> {
                val errorTypes = listOf("network", "payment", "unknown")
                when (errorTypes.random()) {
                    "network" -> PurchaseResult.Error(PurchaseError.NetworkError("Network connection failed"))
                    "payment" -> PurchaseResult.Error(PurchaseError.PaymentFailed("Payment method declined"))
                    else -> PurchaseResult.Error(PurchaseError.Unknown("Unknown error occurred"))
                }
            }
            "canceled" -> PurchaseResult.Canceled
            "pending" -> PurchaseResult.Pending
            else -> {
                val purchase = createPurchase(productId, PurchaseState.PURCHASED)

                // Remove any existing purchase for this product first
                _purchases.removeAll { it.productId == productId }
                _purchases.add(purchase)

                // Emit purchase update asynchronously to ensure the flow collectors receive it
                scope.launch {
                    println("MockInAppPurchaseManager: Emitting purchase update for $productId")
                    _purchaseUpdates.emit(purchase)
                }

                PurchaseResult.Success(purchase)
            }
        }
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit> {
        delay(200)

        val purchase = _purchases.find { it.purchaseToken == purchaseToken }
            ?: return Result.failure(Exception("Purchase not found"))

        if (simulateErrors && Random.nextFloat() < failureRate) {
            return Result.failure(Exception("Failed to acknowledge purchase"))
        }

        // Update purchase as acknowledged
        val updatedPurchase = purchase.copy(isAcknowledged = true)
        _purchases.remove(purchase)
        _purchases.add(updatedPurchase)

        // Emit the updated purchase
        scope.launch {
            _purchaseUpdates.emit(updatedPurchase)
        }

        return Result.success(Unit)
    }

    override suspend fun consumePurchase(purchaseToken: String): Result<Unit> {
        delay(200)

        val purchase = _purchases.find { it.purchaseToken == purchaseToken }
            ?: return Result.failure(Exception("Purchase not found"))

        if (simulateErrors && Random.nextFloat() < failureRate) {
            return Result.failure(Exception("Failed to consume purchase"))
        }

        // Premium is non-consumable, so this should not be called
        // But if it is, just return success
        return Result.success(Unit)
    }

    override suspend fun restorePurchases(): Result<List<Purchase>> {
        if (!_isInitialized.value) {
            return Result.failure(Exception("Billing service not initialized"))
        }

        if (simulateSlowNetwork) {
            delay(2000)
        } else {
            delay(800)
        }

        if (simulateErrors && Random.nextFloat() < failureRate) {
            return Result.failure(Exception("Failed to restore purchases"))
        }

        // Emit updates for all existing purchases
        scope.launch {
            _purchases.forEach { purchase ->
                _purchaseUpdates.emit(purchase)
            }
        }

        // Return all purchases (should only be the premium one)
        return Result.success(_purchases.toList())
    }

    override suspend fun getPurchases(): Result<List<Purchase>> {
        if (!_isInitialized.value) {
            return Result.failure(Exception("Billing service not initialized"))
        }

        delay(100)

        if (simulateErrors && Random.nextFloat() < failureRate) {
            return Result.failure(Exception("Failed to retrieve purchases"))
        }

        return Result.success(_purchases.toList())
    }

    override fun dispose() {
        _connectionState.value = BillingConnectionState.CLOSED
        _isInitialized.value = false
    }

    // Helper methods for testing
    fun simulatePremiumPurchase(state: PurchaseState = PurchaseState.PURCHASED) {
        val purchase = createPurchase(PurchaseRepository.DOODLEVERSE_PREMIUM, state)
        _purchases.removeAll { it.productId == PurchaseRepository.DOODLEVERSE_PREMIUM }
        _purchases.add(purchase)
        scope.launch {
            _purchaseUpdates.emit(purchase)
        }
    }

    fun clearPurchases() {
        _purchases.clear()
    }

    fun setPendingPremiumPurchase() {
        val purchase = createPurchase(PurchaseRepository.DOODLEVERSE_PREMIUM, PurchaseState.PENDING)
        _purchases.removeAll { it.productId == PurchaseRepository.DOODLEVERSE_PREMIUM }
        _purchases.add(purchase)
        scope.launch {
            _purchaseUpdates.emit(purchase)
        }
    }

    private fun createPurchase(
        productId: String,
        state: PurchaseState,
        acknowledged: Boolean = false
    ): Purchase {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return Purchase(
            productId = productId,
            purchaseToken = "mock_token_${productId}_${currentTime}",
            orderId = "mock_order_${currentTime}",
            purchaseTime = currentTime,
            purchaseState = state,
            isAcknowledged = acknowledged,
            originalJson = """{"productId":"$productId","purchaseTime":$currentTime}""",
            signature = "mock_signature_${productId}"
        )
    }

    // Methods to configure mock behavior
    fun enableErrorSimulation(enabled: Boolean, failureRate: Float = 0.1f) {
        this.simulateErrors = enabled
        this.failureRate = failureRate
    }

    fun enableSlowNetworkSimulation(enabled: Boolean) {
        this.simulateSlowNetwork = enabled
    }
}