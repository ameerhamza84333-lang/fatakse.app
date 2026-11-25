package com.example.ui.screens.vendor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithItems
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.ShopCategory
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FatakseBrandBadge
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.RoleSwitcherPill
import com.example.ui.components.ShopStatusPill
import com.example.ui.components.VegNonVegIndicator
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.formatInr
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.CanvasLight
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.FreshGreenDark
import com.example.ui.theme.FreshGreenSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberSoft
import com.example.viewmodel.KundarkiUiState
import com.example.viewmodel.VendorTab

@Composable
fun VendorPanelScreen(
    state: KundarkiUiState,
    onSelectShop: (String) -> Unit,
    onToggleShopOpen: (Shop) -> Unit,
    onUpdateShopCategory: (Shop, ShopCategory) -> Unit,
    onToggleProductStock: (Product) -> Unit,
    onAddProduct: (Shop, String, String, String, Boolean, String) -> Unit,
    onUpdateOrderStatus: (String, String, OrderStatus) -> Unit,
    onOpenEarningsTab: () -> Unit,
    onOpenRoleSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeShop = state.selectedVendorShop
    var showAddProductDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        // Vendor Header + Shop Selector Strip
        Surface(
            color = SurfaceWhite,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FatakseBrandBadge(size = 40.dp, cornerRadius = 10.dp)
                        Column {
                            Text(
                                text = "Vendor Dashboard",
                                style = MaterialTheme.typography.headlineSmall,
                                color = InkPrimary
                            )
                            Text(
                                text = activeShop?.name ?: "Select your shop",
                                style = MaterialTheme.typography.bodySmall,
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    RoleSwitcherPill(
                        activeRole = state.activeRole,
                        onClick = onOpenRoleSheet
                    )
                }

                // Switch between live Supabase shops
                if (state.shops.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.shops, key = { it.id }) { shop ->
                            val selected = shop.id == activeShop?.id
                            Surface(
                                onClick = { onSelectShop(shop.id) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) CrimsonPrimary else SurfaceMuted,
                                modifier = Modifier.testTag("vendor_shop_chip_${shop.id}")
                            ) {
                                Text(
                                    text = shop.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) SurfaceWhite else InkPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeShop == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Filled.Storefront,
                    title = "No Shops Available",
                    subtitle = "Add a shop in Supabase or the Admin Panel to manage menu items and orders."
                )
            }
            return
        }

        val shopOrders = state.orders.filter { it.shopId == activeShop.id }
        val shopProducts = state.products.filter { it.shopId == activeShop.id }
        val shopHisab = state.shopHisabSummaries.firstOrNull { it.shop.id == activeShop.id }

        when (state.vendorTab) {
            VendorTab.ORDERS -> VendorOrdersTab(
                orders = shopOrders,
                deliveredNetEarnings = shopHisab?.netPayableToVendor ?: 0,
                deliveredCount = shopHisab?.deliveredOrderCount ?: 0,
                isPayoutSettled = shopHisab?.isSettled == true,
                onOpenEarningsTab = onOpenEarningsTab,
                onUpdateStatus = onUpdateOrderStatus
            )
            VendorTab.MENU -> VendorMenuTab(
                shop = activeShop,
                products = shopProducts,
                onToggleShopOpen = { onToggleShopOpen(activeShop) },
                onUpdateShopCategory = { cat -> onUpdateShopCategory(activeShop, cat) },
                onToggleProductStock = onToggleProductStock,
                onOpenAddDialog = { showAddProductDialog = true }
            )
            VendorTab.EARNINGS -> VendorEarningsTab(
                shop = activeShop,
                orders = shopOrders,
                isPayoutSettled = shopHisab?.isSettled == true,
                settledAt = shopHisab?.settledAt
            )
        }
    }

    if (showAddProductDialog && activeShop != null) {
        AddProductDialog(
            shopName = activeShop.name,
            defaultCategory = activeShop.category,
            onDismiss = { showAddProductDialog = false },
            onConfirm = { name, price, category, isVeg, desc ->
                onAddProduct(activeShop, name, price, category, isVeg, desc)
                showAddProductDialog = false
            }
        )
    }
}

@Composable
private fun VendorOrdersTab(
    orders: List<OrderWithItems>,
    deliveredNetEarnings: Int,
    deliveredCount: Int,
    isPayoutSettled: Boolean,
    onOpenEarningsTab: () -> Unit,
    onUpdateStatus: (String, String, OrderStatus) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Compact Today's Net Earnings & Payout Status strip (taps into full Earnings & Hisab)
        item {
            Surface(
                onClick = onOpenEarningsTab,
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                border = BorderStroke(
                    1.dp,
                    if (isPayoutSettled) FreshGreen.copy(alpha = 0.45f) else WarmAmber.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vendor_hisab_summary_strip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Net Earnings ($deliveredCount delivered)",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                        Text(
                            text = formatInr(deliveredNetEarnings),
                            style = MaterialTheme.typography.titleLarge,
                            color = FreshGreenDark,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    VendorPayoutStatusPill(
                        hasPayout = deliveredNetEarnings > 0,
                        isSettled = isPayoutSettled
                    )
                }
            }
        }

        item {
            Text(
                text = "Incoming & Active Orders (${orders.size})",
                style = MaterialTheme.typography.titleLarge,
                color = InkPrimary
            )
        }

        if (orders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No incoming orders yet",
                    subtitle = "Place an order from the Customer panel to see live order alerts here."
                )
            }
        } else {
            items(orders, key = { it.id }) { order ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_order_${order.orderCode}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${order.orderCode} • ${formatRelativeTime(order.createdAt)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrimsonPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = order.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkPrimary
                                )
                            }
                            OrderStatusChip(status = order.status)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity} × ${item.productName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = formatInr(item.priceAtOrder * item.quantity),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkSecondary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = HairlineBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Items: ${formatInr(order.itemsTotal)} • ${order.paymentMethod.shortLabel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = InkPrimary
                                )
                                Text(
                                    text = "Comm (${order.commissionRate.toInt()}%): -${formatInr(order.platformCommission)} • Your Net: ${formatInr(order.vendorNetPayout)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FreshGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(onClick = { dialPhoneNumber(context, order.customerPhone) }) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = "Call customer",
                                    tint = FreshGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", color = FreshGreen)
                            }
                        }

                        // Action Buttons by Status
                        when (order.status) {
                            OrderStatus.PENDING -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onUpdateStatus(order.id, order.orderCode, OrderStatus.CANCELLED)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, CrimsonPrimary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp)
                                            .testTag("vendor_reject_${order.orderCode}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = CrimsonPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reject", color = CrimsonPrimary)
                                    }
                                    Button(
                                        onClick = {
                                            onUpdateStatus(order.id, order.orderCode, OrderStatus.ACCEPTED)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FreshGreen),
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .heightIn(min = 44.dp)
                                            .testTag("vendor_accept_${order.orderCode}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Accept Order")
                                    }
                                }
                            }
                            OrderStatus.ACCEPTED -> {
                                Button(
                                    onClick = {
                                        onUpdateStatus(order.id, order.orderCode, OrderStatus.PREPARING)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 46.dp)
                                        .testTag("vendor_preparing_${order.orderCode}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Fastfood,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Preparing")
                                }
                            }
                            OrderStatus.PREPARING -> {
                                Button(
                                    onClick = {
                                        onUpdateStatus(order.id, order.orderCode, OrderStatus.OUT_FOR_DELIVERY)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 46.dp)
                                        .testTag("vendor_ready_${order.orderCode}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalShipping,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ready for Pickup / Dispatch")
                                }
                            }
                            else -> {
                                Text(
                                    text = "Status: ${order.status.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorMenuTab(
    shop: Shop,
    products: List<Product>,
    onToggleShopOpen: () -> Unit,
    onUpdateShopCategory: (ShopCategory) -> Unit,
    onToggleProductStock: (Product) -> Unit,
    onOpenAddDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Shop Open/Closed Switch Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (shop.isOpen) FreshGreenSoft else CrimsonSoft,
                border = BorderStroke(1.dp, if (shop.isOpen) FreshGreen.copy(alpha = 0.4f) else CrimsonPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        ShopStatusPill(isOpen = shop.isOpen)
                        Text(
                            text = if (shop.isOpen) {
                                "Customers in Kundarki can place orders now"
                            } else {
                                "Shop is locked — ADD buttons disabled for customers"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    Switch(
                        checked = shop.isOpen,
                        onCheckedChange = { onToggleShopOpen() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceWhite,
                            checkedTrackColor = FreshGreen
                        ),
                        modifier = Modifier.testTag("vendor_shop_open_switch")
                    )
                }
            }
        }

        // Shop Category Selector Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Shop Category",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ShopCategory.selectableCategories) { cat ->
                            val selected = shop.category == cat
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateShopCategory(cat) },
                                label = {
                                    Text(
                                        text = cat.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonSoft,
                                    selectedLabelColor = CrimsonPrimary
                                ),
                                modifier = Modifier.testTag("vendor_shop_cat_${cat.dbKey}")
                            )
                        }
                    }
                }
            }
        }

        // Menu Header + Add Product Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Products (${products.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary
                )
                Button(
                    onClick = onOpenAddDialog,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("vendor_add_product_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add product",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
            }
        }

        items(products, key = { it.id }) { product ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VegNonVegIndicator(isVeg = product.isVeg)
                            Text(
                                text = product.categoryTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted
                            )
                        }
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                        Text(
                            text = formatInr(product.price),
                            style = MaterialTheme.typography.titleSmall,
                            color = CrimsonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Switch(
                            checked = product.inStock,
                            onCheckedChange = { onToggleProductStock(product) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SurfaceWhite,
                                checkedTrackColor = FreshGreen
                            ),
                            modifier = Modifier.testTag("vendor_stock_${product.id}")
                        )
                        Text(
                            text = if (product.inStock) "In Stock" else "Out of Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (product.inStock) FreshGreen else CrimsonPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorEarningsTab(
    shop: Shop,
    orders: List<OrderWithItems>,
    isPayoutSettled: Boolean,
    settledAt: Long?
) {
    val deliveredOrders = orders.filter { it.status == OrderStatus.DELIVERED }
    val inProgressOrders = orders.filter {
        it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED
    }
    val grossRevenue = deliveredOrders.sumOf { it.itemsTotal }
    val commissionAmount = deliveredOrders.sumOf { it.platformCommission }
    val netPayout = deliveredOrders.sumOf { it.vendorNetPayout }
    val inProgressNet = inProgressOrders.sumOf { it.vendorNetPayout }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CrimsonPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vendor_earnings_hisab_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = SurfaceWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Earnings & Hisab",
                                style = MaterialTheme.typography.labelLarge,
                                color = SurfaceWhite.copy(alpha = 0.95f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        VendorPayoutStatusPill(
                            hasPayout = netPayout > 0,
                            isSettled = isPayoutSettled
                        )
                    }

                    Text(
                        text = "Net Amount You Will Receive",
                        style = MaterialTheme.typography.labelSmall,
                        color = SurfaceWhite.copy(alpha = 0.85f)
                    )
                    Text(
                        text = formatInr(netPayout),
                        style = MaterialTheme.typography.displaySmall,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isPayoutSettled && settledAt != null) {
                            "Settled by Admin (${formatRelativeTime(settledAt)}) • ${deliveredOrders.size} delivered orders"
                        } else {
                            "From ${deliveredOrders.size} delivered orders (after ${shop.commissionRate.toInt()}% platform commission)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SurfaceWhite.copy(alpha = 0.88f)
                    )
                    if (inProgressOrders.isNotEmpty()) {
                        Text(
                            text = "+ ${formatInr(inProgressNet)} expected from ${inProgressOrders.size} in-progress order(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmAmberSoft,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VendorMetricCard(
                    label = "Delivered Orders",
                    value = deliveredOrders.size.toString(),
                    accent = RoyalBlue,
                    modifier = Modifier.weight(1f)
                )
                VendorMetricCard(
                    label = "Gross Item Sales",
                    value = formatInr(grossRevenue),
                    accent = FreshGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VendorMetricCard(
                    label = "Commission (${shop.commissionRate.toInt()}%)",
                    value = "- ${formatInr(commissionAmount)}",
                    accent = WarmAmber,
                    modifier = Modifier.weight(1f)
                )
                VendorMetricCard(
                    label = "Payout Status",
                    value = when {
                        netPayout <= 0 -> "No Dues"
                        isPayoutSettled -> "Settled"
                        else -> "Pending"
                    },
                    accent = if (isPayoutSettled) FreshGreen else WarmAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Delivered Order Hisab Log",
                style = MaterialTheme.typography.titleLarge,
                color = InkPrimary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (deliveredOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "No delivered orders yet",
                    subtitle = "Once an order is marked Delivered, its commission and net payout appear here."
                )
            }
        } else {
            items(deliveredOrders, key = { "earn_${it.id}" }) { order ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${order.orderCode} • ${order.customerName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary
                            )
                            Text(
                                text = "Items ${formatInr(order.itemsTotal)} • Comm (${order.commissionRate.toInt()}%) -${formatInr(order.platformCommission)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                        Text(
                            text = "+${formatInr(order.vendorNetPayout)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = FreshGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorPayoutStatusPill(
    hasPayout: Boolean,
    isSettled: Boolean
) {
    val bgColor = when {
        !hasPayout -> SurfaceMuted
        isSettled -> FreshGreenSoft
        else -> WarmAmberSoft
    }
    val textColor = when {
        !hasPayout -> InkMuted
        isSettled -> FreshGreenDark
        else -> androidx.compose.ui.graphics.Color(0xFFB45309)
    }
    val label = when {
        !hasPayout -> "No Dues"
        isSettled -> "● Settled"
        else -> "● Pending"
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun VendorMetricCard(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddProductDialog(
    shopName: String,
    defaultCategory: ShopCategory,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, String) -> Unit
) {
    val initialCat = if (defaultCategory == ShopCategory.ALL) ShopCategory.BIRYANI else defaultCategory
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCat) }
    var description by remember { mutableStateOf("") }
    var isVeg by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                text = "Add Item to $shopName",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    placeholder = { Text("e.g., Kurkure Paneer Momos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShopCategory.selectableCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonSoft,
                                selectedLabelColor = CrimsonPrimary
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VegNonVegIndicator(isVeg = isVeg)
                        Text(
                            text = if (isVeg) "Pure Veg Item" else "Non-Veg Item",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = isVeg,
                        onCheckedChange = { isVeg = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = FreshGreen)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, price, selectedCategory.label, isVeg, description) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Add Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = InkSecondary)
            }
        }
    )
}
