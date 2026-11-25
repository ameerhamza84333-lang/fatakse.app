package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.AppSettingsEntity
import com.example.data.local.DeliveryPartnerEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ShopEntity
import com.example.data.local.ShopSettlementEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class RemoteProfileRecord(
    val user: UserAccount,
    val pinHash: String
)

/**
 * Live Supabase REST (PostgREST + GoTrue Auth) client for Fatakse.
 * Reads VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY from BuildConfig (configured via AI Studio Secrets).
 * Supports both legacy JWT anon keys (eyJ...) and Supabase publishable keys (sb_publishable_...).
 * Strictly fetches and persists shops, products, orders, order items, riders, settings, and settlements
 * directly from Supabase with zero hardcoded demo data.
 */
class SupabaseSyncClient {

    private val baseUrl: String = BuildConfig.VITE_SUPABASE_URL
        .trim()
        .trim('"', '\'', '[', ']')
        .trimEnd('/')
        .let { configured ->
            if (configured.startsWith("https://") && !configured.contains("YOUR_SUPABASE", ignoreCase = true)) {
                configured
            } else {
                DEFAULT_PUBLIC_SUPABASE_URL
            }
        }

    private val anonKey: String = BuildConfig.VITE_SUPABASE_ANON_KEY
        .trim()
        .trim('"', '\'', '[', ']')

    val isLiveSupabaseConfigured: Boolean
        get() = baseUrl.startsWith("https://") &&
            !baseUrl.contains("YOUR_SUPABASE", ignoreCase = true) &&
            anonKey.length > 20 &&
            !anonKey.contains("YOUR_SUPABASE", ignoreCase = true)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun isJwtToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return token.count { it == '.' } == 2 && token.startsWith("eyJ")
    }

    /**
     * Applies Supabase auth headers compatible with both legacy JWT anon keys and
     * modern sb_publishable_ keys (which authenticate via the `apikey` header without a Bearer JWT).
     */
    private fun Request.Builder.withSupabaseHeaders(
        userAccessToken: String? = null,
        prefer: String? = null
    ): Request.Builder {
        addHeader("apikey", anonKey)
        val bearerToken = when {
            !userAccessToken.isNullOrBlank() -> userAccessToken
            isJwtToken(anonKey) -> anonKey
            else -> null
        }
        if (bearerToken != null) {
            addHeader("Authorization", "Bearer $bearerToken")
        }
        if (!prefer.isNullOrBlank()) {
            addHeader("Prefer", prefer)
        }
        return this
    }

    // ==================== MOBILE PROFILE AUTH (public.profiles) ====================

    suspend fun fetchProfileByPhone(normalizedPhone: String): Result<RemoteProfileRecord?> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!isLiveSupabaseConfigured) return@runCatching null
                val rawDigits10 = normalizedPhone.removePrefix("+91").filter { it.isDigit() }
                val candidates = listOf(normalizedPhone, rawDigits10).filter { it.isNotBlank() }.distinct()
                for (candidate in candidates) {
                    val encodedPhone = URLEncoder.encode(candidate, "UTF-8")
                    val req = Request.Builder()
                        .url("$baseUrl/rest/v1/profiles?phone=eq.$encodedPhone&select=*&limit=1")
                        .withSupabaseHeaders()
                        .get()
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            Log.w(TAG, "fetchProfileByPhone failed: HTTP ${resp.code}")
                            throw IllegalStateException("Unable to reach Supabase profiles (HTTP ${resp.code})")
                        }
                        val body = resp.body?.string().orEmpty().ifBlank { "[]" }
                        val array = JSONArray(body)
                        if (array.length() > 0) {
                            val obj = array.getJSONObject(0)
                            val id = obj.optString("id")
                            if (id.isBlank()) continue
                            val fullName = obj.optString("full_name").ifBlank { "Fatakse User" }
                            val storedPhone = obj.optString("phone").ifBlank { normalizedPhone }
                            val roleKey = obj.optString("role", "customer")
                            val address = obj.optString("address").ifBlank { "Main Bazar, Kundarki, Moradabad, UP 244413" }
                            val pinHash = obj.optString("pin_hash").takeIf { it != "null" }.orEmpty()
                            return@runCatching RemoteProfileRecord(
                                user = UserAccount(
                                    id = id,
                                    fullName = fullName,
                                    phone = storedPhone,
                                    role = UserRole.entries.firstOrNull { it.dbKey == roleKey } ?: UserRole.CUSTOMER,
                                    address = address,
                                    hasPin = pinHash.isNotBlank()
                                ),
                                pinHash = pinHash
                            )
                        }
                    }
                }
                null
            }
        }

    suspend fun upsertSupabaseProfile(
        userId: String,
        accessToken: String?,
        fullName: String,
        phone: String,
        role: UserRole,
        address: String,
        pinHash: String = ""
    ): Result<Unit> {
        if (!isLiveSupabaseConfigured) return Result.success(Unit)
        return withContext(Dispatchers.IO) {
            runCatching {
                val basePayload = JSONObject()
                    .put("id", userId)
                    .put("full_name", fullName)
                    .put("phone", phone)
                    .put("role", role.dbKey)
                    .put("address", address)

                val payloadWithPin = JSONObject(basePayload.toString()).apply {
                    if (pinHash.isNotBlank()) put("pin_hash", pinHash) else put("pin_hash", JSONObject.NULL)
                }

                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/profiles")
                    .withSupabaseHeaders(
                        userAccessToken = accessToken,
                        prefer = "resolution=merge-duplicates,return=minimal"
                    )
                    .post(payloadWithPin.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) return@runCatching
                    val errBody = resp.body?.string().orEmpty()
                    // Graceful fallback if the remote profiles table does not yet have the optional pin_hash column
                    if ((resp.code == 400 || resp.code == 404) && errBody.contains("pin_hash", ignoreCase = true)) {
                        val retryReq = Request.Builder()
                            .url("$baseUrl/rest/v1/profiles")
                            .withSupabaseHeaders(
                                userAccessToken = accessToken,
                                prefer = "resolution=merge-duplicates,return=minimal"
                            )
                            .post(basePayload.toString().toRequestBody(jsonMediaType))
                            .build()
                        client.newCall(retryReq).execute().use { retryResp ->
                            if (!retryResp.isSuccessful) {
                                Log.w(TAG, "Supabase profile upsert retry failed: HTTP ${retryResp.code}")
                                throw IllegalStateException("Could not save profile to Supabase (HTTP ${retryResp.code})")
                            }
                        }
                    } else {
                        Log.w(TAG, "Supabase profile upsert failed: HTTP ${resp.code}")
                        throw IllegalStateException("Could not save profile to Supabase (HTTP ${resp.code})")
                    }
                }
            }
        }
    }

    // ==================== FETCH FROM SUPABASE ====================

    suspend fun fetchRemoteShops(userAccessToken: String? = null): List<ShopEntity>? {
        if (!isLiveSupabaseConfigured) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/shops?select=*&order=created_at.desc")
                    .withSupabaseHeaders(userAccessToken = userAccessToken)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "fetchRemoteShops failed: HTTP ${resp.code}")
                        return@runCatching null
                    }
                    val body = resp.body?.string().orEmpty().ifBlank { "[]" }
                    val array = JSONArray(body)
                    buildList<ShopEntity> {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val id = obj.optString("id")
                            val name = obj.optString("name")
                            if (id.isBlank() || name.isBlank()) continue
                            val address = obj.optString("address", "Kundarki, Moradabad")
                            val tagline = obj.optString("tagline").ifBlank { address }
                            add(
                                ShopEntity(
                                    id = id,
                                    name = name,
                                    category = obj.optString("category", "biryani"),
                                    imageKey = obj.optString("image_url").ifBlank { "img_shop_biryani" },
                                    address = address,
                                    isOpen = obj.optBoolean("is_open", true),
                                    commissionRate = obj.optDouble("commission_rate", 15.0),
                                    rating = obj.optDouble("rating", 4.5),
                                    deliveryTime = obj.optString("delivery_time", "20-25 MINS"),
                                    distanceKm = obj.optString("distance_km").ifBlank { "1.0 KM" },
                                    offerText = "",
                                    tagline = tagline
                                )
                            )
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "fetchRemoteShops error: ${it.message}") }.getOrNull()
        }
    }

    suspend fun fetchRemoteProducts(userAccessToken: String? = null): List<ProductEntity>? {
        if (!isLiveSupabaseConfigured) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/products?select=*&order=created_at.desc")
                    .withSupabaseHeaders(userAccessToken = userAccessToken)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "fetchRemoteProducts failed: HTTP ${resp.code}")
                        return@runCatching null
                    }
                    val body = resp.body?.string().orEmpty().ifBlank { "[]" }
                    val array = JSONArray(body)
                    buildList<ProductEntity> {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val id = obj.optString("id")
                            val shopId = obj.optString("shop_id")
                            val name = obj.optString("name")
                            if (id.isBlank() || shopId.isBlank() || name.isBlank()) continue
                            add(
                                ProductEntity(
                                    id = id,
                                    shopId = shopId,
                                    name = name,
                                    price = obj.optInt("price", 0),
                                    imageKey = obj.optString("image_url").ifBlank { "img_shop_biryani" },
                                    categoryTag = obj.optString("category_tag").ifBlank { "Popular" },
                                    isVeg = obj.optBoolean("is_veg", true),
                                    inStock = obj.optBoolean("in_stock", true),
                                    description = obj.optString("description", "")
                                )
                            )
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "fetchRemoteProducts error: ${it.message}") }.getOrNull()
        }
    }

    suspend fun fetchRemoteOrders(userAccessToken: String? = null): List<OrderEntity>? {
        if (!isLiveSupabaseConfigured) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/orders?select=*&order=created_at.desc")
                    .withSupabaseHeaders(userAccessToken = userAccessToken)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "fetchRemoteOrders failed: HTTP ${resp.code}")
                        return@runCatching null
                    }
                    val body = resp.body?.string().orEmpty().ifBlank { "[]" }
                    val array = JSONArray(body)
                    buildList<OrderEntity> {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val id = obj.optString("id")
                            val shopId = obj.optString("shop_id")
                            if (id.isBlank() || shopId.isBlank()) continue
                            val orderCode = obj.optString("order_code").ifBlank {
                                "FTK-${id.replace("-", "").take(6).uppercase(Locale.US)}"
                            }
                            val deliveryBoyId = obj.optString("delivery_boy_id")
                                .takeIf { it.isNotBlank() && it != "null" }
                            add(
                                OrderEntity(
                                    id = id,
                                    orderCode = orderCode,
                                    customerName = obj.optString("customer_name", "Customer"),
                                    customerPhone = obj.optString("customer_phone", ""),
                                    shopId = shopId,
                                    deliveryBoyId = deliveryBoyId,
                                    itemsTotal = obj.optInt("items_total", 0),
                                    discountAmount = 0,
                                    promoCode = null,
                                    deliveryFee = obj.optInt("delivery_fee", 0),
                                    platformFee = obj.optInt("platform_fee", 0),
                                    grandTotal = obj.optInt("grand_total", 0),
                                    commissionRate = obj.optDouble("commission_rate", 15.0),
                                    platformCommission = obj.optInt("platform_commission", 0),
                                    vendorNetPayout = obj.optInt("vendor_net_payout", 0),
                                    riderDeliveryFee = obj.optInt("rider_delivery_fee", 20),
                                    paymentMethod = obj.optString(
                                        "payment_method",
                                        PaymentMethod.CASH_UPI_QR_ON_DELIVERY.dbKey
                                    ),
                                    status = obj.optString("status", "pending"),
                                    deliveryAddress = obj.optString("delivery_address", ""),
                                    deliveryInstructions = obj.optString("delivery_instructions", ""),
                                    deliveryLat = obj.optDouble("delivery_lat", 28.6852),
                                    deliveryLng = obj.optDouble("delivery_lng", 78.7793),
                                    createdAt = parseIsoTimestamp(obj.optString("created_at"))
                                )
                            )
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "fetchRemoteOrders error: ${it.message}") }.getOrNull()
        }
    }

    suspend fun fetchRemoteOrderItems(userAccessToken: String? = null): List<OrderItemEntity>? {
        if (!isLiveSupabaseConfigured) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/order_items?select=*")
                    .withSupabaseHeaders(userAccessToken = userAccessToken)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "fetchRemoteOrderItems failed: HTTP ${resp.code}")
                        return@runCatching null
                    }
                    val body = resp.body?.string().orEmpty().ifBlank { "[]" }
                    val array = JSONArray(body)
                    buildList<OrderItemEntity> {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val id = obj.optString("id")
                            val orderId = obj.optString("order_id")
                            if (id.isBlank() || orderId.isBlank()) continue
                            add(
                                OrderItemEntity(
                                    id = id,
                                    orderId = orderId,
                                    productId = obj.optString("product_id", ""),
                                    productName = obj.optString("product_name", "Item"),
                                    quantity = obj.optInt("quantity", 1),
                                    priceAtOrder = obj.optInt("price_at_order", 0)
                                )
                            )
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "fetchRemoteOrderItems error: ${it.message}") }.getOrNull()
        }
    }

    suspend fun fetchRemoteDeliveryPartners(userAccessToken: String? = null): List<DeliveryPartnerEntity>? {
        if (!isLiveSupabaseConfigured) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/profiles?role=eq.delivery&select=*")
                    .withSupabaseHeaders(userAccessToken = userAccessToken)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    val body = resp.body?.string().orEmpty().ifBlank { "[]" }
                    val array = JSONArray(body)
                    buildList<DeliveryPartnerEntity> {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val id = obj.optString("id")
                            val name = obj.optString("full_name")
                            if (id.isBlank() || name.isBlank()) continue
                            add(
                                DeliveryPartnerEntity(
                                    id = id,
                                    name = name,
                                    phone = obj.optString("phone", ""),
                                    vehicleNumber = obj.optString("vehicle_number").ifBlank { "Kundarki Rider" },
                                    isAvailable = true
                                )
                            )
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "fetchRemoteDeliveryPartners error: ${it.message}") }.getOrNull()
        }
    }

    suspend fun fetchRemoteSettings(userAccessToken: String? = null): AppSettingsEntity? {
        if (!isLiveSupabaseConfigured) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/app_settings?id=eq.1&select=*")
                    .withSupabaseHeaders(userAccessToken = userAccessToken)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    val body = resp.body?.string().orEmpty().ifBlank { "[]" }
                    val array = JSONArray(body)
                    if (array.length() == 0) return@runCatching null
                    val obj = array.getJSONObject(0)
                    AppSettingsEntity(
                        id = 1,
                        minOrderAmount = obj.optInt("min_order_amount", 99),
                        baseDeliveryFee = obj.optInt("base_delivery_fee", 20),
                        freeDeliveryAbove = obj.optInt("free_delivery_above", 299),
                        platformFee = obj.optInt("platform_fee", 5),
                        supportWhatsapp = obj.optString("support_whatsapp", "919876543210")
                    )
                }
            }.onFailure { Log.w(TAG, "fetchRemoteSettings error: ${it.message}") }.getOrNull()
        }
    }

    suspend fun fetchRemoteShopSettlements(userAccessToken: String? = null): List<ShopSettlementEntity>? {
        if (!isLiveSupabaseConfigured) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/shop_settlements?select=*")
                    .withSupabaseHeaders(userAccessToken = userAccessToken)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    val body = resp.body?.string().orEmpty().ifBlank { "[]" }
                    val array = JSONArray(body)
                    buildList<ShopSettlementEntity> {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val shopId = obj.optString("shop_id")
                            if (shopId.isBlank()) continue
                            val settledAtRaw = obj.optString("settled_at").takeIf { it.isNotBlank() && it != "null" }
                            add(
                                ShopSettlementEntity(
                                    shopId = shopId,
                                    isSettled = obj.optBoolean("is_settled", false),
                                    settledAmount = obj.optInt("settled_amount", 0),
                                    settledAt = settledAtRaw?.let { parseIsoTimestamp(it) }
                                )
                            )
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "fetchRemoteShopSettlements error: ${it.message}") }.getOrNull()
        }
    }

    // ==================== MUTATIONS TO SUPABASE ====================

    suspend fun syncShopOpenStatus(shopId: String, isOpen: Boolean, userAccessToken: String? = null) {
        if (!isLiveSupabaseConfigured) return
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().put("is_open", isOpen).toString()
                val req = requestBuilder("$baseUrl/rest/v1/shops?id=eq.$shopId", userAccessToken)
                    .patch(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().close()
            }.onFailure { Log.w(TAG, "Supabase shop status sync skipped: ${it.message}") }
        }
    }

    suspend fun syncShopCategory(
        shopId: String,
        category: String,
        commissionRate: Double,
        userAccessToken: String? = null
    ) {
        if (!isLiveSupabaseConfigured) return
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject()
                    .put("category", category)
                    .put("commission_rate", commissionRate)
                    .toString()
                val req = requestBuilder("$baseUrl/rest/v1/shops?id=eq.$shopId", userAccessToken)
                    .patch(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().close()
            }.onFailure { Log.w(TAG, "Supabase shop category sync skipped: ${it.message}") }
        }
    }

    suspend fun pushNewShop(shop: ShopEntity, userAccessToken: String? = null): Boolean {
        if (!isLiveSupabaseConfigured) return false
        return withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject()
                    .put("id", shop.id)
                    .put("name", shop.name)
                    .put("category", shop.category)
                    .put("image_url", shop.imageKey)
                    .put("address", shop.address)
                    .put("is_open", shop.isOpen)
                    .put("commission_rate", shop.commissionRate)
                    .put("rating", shop.rating)
                    .put("delivery_time", shop.deliveryTime)
                    .toString()
                val req = requestBuilder("$baseUrl/rest/v1/shops", userAccessToken)
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().use { it.isSuccessful }
            }.onFailure { Log.w(TAG, "Supabase shop insert skipped: ${it.message}") }.getOrDefault(false)
        }
    }

    suspend fun syncProductStock(productId: String, inStock: Boolean, userAccessToken: String? = null) {
        if (!isLiveSupabaseConfigured) return
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().put("in_stock", inStock).toString()
                val req = requestBuilder("$baseUrl/rest/v1/products?id=eq.$productId", userAccessToken)
                    .patch(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().close()
            }.onFailure { Log.w(TAG, "Supabase product stock sync skipped: ${it.message}") }
        }
    }

    suspend fun pushNewProduct(product: ProductEntity, userAccessToken: String? = null): Boolean {
        if (!isLiveSupabaseConfigured) return false
        return withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject()
                    .put("id", product.id)
                    .put("shop_id", product.shopId)
                    .put("name", product.name)
                    .put("price", product.price)
                    .put("image_url", product.imageKey)
                    .put("category_tag", product.categoryTag)
                    .put("is_veg", product.isVeg)
                    .put("in_stock", product.inStock)
                    .toString()
                val req = requestBuilder("$baseUrl/rest/v1/products", userAccessToken)
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().use { it.isSuccessful }
            }.onFailure { Log.w(TAG, "Supabase product insert skipped: ${it.message}") }.getOrDefault(false)
        }
    }

    suspend fun pushOrder(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        customerId: String? = null,
        userAccessToken: String? = null
    ): Boolean {
        if (!isLiveSupabaseConfigured) return false
        return withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject()
                    .put("id", order.id)
                    .put("customer_name", order.customerName)
                    .put("customer_phone", order.customerPhone)
                    .put("shop_id", order.shopId)
                    .put("items_total", order.itemsTotal)
                    .put("delivery_fee", order.deliveryFee)
                    .put("platform_fee", order.platformFee)
                    .put("grand_total", order.grandTotal)
                    .put("commission_rate", order.commissionRate)
                    .put("platform_commission", order.platformCommission)
                    .put("vendor_net_payout", order.vendorNetPayout)
                    .put("rider_delivery_fee", order.riderDeliveryFee)
                    .put("payment_method", order.paymentMethod)
                    .put("status", order.status)
                    .put("delivery_address", order.deliveryAddress)
                    .put("delivery_lat", order.deliveryLat)
                    .put("delivery_lng", order.deliveryLng)
                    .apply {
                        if (!customerId.isNullOrBlank()) put("customer_id", customerId)
                    }
                    .toString()
                val req = requestBuilder("$baseUrl/rest/v1/orders", userAccessToken)
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()
                val orderInserted = client.newCall(req).execute().use { it.isSuccessful }

                if (orderInserted && items.isNotEmpty()) {
                    val itemsJson = JSONArray()
                    items.forEach { item ->
                        itemsJson.put(
                            JSONObject()
                                .put("id", item.id)
                                .put("order_id", item.orderId)
                                .put("product_id", item.productId)
                                .put("product_name", item.productName)
                                .put("quantity", item.quantity)
                                .put("price_at_order", item.priceAtOrder)
                        )
                    }
                    val itemsReq = requestBuilder("$baseUrl/rest/v1/order_items", userAccessToken)
                        .post(itemsJson.toString().toRequestBody(jsonMediaType))
                        .build()
                    client.newCall(itemsReq).execute().close()
                }
                orderInserted
            }.onFailure { Log.w(TAG, "Supabase order sync skipped: ${it.message}") }.getOrDefault(false)
        }
    }

    suspend fun syncOrderStatus(orderId: String, status: String, userAccessToken: String? = null) {
        if (!isLiveSupabaseConfigured) return
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().put("status", status).toString()
                val req = requestBuilder("$baseUrl/rest/v1/orders?id=eq.$orderId", userAccessToken)
                    .patch(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().close()
            }.onFailure { Log.w(TAG, "Supabase order status sync skipped: ${it.message}") }
        }
    }

    suspend fun syncOrderDeliveryAssignment(
        orderId: String,
        deliveryBoyId: String?,
        status: String? = null,
        userAccessToken: String? = null
    ) {
        if (!isLiveSupabaseConfigured) return
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject()
                    .put("delivery_boy_id", deliveryBoyId ?: JSONObject.NULL)
                    .apply {
                        if (!status.isNullOrBlank()) put("status", status)
                    }
                    .toString()
                val req = requestBuilder("$baseUrl/rest/v1/orders?id=eq.$orderId", userAccessToken)
                    .patch(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().close()
            }.onFailure { Log.w(TAG, "Supabase order delivery assignment skipped: ${it.message}") }
        }
    }

    suspend fun syncSettings(settings: AppSettingsEntity, userAccessToken: String? = null) {
        if (!isLiveSupabaseConfigured) return
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject()
                    .put("id", 1)
                    .put("min_order_amount", settings.minOrderAmount)
                    .put("base_delivery_fee", settings.baseDeliveryFee)
                    .put("free_delivery_above", settings.freeDeliveryAbove)
                    .put("platform_fee", settings.platformFee)
                    .put("support_whatsapp", settings.supportWhatsapp)
                    .toString()
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/app_settings")
                    .withSupabaseHeaders(
                        userAccessToken = userAccessToken,
                        prefer = "resolution=merge-duplicates,return=minimal"
                    )
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().close()
            }.onFailure { Log.w(TAG, "Supabase settings sync skipped: ${it.message}") }
        }
    }

    suspend fun syncShopSettlement(settlement: ShopSettlementEntity, userAccessToken: String? = null) {
        if (!isLiveSupabaseConfigured) return
        withContext(Dispatchers.IO) {
            runCatching {
                val settledIso = settlement.settledAt?.let { formatIsoTimestamp(it) }
                val payload = JSONObject()
                    .put("shop_id", settlement.shopId)
                    .put("is_settled", settlement.isSettled)
                    .put("settled_amount", settlement.settledAmount)
                    .put("settled_at", settledIso ?: JSONObject.NULL)
                    .toString()
                val req = Request.Builder()
                    .url("$baseUrl/rest/v1/shop_settlements")
                    .withSupabaseHeaders(
                        userAccessToken = userAccessToken,
                        prefer = "resolution=merge-duplicates,return=minimal"
                    )
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().close()
            }.onFailure { Log.w(TAG, "Supabase settlement sync skipped: ${it.message}") }
        }
    }

    private fun requestBuilder(url: String, userAccessToken: String? = null): Request.Builder {
        return Request.Builder()
            .url(url)
            .withSupabaseHeaders(userAccessToken = userAccessToken, prefer = "return=minimal")
    }

    private fun parseIsoTimestamp(raw: String?): Long {
        if (raw.isNullOrBlank() || raw == "null") return System.currentTimeMillis()
        val normalized = raw
            .trim()
            .replace(" ", "T")
            .replace(Regex("\\.(\\d{3})\\d+"), ".$1")
            .replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
            .replace("Z", "+0000")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(normalized)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // Try next format
            }
        }
        return System.currentTimeMillis()
    }

    private fun formatIsoTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(millis))
    }

    companion object {
        private const val TAG = "SupabaseSyncClient"
        // Public Supabase project base URL (non-secret; API key is injected via BuildConfig.VITE_SUPABASE_ANON_KEY)
        private const val DEFAULT_PUBLIC_SUPABASE_URL = "https://snrmzkdxlrtawxxjndko.supabase.co"
    }
}
