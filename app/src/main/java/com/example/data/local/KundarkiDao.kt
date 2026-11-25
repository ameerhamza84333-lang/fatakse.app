package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KundarkiDao {

    // Shops
    @Query("SELECT * FROM shops ORDER BY rating DESC")
    fun observeShops(): Flow<List<ShopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShops(shops: List<ShopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Query("DELETE FROM shops")
    suspend fun clearShops()

    @Transaction
    suspend fun replaceShops(shops: List<ShopEntity>) {
        clearShops()
        if (shops.isNotEmpty()) {
            insertShops(shops)
        }
    }

    @Query("UPDATE shops SET isOpen = :isOpen WHERE id = :shopId")
    suspend fun updateShopOpenStatus(shopId: String, isOpen: Boolean)

    @Query("UPDATE shops SET category = :category, commissionRate = :commissionRate WHERE id = :shopId")
    suspend fun updateShopCategory(shopId: String, category: String, commissionRate: Double)

    // Favorites
    @Query("SELECT shopId FROM favorite_shops")
    fun observeFavoriteShopIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM favorite_shops WHERE shopId = :shopId")
    suspend fun isFavoriteShop(shopId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteShop(fav: FavoriteShopEntity)

    @Query("DELETE FROM favorite_shops WHERE shopId = :shopId")
    suspend fun removeFavoriteShop(shopId: String)

    // Promo Codes
    @Query("SELECT * FROM promo_codes ORDER BY isActive DESC, minOrderAmount ASC")
    fun observePromoCodes(): Flow<List<PromoCodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPromoCode(promo: PromoCodeEntity)

    @Query("UPDATE promo_codes SET isActive = :isActive WHERE code = :code")
    suspend fun updatePromoActive(code: String, isActive: Boolean)

    @Query("DELETE FROM promo_codes WHERE code = :code")
    suspend fun deletePromoCode(code: String)

    // Products
    @Query("SELECT * FROM products ORDER BY categoryTag ASC, name ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getProductsByIds(ids: List<String>): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Transaction
    suspend fun replaceProducts(products: List<ProductEntity>) {
        clearProducts()
        if (products.isNotEmpty()) {
            insertProducts(products)
        }
    }

    @Query("UPDATE products SET inStock = :inStock WHERE id = :productId")
    suspend fun updateProductStock(productId: String, inStock: Boolean)

    // Cart
    @Query("SELECT * FROM cart_items")
    fun observeCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items")
    suspend fun getCartItemsOnce(): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItem(item: CartItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItems(items: List<CartItemEntity>)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Transaction
    suspend fun replaceCart(items: List<CartItemEntity>) {
        clearCart()
        upsertCartItems(items)
    }

    // Orders & Order Items
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM order_items")
    fun observeOrderItems(): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM orders")
    suspend fun clearOrders()

    @Query("DELETE FROM order_items")
    suspend fun clearOrderItems()

    @Transaction
    suspend fun replaceOrdersAndItems(orders: List<OrderEntity>, items: List<OrderItemEntity>) {
        clearOrderItems()
        clearOrders()
        if (orders.isNotEmpty()) {
            insertOrders(orders)
        }
        if (items.isNotEmpty()) {
            insertOrderItems(items)
        }
    }

    @Transaction
    suspend fun placeOrderAndClearCart(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertOrderItems(items)
        clearCart()
    }

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("UPDATE orders SET deliveryBoyId = :deliveryBoyId WHERE id = :orderId")
    suspend fun assignDeliveryPartner(orderId: String, deliveryBoyId: String?)

    @Query("UPDATE orders SET deliveryBoyId = :deliveryBoyId, status = :status WHERE id = :orderId")
    suspend fun acceptAndAssignDelivery(orderId: String, deliveryBoyId: String, status: String)

    @Query("UPDATE orders SET shopRating = :shopRating, riderRating = :riderRating, ratingComment = :comment WHERE id = :orderId")
    suspend fun rateOrder(orderId: String, shopRating: Int, riderRating: Int, comment: String?)

    // Delivery Partners
    @Query("SELECT * FROM delivery_partners ORDER BY name ASC")
    fun observeDeliveryPartners(): Flow<List<DeliveryPartnerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryPartners(partners: List<DeliveryPartnerEntity>)

    @Query("DELETE FROM delivery_partners")
    suspend fun clearDeliveryPartners()

    @Transaction
    suspend fun replaceDeliveryPartners(partners: List<DeliveryPartnerEntity>) {
        clearDeliveryPartners()
        if (partners.isNotEmpty()) {
            insertDeliveryPartners(partners)
        }
    }

    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    // User Accounts (Cached Supabase Profile)
    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    fun observeUserById(id: String): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE phone = :normalizedPhone OR phone = :rawDigits10 LIMIT 1")
    suspend fun findUserByPhone(normalizedPhone: String, rawDigits10: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserAccount(user: UserAccountEntity)

    @Query("UPDATE user_accounts SET fullName = :fullName, phone = :phone, address = :address WHERE id = :userId")
    suspend fun updateUserProfile(userId: String, fullName: String, phone: String, address: String)

    // Daily Hisab — Shop Settlements
    @Query("SELECT * FROM shop_settlements")
    fun observeShopSettlements(): Flow<List<ShopSettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShopSettlement(settlement: ShopSettlementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopSettlements(settlements: List<ShopSettlementEntity>)

    @Query("DELETE FROM shop_settlements")
    suspend fun clearShopSettlements()

    @Transaction
    suspend fun replaceShopSettlements(settlements: List<ShopSettlementEntity>) {
        clearShopSettlements()
        if (settlements.isNotEmpty()) {
            insertShopSettlements(settlements)
        }
    }
}
