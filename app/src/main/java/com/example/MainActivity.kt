package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.ui.components.FatakseBrandBadge
import com.example.ui.components.FatakseSplashScreen
import com.example.ui.components.RolePickerBottomSheet
import kotlinx.coroutines.delay
import com.example.ui.screens.admin.AdminPanelScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.customer.CartCheckoutScreen
import com.example.ui.screens.customer.CustomerHomeScreen
import com.example.ui.screens.customer.CustomerOrdersScreen
import com.example.ui.screens.customer.ShopMenuScreen
import com.example.ui.screens.delivery.DeliveryPanelScreen
import com.example.ui.screens.profile.AccountRoleScreen
import com.example.ui.screens.vendor.VendorPanelScreen
import com.example.ui.theme.CanvasLight
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.KundarkiDeliveryTheme
import com.example.ui.theme.SurfaceWhite
import com.example.viewmodel.AdminTab
import com.example.viewmodel.CustomerTab
import com.example.viewmodel.KundarkiUiState
import com.example.viewmodel.KundarkiViewModel
import com.example.viewmodel.VendorTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KundarkiDeliveryTheme {
                KundarkiApp()
            }
        }
    }
}

@Composable
fun KundarkiApp(
    viewModel: KundarkiViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showRoleSheet by viewModel.showRoleSheet.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(950)
        showSplash = false
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight),
        contentAlignment = Alignment.TopCenter
    ) {
        val useNavigationRail = maxWidth >= 700.dp
        val showChromeNav = !state.showAuthScreen && state.openShopId == null

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp),
            containerColor = CanvasLight,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = InkPrimary,
                        contentColor = SurfaceWhite,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            bottomBar = {
                if (showChromeNav && !useNavigationRail) {
                    RoleBottomNavigationBar(
                        state = state,
                        onSelectCustomerTab = viewModel::selectCustomerTab,
                        onSelectVendorTab = viewModel::selectVendorTab,
                        onSelectAdminTab = viewModel::selectAdminTab,
                        onOpenRoleSheet = viewModel::openRoleSheet
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChromeNav && useNavigationRail) {
                    RoleNavigationRail(
                        state = state,
                        onSelectCustomerTab = viewModel::selectCustomerTab,
                        onSelectVendorTab = viewModel::selectVendorTab,
                        onSelectAdminTab = viewModel::selectAdminTab,
                        onOpenRoleSheet = viewModel::openRoleSheet
                    )
                    VerticalDivider(color = HairlineBorder)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (state.showAuthScreen) {
                        AuthScreen(
                            isLiveSupabase = state.isLiveSupabase,
                            isBusy = state.isAuthBusy,
                            promptMessage = state.authPromptMessage,
                            onLogin = viewModel::login,
                            onSignUp = viewModel::signUp,
                            onContinueAsGuest = viewModel::continueAsGuest
                        )
                    } else when (state.activeRole) {
                        UserRole.CUSTOMER -> {
                            val openShop = state.activeShop
                            if (openShop != null) {
                                val shopProducts = state.products.filter { it.shopId == openShop.id }
                                ShopMenuScreen(
                                    shop = openShop,
                                    isFavorite = openShop.id in state.favoriteShopIds,
                                    products = shopProducts,
                                    cartItems = state.cartItems,
                                    cartQuantities = state.cartQuantities,
                                    cartItemCount = state.cartPricing.totalItemCount,
                                    cartTotal = state.cartPricing.grandTotal,
                                    onBack = viewModel::closeShop,
                                    onToggleFavorite = { viewModel.toggleFavoriteShop(openShop) },
                                    onAddProduct = { product -> viewModel.addToCart(product, openShop) },
                                    onDecrementProduct = viewModel::decrementCart,
                                    onViewCart = viewModel::openCartFromFloatingBar
                                )
                            } else {
                                when (state.customerTab) {
                                    CustomerTab.HOME -> CustomerHomeScreen(
                                        state = state,
                                        onSelectNeighborhood = viewModel::selectNeighborhood,
                                        onSearchChange = viewModel::updateSearchQuery,
                                        onToggleDishShortcut = viewModel::toggleDishShortcut,
                                        onToggleVegOnly = viewModel::toggleVegOnlyFilter,
                                        onToggleRating4Plus = viewModel::toggleRating4PlusFilter,
                                        onToggleFavoritesOnly = viewModel::toggleFavoritesOnlyFilter,
                                        onResetFilters = viewModel::resetHomeFilters,
                                        onToggleFavoriteShop = viewModel::toggleFavoriteShop,
                                        onOpenShop = viewModel::openShop,
                                        onAddProduct = viewModel::addToCart,
                                        onDecrementProduct = viewModel::decrementCart,
                                        onOpenRoleSheet = viewModel::openRoleSheet,
                                        onViewCart = { viewModel.selectCustomerTab(CustomerTab.CART) },
                                        onTrackActiveOrder = { viewModel.selectCustomerTab(CustomerTab.ORDERS) },
                                        onRefreshFromSupabase = { viewModel.refreshFromSupabase() }
                                    )
                                    CustomerTab.CART -> CartCheckoutScreen(
                                        state = state,
                                        onSelectNeighborhood = viewModel::selectNeighborhood,
                                        onAddProduct = viewModel::addToCart,
                                        onDecrementProduct = viewModel::decrementCart,
                                        onClearCart = viewModel::clearCart,
                                        onBrowseShops = { viewModel.selectCustomerTab(CustomerTab.HOME) },
                                        onToggleInstruction = viewModel::toggleDeliveryInstruction,
                                        onCustomerNameChange = viewModel::updateCustomerName,
                                        onCustomerPhoneChange = viewModel::updateCustomerPhone,
                                        onAddressChange = viewModel::updateDeliveryAddress,
                                        onPaymentMethodChange = viewModel::updatePaymentMethod,
                                        onGpsDetectStart = { viewModel.setGpsDetecting(true) },
                                        onGpsDetected = viewModel::onLocationDetected,
                                        onGpsMessage = viewModel::emitToast,
                                        onPlaceOrder = viewModel::placeOrder
                                    )
                                    CustomerTab.ORDERS -> CustomerOrdersScreen(
                                        orders = state.orders,
                                        onExploreShops = { viewModel.selectCustomerTab(CustomerTab.HOME) },
                                        onReorder = viewModel::reorderFromOrder,
                                        onSubmitRating = viewModel::submitOrderRating
                                    )
                                    CustomerTab.ACCOUNT -> AccountRoleScreen(
                                        state = state,
                                        onSelectRole = viewModel::switchRole,
                                        onUpdateProfile = viewModel::updateProfile,
                                        onLogout = viewModel::logout,
                                        onOpenAuth = { viewModel.openAuthScreen() }
                                    )
                                }
                            }
                        }

                        UserRole.VENDOR -> VendorPanelScreen(
                            state = state,
                            onSelectShop = viewModel::selectVendorShop,
                            onToggleShopOpen = viewModel::toggleShopOpen,
                            onUpdateShopCategory = viewModel::updateShopCategory,
                            onToggleProductStock = viewModel::toggleProductStock,
                            onAddProduct = viewModel::addVendorProduct,
                            onUpdateOrderStatus = viewModel::updateOrderStatus,
                            onOpenEarningsTab = { viewModel.selectVendorTab(VendorTab.EARNINGS) },
                            onOpenRoleSheet = viewModel::openRoleSheet
                        )

                        UserRole.DELIVERY -> DeliveryPanelScreen(
                            state = state,
                            onSelectPartner = viewModel::selectDeliveryPartner,
                            onAcceptDelivery = viewModel::acceptDelivery,
                            onMarkDelivered = viewModel::markOrderDelivered,
                            onOpenRoleSheet = viewModel::openRoleSheet
                        )

                        UserRole.ADMIN -> AdminPanelScreen(
                            state = state,
                            onAddShop = viewModel::addShop,
                            onToggleShopOpen = viewModel::toggleShopOpen,
                            onUpdateShopCategory = viewModel::updateShopCategory,
                            onToggleShopSettlement = viewModel::toggleShopSettlement,
                            onOpenDailyHisab = { viewModel.selectAdminTab(AdminTab.HISAB) },
                            onAssignDeliveryBoy = viewModel::assignDeliveryBoy,
                            onSaveSettings = viewModel::saveAppSettings,
                            onOpenRoleSheet = viewModel::openRoleSheet
                        )
                    }
                }
            }
        }
    }

    if (showRoleSheet) {
        RolePickerBottomSheet(
            currentUser = state.currentUser,
            activeRole = state.activeRole,
            isLiveSupabase = state.isLiveSupabase,
            onSelectRole = viewModel::switchRole,
            onOpenAuth = {
                viewModel.dismissRoleSheet()
                viewModel.openAuthScreen()
            },
            onLogout = viewModel::logout,
            onDismiss = viewModel::dismissRoleSheet
        )
    }

    AnimatedVisibility(
        visible = showSplash,
        exit = fadeOut(animationSpec = tween(durationMillis = 320))
    ) {
        FatakseSplashScreen()
    }
}

@Composable
private fun RoleNavigationRail(
    state: KundarkiUiState,
    onSelectCustomerTab: (CustomerTab) -> Unit,
    onSelectVendorTab: (VendorTab) -> Unit,
    onSelectAdminTab: (AdminTab) -> Unit,
    onOpenRoleSheet: () -> Unit
) {
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = CrimsonPrimary,
        selectedTextColor = CrimsonPrimary,
        indicatorColor = CrimsonSoft,
        unselectedIconColor = InkSecondary,
        unselectedTextColor = InkSecondary
    )

    NavigationRail(
        containerColor = SurfaceWhite,
        header = {
            FatakseBrandBadge(
                size = 46.dp,
                cornerRadius = 12.dp,
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    ) {
        when (state.activeRole) {
            UserRole.CUSTOMER -> {
                NavigationRailItem(
                    selected = state.customerTab == CustomerTab.HOME,
                    onClick = { onSelectCustomerTab(CustomerTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (state.customerTab == CustomerTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    colors = railColors,
                    modifier = Modifier.testTag("rail_customer_home")
                )
                NavigationRailItem(
                    selected = state.customerTab == CustomerTab.CART,
                    onClick = { onSelectCustomerTab(CustomerTab.CART) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (state.cartPricing.totalItemCount > 0) {
                                    Badge(containerColor = CrimsonPrimary, contentColor = SurfaceWhite) {
                                        Text(state.cartPricing.totalItemCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (state.customerTab == CustomerTab.CART) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                                contentDescription = "Cart"
                            )
                        }
                    },
                    label = { Text("Cart") },
                    colors = railColors,
                    modifier = Modifier.testTag("rail_customer_cart")
                )
                NavigationRailItem(
                    selected = state.customerTab == CustomerTab.ORDERS,
                    onClick = { onSelectCustomerTab(CustomerTab.ORDERS) },
                    icon = {
                        Icon(
                            imageVector = if (state.customerTab == CustomerTab.ORDERS) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Orders"
                        )
                    },
                    label = { Text("Orders") },
                    colors = railColors,
                    modifier = Modifier.testTag("rail_customer_orders")
                )
                NavigationRailItem(
                    selected = state.customerTab == CustomerTab.ACCOUNT,
                    onClick = { onSelectCustomerTab(CustomerTab.ACCOUNT) },
                    icon = {
                        Icon(
                            imageVector = if (state.customerTab == CustomerTab.ACCOUNT) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Account"
                        )
                    },
                    label = { Text("Account") },
                    colors = railColors,
                    modifier = Modifier.testTag("rail_customer_account")
                )
            }

            UserRole.VENDOR -> {
                NavigationRailItem(
                    selected = state.vendorTab == VendorTab.ORDERS,
                    onClick = { onSelectVendorTab(VendorTab.ORDERS) },
                    icon = {
                        Icon(
                            imageVector = if (state.vendorTab == VendorTab.ORDERS) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Incoming Orders"
                        )
                    },
                    label = { Text("Orders") },
                    colors = railColors
                )
                NavigationRailItem(
                    selected = state.vendorTab == VendorTab.MENU,
                    onClick = { onSelectVendorTab(VendorTab.MENU) },
                    icon = {
                        Icon(
                            imageVector = if (state.vendorTab == VendorTab.MENU) Icons.Filled.RestaurantMenu else Icons.Outlined.RestaurantMenu,
                            contentDescription = "Menu Manager"
                        )
                    },
                    label = { Text("Menu") },
                    colors = railColors
                )
                NavigationRailItem(
                    selected = state.vendorTab == VendorTab.EARNINGS,
                    onClick = { onSelectVendorTab(VendorTab.EARNINGS) },
                    icon = {
                        Icon(
                            imageVector = if (state.vendorTab == VendorTab.EARNINGS) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Earnings"
                        )
                    },
                    label = { Text("Earnings") },
                    colors = railColors
                )
                NavigationRailItem(
                    selected = false,
                    onClick = onOpenRoleSheet,
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Switch Role") },
                    label = { Text("Roles") },
                    colors = railColors
                )
            }

            UserRole.DELIVERY -> {
                NavigationRailItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Filled.DeliveryDining, contentDescription = "Deliveries") },
                    label = { Text("Deliveries") },
                    colors = railColors
                )
                NavigationRailItem(
                    selected = false,
                    onClick = onOpenRoleSheet,
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Switch Role") },
                    label = { Text("Roles") },
                    colors = railColors
                )
            }

            UserRole.ADMIN -> {
                NavigationRailItem(
                    selected = state.adminTab == AdminTab.OVERVIEW,
                    onClick = { onSelectAdminTab(AdminTab.OVERVIEW) },
                    icon = {
                        Icon(
                            imageVector = if (state.adminTab == AdminTab.OVERVIEW) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Overview"
                        )
                    },
                    label = { Text("Overview") },
                    colors = railColors
                )
                NavigationRailItem(
                    selected = state.adminTab == AdminTab.HISAB,
                    onClick = { onSelectAdminTab(AdminTab.HISAB) },
                    icon = {
                        Icon(
                            imageVector = if (state.adminTab == AdminTab.HISAB) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Daily Hisab"
                        )
                    },
                    label = { Text("Daily Hisab") },
                    colors = railColors
                )
                NavigationRailItem(
                    selected = state.adminTab == AdminTab.DISPATCH,
                    onClick = { onSelectAdminTab(AdminTab.DISPATCH) },
                    icon = {
                        Icon(
                            imageVector = if (state.adminTab == AdminTab.DISPATCH) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                            contentDescription = "Dispatch"
                        )
                    },
                    label = { Text("Dispatch") },
                    colors = railColors
                )
                NavigationRailItem(
                    selected = state.adminTab == AdminTab.SETTINGS,
                    onClick = { onSelectAdminTab(AdminTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (state.adminTab == AdminTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = railColors
                )
            }
        }
    }
}

@Composable
private fun RoleBottomNavigationBar(
    state: KundarkiUiState,
    onSelectCustomerTab: (CustomerTab) -> Unit,
    onSelectVendorTab: (VendorTab) -> Unit,
    onSelectAdminTab: (AdminTab) -> Unit,
    onOpenRoleSheet: () -> Unit
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = CrimsonPrimary,
        selectedTextColor = CrimsonPrimary,
        indicatorColor = CrimsonSoft,
        unselectedIconColor = InkSecondary,
        unselectedTextColor = InkSecondary
    )

    NavigationBar(
        containerColor = SurfaceWhite,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (state.activeRole) {
            UserRole.CUSTOMER -> {
                NavigationBarItem(
                    selected = state.customerTab == CustomerTab.HOME,
                    onClick = { onSelectCustomerTab(CustomerTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (state.customerTab == CustomerTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_customer_home")
                )
                NavigationBarItem(
                    selected = state.customerTab == CustomerTab.CART,
                    onClick = { onSelectCustomerTab(CustomerTab.CART) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (state.cartPricing.totalItemCount > 0) {
                                    Badge(containerColor = CrimsonPrimary, contentColor = SurfaceWhite) {
                                        Text(state.cartPricing.totalItemCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (state.customerTab == CustomerTab.CART) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                                contentDescription = "Cart"
                            )
                        }
                    },
                    label = { Text("Cart") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_customer_cart")
                )
                NavigationBarItem(
                    selected = state.customerTab == CustomerTab.ORDERS,
                    onClick = { onSelectCustomerTab(CustomerTab.ORDERS) },
                    icon = {
                        Icon(
                            imageVector = if (state.customerTab == CustomerTab.ORDERS) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Orders"
                        )
                    },
                    label = { Text("Orders") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_customer_orders")
                )
                NavigationBarItem(
                    selected = state.customerTab == CustomerTab.ACCOUNT,
                    onClick = { onSelectCustomerTab(CustomerTab.ACCOUNT) },
                    icon = {
                        Icon(
                            imageVector = if (state.customerTab == CustomerTab.ACCOUNT) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Account"
                        )
                    },
                    label = { Text("Account") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_customer_account")
                )
            }

            UserRole.VENDOR -> {
                NavigationBarItem(
                    selected = state.vendorTab == VendorTab.ORDERS,
                    onClick = { onSelectVendorTab(VendorTab.ORDERS) },
                    icon = {
                        Icon(
                            imageVector = if (state.vendorTab == VendorTab.ORDERS) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Incoming Orders"
                        )
                    },
                    label = { Text("Orders") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_vendor_orders")
                )
                NavigationBarItem(
                    selected = state.vendorTab == VendorTab.MENU,
                    onClick = { onSelectVendorTab(VendorTab.MENU) },
                    icon = {
                        Icon(
                            imageVector = if (state.vendorTab == VendorTab.MENU) Icons.Filled.RestaurantMenu else Icons.Outlined.RestaurantMenu,
                            contentDescription = "Menu Manager"
                        )
                    },
                    label = { Text("Menu") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_vendor_menu")
                )
                NavigationBarItem(
                    selected = state.vendorTab == VendorTab.EARNINGS,
                    onClick = { onSelectVendorTab(VendorTab.EARNINGS) },
                    icon = {
                        Icon(
                            imageVector = if (state.vendorTab == VendorTab.EARNINGS) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Earnings"
                        )
                    },
                    label = { Text("Earnings") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_vendor_earnings")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenRoleSheet,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Switch Role"
                        )
                    },
                    label = { Text("Roles") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_vendor_roles")
                )
            }

            UserRole.DELIVERY -> {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.DeliveryDining,
                            contentDescription = "Deliveries"
                        )
                    },
                    label = { Text("Deliveries") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_delivery_active")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenRoleSheet,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Switch Role"
                        )
                    },
                    label = { Text("Switch Role") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_delivery_roles")
                )
            }

            UserRole.ADMIN -> {
                NavigationBarItem(
                    selected = state.adminTab == AdminTab.OVERVIEW,
                    onClick = { onSelectAdminTab(AdminTab.OVERVIEW) },
                    icon = {
                        Icon(
                            imageVector = if (state.adminTab == AdminTab.OVERVIEW) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Overview"
                        )
                    },
                    label = { Text("Overview") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_admin_overview")
                )
                NavigationBarItem(
                    selected = state.adminTab == AdminTab.HISAB,
                    onClick = { onSelectAdminTab(AdminTab.HISAB) },
                    icon = {
                        Icon(
                            imageVector = if (state.adminTab == AdminTab.HISAB) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Daily Hisab"
                        )
                    },
                    label = { Text("Daily Hisab") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_admin_hisab")
                )
                NavigationBarItem(
                    selected = state.adminTab == AdminTab.DISPATCH,
                    onClick = { onSelectAdminTab(AdminTab.DISPATCH) },
                    icon = {
                        Icon(
                            imageVector = if (state.adminTab == AdminTab.DISPATCH) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                            contentDescription = "Dispatch"
                        )
                    },
                    label = { Text("Dispatch") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_admin_dispatch")
                )
                NavigationBarItem(
                    selected = state.adminTab == AdminTab.SETTINGS,
                    onClick = { onSelectAdminTab(AdminTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (state.adminTab == AdminTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_admin_settings")
                )
            }
        }
    }
}
