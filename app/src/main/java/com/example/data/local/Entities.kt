package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AppSettings
import com.example.data.model.DeliveryPartner
import com.example.data.model.OrderItem
import com.example.data.model.Product
import com.example.data.model.PromoCode
import com.example.data.model.PromoDiscountType
import com.example.data.model.Shop
import com.example.data.model.ShopCategory

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val imageKey: String,
    val address: String,
    val isOpen: Boolean,
    val commissionRate: Double,
    val rating: Double,
    val deliveryTime: String,
    val distanceKm: String = "1.2 KM",
    val offerText: String = "",
    val tagline: String
) {
    fun toDomain(): Shop = Shop(
        id = id,
        name = name,
        category = ShopCategory.fromKey(category),
        imageKey = imageKey,
        address = address,
        isOpen = isOpen,
        commissionRate = commissionRate,
        rating = rating,
        deliveryTime = deliveryTime,
        distanceKm = distanceKm,
        offerText = offerText,
        tagline = tagline
    )
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val price: Int,
    val imageKey: String,
    val categoryTag: String,
    val isVeg: Boolean,
    val inStock: Boolean,
    val description: String
) {
    fun toDomain(): Product = Product(
        id = id,
        shopId = shopId,
        name = name,
        price = price,
        imageKey = imageKey,
        categoryTag = categoryTag,
        isVeg = isVeg,
        inStock = inStock,
        description = description
    )
}

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: String,
    val shopId: String,
    val quantity: Int
)

@Entity(tableName = "favorite_shops")
data class FavoriteShopEntity(
    @PrimaryKey val shopId: String
)

@Entity(tableName = "promo_codes")
data class PromoCodeEntity(
    @PrimaryKey val code: String,
    val title: String,
    val subtitle: String,
    val discountType: String,
    val discountValue: Int,
    val maxDiscount: Int,
    val minOrderAmount: Int,
    val isActive: Boolean
) {
    fun toDomain(): PromoCode = PromoCode(
        code = code,
        title = title,
        subtitle = subtitle,
        discountType = PromoDiscountType.fromKey(discountType),
        discountValue = discountValue,
        maxDiscount = maxDiscount,
        minOrderAmount = minOrderAmount,
        isActive = isActive
    )
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderCode: String,
    val customerName: String,
    val customerPhone: String,
    val shopId: String,
    val deliveryBoyId: String?,
    val itemsTotal: Int,
    val discountAmount: Int = 0,
    val promoCode: String? = null,
    val deliveryFee: Int,
    val platformFee: Int,
    val grandTotal: Int,
    val commissionRate: Double = 15.0,
    val platformCommission: Int = 0,
    val vendorNetPayout: Int = 0,
    val riderDeliveryFee: Int = 20,
    val paymentMethod: String,
    val status: String,
    val deliveryAddress: String,
    val deliveryInstructions: String = "",
    val deliveryLat: Double,
    val deliveryLng: Double,
    val shopRating: Int? = null,
    val riderRating: Int? = null,
    val ratingComment: String? = null,
    val createdAt: Long
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val priceAtOrder: Int
) {
    fun toDomain(): OrderItem = OrderItem(
        id = id,
        orderId = orderId,
        productId = productId,
        productName = productName,
        quantity = quantity,
        priceAtOrder = priceAtOrder
    )
}

@Entity(tableName = "delivery_partners")
data class DeliveryPartnerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val vehicleNumber: String,
    val isAvailable: Boolean
) {
    fun toDomain(): DeliveryPartner = DeliveryPartner(
        id = id,
        name = name,
        phone = phone,
        vehicleNumber = vehicleNumber,
        isAvailable = isAvailable
    )
}

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val minOrderAmount: Int,
    val baseDeliveryFee: Int,
    val freeDeliveryAbove: Int,
    val platformFee: Int,
    val supportWhatsapp: String
) {
    fun toDomain(): AppSettings = AppSettings(
        minOrderAmount = minOrderAmount,
        baseDeliveryFee = baseDeliveryFee,
        freeDeliveryAbove = freeDeliveryAbove,
        platformFee = platformFee,
        supportWhatsapp = supportWhatsapp
    )
}

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val pinHash: String = "",
    val role: String,
    val address: String,
    val createdAt: Long
) {
    fun toDomain(): com.example.data.model.UserAccount = com.example.data.model.UserAccount(
        id = id,
        fullName = fullName,
        phone = phone,
        role = com.example.data.model.UserRole.entries.firstOrNull { it.dbKey == role }
            ?: com.example.data.model.UserRole.CUSTOMER,
        address = address,
        hasPin = pinHash.isNotBlank(),
        createdAt = createdAt
    )
}

@Entity(tableName = "shop_settlements")
data class ShopSettlementEntity(
    @PrimaryKey val shopId: String,
    val isSettled: Boolean = false,
    val settledAmount: Int = 0,
    val settledAt: Long? = null
) {
    fun toDomain(): com.example.data.model.ShopSettlement = com.example.data.model.ShopSettlement(
        shopId = shopId,
        isSettled = isSettled,
        settledAmount = settledAmount,
        settledAt = settledAt
    )
}

