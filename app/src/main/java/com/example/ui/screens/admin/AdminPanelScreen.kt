package com.example.ui.screens.admin

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.KundarkiConfig
import com.example.data.model.AppSettings
import com.example.data.model.DeliveryPartner
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithItems
import com.example.data.model.Shop
import com.example.data.model.ShopCategory
import com.example.data.model.ShopHisabSummary
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FatakseBrandBadge
import com.example.ui.components.ModeBadge
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.RoleSwitcherPill
import com.example.ui.components.ShopStatusPill
import com.example.ui.components.formatInr
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.CanvasLight
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.FreshGreenSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.WarmAmber
import com.example.viewmodel.AdminTab
import com.example.viewmodel.KundarkiUiState
import kotlin.math.roundToInt

@Composable
fun AdminPanelScreen(
    state: KundarkiUiState,
    onAddShop: (String, ShopCategory, String, String, String, Boolean) -> Unit,
    onToggleShopOpen: (Shop) -> Unit,
    onUpdateShopCategory: (Shop, ShopCategory) -> Unit,
    onToggleShopSettlement: (ShopHisabSummary) -> Unit,
    onOpenDailyHisab: () -> Unit,
    onAssignDeliveryBoy: (OrderWithItems, DeliveryPartner?) -> Unit,
    onSaveSettings: (Int, Int, Int, Int, String) -> Unit,
    onOpenRoleSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        Surface(
            color = SurfaceWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            text = "Fatakse Admin",
                            style = MaterialTheme.typography.headlineSmall,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ModeBadge(isLiveSupabase = state.isLiveSupabase)
                    }
                }
                RoleSwitcherPill(
                    activeRole = state.activeRole,
                    onClick = onOpenRoleSheet
                )
            }
        }

        when (state.adminTab) {
            AdminTab.OVERVIEW -> AdminOverviewTab(
                state = state,
                onAddShop = onAddShop,
                onToggleShopOpen = onToggleShopOpen,
                onUpdateShopCategory = onUpdateShopCategory,
                onOpenDailyHisab = onOpenDailyHisab
            )
            AdminTab.HISAB -> AdminDailyHisabTab(
                state = state,
                onToggleShopSettlement = onToggleShopSettlement
            )
            AdminTab.DISPATCH -> AdminDispatchTab(
                orders = state.orders,
                partners = state.deliveryPartners,
                onAssign = onAssignDeliveryBoy
            )
            AdminTab.SETTINGS -> AdminSettingsTab(
                settings = state.settings,
                onSave = onSaveSettings
            )
        }
    }
}

@Composable
private fun AdminOverviewTab(
    state: KundarkiUiState,
    onAddShop: (String, ShopCategory, String, String, String, Boolean) -> Unit,
    onToggleShopOpen: (Shop) -> Unit,
    onUpdateShopCategory: (Shop, ShopCategory) -> Unit,
    onOpenDailyHisab: () -> Unit
) {
    var showAddShopDialog by remember { mutableStateOf(false) }
    var shopToEditCategory by remember { mutableStateOf<Shop?>(null) }
    val activeOrders = state.orders.filter { it.status != OrderStatus.CANCELLED }
    val totalGmv = activeOrders.sumOf { it.grandTotal }
    val netCommission = activeOrders.sumOf { it.platformCommission }
    val totalVendorPayable = activeOrders.sumOf { it.vendorNetPayout }
    val totalRiderFees = activeOrders.sumOf { it.riderDeliveryFee }
    val topItems = state.topSellingItems

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Fatakse Platform Metrics",
                style = MaterialTheme.typography.titleLarge,
                color = InkPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiCard(
                    label = "Total Orders",
                    value = activeOrders.size.toString(),
                    accent = RoyalBlue,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    label = "Total GMV Sales",
                    value = formatInr(totalGmv),
                    accent = CrimsonPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiCard(
                    label = "Platform Commission",
                    value = formatInr(netCommission),
                    accent = FreshGreen,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    label = "Vendor Net Payout",
                    value = formatInr(totalVendorPayable),
                    accent = RoyalBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            AdminKpiCard(
                label = "Total Rider Delivery Fees",
                value = formatInr(totalRiderFees),
                accent = WarmAmber,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Daily Hisab Quick-Access Banner Card
        item {
            val pendingVendorTotal = state.shopHisabSummaries
                .filter { !it.isSettled }
                .sumOf { it.netPayableToVendor }
            val riderNetCash = state.riderHisabSummaries.sumOf { it.netCashToAdmin }
            Surface(
                onClick = onOpenDailyHisab,
                shape = RoundedCornerShape(18.dp),
                color = InkPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_overview_hisab_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = "Open Daily Hisab",
                                tint = SurfaceWhite,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Daily Hisab & Cash Settlement",
                                style = MaterialTheme.typography.titleSmall,
                                color = SurfaceWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vendor Pending: ${formatInr(pendingVendorTotal)} • Rider Cash Due: ${formatInr(riderNetCash)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FreshGreenSoft
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Daily Hisab",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Top-Selling Items in Kundarki
        if (topItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Top-Selling Items in Kundarki",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        topItems.forEachIndexed { idx, item ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = HairlineBorder,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (idx == 0) CrimsonPrimary else CrimsonSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (idx == 0) SurfaceWhite else CrimsonPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = item.productName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = InkPrimary
                                        )
                                        Text(
                                            text = "${item.totalUnits} units ordered",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = formatInr(item.totalRevenue),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FreshGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Partner Shops (${state.shops.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddShopDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("admin_add_shop_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add shop",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Shop", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        items(state.shops, key = { it.id }) { shop ->
            val shopOrders = activeOrders.filter { it.shopId == shop.id }
            val shopSales = shopOrders.sumOf { it.itemsTotal }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shop.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${shopOrders.size} orders • ${shop.commissionRate.toInt()}% comm • Sales ${formatInr(shopSales)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                        ShopStatusPill(isOpen = shop.isOpen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { shopToEditCategory = shop },
                            shape = RoundedCornerShape(999.dp),
                            color = CrimsonSoft,
                            border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("admin_shop_cat_${shop.id}")
                        ) {
                            Text(
                                text = "${shop.category.label} ▾",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (shop.isOpen) "Open" else "Closed",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (shop.isOpen) FreshGreen else InkMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = shop.isOpen,
                                onCheckedChange = { onToggleShopOpen(shop) },
                                colors = SwitchDefaults.colors(checkedTrackColor = FreshGreen)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddShopDialog) {
        AdminAddShopDialog(
            onDismiss = { showAddShopDialog = false },
            onConfirm = { name, category, address, deliveryTime, tagline, isOpen ->
                onAddShop(name, category, address, deliveryTime, tagline, isOpen)
                showAddShopDialog = false
            }
        )
    }

    shopToEditCategory?.let { targetShop ->
        AdminEditShopCategoryDialog(
            shop = targetShop,
            onDismiss = { shopToEditCategory = null },
            onSelectCategory = { cat ->
                onUpdateShopCategory(targetShop, cat)
                shopToEditCategory = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminAddShopDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ShopCategory, String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ShopCategory.MOMOS_FAST_FOOD) }
    var selectedArea by remember { mutableStateOf(KundarkiConfig.neighborhoods.first()) }
    var deliveryTime by remember { mutableStateOf("15-20 MINS") }
    var tagline by remember { mutableStateOf("") }
    var isOpen by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                text = "Add Partner Shop",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
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
                    label = { Text("Shop Name") },
                    placeholder = { Text("e.g., Kundarki Spice Momos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Select Shop Category",
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
                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonSoft,
                                selectedLabelColor = CrimsonPrimary
                            )
                        )
                    }
                }
                Text(
                    text = "Kundarki Location",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    KundarkiConfig.neighborhoods.forEach { area ->
                        FilterChip(
                            selected = selectedArea == area,
                            onClick = { selectedArea = area },
                            label = { Text(area, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonSoft,
                                selectedLabelColor = CrimsonPrimary
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = deliveryTime,
                    onValueChange = { deliveryTime = it },
                    label = { Text("Delivery ETA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Specialties / Tagline (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOpen) "Open Now" else "Start Closed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isOpen,
                        onCheckedChange = { isOpen = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = FreshGreen)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedCategory, selectedArea, deliveryTime, tagline, isOpen) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Add Shop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = InkSecondary)
            }
        }
    )
}

@Composable
private fun AdminEditShopCategoryDialog(
    shop: Shop,
    onDismiss: () -> Unit,
    onSelectCategory: (ShopCategory) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                text = "Category for ${shop.name}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShopCategory.selectableCategories.forEach { cat ->
                    val selected = shop.category == cat
                    Surface(
                        onClick = { onSelectCategory(cat) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) CrimsonSoft else SurfaceMuted,
                        border = BorderStroke(1.dp, if (selected) CrimsonPrimary else HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = InkSecondary)
            }
        }
    )
}

@Composable
private fun AdminDispatchTab(
    orders: List<OrderWithItems>,
    partners: List<DeliveryPartner>,
    onAssign: (OrderWithItems, DeliveryPartner?) -> Unit
) {
    var orderToAssign by remember { mutableStateOf<OrderWithItems?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Live Order Dispatch (${orders.size})",
                style = MaterialTheme.typography.titleLarge,
                color = InkPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        if (orders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No orders to dispatch",
                    subtitle = "Incoming orders across all Kundarki shops will appear here."
                )
            }
        } else {
            items(orders, key = { it.id }) { order ->
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
                                    text = "${order.shopName} → ${order.customerName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkPrimary
                                )
                            }
                            OrderStatusChip(status = order.status)
                        }

                        Text(
                            text = "${order.deliveryAddress} • Total ${formatInr(order.grandTotal)} (${order.paymentMethod.shortLabel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceMuted,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Comm (${order.commissionRate.toInt()}%): ${formatInr(order.platformCommission)} • Vendor Net: ${formatInr(order.vendorNetPayout)} • Rider Fee: ${formatInr(order.riderDeliveryFee)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }

                        HorizontalDivider(color = HairlineBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeliveryDining,
                                    contentDescription = null,
                                    tint = if (order.deliveryBoyName != null) FreshGreen else InkMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = order.deliveryBoyName ?: "Unassigned Rider",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (order.deliveryBoyName != null) InkPrimary else CrimsonPrimary
                                )
                            }
                            OutlinedButton(
                                onClick = { orderToAssign = order },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CrimsonPrimary),
                                modifier = Modifier.testTag("admin_assign_${order.orderCode}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AssignmentInd,
                                    contentDescription = "Assign rider",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (order.deliveryBoyName != null) "Reassign" else "Assign Rider",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrimsonPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    orderToAssign?.let { targetOrder ->
        AlertDialog(
            onDismissRequest = { orderToAssign = null },
            containerColor = SurfaceWhite,
            title = {
                Text(
                    text = "Assign Rider for ${targetOrder.orderCode}",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    partners.forEach { partner ->
                        val isAssigned = targetOrder.deliveryBoyId == partner.id
                        Surface(
                            onClick = {
                                onAssign(targetOrder, partner)
                                orderToAssign = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAssigned) CrimsonSoft else SurfaceMuted,
                            border = BorderStroke(
                                1.dp,
                                if (isAssigned) CrimsonPrimary else HairlineBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = partner.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = "${partner.vehicleNumber} • ${partner.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                                if (isAssigned) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Assigned",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { orderToAssign = null }) {
                    Text("Close", color = InkSecondary)
                }
            }
        )
    }
}

@Composable
private fun AdminSettingsTab(
    settings: AppSettings,
    onSave: (Int, Int, Int, Int, String) -> Unit
) {
    var minOrderText by remember { mutableStateOf(settings.minOrderAmount.toString()) }
    var deliveryFeeText by remember { mutableStateOf(settings.baseDeliveryFee.toString()) }
    var freeAboveText by remember { mutableStateOf(settings.freeDeliveryAbove.toString()) }
    var platformFeeText by remember { mutableStateOf(settings.platformFee.toString()) }
    var whatsappText by remember { mutableStateOf(settings.supportWhatsapp) }

    LaunchedEffect(settings) {
        minOrderText = settings.minOrderAmount.toString()
        deliveryFeeText = settings.baseDeliveryFee.toString()
        freeAboveText = settings.freeDeliveryAbove.toString()
        platformFeeText = settings.platformFee.toString()
        whatsappText = settings.supportWhatsapp
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Platform Settings Editor",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Changes immediately update Customer checkout bills & banners",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minOrderText,
                        onValueChange = { minOrderText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minimum Order Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_min_order_input")
                    )
                    OutlinedTextField(
                        value = deliveryFeeText,
                        onValueChange = { deliveryFeeText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Base Delivery Fee (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_delivery_fee_input")
                    )
                    OutlinedTextField(
                        value = freeAboveText,
                        onValueChange = { freeAboveText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Free Delivery Above (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_free_above_input")
                    )
                    OutlinedTextField(
                        value = platformFeeText,
                        onValueChange = { platformFeeText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Platform Fee (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_platform_fee_input")
                    )
                    OutlinedTextField(
                        value = whatsappText,
                        onValueChange = { whatsappText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Support WhatsApp Number (with country code)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            onSave(
                                minOrderText.toIntOrNull() ?: settings.minOrderAmount,
                                deliveryFeeText.toIntOrNull() ?: settings.baseDeliveryFee,
                                freeAboveText.toIntOrNull() ?: settings.freeDeliveryAbove,
                                platformFeeText.toIntOrNull() ?: settings.platformFee,
                                whatsappText
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("admin_save_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save Settings",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Platform Settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    label: String,
    value: String,
    accent: Color,
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
