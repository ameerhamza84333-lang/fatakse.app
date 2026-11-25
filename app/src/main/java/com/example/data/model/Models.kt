package com.example.data.model

import kotlin.math.roundToInt

enum class UserRole(val dbKey: String, val label: String, val subtitle: String) {
    CUSTOMER("customer", "Customer", "Order food, chai, sweets & groceries"),
    VENDOR("vendor", "Vendor", "Manage shop orders, menu & payouts"),
    DELIVERY("delivery", "Delivery", "Accept pickups, navigate & deliver"),
    ADMIN("admin", "Admin", "Monitor GMV, dispatch riders, shops & fees")
}

enum class ShopCategory(val dbKey: String, val label: String) {
    ALL("all", "All"),
    BIRYANI("biryani", "Biryani"),
    MOMOS_FAST_FOOD("momos_fast_food", "Momos & Fast Food"),
    CHAI_COFFEE("chai_coffee", "Chai & Coffee"),
    COLD_DRINKS_SHAKES("cold_drinks_shakes", "Cold Drinks & Shakes"),
    SWEETS_BAKERY("sweets_bakery", "Sweets & Bakery"),
    GROCERY_KIRANA("grocery_kirana", "Grocery & Kirana"),
    DAIRY_FRUITS("dairy_fruits", "Dairy & Fruits");

    val defaultCommissionRate: Double
        get() = when (this) {
            GROCERY_KIRANA, DAIRY_FRUITS -> 10.0
            else -> 15.0
        }

    companion object {
        val selectableCategories: List<ShopCategory>
            get() = entries.filter { it != ALL }

        fun fromKey(key: String): ShopCategory = when (key.trim().lowercase()) {
            "all" -> ALL
            "biryani", "food" -> BIRYANI
            "momos_fast_food", "fast_food", "momos" -> MOMOS_FAST_FOOD
            "chai_coffee", "tea_coffee" -> CHAI_COFFEE
            "cold_drinks_shakes", "cold_drinks", "shakes", "drinks" -> COLD_DRINKS_SHAKES
            "sweets_bakery", "sweets", "bakery" -> SWEETS_BAKERY
            "grocery_kirana", "grocery", "kirana" -> GROCERY_KIRANA
            "dairy_fruits", "dairy", "fruits" -> DAIRY_FRUITS
            else -> entries.firstOrNull { it.dbKey == key } ?: BIRYANI
        }
    }
}

enum class OrderStatus(val dbKey: String, val label: String, val stepIndex: Int) {
    PENDING("pending", "Order Placed", 0),
    ACCEPTED("accepted", "Accepted by Shop", 1),
    PREPARING("preparing", "Preparing", 2),
    OUT_FOR_DELIVERY("out_for_delivery", "Out for Delivery", 3),
    DELIVERED("delivered", "Delivered", 4),
    CANCELLED("cancelled", "Cancelled", -1);

    companion object {
        fun fromKey(key: String): OrderStatus =
            entries.firstOrNull { it.dbKey == key } ?: PENDING
    }
}

enum class PaymentMethod(
    val dbKey: String,
    val label: String,
    val shortLabel: String,
    val description: String
) {
    CASH_UPI_QR_ON_DELIVERY(
        dbKey = "CASH_UPI_QR",
        label = "Cash / UPI QR on Delivery",
        shortLabel = "Cash / UPI QR",
        description = "Pay via Cash or scan UPI QR code when your order arrives"
    );

    companion object {
        fun fromKey(key: String): PaymentMethod = CASH_UPI_QR_ON_DELIVERY
    }
}

data class OrderFinancialSplit(
    val commissionRatePercent: Double,
    val platformCommission: Int,
    val vendorNetPayout: Int,
    val riderDeliveryFee: Int
)

fun calculateOrderSplit(
    itemsTotal: Int,
    category: ShopCategory,
    deliveryFeeCharged: Int,
    baseRiderDeliveryFee: Int
): OrderFinancialSplit {
    val rate = category.defaultCommissionRate
    val commission = (itemsTotal * (rate / 100.0)).roundToInt()
    val vendorNet = (itemsTotal - commission).coerceAtLeast(0)
    val riderFee = if (deliveryFeeCharged > 0) deliveryFeeCharged else baseRiderDeliveryFee
    return OrderFinancialSplit(
        commissionRatePercent = rate,
        platformCommission = commission,
        vendorNetPayout = vendorNet,
        riderDeliveryFee = riderFee
    )
}

enum class PromoDiscountType(val dbKey: String, val label: String) {
    FLAT("flat", "Flat ₹ Off"),
    PERCENT("percent", "% Percentage Off"),
    FREE_DELIVERY("free_delivery", "Free Delivery");

    companion object {
        fun fromKey(key: String): PromoDiscountType =
            entries.firstOrNull { it.dbKey == key } ?: FLAT
    }
}

data class PromoCode(
    val code: String,
    val title: String,
    val subtitle: String,
    val discountType: PromoDiscountType,
    val discountValue: Int,
    val maxDiscount: Int,
    val minOrderAmount: Int,
    val isActive: Boolean
) {
    fun computeDiscount(itemsTotal: Int): Int {
        if (!isActive || itemsTotal < minOrderAmount) return 0
        return when (discountType) {
            PromoDiscountType.FLAT -> discountValue.coerceAtMost(itemsTotal)
            PromoDiscountType.PERCENT -> ((itemsTotal * discountValue) / 100)
                .coerceAtMost(maxDiscount)
                .coerceAtMost(itemsTotal)
            PromoDiscountType.FREE_DELIVERY -> 0
        }
    }

    fun unlocksFreeDelivery(itemsTotal: Int): Boolean {
        return isActive && discountType == PromoDiscountType.FREE_DELIVERY && itemsTotal >= minOrderAmount
    }
}

data class Shop(
    val id: String,
    val name: String,
    val category: ShopCategory,
    val imageKey: String,
    val address: String,
    val isOpen: Boolean,
    val commissionRate: Double,
    val rating: Double,
    val deliveryTime: String,
    val distanceKm: String,
    val offerText: String,
    val tagline: String
)

data class Product(
    val id: String,
    val shopId: String,
    val name: String,
    val price: Int,
    val imageKey: String,
    val categoryTag: String,
    val isVeg: Boolean,
    val inStock: Boolean,
    val description: String = ""
)

data class CartItemDetail(
    val product: Product,
    val shop: Shop,
    val quantity: Int
) {
    val lineTotal: Int get() = product.price * quantity
}

data class OrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val priceAtOrder: Int
)

data class OrderWithItems(
    val id: String,
    val orderCode: String,
    val customerName: String,
    val customerPhone: String,
    val shopId: String,
    val shopName: String,
    val shopAddress: String,
    val deliveryBoyId: String?,
    val deliveryBoyName: String?,
    val deliveryBoyPhone: String?,
    val itemsTotal: Int,
    val discountAmount: Int,
    val promoCode: String?,
    val deliveryFee: Int,
    val platformFee: Int,
    val grandTotal: Int,
    val commissionRate: Double,
    val platformCommission: Int,
    val vendorNetPayout: Int,
    val riderDeliveryFee: Int,
    val paymentMethod: PaymentMethod,
    val status: OrderStatus,
    val deliveryAddress: String,
    val deliveryInstructions: String,
    val deliveryLat: Double,
    val deliveryLng: Double,
    val shopRating: Int?,
    val riderRating: Int?,
    val ratingComment: String?,
    val createdAt: Long,
    val items: List<OrderItem>
)

data class DeliveryPartner(
    val id: String,
    val name: String,
    val phone: String,
    val vehicleNumber: String,
    val isAvailable: Boolean
)

data class AppSettings(
    val minOrderAmount: Int = 99,
    val baseDeliveryFee: Int = 20,
    val freeDeliveryAbove: Int = 299,
    val platformFee: Int = 5,
    val supportWhatsapp: String = "919876543210"
)

data class TopSellingItem(
    val productName: String,
    val totalUnits: Int,
    val totalRevenue: Int
)

data class VisualDishShortcut(
    val id: String,
    val title: String,
    val category: ShopCategory,
    val searchKeyword: String = "",
    val imageKey: String = ""
)

data class UserAccount(
    val id: String,
    val fullName: String,
    val phone: String,
    val role: UserRole,
    val address: String,
    val hasPin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ShopSettlement(
    val shopId: String,
    val isSettled: Boolean,
    val settledAmount: Int,
    val settledAt: Long?
)

data class ShopHisabSummary(
    val shop: Shop,
    val deliveredOrderCount: Int,
    val grossItemSales: Int,
    val commissionDeducted: Int,
    val netPayableToVendor: Int,
    val isSettled: Boolean,
    val settledAt: Long?
)

data class RiderHisabSummary(
    val partner: DeliveryPartner,
    val deliveredOrderCount: Int,
    val cashCollected: Int,
    val deliveryFeeEarned: Int,
    val netCashToAdmin: Int
)

