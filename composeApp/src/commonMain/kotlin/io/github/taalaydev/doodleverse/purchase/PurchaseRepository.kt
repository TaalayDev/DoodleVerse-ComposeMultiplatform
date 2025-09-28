package io.github.taalaydev.doodleverse.purchase

import io.github.taalaydev.doodleverse.purchase.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PurchaseRepository(
    val purchaseManager: InAppPurchaseManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _premiumStatus = MutableStateFlow(false)
    val premiumStatus: StateFlow<Boolean> = _premiumStatus.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<Product>>(emptyList())
    val availableProducts: StateFlow<List<Product>> = _availableProducts.asStateFlow()

    private val _purchaseHistory = MutableStateFlow<List<Purchase>>(emptyList())
    val purchaseHistory: StateFlow<List<Purchase>> = _purchaseHistory.asStateFlow()

    companion object {
        // Single premium product that unlocks everything
        const val DOODLEVERSE_PREMIUM = "io.github.taalaydev.doodleverse.premium"

        val ALL_PRODUCT_IDS = listOf(DOODLEVERSE_PREMIUM)

        // Feature sets for easy management
        object Features {
            // Free features available to all users
            val FREE_LESSONS = listOf(1L, 2L, 3L) // First 3 lessons are free
            val FREE_BRUSHES = listOf("basic_pen", "basic_pencil", "basic_marker", "eraser")
            val FREE_TOOLS = listOf("pen", "pencil", "eraser", "color_picker", "zoom", "pan")

            // Limits for free users
            const val FREE_MAX_LAYERS = 5
            const val FREE_MAX_EXPORT_SIZE = 1080 // 1080p max for free users

            // Premium features
            const val PREMIUM_MAX_LAYERS = Int.MAX_VALUE // Unlimited
            const val PREMIUM_MAX_EXPORT_SIZE = 4320 // 4K export for premium
        }
    }

    init {
        // Listen to purchase updates with better error handling
        scope.launch {
            purchaseManager.purchaseUpdates
                .catch { exception ->
                    println("PurchaseRepository: Error in purchaseUpdates flow: ${exception.message}")
                }
                .collect { purchase ->
                    println("PurchaseRepository: Received purchase update: ${purchase.productId}, state: ${purchase.purchaseState}")
                    updatePurchaseStatus(purchase)
                }
        }
    }

    suspend fun initialize(): Result<Unit> {
        return try {
            purchaseManager.initialize()
                .onSuccess {
                    println("PurchaseRepository: Purchase manager initialized successfully")
                    loadProducts()
                    loadPurchaseHistory()
                }
                .onFailure { error ->
                    println("PurchaseRepository: Failed to initialize purchase manager: ${error.message}")
                }
        } catch (e: Exception) {
            println("PurchaseRepository: Exception during initialization: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun loadProducts() {
        purchaseManager.getProducts(ALL_PRODUCT_IDS)
            .onSuccess { products ->
                println("PurchaseRepository: Loaded ${products.size} products")
                _availableProducts.value = products
            }
            .onFailure { error ->
                println("PurchaseRepository: Failed to load products: ${error.message}")
            }
    }

    private suspend fun loadPurchaseHistory() {
        purchaseManager.getPurchases()
            .onSuccess { purchases ->
                println("PurchaseRepository: Loaded ${purchases.size} purchases from history")
                _purchaseHistory.value = purchases
                updatePremiumStatus(purchases)
            }
            .onFailure { error ->
                println("PurchaseRepository: Failed to load purchase history: ${error.message}")
            }
    }

    suspend fun purchaseProduct(productId: String): PurchaseResult {
        return purchaseManager.launchPurchaseFlow(productId)
    }

    suspend fun restorePurchases(): Result<List<Purchase>> {
        return purchaseManager.restorePurchases()
            .onSuccess { purchases ->
                println("PurchaseRepository: Restored ${purchases.size} purchases")
                _purchaseHistory.value = purchases
                updatePremiumStatus(purchases)
            }
            .onFailure { error ->
                println("PurchaseRepository: Failed to restore purchases: ${error.message}")
            }
    }

    private fun updatePurchaseStatus(purchase: Purchase) {
        println("PurchaseRepository: Updating purchase status for ${purchase.productId}")

        val currentHistory = _purchaseHistory.value.toMutableList()
        val existingIndex = currentHistory.indexOfFirst { it.productId == purchase.productId }

        if (existingIndex >= 0) {
            println("PurchaseRepository: Updating existing purchase")
            currentHistory[existingIndex] = purchase
        } else {
            println("PurchaseRepository: Adding new purchase")
            currentHistory.add(purchase)
        }

        _purchaseHistory.value = currentHistory
        updatePremiumStatus(currentHistory)
    }

    private fun updatePremiumStatus(purchases: List<Purchase>) {
        val hasPremium = purchases.any { purchase ->
            val isPremiumProduct = purchase.productId == DOODLEVERSE_PREMIUM
            val isPurchased = purchase.purchaseState == PurchaseState.PURCHASED

            println("PurchaseRepository: Checking purchase ${purchase.productId}: isPremiumProduct=$isPremiumProduct, isPurchased=$isPurchased")

            isPremiumProduct && isPurchased
        }

        val previousStatus = _premiumStatus.value
        _premiumStatus.value = hasPremium

        println("PurchaseRepository: Premium status changed from $previousStatus to $hasPremium")

        if (previousStatus != hasPremium) {
            println("PurchaseRepository: Premium status update emitted: $hasPremium")
        }
    }

    // Convenience methods
    fun hasPremium(): Boolean {
        return _premiumStatus.value
    }

    fun getProduct(productId: String): Product? {
        return _availableProducts.value.find { it.id == productId }
    }

    fun getPremiumProduct(): Product? {
        return getProduct(DOODLEVERSE_PREMIUM)
    }

    // Feature access methods
    fun canAccessLesson(lessonId: Long): Boolean {
        return hasPremium() || lessonId in Features.FREE_LESSONS
    }

    fun canAccessBrush(brushId: String): Boolean {
        return hasPremium() || brushId in Features.FREE_BRUSHES
    }

    fun canAccessTool(toolId: String): Boolean {
        return hasPremium() || toolId in Features.FREE_TOOLS
    }

    fun getMaxLayers(): Int {
        return if (hasPremium()) {
            Features.PREMIUM_MAX_LAYERS
        } else {
            Features.FREE_MAX_LAYERS
        }
    }

    fun getMaxExportSize(): Int {
        return if (hasPremium()) {
            Features.PREMIUM_MAX_EXPORT_SIZE
        } else {
            Features.FREE_MAX_EXPORT_SIZE
        }
    }

    fun canExportHD(): Boolean {
        return hasPremium()
    }

    fun canUseUnlimitedLayers(): Boolean {
        return hasPremium()
    }

    fun canAccessTextures(): Boolean {
        // Coming soon feature - only for premium when released
        return hasPremium() // && texturesFeatureEnabled
    }

    fun canAccessStickers(): Boolean {
        // Coming soon feature - only for premium when released
        return hasPremium() // && stickersFeatureEnabled
    }

    // Get premium purchase info
    fun getPremiumPurchase(): Purchase? {
        return _purchaseHistory.value.find {
            it.productId == DOODLEVERSE_PREMIUM &&
                    it.purchaseState == PurchaseState.PURCHASED
        }
    }

    fun getPremiumPurchaseDate(): Long? {
        return getPremiumPurchase()?.purchaseTime
    }

    // Analytics and monitoring
    fun logFeatureAccess(feature: String, hasAccess: Boolean) {
        // You can integrate analytics here
        scope.launch {
            // Analytics.logEvent("feature_access", mapOf(
            //     "feature" to feature,
            //     "has_access" to hasAccess,
            //     "is_premium" to hasPremium()
            // ))
        }
    }
}