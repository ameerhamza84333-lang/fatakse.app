package com.example.data.repository

import com.example.data.local.AppSettingsEntity
import com.example.data.local.CartItemEntity
import com.example.data.local.FavoriteShopEntity
import com.example.data.local.KundarkiConfig
import com.example.data.local.KundarkiDao
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.ProductEntity
import com.example.data.local.PromoCodeEntity
import com.example.data.local.SessionManager
import com.example.data.local.ShopEntity
import com.example.data.local.ShopSettlementEntity
import com.example.data.local.UserAccountEntity
import com.example.data.model.AppSettings
import com.example.data.model.CartItemDetail
import com.example.data.model.DeliveryPartner
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithItems
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.PromoCode
import com.example.data.model.Shop
import com.example.data.model.ShopCategory
import com.example.data.model.ShopSettlement
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.data.model.calculateOrderSplit
import com.example.data.remote.SupabaseSyncClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

class KundarkiRepository(
    private val dao: KundarkiDao,
    private val supabase: SupabaseSyncClient,
    private val sessionManager: SessionManager
) {
    val isLiveSupabaseMode: Boolean
        get() = supabase.isLiveSupabaseConfigured

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserAccount?> = sessionManager.sessionFlow.flatMapLatest { session ->
        val uid = session.userId
        if (uid.isNullOrBlank()) {
            flowOf(null)
        } else {
            dao.observeUserById(uid).map { it?.toDomain() }
        }
    }

    val shops: Flow<List<Shop>> = dao.observeShops().map { list ->
        list.map { it.toDomain() }
    }

    val favoriteShopIds: Flow<Set<String>> = dao.observeFavoriteShopIds().map { it.toSet() }

    val promoCodes: Flow<List<PromoCode>> = dao.observePromoCodes().map { list ->
        list.map { it.toDomain() }
    }

    val products: Flow<List<Product>> = dao.observeProducts().map { list ->
        list.map { it.toDomain() }
    }

    val deliveryPartners: Flow<List<DeliveryPartner>> = dao.observeDeliveryPartners().map { list ->
        list.map { it.toDomain() }
    }

    val settings: Flow<AppSettings> = dao.observeSettings().map { entity ->
        entity?.toDomain() ?: AppSettings()
    }

    val shopSettlements: Flow<Map<String, ShopSettlement>> = dao.observeShopSettlements().map { list ->
        list.associate { it.shopId to it.toDomain() }
    }

    val cartDetails: Flow<List<CartItemDetail>> = combine(
        dao.observeCartItems(),
        shops,
        products
    ) { cartEntities, shopList, productList ->
        val shopMap = shopList.associateBy { it.id }
        val productMap = productList.associateBy { it.id }
        cartEntities.mapNotNull { cart ->
            val product = productMap[cart.productId] ?: return@mapNotNull null
            val shop = shopMap[cart.shopId] ?: return@mapNotNull null
            CartItemDetail(product = product, shop = shop, quantity = cart.quantity)
        }
    }

    val ordersWithItems: Flow<List<OrderWithItems>> = combine(
        dao.observeOrders(),
        dao.observeOrderItems(),
        shops,
        deliveryPartners
    ) { orderEntities, allOrderItems, shopList, partnerList ->
        val itemsByOrder = allOrderItems.groupBy { it.orderId }
        val shopMap = shopList.associateBy { it.id }
        val partnerMap = partnerList.associateBy { it.id }
        orderEntities.map { order ->
            val shop = shopMap[order.shopId]
            val partner = order.deliveryBoyId?.let { partnerMap[it] }
            val fallbackSplit = if (order.itemsTotal > 0 && order.vendorNetPayout == 0 && order.platformCommission == 0) {
                calculateOrderSplit(
                    itemsTotal = order.itemsTotal,
                    category = shop?.category ?: ShopCategory.BIRYANI,
                    deliveryFeeCharged = order.deliveryFee,
                    baseRiderDeliveryFee = KundarkiConfig.defaultSettings.baseDeliveryFee
                )
            } else {
                null
            }
            OrderWithItems(
                id = order.id,
                orderCode = order.orderCode,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                shopId = order.shopId,
                shopName = shop?.name ?: "Fatakse Partner Shop",
                shopAddress = shop?.address ?: "Kundarki, Moradabad",
                deliveryBoyId = order.deliveryBoyId,
                deliveryBoyName = partner?.name,
                deliveryBoyPhone = partner?.phone,
                itemsTotal = order.itemsTotal,
                discountAmount = order.discountAmount,
                promoCode = order.promoCode,
                deliveryFee = order.deliveryFee,
                platformFee = order.platformFee,
                grandTotal = order.grandTotal,
                commissionRate = fallbackSplit?.commissionRatePercent ?: order.commissionRate,
                platformCommission = fallbackSplit?.platformCommission ?: order.platformCommission,
                vendorNetPayout = fallbackSplit?.vendorNetPayout ?: order.vendorNetPayout,
                riderDeliveryFee = fallbackSplit?.riderDeliveryFee ?: order.riderDeliveryFee,
                paymentMethod = PaymentMethod.fromKey(order.paymentMethod),
                status = OrderStatus.fromKey(order.status),
                deliveryAddress = order.deliveryAddress,
                deliveryInstructions = order.deliveryInstructions,
                deliveryLat = order.deliveryLat,
                deliveryLng = order.deliveryLng,
                shopRating = order.shopRating,
                riderRating = order.riderRating,
                ratingComment = order.ratingComment,
                createdAt = order.createdAt,
                items = itemsByOrder[order.id].orEmpty().map { it.toDomain() }
            )
        }
    }

    /**
     * Fetches shops, products, orders, order items, delivery partners, settings, and settlements
     * strictly from Supabase. Never inserts mock or fallback data — if Supabase tables are empty,
     * local cache tables are cleared so 0 stores and no live order banner are shown.
     */
    suspend fun syncFromSupabase(): Result<Unit> = coroutineScope {
        if (!isLiveSupabaseMode) {
            dao.replaceShops(emptyList())
            dao.replaceProducts(emptyList())
            dao.replaceOrdersAndItems(emptyList(), emptyList())
            dao.replaceDeliveryPartners(emptyList())
            dao.replaceShopSettlements(emptyList())
            return@coroutineScope Result.failure(
                IllegalStateException("Supabase credentials not configured. Add VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY in AI Studio Secrets.")
            )
        }

        val token = sessionManager.sessionFlow.first().accessToken

        val shopsDeferred = async { supabase.fetchRemoteShops(token) }
        val productsDeferred = async { supabase.fetchRemoteProducts(token) }
        val ordersDeferred = async { supabase.fetchRemoteOrders(token) }
        val orderItemsDeferred = async { supabase.fetchRemoteOrderItems(token) }
        val partnersDeferred = async { supabase.fetchRemoteDeliveryPartners(token) }
        val settingsDeferred = async { supabase.fetchRemoteSettings(token) }
        val settlementsDeferred = async { supabase.fetchRemoteShopSettlements(token) }

        val remoteShops = shopsDeferred.await()
        val remoteProducts = productsDeferred.await()
        val remoteOrders = ordersDeferred.await()
        val remoteOrderItems = orderItemsDeferred.await()
        val remotePartners = partnersDeferred.await()
        val remoteSettings = settingsDeferred.await()
        val remoteSettlements = settlementsDeferred.await()

        dao.replaceShops(remoteShops.orEmpty())
        dao.replaceProducts(remoteProducts.orEmpty())
        dao.replaceOrdersAndItems(remoteOrders.orEmpty(), remoteOrderItems.orEmpty())
        dao.replaceDeliveryPartners(remotePartners.orEmpty())
        dao.replaceShopSettlements(remoteSettlements.orEmpty())
        if (remoteSettings != null) {
            dao.upsertSettings(remoteSettings)
        }

        if (remoteShops == null && remoteProducts == null && remoteOrders == null) {
            Result.failure(IllegalStateException("Could not reach Supabase. Please check your connection and Supabase policies."))
        } else {
            Result.success(Unit)
        }
    }

    // ==================== AUTHENTICATION (+91 MOBILE NUMBER ONLY) ====================

    suspend fun signUp(
        fullName: String,
        phoneDigits10: String,
        pin4: String,
        role: UserRole,
        defaultAddress: String
    ): Result<UserAccount> {
        val cleanDigits = phoneDigits10.filter { it.isDigit() }
        val formattedPhone = KundarkiConfig.normalizeIndianPhone(cleanDigits)
            ?: return Result.failure(IllegalStateException("Enter a valid 10-digit Indian mobile number."))
        val cleanName = fullName.trim()
        val cleanPin = pin4.filter { it.isDigit() }
        if (cleanPin.isNotEmpty() && cleanPin.length != 4) {
            return Result.failure(IllegalStateException("PIN must be 4 digits, or leave blank for instant login."))
        }
        val pinHash = KundarkiConfig.hashPin(formattedPhone, cleanPin)

        if (isLiveSupabaseMode) {
            val remoteLookup = supabase.fetchProfileByPhone(formattedPhone)
            val existingRemote = remoteLookup.getOrNull()
            if (existingRemote != null) {
                return Result.failure(
                    IllegalStateException("Mobile number $formattedPhone is already registered. Please log in instead.")
                )
            }
        } else {
            val existingLocal = dao.findUserByPhone(formattedPhone, cleanDigits)
            if (existingLocal != null) {
                return Result.failure(
                    IllegalStateException("Mobile number $formattedPhone is already registered. Please log in instead.")
                )
            }
        }

        val userId = UUID.randomUUID().toString()
        val entity = UserAccountEntity(
            id = userId,
            fullName = cleanName,
            phone = formattedPhone,
            pinHash = pinHash,
            role = role.dbKey,
            address = defaultAddress,
            createdAt = System.currentTimeMillis()
        )

        if (isLiveSupabaseMode) {
            // Sync new profile to Supabase public.profiles (best-effort if RLS restricts anon inserts)
            supabase.upsertSupabaseProfile(
                userId = userId,
                accessToken = null,
                fullName = cleanName,
                phone = formattedPhone,
                role = role,
                address = defaultAddress,
                pinHash = pinHash
            )
        }

        dao.upsertUserAccount(entity)
        sessionManager.saveSession(userId, accessToken = null)
        return Result.success(entity.toDomain())
    }

    suspend fun login(
        phoneDigits10: String,
        pin4: String
    ): Result<UserAccount> {
        val cleanDigits = phoneDigits10.filter { it.isDigit() }
        val formattedPhone = KundarkiConfig.normalizeIndianPhone(cleanDigits)
            ?: return Result.failure(IllegalStateException("Enter a valid 10-digit Indian mobile number."))
        val cleanPin = pin4.filter { it.isDigit() }
        if (cleanPin.isNotEmpty() && cleanPin.length != 4) {
            return Result.failure(IllegalStateException("Enter your 4-digit PIN, or leave blank if no PIN is set."))
        }

        val localCached = dao.findUserByPhone(formattedPhone, cleanDigits)

        if (isLiveSupabaseMode) {
            val remoteResult = supabase.fetchProfileByPhone(formattedPhone)
            val remoteProfile = remoteResult.getOrNull()
            if (remoteProfile != null) {
                val effectivePinHash = remoteProfile.pinHash.ifBlank { localCached?.pinHash.orEmpty() }
                if (effectivePinHash.isNotBlank()) {
                    if (cleanPin.isEmpty()) {
                        return Result.failure(
                            IllegalStateException("This account is protected with a 4-digit PIN. Please enter your PIN.")
                        )
                    }
                    if (!KundarkiConfig.verifyPinHash(formattedPhone, cleanPin, effectivePinHash)) {
                        return Result.failure(
                            IllegalStateException("Incorrect 4-digit PIN. Please try again.")
                        )
                    }
                }
                val mergedEntity = UserAccountEntity(
                    id = remoteProfile.user.id,
                    fullName = remoteProfile.user.fullName,
                    phone = formattedPhone,
                    pinHash = effectivePinHash,
                    role = remoteProfile.user.role.dbKey,
                    address = remoteProfile.user.address,
                    createdAt = localCached?.createdAt ?: System.currentTimeMillis()
                )
                dao.upsertUserAccount(mergedEntity)
                sessionManager.saveSession(mergedEntity.id, accessToken = null)
                return Result.success(mergedEntity.toDomain())
            }
        }

        if (localCached != null) {
            if (localCached.pinHash.isNotBlank()) {
                if (cleanPin.isEmpty()) {
                    return Result.failure(
                        IllegalStateException("This account is protected with a 4-digit PIN. Please enter your PIN.")
                    )
                }
                if (!KundarkiConfig.verifyPinHash(formattedPhone, cleanPin, localCached.pinHash)) {
                    return Result.failure(
                        IllegalStateException("Incorrect 4-digit PIN. Please try again.")
                    )
                }
            }
            sessionManager.saveSession(localCached.id, accessToken = null)
            return Result.success(localCached.toDomain())
        }

        return Result.failure(
            IllegalStateException("No account found for $formattedPhone. Switch to Create Account to join instantly.")
        )
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun updateUserProfile(
        userId: String,
        fullName: String,
        phone: String,
        address: String
    ) {
        val cleanName = fullName.trim()
        val normalizedPhone = KundarkiConfig.normalizeIndianPhone(phone) ?: phone.trim()
        val cleanAddress = address.trim()
        dao.updateUserProfile(userId, cleanName, normalizedPhone, cleanAddress)
        val session = sessionManager.sessionFlow.first()
        val existingEntity = dao.getUserById(userId)
        val user = existingEntity?.toDomain()
        if (isLiveSupabaseMode && user != null) {
            supabase.upsertSupabaseProfile(
                userId = user.id,
                accessToken = session.accessToken,
                fullName = cleanName,
                phone = normalizedPhone,
                role = user.role,
                address = cleanAddress,
                pinHash = existingEntity.pinHash
            )
        }
    }

    // ==================== SHOPS, CART, ORDERS & ADMIN ====================

    suspend fun toggleFavoriteShop(shopId: String): Boolean {
        val exists = dao.isFavoriteShop(shopId) > 0
        if (exists) {
            dao.removeFavoriteShop(shopId)
            return false
        } else {
            dao.addFavoriteShop(FavoriteShopEntity(shopId))
            return true
        }
    }

    suspend fun incrementCartItem(product: Product): Boolean {
        val currentCart = dao.getCartItemsOnce()
        var switchedShop = false
        if (currentCart.isNotEmpty() && currentCart.first().shopId != product.shopId) {
            dao.clearCart()
            switchedShop = true
        }
        val existingQty = if (switchedShop) 0 else currentCart.firstOrNull { it.productId == product.id }?.quantity ?: 0
        dao.upsertCartItem(
            CartItemEntity(
                productId = product.id,
                shopId = product.shopId,
                quantity = existingQty + 1
            )
        )
        return switchedShop
    }

    suspend fun decrementCartItem(productId: String) {
        val existing = dao.getCartItemsOnce().firstOrNull { it.productId == productId } ?: return
        if (existing.quantity <= 1) {
            dao.deleteCartItem(productId)
        } else {
            dao.upsertCartItem(existing.copy(quantity = existing.quantity - 1))
        }
    }

    suspend fun clearCart() = dao.clearCart()

    suspend fun reorderFromOrder(order: OrderWithItems): Int {
        val productIds = order.items.map { it.productId }
        val availableProducts = dao.getProductsByIds(productIds)
            .filter { it.inStock }
            .associateBy { it.id }
        val cartEntities = order.items.mapNotNull { item ->
            val product = availableProducts[item.productId] ?: return@mapNotNull null
            CartItemEntity(
                productId = product.id,
                shopId = product.shopId,
                quantity = item.quantity
            )
        }
        if (cartEntities.isNotEmpty()) {
            dao.replaceCart(cartEntities)
        }
        return cartEntities.size
    }

    suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        deliveryInstructions: String,
        deliveryLat: Double,
        deliveryLng: Double,
        paymentMethod: PaymentMethod,
        cartItems: List<CartItemDetail>,
        settings: AppSettings,
        appliedPromo: PromoCode?
    ): String {
        val shop = cartItems.first().shop
        val shopId = shop.id
        val itemsTotal = cartItems.sumOf { it.lineTotal }
        val discountAmount = appliedPromo?.computeDiscount(itemsTotal) ?: 0
        val promoFreeDelivery = appliedPromo?.unlocksFreeDelivery(itemsTotal) == true
        val deliveryFee = if (itemsTotal >= settings.freeDeliveryAbove || promoFreeDelivery) {
            0
        } else {
            settings.baseDeliveryFee
        }
        val platformFee = settings.platformFee
        val grandTotal = (itemsTotal - discountAmount).coerceAtLeast(0) + deliveryFee + platformFee
        val split = calculateOrderSplit(
            itemsTotal = itemsTotal,
            category = shop.category,
            deliveryFeeCharged = deliveryFee,
            baseRiderDeliveryFee = settings.baseDeliveryFee
        )
        val orderId = UUID.randomUUID().toString()
        val orderCode = "FTK-${orderId.replace("-", "").take(6).uppercase()}"

        val usedPromoCode = if (discountAmount > 0 || promoFreeDelivery) appliedPromo?.code else null

        val orderEntity = OrderEntity(
            id = orderId,
            orderCode = orderCode,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            shopId = shopId,
            deliveryBoyId = null,
            itemsTotal = itemsTotal,
            discountAmount = discountAmount,
            promoCode = usedPromoCode,
            deliveryFee = deliveryFee,
            platformFee = platformFee,
            grandTotal = grandTotal,
            commissionRate = split.commissionRatePercent,
            platformCommission = split.platformCommission,
            vendorNetPayout = split.vendorNetPayout,
            riderDeliveryFee = split.riderDeliveryFee,
            paymentMethod = PaymentMethod.CASH_UPI_QR_ON_DELIVERY.dbKey,
            status = OrderStatus.PENDING.dbKey,
            deliveryAddress = deliveryAddress.trim(),
            deliveryInstructions = deliveryInstructions.trim(),
            deliveryLat = deliveryLat,
            deliveryLng = deliveryLng,
            createdAt = System.currentTimeMillis()
        )

        val itemEntities = cartItems.map { detail ->
            OrderItemEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                productId = detail.product.id,
                productName = detail.product.name,
                quantity = detail.quantity,
                priceAtOrder = detail.product.price
            )
        }

        dao.placeOrderAndClearCart(orderEntity, itemEntities)
        val session = sessionManager.sessionFlow.first()
        supabase.pushOrder(
            order = orderEntity,
            items = itemEntities,
            customerId = session.userId,
            userAccessToken = session.accessToken
        )
        return orderCode
    }

    suspend fun rateOrder(orderId: String, shopRating: Int, riderRating: Int, comment: String) {
        dao.rateOrder(
            orderId = orderId,
            shopRating = shopRating.coerceIn(1, 5),
            riderRating = riderRating.coerceIn(1, 5),
            comment = comment.trim().ifBlank { null }
        )
    }

    suspend fun toggleShopOpen(shopId: String, isOpen: Boolean) {
        val token = sessionManager.sessionFlow.first().accessToken
        dao.updateShopOpenStatus(shopId, isOpen)
        supabase.syncShopOpenStatus(shopId, isOpen, token)
    }

    suspend fun updateShopCategory(shopId: String, category: ShopCategory) {
        val token = sessionManager.sessionFlow.first().accessToken
        val rate = category.defaultCommissionRate
        dao.updateShopCategory(shopId, category.dbKey, rate)
        supabase.syncShopCategory(shopId, category.dbKey, rate, token)
    }

    suspend fun addShop(
        name: String,
        category: ShopCategory,
        address: String,
        deliveryTime: String,
        tagline: String,
        isOpen: Boolean
    ): String {
        val token = sessionManager.sessionFlow.first().accessToken
        val imageKey = when (category) {
            ShopCategory.BIRYANI, ShopCategory.MOMOS_FAST_FOOD -> "img_shop_biryani"
            ShopCategory.CHAI_COFFEE, ShopCategory.COLD_DRINKS_SHAKES -> "img_shop_chai"
            ShopCategory.SWEETS_BAKERY -> "img_shop_sweets"
            ShopCategory.GROCERY_KIRANA, ShopCategory.DAIRY_FRUITS, ShopCategory.ALL -> "img_shop_kirana"
        }
        val shopId = UUID.randomUUID().toString()
        val entity = ShopEntity(
            id = shopId,
            name = name.trim(),
            category = category.dbKey,
            imageKey = imageKey,
            address = address.trim(),
            isOpen = isOpen,
            commissionRate = category.defaultCommissionRate,
            rating = 4.6,
            deliveryTime = deliveryTime.trim().ifBlank { "20-25 MINS" },
            distanceKm = "1.0 KM",
            offerText = "",
            tagline = tagline.trim().ifBlank { "${category.label} Partner in Kundarki" }
        )
        dao.insertShop(entity)
        supabase.pushNewShop(entity, token)
        return shopId
    }

    suspend fun toggleProductStock(productId: String, inStock: Boolean) {
        val token = sessionManager.sessionFlow.first().accessToken
        dao.updateProductStock(productId, inStock)
        supabase.syncProductStock(productId, inStock, token)
    }

    suspend fun addProduct(
        shopId: String,
        shopImageKey: String,
        name: String,
        price: Int,
        categoryTag: String,
        isVeg: Boolean,
        description: String
    ) {
        val token = sessionManager.sessionFlow.first().accessToken
        val entity = ProductEntity(
            id = UUID.randomUUID().toString(),
            shopId = shopId,
            name = name.trim(),
            price = price,
            imageKey = shopImageKey,
            categoryTag = categoryTag.trim().ifBlank { "Special" },
            isVeg = isVeg,
            inStock = true,
            description = description.trim().ifBlank { "Freshly prepared at our Kundarki store" }
        )
        dao.insertProduct(entity)
        supabase.pushNewProduct(entity, token)
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus) {
        val token = sessionManager.sessionFlow.first().accessToken
        dao.updateOrderStatus(orderId, status.dbKey)
        supabase.syncOrderStatus(orderId, status.dbKey, token)
    }

    suspend fun acceptDelivery(orderId: String, deliveryPartnerId: String) {
        val token = sessionManager.sessionFlow.first().accessToken
        dao.acceptAndAssignDelivery(
            orderId = orderId,
            deliveryBoyId = deliveryPartnerId,
            status = OrderStatus.OUT_FOR_DELIVERY.dbKey
        )
        supabase.syncOrderDeliveryAssignment(
            orderId = orderId,
            deliveryBoyId = deliveryPartnerId,
            status = OrderStatus.OUT_FOR_DELIVERY.dbKey,
            userAccessToken = token
        )
    }

    suspend fun assignDeliveryPartner(orderId: String, deliveryPartnerId: String?) {
        val token = sessionManager.sessionFlow.first().accessToken
        dao.assignDeliveryPartner(orderId, deliveryPartnerId)
        supabase.syncOrderDeliveryAssignment(
            orderId = orderId,
            deliveryBoyId = deliveryPartnerId,
            status = null,
            userAccessToken = token
        )
    }

    suspend fun upsertPromoCode(promo: PromoCode) {
        dao.upsertPromoCode(
            PromoCodeEntity(
                code = promo.code.trim().uppercase(),
                title = promo.title.trim(),
                subtitle = promo.subtitle.trim(),
                discountType = promo.discountType.dbKey,
                discountValue = promo.discountValue,
                maxDiscount = promo.maxDiscount,
                minOrderAmount = promo.minOrderAmount,
                isActive = promo.isActive
            )
        )
    }

    suspend fun togglePromoActive(code: String, isActive: Boolean) {
        dao.updatePromoActive(code, isActive)
    }

    suspend fun deletePromoCode(code: String) {
        dao.deletePromoCode(code)
    }

    suspend fun updateSettings(settings: AppSettings) {
        val token = sessionManager.sessionFlow.first().accessToken
        val entity = AppSettingsEntity(
            id = 1,
            minOrderAmount = settings.minOrderAmount,
            baseDeliveryFee = settings.baseDeliveryFee,
            freeDeliveryAbove = settings.freeDeliveryAbove,
            platformFee = settings.platformFee,
            supportWhatsapp = settings.supportWhatsapp.trim()
        )
        dao.upsertSettings(entity)
        supabase.syncSettings(entity, token)
    }

    suspend fun setShopSettlementStatus(shopId: String, isSettled: Boolean, settledAmount: Int) {
        val token = sessionManager.sessionFlow.first().accessToken
        val entity = ShopSettlementEntity(
            shopId = shopId,
            isSettled = isSettled,
            settledAmount = if (isSettled) settledAmount else 0,
            settledAt = if (isSettled) System.currentTimeMillis() else null
        )
        dao.upsertShopSettlement(entity)
        supabase.syncShopSettlement(entity, token)
    }
}
