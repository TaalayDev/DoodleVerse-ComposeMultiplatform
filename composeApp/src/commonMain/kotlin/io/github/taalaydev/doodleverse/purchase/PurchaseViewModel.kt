package io.github.taalaydev.doodleverse.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taalaydev.doodleverse.purchase.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class PurchaseUiState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val availableProducts: List<Product> = emptyList(),
    val purchaseHistory: List<Purchase> = emptyList(),
    val premiumStatus: PremiumStatus = PremiumStatus(),
    val purchaseInProgress: String? = null,
    val lastPurchaseResult: PurchaseResult? = null,
    val errorMessage: String? = null,
    val isRestoring: Boolean = false,
    val connectionState: BillingConnectionState = BillingConnectionState.DISCONNECTED
)

data class PremiumStatus(
    val isPremium: Boolean = false,
    val hasAllLessons: Boolean = false,
    val hasAllBrushes: Boolean = false,
    val hasAllTools: Boolean = false,
    val hasTextures: Boolean = false, // Coming soon
    val hasStickers: Boolean = false, // Coming soon
    val purchaseDate: Long? = null
)

sealed class PurchaseUiEvent {
    object Initialize : PurchaseUiEvent()
    data class PurchaseProduct(val productId: String) : PurchaseUiEvent()
    object RestorePurchases : PurchaseUiEvent()
    object ClearError : PurchaseUiEvent()
    object ClearPurchaseResult : PurchaseUiEvent()
    data class ValidatePurchase(val purchase: Purchase) : PurchaseUiEvent()
    object RefreshProducts : PurchaseUiEvent()
}

class PurchaseViewModel(
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    // Convenience properties for UI
    val isLoading: StateFlow<Boolean> = _uiState.map { it.isLoading }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val premiumStatus: StateFlow<PremiumStatus> = _uiState.map { it.premiumStatus }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PremiumStatus()
    )

    val availableProducts: StateFlow<List<Product>> = _uiState.map { it.availableProducts }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Observe repository changes
        observeRepositoryChanges()

        // Auto-initialize
        handleEvent(PurchaseUiEvent.Initialize)
    }

    fun handleEvent(event: PurchaseUiEvent) {
        when (event) {
            is PurchaseUiEvent.Initialize -> initialize()
            is PurchaseUiEvent.PurchaseProduct -> purchaseProduct(event.productId)
            is PurchaseUiEvent.RestorePurchases -> restorePurchases()
            is PurchaseUiEvent.ClearError -> clearError()
            is PurchaseUiEvent.ClearPurchaseResult -> clearPurchaseResult()
            is PurchaseUiEvent.ValidatePurchase -> validatePurchase(event.purchase)
            is PurchaseUiEvent.RefreshProducts -> refreshProducts()
        }
    }

    private fun observeRepositoryChanges() {
        viewModelScope.launch {
            // Observe available products
            purchaseRepository.availableProducts.collect { products ->
                _uiState.value = _uiState.value.copy(
                    availableProducts = products
                )
            }
        }

        viewModelScope.launch {
            // Observe purchase history
            purchaseRepository.purchaseHistory.collect { purchases ->
                _uiState.value = _uiState.value.copy(
                    purchaseHistory = purchases,
                    premiumStatus = calculatePremiumStatus(purchases)
                )
            }
        }

        viewModelScope.launch {
            // Observe premium status from repository
            purchaseRepository.premiumStatus.collect { isPremium ->
                println("Premium status updated: $isPremium")
                val currentPurchases = _uiState.value.purchaseHistory
                _uiState.value = _uiState.value.copy(
                    premiumStatus = calculatePremiumStatus(currentPurchases)
                )
            }
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                purchaseRepository.initialize()
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isInitialized = true,
                            isLoading = false,
                            connectionState = BillingConnectionState.CONNECTED
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to initialize purchases: ${error.message}",
                            connectionState = BillingConnectionState.DISCONNECTED
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Unexpected error: ${e.message}",
                    connectionState = BillingConnectionState.DISCONNECTED
                )
            }
        }
    }

    private fun purchaseProduct(productId: String) {
        viewModelScope.launch {
            // Check if already purchased
            if (isProductAlreadyPurchased(productId)) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "You already have DoodleVerse Premium!"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                purchaseInProgress = productId,
                errorMessage = null,
                lastPurchaseResult = null
            )

            try {
                val result = purchaseRepository.purchaseProduct(productId)

                _uiState.value = _uiState.value.copy(
                    purchaseInProgress = null,
                    lastPurchaseResult = result
                )

                when (result) {
                    is PurchaseResult.Success -> {
                        handleSuccessfulPurchase(result.purchase)
                    }
                    is PurchaseResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.error.message
                        )
                    }
                    is PurchaseResult.Canceled -> {
                        // User canceled, no action needed
                    }
                    is PurchaseResult.Pending -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Purchase is pending approval. You'll get premium access once approved."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    purchaseInProgress = null,
                    errorMessage = "Purchase failed: ${e.message}"
                )
            }
        }
    }

    private fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRestoring = true,
                errorMessage = null
            )

            try {
                purchaseRepository.restorePurchases()
                    .onSuccess { restoredPurchases ->
                        _uiState.value = _uiState.value.copy(
                            isRestoring = false,
                            lastPurchaseResult = null
                        )

                        if (restoredPurchases.isEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "No previous purchases found to restore"
                            )
                        } else {
                            val hasPremium = restoredPurchases.any {
                                it.productId == PurchaseRepository.DOODLEVERSE_PREMIUM &&
                                        it.purchaseState == PurchaseState.PURCHASED
                            }

                            val message = if (hasPremium) {
                                "✨ DoodleVerse Premium restored successfully!"
                            } else {
                                "Purchases restored, but no premium subscription found"
                            }

                            _uiState.value = _uiState.value.copy(
                                errorMessage = message
                            )

                            // Clear message after delay
                            delay(4000)
                            if (_uiState.value.errorMessage == message) {
                                _uiState.value = _uiState.value.copy(errorMessage = null)
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isRestoring = false,
                            errorMessage = "Failed to restore purchases: ${error.message}"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    errorMessage = "Restore failed: ${e.message}"
                )
            }
        }
    }

    private fun refreshProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Re-initialize to refresh products
            initialize()
        }
    }

    private fun validatePurchase(purchase: Purchase) {
        viewModelScope.launch {
            // Here you could add server-side validation
            // For now, we'll just acknowledge the purchase
            try {
                purchaseRepository.purchaseManager.acknowledgePurchase(purchase.purchaseToken)

                // Premium is non-consumable, so don't consume it
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to validate purchase: ${e.message}"
                )
            }
        }
    }

    private fun handleSuccessfulPurchase(purchase: Purchase) {
        viewModelScope.launch {
            // Validate and acknowledge the purchase
            validatePurchase(purchase)

            // Update UI to reflect successful purchase
            val successMessage = when (purchase.productId) {
                PurchaseRepository.DOODLEVERSE_PREMIUM -> "🎉 Welcome to DoodleVerse Premium! All features are now unlocked!"
                else -> "Purchase successful!"
            }

            _uiState.value = _uiState.value.copy(
                errorMessage = successMessage
            )

            // Clear success message after delay
            delay(5000)
            if (_uiState.value.errorMessage == successMessage) {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }
        }
    }

    private fun calculatePremiumStatus(purchases: List<Purchase>): PremiumStatus {
        val premiumPurchase = purchases.find {
            it.productId == PurchaseRepository.DOODLEVERSE_PREMIUM &&
                    it.purchaseState == PurchaseState.PURCHASED
        }

        println("Calculating premium status from purchases: $purchases")

        val isPremium = premiumPurchase != null

        return PremiumStatus(
            isPremium = isPremium,
            hasAllLessons = isPremium, // All lessons unlocked with premium
            hasAllBrushes = isPremium, // All brushes unlocked with premium
            hasAllTools = isPremium,   // All tools unlocked with premium
            hasTextures = isPremium,   // Will be available when feature is released
            hasStickers = isPremium,   // Will be available when feature is released
            purchaseDate = premiumPurchase?.purchaseTime
        )
    }

    private fun isProductAlreadyPurchased(productId: String): Boolean {
        return when (productId) {
            PurchaseRepository.DOODLEVERSE_PREMIUM -> _uiState.value.premiumStatus.isPremium
            else -> false
        }
    }

    private fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun clearPurchaseResult() {
        _uiState.value = _uiState.value.copy(lastPurchaseResult = null)
    }

    // Convenience methods for UI
    fun isPremiumUser(): Boolean {
        return _uiState.value.premiumStatus.isPremium
    }

    fun getProduct(productId: String): Product? {
        return _uiState.value.availableProducts.find { it.id == productId }
    }

    fun getPremiumProduct(): Product? {
        return getProduct(PurchaseRepository.DOODLEVERSE_PREMIUM)
    }

    fun canMakePurchases(): Boolean {
        return _uiState.value.isInitialized &&
                _uiState.value.connectionState == BillingConnectionState.CONNECTED
    }

    fun isPurchaseInProgress(productId: String? = null): Boolean {
        return if (productId != null) {
            _uiState.value.purchaseInProgress == productId
        } else {
            _uiState.value.purchaseInProgress != null
        }
    }

    // Feature access methods - now everything is tied to single premium status
    fun hasFeatureAccess(feature: PremiumFeature): Boolean {
        val status = _uiState.value.premiumStatus
        return when (feature) {
            PremiumFeature.ALL_LESSONS -> status.hasAllLessons
            PremiumFeature.ALL_BRUSHES -> status.hasAllBrushes
            PremiumFeature.ALL_TOOLS -> status.hasAllTools
            PremiumFeature.TEXTURES -> status.hasTextures     // Coming soon
            PremiumFeature.STICKERS -> status.hasStickers     // Coming soon
            PremiumFeature.ALL_FEATURES -> status.isPremium
        }
    }

    // Helper methods for specific features
    fun canAccessLesson(lessonId: Long): Boolean {
        // You can implement logic here for free vs premium lessons
        // For example, first 3 lessons are free, rest require premium
        return if (lessonId <= 3) {
            true // Free lessons
        } else {
            hasFeatureAccess(PremiumFeature.ALL_LESSONS)
        }
    }

    fun canAccessBrush(brushId: String): Boolean {
        // You can implement logic here for free vs premium brushes
        return if (isFreeBrush(brushId)) {
            true
        } else {
            hasFeatureAccess(PremiumFeature.ALL_BRUSHES)
        }
    }

    fun canAccessTool(toolId: String): Boolean {
        // Basic tools are free, advanced tools require premium
        return if (isBasicTool(toolId)) {
            true
        } else {
            hasFeatureAccess(PremiumFeature.ALL_TOOLS)
        }
    }

    fun canExportHD(): Boolean {
        return hasFeatureAccess(PremiumFeature.ALL_TOOLS)
    }

    fun canUseUnlimitedLayers(): Boolean {
        return hasFeatureAccess(PremiumFeature.ALL_TOOLS)
    }

    fun getMaxLayers(): Int {
        return if (hasFeatureAccess(PremiumFeature.ALL_TOOLS)) {
            Int.MAX_VALUE // Unlimited
        } else {
            5 // Free users get 5 layers
        }
    }

    // Helper methods to determine free vs premium content
    private fun isFreeBrush(brushId: String): Boolean {
        // Define which brushes are free
        val freeBrushes = listOf("basic_pen", "basic_pencil", "basic_marker", "eraser")
        return brushId in freeBrushes
    }

    private fun isBasicTool(toolId: String): Boolean {
        // Define which tools are basic/free
        val basicTools = listOf("pen", "pencil", "eraser", "color_picker", "zoom", "pan")
        return toolId in basicTools
    }

    // Analytics integration
    fun logFeatureBlocked(feature: PremiumFeature) {
        // You can integrate with your analytics here
        // PurchaseAnalytics.logFeatureBlocked(analytics, feature.name)
    }

    fun logPremiumFeatureUsed(feature: PremiumFeature) {
        // You can integrate with your analytics here
        // PurchaseAnalytics.logPremiumFeatureUsed(analytics, feature.name)
    }
}

enum class PremiumFeature {
    ALL_LESSONS,    // Access to all drawing lessons
    ALL_BRUSHES,    // Access to all premium brushes
    ALL_TOOLS,      // Access to advanced tools, unlimited layers, HD export
    TEXTURES,       // Premium textures (coming soon)
    STICKERS,       // Sticker library (coming soon)
    ALL_FEATURES    // Everything
}

// Extension functions for easier usage
val PurchaseViewModel.hasPremium: Boolean
    get() = isPremiumUser()

fun PurchaseViewModel.requirePremium(
    feature: PremiumFeature,
    onPremiumRequired: () -> Unit,
    onAccessGranted: () -> Unit
) {
    if (hasFeatureAccess(feature)) {
        onAccessGranted()
    } else {
        logFeatureBlocked(feature)
        onPremiumRequired()
    }
}