package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.KundarkiConfig
import com.example.data.local.KundarkiDatabase
import com.example.data.local.SessionManager
import com.example.data.model.AppSettings
import com.example.data.model.CartItemDetail
import com.example.data.model.DeliveryPartner
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithItems
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.RiderHisabSummary
import com.example.data.model.Shop
import com.example.data.model.ShopCategory
import com.example.data.model.ShopHisabSummary
import com.example.data.model.ShopSettlement
import com.example.data.model.TopSellingItem
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.data.model.VisualDishShortcut
import com.example.data.remote.SupabaseSyncClient
import com.example.data.repository.KundarkiRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CustomerTab { HOME, CART, ORDERS, ACCOUNT }
enum class VendorTab { ORDERS, MENU, EARNINGS }
enum class AdminTab { OVERVIEW, HISAB, DISPATCH, SETTINGS }

enum class PendingAuthAction { NONE, OPEN_ORDERS, OPEN_ACCOUNT, PLACE_ORDER }

data class CheckoutFormState(
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = KundarkiConfig.formatDeliveryAddress(KundarkiConfig.neighborhoods.first()),
    val deliveryLat: Double = 28.6852,
    val deliveryLng: Double = 78.7793,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH_UPI_QR_ON_DELIVERY,
    val selectedInstructions: Set<String> = setOf("Call on arrival"),
    val isDetectingGps: Boolean = false
)

data class CartPricingSummary(
    val totalItemCount: Int = 0,
    val itemsTotal: Int = 0,
    val deliveryFee: Int = 0,
    val platformFee: Int = 0,
    val grandTotal: Int = 0,
    val minOrderShortfall: Int = 0,
    val freeDeliveryShortfall: Int = 0,
    val isFreeDeliveryUnlocked: Boolean = false,
    val meetsMinOrder: Boolean = false
)

data class KundarkiUiState(
    val currentUser: UserAccount? = null,
    val showAuthScreen: Boolean = false,
    val authPromptMessage: String? = null,
    val isAuthBusy: Boolean = false,
    val activeRole: UserRole = UserRole.CUSTOMER,
    val customerTab: CustomerTab = CustomerTab.HOME,
    val vendorTab: VendorTab = VendorTab.ORDERS,
    val adminTab: AdminTab = AdminTab.OVERVIEW,
    val isLiveSupabase: Boolean = false,
    val isSyncing: Boolean = false,
    val selectedNeighborhood: String = KundarkiConfig.neighborhoods.first(),
    val searchQuery: String = "",
    val selectedCategory: ShopCategory = ShopCategory.ALL,
    val selectedDishShortcutId: String? = null,
    val vegOnlyFilter: Boolean = false,
    val rating4PlusFilter: Boolean = false,
    val favoritesOnlyFilter: Boolean = false,
    val openShopId: String? = null,
    val selectedVendorShopId: String = "",
    val selectedDeliveryPartnerId: String = "",
    val shops: List<Shop> = emptyList(),
    val favoriteShopIds: Set<String> = emptySet(),
    val products: List<Product> = emptyList(),
    val cartItems: List<CartItemDetail> = emptyList(),
    val orders: List<OrderWithItems> = emptyList(),
    val deliveryPartners: List<DeliveryPartner> = emptyList(),
    val shopSettlements: Map<String, ShopSettlement> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val checkoutForm: CheckoutFormState = CheckoutFormState()
) {
    val isLoggedIn: Boolean get() = currentUser != null

    val activeShop: Shop?
        get() = openShopId?.let { id -> shops.firstOrNull { it.id == id } }

    val selectedVendorShop: Shop?
        get() = shops.firstOrNull { it.id == selectedVendorShopId } ?: shops.firstOrNull()

    val selectedDeliveryPartner: DeliveryPartner?
        get() = deliveryPartners.firstOrNull { it.id == selectedDeliveryPartnerId } ?: deliveryPartners.firstOrNull()

    val cartQuantities: Map<String, Int>
        get() = cartItems.associate { it.product.id to it.quantity }

    val cartPricing: CartPricingSummary
        get() {
            if (cartItems.isEmpty()) {
                return CartPricingSummary(
                    minOrderShortfall = settings.minOrderAmount,
                    freeDeliveryShortfall = settings.freeDeliveryAbove
                )
            }
            val count = cartItems.sumOf { it.quantity }
            val itemsTotal = cartItems.sumOf { it.lineTotal }
            val isFree = itemsTotal >= settings.freeDeliveryAbove
            val deliveryFee = if (isFree) 0 else settings.baseDeliveryFee
            val platformFee = settings.platformFee
            val shortfallMin = (settings.minOrderAmount - itemsTotal).coerceAtLeast(0)
            val shortfallFree = if (isFree) 0 else (settings.freeDeliveryAbove - itemsTotal).coerceAtLeast(0)
            val grandTotal = itemsTotal + deliveryFee + platformFee

            return CartPricingSummary(
                totalItemCount = count,
                itemsTotal = itemsTotal,
                deliveryFee = deliveryFee,
                platformFee = platformFee,
                grandTotal = grandTotal,
                minOrderShortfall = shortfallMin,
                freeDeliveryShortfall = shortfallFree,
                isFreeDeliveryUnlocked = isFree,
                meetsMinOrder = itemsTotal >= settings.minOrderAmount
            )
        }

    private fun productMatchesCategory(product: Product, shop: Shop, category: ShopCategory): Boolean {
        if (category == ShopCategory.ALL) return true
        if (shop.category == category) return true
        val tag = product.categoryTag.lowercase()
        val name = product.name.lowercase()
        return when (category) {
            ShopCategory.ALL -> true
            ShopCategory.BIRYANI -> tag.contains("biryani") || name.contains("biryani")
            ShopCategory.MOMOS_FAST_FOOD -> tag.contains("momo") || tag.contains("fast food") ||
                tag.contains("roll") || tag.contains("burger") || tag.contains("chowmein") ||
                tag.contains("snacks") || name.contains("momo") || name.contains("burger") ||
                name.contains("chowmein") || name.contains("roll") || name.contains("sandwich")
            ShopCategory.CHAI_COFFEE -> tag.contains("chai") || tag.contains("tea") ||
                tag.contains("coffee") || name.contains("chai") || name.contains("coffee")
            ShopCategory.COLD_DRINKS_SHAKES -> tag.contains("cold drink") || tag.contains("shake") ||
                tag.contains("cold coffee") || name.contains("shake") || name.contains("cola") ||
                name.contains("mojito") || name.contains("sprite") || name.contains("cold coffee")
            ShopCategory.SWEETS_BAKERY -> tag.contains("sweet") || tag.contains("mithai") ||
                tag.contains("bakery") || name.contains("cake") || name.contains("jalebi") ||
                name.contains("gulab jamun") || name.contains("laddoo") || name.contains("kaju")
            ShopCategory.GROCERY_KIRANA -> tag.contains("grocery") || tag.contains("kirana") ||
                tag.contains("staples") || tag.contains("oil") || name.contains("atta") || name.contains("rice")
            ShopCategory.DAIRY_FRUITS -> tag.contains("dairy") || tag.contains("fruit") ||
                name.contains("milk") || name.contains("paneer") || name.contains("apple") ||
                name.contains("banana") || name.contains("dahi") || name.contains("curd")
        }
    }

    val filteredShops: List<Shop>
        get() {
            val q = searchQuery.trim().lowercase()
            return shops.filter { shop ->
                val shopProducts = products.filter { it.shopId == shop.id }
                val matchesCategory = selectedCategory == ShopCategory.ALL ||
                    shop.category == selectedCategory ||
                    shopProducts.any { productMatchesCategory(it, shop, selectedCategory) }
                if (!matchesCategory) return@filter false
                if (favoritesOnlyFilter && shop.id !in favoriteShopIds) return@filter false
                if (rating4PlusFilter && shop.rating < 4.0) return@filter false
                if (vegOnlyFilter && shopProducts.none { it.isVeg }) return@filter false
                if (q.isEmpty()) return@filter true
                val matchesShop = shop.name.lowercase().contains(q) ||
                    shop.tagline.lowercase().contains(q) ||
                    shop.category.label.lowercase().contains(q)
                val matchesProduct = shopProducts.any { p ->
                    (!vegOnlyFilter || p.isVeg) &&
                        (p.name.lowercase().contains(q) || p.categoryTag.lowercase().contains(q))
                }
                matchesShop || matchesProduct
            }.let { list ->
                if (rating4PlusFilter) {
                    list.sortedWith(compareByDescending<Shop> { it.isOpen }.thenByDescending { it.rating })
                } else {
                    list.sortedByDescending { it.isOpen }
                }
            }
        }

    val matchedProductsForHome: List<Pair<Product, Shop>>
        get() {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty() && selectedCategory == ShopCategory.ALL) return emptyList()
            val shopMap = shops.associateBy { it.id }
            return products.mapNotNull { product ->
                if (vegOnlyFilter && !product.isVeg) return@mapNotNull null
                val shop = shopMap[product.shopId] ?: return@mapNotNull null
                if (favoritesOnlyFilter && shop.id !in favoriteShopIds) return@mapNotNull null
                val matchesCategory = productMatchesCategory(product, shop, selectedCategory)
                val matchesText = q.isEmpty() ||
                    product.name.lowercase().contains(q) ||
                    product.categoryTag.lowercase().contains(q) ||
                    product.description.lowercase().contains(q) ||
                    shop.name.lowercase().contains(q)
                if (matchesCategory && matchesText) product to shop else null
            }.sortedByDescending { (_, shop) -> shop.isOpen }
        }

    val topSellingItems: List<TopSellingItem>
        get() = orders
            .filter { it.status != OrderStatus.CANCELLED }
            .flatMap { it.items }
            .groupBy { it.productName }
            .map { (name, items) ->
                TopSellingItem(
                    productName = name,
                    totalUnits = items.sumOf { it.quantity },
                    totalRevenue = items.sumOf { it.quantity * it.priceAtOrder }
                )
            }
            .sortedByDescending { it.totalRevenue }
            .take(6)

    val shopHisabSummaries: List<ShopHisabSummary>
        get() {
            val deliveredOrders = orders.filter { it.status == OrderStatus.DELIVERED }
            return shops.map { shop ->
                val shopDelivered = deliveredOrders.filter { it.shopId == shop.id }
                val gross = shopDelivered.sumOf { it.itemsTotal }
                val comm = shopDelivered.sumOf { it.platformCommission }
                val netPayable = shopDelivered.sumOf { it.vendorNetPayout }
                val settlement = shopSettlements[shop.id]
                ShopHisabSummary(
                    shop = shop,
                    deliveredOrderCount = shopDelivered.size,
                    grossItemSales = gross,
                    commissionDeducted = comm,
                    netPayableToVendor = netPayable,
                    isSettled = settlement?.isSettled == true &&
                        netPayable > 0 &&
                        settlement.settledAmount >= netPayable,
                    settledAt = settlement?.settledAt
                )
            }.sortedWith(
                compareByDescending<ShopHisabSummary> { it.netPayableToVendor }
                    .thenBy { it.shop.name }
            )
        }

    val riderHisabSummaries: List<RiderHisabSummary>
        get() {
            val deliveredOrders = orders.filter { it.status == OrderStatus.DELIVERED }
            return deliveryPartners.map { partner ->
                val partnerDelivered = deliveredOrders.filter { it.deliveryBoyId == partner.id }
                val cashCollected = partnerDelivered.sumOf { it.grandTotal }
                val riderFees = partnerDelivered.sumOf { it.riderDeliveryFee }
                RiderHisabSummary(
                    partner = partner,
                    deliveredOrderCount = partnerDelivered.size,
                    cashCollected = cashCollected,
                    deliveryFeeEarned = riderFees,
                    netCashToAdmin = (cashCollected - riderFees).coerceAtLeast(0)
                )
            }.sortedByDescending { it.cashCollected }
        }

    companion object {
        const val PARTNER_PASSCODE = "1234"

        val visualDishShortcuts = listOf(
            VisualDishShortcut("all", "All", ShopCategory.ALL),
            VisualDishShortcut("biryani", "Biryani", ShopCategory.BIRYANI, "Biryani", "img_shop_biryani"),
            VisualDishShortcut("momos_fast_food", "Momos & Fast Food", ShopCategory.MOMOS_FAST_FOOD, "Momos", "img_shop_biryani"),
            VisualDishShortcut("chai_coffee", "Chai & Coffee", ShopCategory.CHAI_COFFEE, "Chai", "img_shop_chai"),
            VisualDishShortcut("cold_drinks_shakes", "Cold Drinks & Shakes", ShopCategory.COLD_DRINKS_SHAKES, "Shake", "img_shop_chai"),
            VisualDishShortcut("sweets_bakery", "Sweets & Bakery", ShopCategory.SWEETS_BAKERY, "Sweets", "img_shop_sweets"),
            VisualDishShortcut("grocery_kirana", "Grocery & Kirana", ShopCategory.GROCERY_KIRANA, "Grocery", "img_shop_kirana"),
            VisualDishShortcut("dairy_fruits", "Dairy & Fruits", ShopCategory.DAIRY_FRUITS, "Dairy", "img_shop_kirana")
        )
    }
}

class KundarkiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KundarkiDatabase.getInstance(application)
    private val supabaseClient = SupabaseSyncClient()
    private val sessionManager = SessionManager(application)
    private val repository = KundarkiRepository(database.dao(), supabaseClient, sessionManager)

    private val _localState = MutableStateFlow(
        LocalUiHolders(isLiveSupabase = repository.isLiveSupabaseMode)
    )
    private val _checkoutForm = MutableStateFlow(CheckoutFormState())
    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _showRoleSheet = MutableStateFlow(false)
    val showRoleSheet: StateFlow<Boolean> = _showRoleSheet.asStateFlow()

    private var pendingAuthAction: PendingAuthAction = PendingAuthAction.NONE

    private val coreDataFlow = combine(
        repository.shops,
        repository.products,
        repository.cartDetails,
        repository.ordersWithItems,
        repository.deliveryPartners
    ) { shops, products, cart, orders, partners ->
        DataBundle(shops, products, cart, orders, partners)
    }

    private val extraDataFlow = combine(
        repository.favoriteShopIds,
        repository.settings,
        repository.currentUser,
        repository.shopSettlements
    ) { favs, settings, user, settlements ->
        ExtraBundle(favs, settings, user, settlements)
    }

    val uiState: StateFlow<KundarkiUiState> = combine(
        _localState,
        _checkoutForm,
        coreDataFlow,
        extraDataFlow
    ) { local, checkout, bundle, extra ->
        KundarkiUiState(
            currentUser = extra.currentUser,
            showAuthScreen = local.showAuthScreen,
            authPromptMessage = local.authPromptMessage,
            isAuthBusy = local.isAuthBusy,
            activeRole = local.activeRole,
            customerTab = local.customerTab,
            vendorTab = local.vendorTab,
            adminTab = local.adminTab,
            isLiveSupabase = local.isLiveSupabase,
            isSyncing = local.isSyncing,
            selectedNeighborhood = local.selectedNeighborhood,
            searchQuery = local.searchQuery,
            selectedCategory = local.selectedCategory,
            selectedDishShortcutId = local.selectedDishShortcutId,
            vegOnlyFilter = local.vegOnlyFilter,
            rating4PlusFilter = local.rating4PlusFilter,
            favoritesOnlyFilter = local.favoritesOnlyFilter,
            openShopId = local.openShopId,
            selectedVendorShopId = local.selectedVendorShopId,
            selectedDeliveryPartnerId = local.selectedDeliveryPartnerId,
            shops = bundle.shops,
            favoriteShopIds = extra.favorites,
            products = bundle.products,
            cartItems = bundle.cart,
            orders = bundle.orders,
            deliveryPartners = bundle.partners,
            shopSettlements = extra.settlements,
            settings = extra.settings,
            checkoutForm = checkout
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KundarkiUiState(isLiveSupabase = repository.isLiveSupabaseMode)
    )

    init {
        refreshFromSupabase(silent = true)
        // Automatically sync persisted session profile to role routing and checkout fields
        viewModelScope.launch {
            repository.currentUser
                .distinctUntilChanged { old, new -> old?.id == new?.id && old?.address == new?.address && old?.fullName == new?.fullName && old?.phone == new?.phone }
                .collect { user ->
                    if (user != null) {
                        _checkoutForm.update {
                            it.copy(
                                customerName = user.fullName,
                                customerPhone = user.phone,
                                deliveryAddress = user.address.ifBlank { it.deliveryAddress }
                            )
                        }
                        _localState.update { current ->
                            if (current.lastRoutedUserId != user.id) {
                                current.copy(
                                    activeRole = user.role,
                                    lastRoutedUserId = user.id
                                )
                            } else {
                                current
                            }
                        }
                    } else {
                        _localState.update { it.copy(lastRoutedUserId = null) }
                    }
                }
        }
    }

    fun refreshFromSupabase(silent: Boolean = false) {
        viewModelScope.launch {
            _localState.update { it.copy(isSyncing = true) }
            val result = repository.syncFromSupabase()
            _localState.update { it.copy(isSyncing = false) }
            if (!silent) {
                result.onSuccess {
                    emitToast("Synced live data from Supabase")
                }.onFailure { err ->
                    emitToast(err.message ?: "Failed to sync from Supabase")
                }
            }
        }
    }

    // ==================== AUTH ACTIONS ====================

    fun openAuthScreen(promptMessage: String? = null) {
        _showRoleSheet.value = false
        _localState.update {
            it.copy(showAuthScreen = true, authPromptMessage = promptMessage)
        }
    }

    fun continueAsGuest() {
        pendingAuthAction = PendingAuthAction.NONE
        _localState.update {
            it.copy(showAuthScreen = false, authPromptMessage = null)
        }
        emitToast("Browsing as Guest — log in anytime to place orders")
    }

    fun login(phoneDigits10: String, pin4: String) {
        val cleanDigits = phoneDigits10.filter { it.isDigit() }
        val cleanPin = pin4.filter { it.isDigit() }
        if (cleanDigits.length != 10) {
            emitToast("Enter a valid 10-digit Indian mobile number")
            return
        }
        if (cleanPin.isNotEmpty() && cleanPin.length != 4) {
            emitToast("Enter your 4-digit PIN, or leave blank for instant login")
            return
        }
        viewModelScope.launch {
            _localState.update { it.copy(isAuthBusy = true) }
            val result = repository.login(cleanDigits, cleanPin)
            _localState.update { it.copy(isAuthBusy = false) }
            result.onSuccess { user ->
                handleSuccessfulAuth(user, isNewUser = false)
            }.onFailure { err ->
                emitToast(err.message ?: "Login failed. Please try again.")
            }
        }
    }

    fun signUp(
        fullName: String,
        phoneDigits10: String,
        pin4: String,
        role: UserRole,
        partnerPasscode: String
    ) {
        val cleanDigits = phoneDigits10.filter { it.isDigit() }
        val cleanPin = pin4.filter { it.isDigit() }
        if (fullName.trim().length < 2) {
            emitToast("Please enter your full name")
            return
        }
        if (cleanDigits.length != 10) {
            emitToast("Enter a valid 10-digit Indian mobile number")
            return
        }
        if (cleanPin.isNotEmpty() && cleanPin.length != 4) {
            emitToast("PIN must be 4 digits, or leave it blank for instant login")
            return
        }
        if (role != UserRole.CUSTOMER && partnerPasscode.trim() != KundarkiUiState.PARTNER_PASSCODE) {
            emitToast("Invalid Partner/Admin passcode. Enter 1234 to verify ${role.label} signup.")
            return
        }

        val defaultAddr = KundarkiConfig.formatDeliveryAddress(uiState.value.selectedNeighborhood)
        viewModelScope.launch {
            _localState.update { it.copy(isAuthBusy = true) }
            val result = repository.signUp(
                fullName = fullName,
                phoneDigits10 = cleanDigits,
                pin4 = cleanPin,
                role = role,
                defaultAddress = defaultAddr
            )
            _localState.update { it.copy(isAuthBusy = false) }
            result.onSuccess { user ->
                handleSuccessfulAuth(user, isNewUser = true)
            }.onFailure { err ->
                emitToast(err.message ?: "Sign up failed. Please try again.")
            }
        }
    }

    private fun handleSuccessfulAuth(user: UserAccount, isNewUser: Boolean) {
        val action = pendingAuthAction
        pendingAuthAction = PendingAuthAction.NONE

        _checkoutForm.update {
            it.copy(
                customerName = user.fullName,
                customerPhone = user.phone,
                deliveryAddress = user.address.ifBlank { it.deliveryAddress }
            )
        }

        val nextCustomerTab = when {
            user.role != UserRole.CUSTOMER -> CustomerTab.HOME
            action == PendingAuthAction.OPEN_ORDERS -> CustomerTab.ORDERS
            action == PendingAuthAction.OPEN_ACCOUNT -> CustomerTab.ACCOUNT
            action == PendingAuthAction.PLACE_ORDER -> CustomerTab.CART
            else -> _localState.value.customerTab
        }

        _localState.update {
            it.copy(
                showAuthScreen = false,
                authPromptMessage = null,
                activeRole = user.role,
                customerTab = nextCustomerTab,
                openShopId = null,
                lastRoutedUserId = user.id
            )
        }

        val greeting = if (isNewUser) "Welcome to Fatakse, ${user.fullName}!" else "Welcome back, ${user.fullName}!"
        emitToast(greeting)
        refreshFromSupabase(silent = true)

        if (action == PendingAuthAction.PLACE_ORDER && user.role == UserRole.CUSTOMER) {
            placeOrderInternal()
        }
    }

    fun logout() {
        _showRoleSheet.value = false
        pendingAuthAction = PendingAuthAction.NONE
        viewModelScope.launch {
            repository.logout()
            _localState.update {
                it.copy(
                    activeRole = UserRole.CUSTOMER,
                    customerTab = CustomerTab.HOME,
                    openShopId = null,
                    showAuthScreen = false,
                    lastRoutedUserId = null
                )
            }
            refreshFromSupabase(silent = true)
            emitToast("Logged out successfully")
        }
    }

    fun updateProfile(fullName: String, phone: String, address: String) {
        val user = uiState.value.currentUser ?: return
        if (fullName.isBlank() || phone.isBlank() || address.isBlank()) {
            emitToast("Name, phone, and address cannot be empty")
            return
        }
        viewModelScope.launch {
            repository.updateUserProfile(user.id, fullName, phone, address)
            _checkoutForm.update {
                it.copy(
                    customerName = fullName.trim(),
                    customerPhone = phone.trim(),
                    deliveryAddress = address.trim()
                )
            }
            emitToast("Profile & saved Kundarki address updated!")
        }
    }

    // ==================== NAVIGATION & ROLE SWITCHING ====================

    fun openRoleSheet() {
        _showRoleSheet.value = true
    }

    fun dismissRoleSheet() {
        _showRoleSheet.value = false
    }

    fun switchRole(role: UserRole) {
        _showRoleSheet.value = false
        _localState.update { it.copy(activeRole = role, openShopId = null) }
        emitToast("Switched to ${role.label} Panel")
    }

    fun selectCustomerTab(tab: CustomerTab) {
        val loggedIn = uiState.value.isLoggedIn
        if (!loggedIn && (tab == CustomerTab.ORDERS || tab == CustomerTab.ACCOUNT)) {
            pendingAuthAction = if (tab == CustomerTab.ORDERS) {
                PendingAuthAction.OPEN_ORDERS
            } else {
                PendingAuthAction.OPEN_ACCOUNT
            }
            val reason = if (tab == CustomerTab.ORDERS) {
                "Log in or sign up to track your live orders"
            } else {
                "Log in or sign up to manage your profile & saved address"
            }
            openAuthScreen(reason)
            return
        }
        _localState.update { it.copy(customerTab = tab, openShopId = null) }
    }

    fun selectVendorTab(tab: VendorTab) {
        _localState.update { it.copy(vendorTab = tab) }
    }

    fun selectAdminTab(tab: AdminTab) {
        _localState.update { it.copy(adminTab = tab) }
    }

    fun selectNeighborhood(neighborhood: String) {
        val fullAddress = KundarkiConfig.formatDeliveryAddress(neighborhood)
        _localState.update { it.copy(selectedNeighborhood = neighborhood) }
        _checkoutForm.update {
            it.copy(deliveryAddress = fullAddress)
        }
        val activeUser = uiState.value.currentUser
        if (activeUser != null) {
            viewModelScope.launch {
                repository.updateUserProfile(
                    userId = activeUser.id,
                    fullName = activeUser.fullName,
                    phone = activeUser.phone,
                    address = fullAddress
                )
            }
        }
        emitToast("Delivering to $neighborhood")
    }

    fun updateSearchQuery(query: String) {
        _localState.update { it.copy(searchQuery = query, selectedDishShortcutId = null) }
    }

    fun selectCategory(category: ShopCategory) {
        _localState.update {
            it.copy(
                selectedCategory = category,
                selectedDishShortcutId = if (category == ShopCategory.ALL) null else category.dbKey
            )
        }
    }

    fun toggleDishShortcut(shortcut: VisualDishShortcut) {
        _localState.update { current ->
            if (shortcut.category == ShopCategory.ALL || current.selectedCategory == shortcut.category) {
                current.copy(
                    selectedCategory = ShopCategory.ALL,
                    selectedDishShortcutId = null,
                    searchQuery = ""
                )
            } else {
                current.copy(
                    selectedCategory = shortcut.category,
                    selectedDishShortcutId = shortcut.id,
                    searchQuery = ""
                )
            }
        }
    }

    fun toggleVegOnlyFilter() {
        _localState.update { it.copy(vegOnlyFilter = !it.vegOnlyFilter) }
    }

    fun toggleRating4PlusFilter() {
        _localState.update { it.copy(rating4PlusFilter = !it.rating4PlusFilter) }
    }

    fun toggleFavoritesOnlyFilter() {
        _localState.update { it.copy(favoritesOnlyFilter = !it.favoritesOnlyFilter) }
    }

    fun resetHomeFilters() {
        _localState.update {
            it.copy(
                searchQuery = "",
                selectedCategory = ShopCategory.ALL,
                selectedDishShortcutId = null,
                vegOnlyFilter = false,
                rating4PlusFilter = false,
                favoritesOnlyFilter = false
            )
        }
    }

    fun toggleFavoriteShop(shop: Shop) {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavoriteShop(shop.id)
            emitToast(if (isNowFav) "Saved ${shop.name} to Favorites ♥" else "Removed ${shop.name} from Favorites")
        }
    }

    fun openShop(shopId: String) {
        _localState.update { it.copy(openShopId = shopId) }
    }

    fun closeShop() {
        _localState.update { it.copy(openShopId = null) }
    }

    fun openCartFromFloatingBar() {
        _localState.update { it.copy(openShopId = null, customerTab = CustomerTab.CART) }
    }

    fun selectVendorShop(shopId: String) {
        _localState.update { it.copy(selectedVendorShopId = shopId) }
    }

    fun selectDeliveryPartner(partnerId: String) {
        _localState.update { it.copy(selectedDeliveryPartnerId = partnerId) }
    }

    // ==================== CART ====================

    fun addToCart(product: Product, shop: Shop) {
        if (!shop.isOpen) {
            emitToast("This shop is currently closed and not accepting orders right now.")
            return
        }
        if (!product.inStock) {
            emitToast("${product.name} is out of stock")
            return
        }
        viewModelScope.launch {
            val switched = repository.incrementCartItem(product)
            if (switched) {
                emitToast("Cart updated for ${shop.name}")
            } else {
                emitToast("Added ${product.name}")
            }
        }
    }

    fun decrementCart(productId: String) {
        viewModelScope.launch {
            repository.decrementCartItem(productId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
            emitToast("Cart cleared")
        }
    }

    fun reorderFromOrder(order: OrderWithItems) {
        viewModelScope.launch {
            val count = repository.reorderFromOrder(order)
            if (count > 0) {
                _localState.update { it.copy(customerTab = CustomerTab.CART, openShopId = null) }
                emitToast("Added $count items from ${order.shopName} to Cart")
            } else {
                emitToast("Items from ${order.orderCode} are currently out of stock")
            }
        }
    }

    // ==================== CHECKOUT & ORDER PLACEMENT ====================

    fun updateCustomerName(value: String) {
        _checkoutForm.update { it.copy(customerName = value) }
    }

    fun updateCustomerPhone(value: String) {
        _checkoutForm.update { it.copy(customerPhone = value) }
    }

    fun updateDeliveryAddress(value: String) {
        _checkoutForm.update { it.copy(deliveryAddress = value) }
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        _checkoutForm.update { it.copy(paymentMethod = method) }
    }

    fun toggleDeliveryInstruction(instruction: String) {
        _checkoutForm.update { current ->
            val next = if (instruction in current.selectedInstructions) {
                current.selectedInstructions - instruction
            } else {
                current.selectedInstructions + instruction
            }
            current.copy(selectedInstructions = next)
        }
    }

    fun setGpsDetecting(detecting: Boolean) {
        _checkoutForm.update { it.copy(isDetectingGps = detecting) }
    }

    fun onLocationDetected(address: String, lat: Double, lng: Double) {
        _checkoutForm.update {
            it.copy(
                deliveryAddress = address,
                deliveryLat = lat,
                deliveryLng = lng,
                isDetectingGps = false
            )
        }
        emitToast("GPS location updated")
    }

    fun placeOrder() {
        if (!uiState.value.isLoggedIn) {
            pendingAuthAction = PendingAuthAction.PLACE_ORDER
            openAuthScreen("Log in or create an account to confirm and track your order")
            return
        }
        placeOrderInternal()
    }

    private fun placeOrderInternal() {
        val state = uiState.value
        val cart = state.cartItems
        if (cart.isEmpty()) {
            emitToast("Your cart is empty")
            return
        }
        val cartShop = cart.first().shop
        if (!cartShop.isOpen) {
            emitToast("${cartShop.name} is currently closed and not accepting orders right now.")
            return
        }
        if (!state.cartPricing.meetsMinOrder) {
            emitToast("Add ₹${state.cartPricing.minOrderShortfall} more to meet minimum order")
            return
        }
        val form = state.checkoutForm
        if (form.customerName.isBlank() || form.customerPhone.isBlank() || form.deliveryAddress.isBlank()) {
            emitToast("Please fill name, phone, and delivery address")
            return
        }
        viewModelScope.launch {
            val orderCode = repository.placeOrder(
                customerName = form.customerName,
                customerPhone = form.customerPhone,
                deliveryAddress = form.deliveryAddress,
                deliveryInstructions = form.selectedInstructions.joinToString(" • "),
                deliveryLat = form.deliveryLat,
                deliveryLng = form.deliveryLng,
                paymentMethod = PaymentMethod.CASH_UPI_QR_ON_DELIVERY,
                cartItems = cart,
                settings = state.settings,
                appliedPromo = null
            )
            _localState.update {
                it.copy(
                    customerTab = CustomerTab.ORDERS,
                    openShopId = null
                )
            }
            emitToast("Order $orderCode placed successfully!")
        }
    }

    fun submitOrderRating(
        orderId: String,
        orderCode: String,
        shopRating: Int,
        riderRating: Int,
        comment: String
    ) {
        viewModelScope.launch {
            repository.rateOrder(orderId, shopRating, riderRating, comment)
            emitToast("Thanks for rating $orderCode ($shopRating★ shop, $riderRating★ rider)!")
        }
    }

    // ==================== VENDOR, DELIVERY & ADMIN ====================

    fun toggleShopOpen(shop: Shop) {
        viewModelScope.launch {
            val next = !shop.isOpen
            repository.toggleShopOpen(shop.id, next)
            emitToast("${shop.name} is now ${if (next) "OPEN NOW" else "CLOSED"}")
        }
    }

    fun updateShopCategory(shop: Shop, category: ShopCategory) {
        if (category == ShopCategory.ALL) return
        viewModelScope.launch {
            repository.updateShopCategory(shop.id, category)
            emitToast("${shop.name} category updated to ${category.label}")
        }
    }

    fun addShop(
        name: String,
        category: ShopCategory,
        address: String,
        deliveryTime: String,
        tagline: String,
        isOpen: Boolean
    ) {
        if (name.trim().length < 2) {
            emitToast("Enter a valid shop name")
            return
        }
        if (address.trim().length < 3) {
            emitToast("Enter a valid Kundarki shop address")
            return
        }
        viewModelScope.launch {
            repository.addShop(
                name = name,
                category = if (category == ShopCategory.ALL) ShopCategory.BIRYANI else category,
                address = address,
                deliveryTime = deliveryTime,
                tagline = tagline,
                isOpen = isOpen
            )
            emitToast("Added ${name.trim()} under ${category.label}")
        }
    }

    fun toggleProductStock(product: Product) {
        viewModelScope.launch {
            val next = !product.inStock
            repository.toggleProductStock(product.id, next)
            emitToast("${product.name}: ${if (next) "In Stock" else "Out of Stock"}")
        }
    }

    fun addVendorProduct(
        shop: Shop,
        name: String,
        priceText: String,
        categoryTag: String,
        isVeg: Boolean,
        description: String
    ) {
        val price = priceText.toIntOrNull()
        if (name.isBlank() || price == null || price <= 0) {
            emitToast("Enter a valid product name and price")
            return
        }
        viewModelScope.launch {
            repository.addProduct(
                shopId = shop.id,
                shopImageKey = shop.imageKey,
                name = name,
                price = price,
                categoryTag = categoryTag,
                isVeg = isVeg,
                description = description
            )
            emitToast("Added $name (₹$price) to ${shop.name}")
        }
    }

    fun updateOrderStatus(orderId: String, orderCode: String, status: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
            emitToast("$orderCode marked as ${status.label}")
        }
    }

    fun acceptDelivery(order: OrderWithItems) {
        val rider = uiState.value.selectedDeliveryPartner ?: return
        viewModelScope.launch {
            repository.acceptDelivery(order.id, rider.id)
            emitToast("You accepted ${order.orderCode} • Out for Delivery")
        }
    }

    fun markOrderDelivered(order: OrderWithItems) {
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, OrderStatus.DELIVERED)
            emitToast("${order.orderCode} delivered successfully!")
        }
    }

    fun assignDeliveryBoy(order: OrderWithItems, partner: DeliveryPartner?) {
        viewModelScope.launch {
            repository.assignDeliveryPartner(order.id, partner?.id)
            val msg = if (partner != null) {
                "Assigned ${partner.name} to ${order.orderCode}"
            } else {
                "Unassigned delivery partner from ${order.orderCode}"
            }
            emitToast(msg)
        }
    }

    fun saveAppSettings(
        minOrder: Int,
        baseDeliveryFee: Int,
        freeDeliveryAbove: Int,
        platformFee: Int,
        supportWhatsapp: String
    ) {
        if (minOrder < 0 || baseDeliveryFee < 0 || freeDeliveryAbove < 0 || platformFee < 0) {
            emitToast("Fee amounts must be zero or positive")
            return
        }
        viewModelScope.launch {
            repository.updateSettings(
                AppSettings(
                    minOrderAmount = minOrder,
                    baseDeliveryFee = baseDeliveryFee,
                    freeDeliveryAbove = freeDeliveryAbove,
                    platformFee = platformFee,
                    supportWhatsapp = supportWhatsapp.ifBlank { "919876543210" }
                )
            )
            emitToast("Platform fees & settings updated live!")
        }
    }

    fun toggleShopSettlement(summary: ShopHisabSummary) {
        if (summary.netPayableToVendor <= 0) {
            emitToast("No delivered payout pending for ${summary.shop.name}")
            return
        }
        val nextSettled = !summary.isSettled
        viewModelScope.launch {
            repository.setShopSettlementStatus(
                shopId = summary.shop.id,
                isSettled = nextSettled,
                settledAmount = summary.netPayableToVendor
            )
            val statusText = if (nextSettled) {
                "Marked ₹${summary.netPayableToVendor} SETTLED for ${summary.shop.name}"
            } else {
                "Marked ${summary.shop.name} payout back to PENDING"
            }
            emitToast(statusText)
        }
    }

    fun emitToast(message: String) {
        _toastEvents.tryEmit(message)
    }

    private data class LocalUiHolders(
        val showAuthScreen: Boolean = false,
        val authPromptMessage: String? = null,
        val isAuthBusy: Boolean = false,
        val lastRoutedUserId: String? = null,
        val activeRole: UserRole = UserRole.CUSTOMER,
        val customerTab: CustomerTab = CustomerTab.HOME,
        val vendorTab: VendorTab = VendorTab.ORDERS,
        val adminTab: AdminTab = AdminTab.OVERVIEW,
        val isLiveSupabase: Boolean = false,
        val isSyncing: Boolean = false,
        val selectedNeighborhood: String = KundarkiConfig.neighborhoods.first(),
        val searchQuery: String = "",
        val selectedCategory: ShopCategory = ShopCategory.ALL,
        val selectedDishShortcutId: String? = null,
        val vegOnlyFilter: Boolean = false,
        val rating4PlusFilter: Boolean = false,
        val favoritesOnlyFilter: Boolean = false,
        val openShopId: String? = null,
        val selectedVendorShopId: String = "",
        val selectedDeliveryPartnerId: String = ""
    )

    private data class DataBundle(
        val shops: List<Shop>,
        val products: List<Product>,
        val cart: List<CartItemDetail>,
        val orders: List<OrderWithItems>,
        val partners: List<DeliveryPartner>
    )

    private data class ExtraBundle(
        val favorites: Set<String>,
        val settings: AppSettings,
        val currentUser: UserAccount?,
        val settlements: Map<String, ShopSettlement>
    )
}
