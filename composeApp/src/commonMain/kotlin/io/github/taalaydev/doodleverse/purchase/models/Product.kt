package io.github.taalaydev.doodleverse.purchase.models

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val type: ProductType
)

enum class ProductType {
    CONSUMABLE,
    NON_CONSUMABLE,
    SUBSCRIPTION
}

data class Purchase(
    val productId: String,
    val purchaseToken: String,
    val orderId: String?,
    val purchaseTime: Long,
    val purchaseState: PurchaseState,
    val isAcknowledged: Boolean = false,
    val originalJson: String? = null,
    val signature: String? = null
)

enum class PurchaseState {
    UNSPECIFIED,
    PURCHASED,
    PENDING,
    FAILED,
    CANCELED,
    DEFERRED
}

sealed class PurchaseResult {
    data class Success(val purchase: Purchase) : PurchaseResult()
    data class Error(val error: PurchaseError) : PurchaseResult()
    object Canceled : PurchaseResult()
    object Pending : PurchaseResult()
}

sealed class PurchaseError(val message: String, val code: String? = null) {
    class NetworkError(message: String) : PurchaseError(message, "NETWORK_ERROR")
    class ProductNotFound(productId: String) : PurchaseError("Product not found: $productId", "PRODUCT_NOT_FOUND")
    class UserCanceled : PurchaseError("User canceled purchase", "USER_CANCELED")
    class PaymentFailed(message: String) : PurchaseError(message, "PAYMENT_FAILED")
    class ItemAlreadyOwned(productId: String) : PurchaseError("Item already owned: $productId", "ITEM_ALREADY_OWNED")
    class ServiceDisconnected : PurchaseError("Billing service disconnected", "SERVICE_DISCONNECTED")
    class Unknown(message: String, code: String? = null) : PurchaseError(message, code)
}