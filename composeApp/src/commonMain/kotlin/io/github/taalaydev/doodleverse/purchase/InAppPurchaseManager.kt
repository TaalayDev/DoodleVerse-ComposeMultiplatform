package io.github.taalaydev.doodleverse.purchase

import io.github.taalaydev.doodleverse.purchase.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InAppPurchaseManager {
    val isInitialized: StateFlow<Boolean>
    val purchaseUpdates: Flow<Purchase>
    val connectionState: StateFlow<BillingConnectionState>

    suspend fun initialize(): Result<Unit>
    suspend fun getProducts(productIds: List<String>): Result<List<Product>>
    suspend fun launchPurchaseFlow(productId: String): PurchaseResult
    suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit>
    suspend fun consumePurchase(purchaseToken: String): Result<Unit>
    suspend fun restorePurchases(): Result<List<Purchase>>
    suspend fun getPurchases(): Result<List<Purchase>>
    fun dispose()
}

enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CLOSED
}