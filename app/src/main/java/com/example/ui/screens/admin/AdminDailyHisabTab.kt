package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.data.model.RiderHisabSummary
import com.example.data.model.ShopHisabSummary
import com.example.ui.components.formatInr
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.CrimsonPrimary
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

@Composable
fun AdminDailyHisabTab(
    state: KundarkiUiState,
    onToggleShopSettlement: (ShopHisabSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val deliveredOrders = state.orders.filter { it.status == OrderStatus.DELIVERED }
    val shopHisabs = state.shopHisabSummaries
    val riderHisabs = state.riderHisabSummaries

    val totalGrossSales = deliveredOrders.sumOf { it.grandTotal }
    val totalAdminCommission = deliveredOrders.sumOf { it.platformCommission }
    val totalVendorPayable = shopHisabs.sumOf { it.netPayableToVendor }
    val pendingVendorPayable = shopHisabs.filter { !it.isSettled }.sumOf { it.netPayableToVendor }
    val totalRiderCashInHand = riderHisabs.sumOf { it.cashCollected }
    val totalNetCashFromRiders = riderHisabs.sumOf { it.netCashToAdmin }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Daily Hisab & Settlement",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Automatic breakdown across ${deliveredOrders.size} delivered Cash / UPI QR orders",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }

        // 2x2 Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HisabMetricCard(
                    label = "Total Gross Sales",
                    value = formatInr(totalGrossSales),
                    subtitle = "${deliveredOrders.size} delivered orders",
                    accent = CrimsonPrimary,
                    modifier = Modifier.weight(1f)
                )
                HisabMetricCard(
                    label = "Admin Commission",
                    value = formatInr(totalAdminCommission),
                    subtitle = "15% Food • 10% Grocery",
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
                HisabMetricCard(
                    label = "Vendor Payable",
                    value = formatInr(totalVendorPayable),
                    subtitle = "Pending: ${formatInr(pendingVendorPayable)}",
                    accent = RoyalBlue,
                    modifier = Modifier.weight(1f)
                )
                HisabMetricCard(
                    label = "Rider Cash to Collect",
                    value = formatInr(totalNetCashFromRiders),
                    subtitle = "Collected ${formatInr(totalRiderCashInHand)} - Fees",
                    accent = WarmAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Shop-wise Payout Table Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
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
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "Shop-wise Vendor Payouts",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${shopHisabs.count { it.isSettled }}/${shopHisabs.count { it.deliveredOrderCount > 0 }} Settled",
                    style = MaterialTheme.typography.labelMedium,
                    color = FreshGreenDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (shopHisabs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No shops in Supabase yet. Add a shop to start tracking vendor payouts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(shopHisabs, key = { "hisab_shop_${it.shop.id}" }) { summary ->
                ShopHisabPayoutCard(
                    summary = summary,
                    onToggleSettled = { onToggleShopSettlement(summary) }
                )
            }
        }

        // Rider Cash Collection Tracker Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeliveryDining,
                    contentDescription = null,
                    tint = FreshGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Rider Cash Collection Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Cash collected from customers minus rider delivery fee share = exact net cash due to Admin",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }

        if (riderHisabs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No delivery partners registered in Supabase yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(riderHisabs, key = { "hisab_rider_${it.partner.id}" }) { riderSummary ->
                RiderCashTrackerCard(summary = riderSummary)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun HisabMetricCard(
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted
            )
        }
    }
}

@Composable
private fun ShopHisabPayoutCard(
    summary: ShopHisabSummary,
    onToggleSettled: () -> Unit
) {
    val hasDues = summary.netPayableToVendor > 0
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            when {
                !hasDues -> HairlineBorder
                summary.isSettled -> FreshGreen.copy(alpha = 0.45f)
                else -> WarmAmber.copy(alpha = 0.55f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hisab_shop_card_${summary.shop.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.shop.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.deliveredOrderCount} delivered • ${summary.shop.category.label} (${summary.shop.commissionRate.toInt()}% comm)",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                SettlementStatusBadge(
                    hasDues = hasDues,
                    isSettled = summary.isSettled
                )
            }

            // Breakdown Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceMuted.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Item Sales",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                        Text(
                            text = formatInr(summary.grossItemSales),
                            style = MaterialTheme.typography.titleSmall,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Commission",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                        Text(
                            text = "-${formatInr(summary.commissionDeducted)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = CrimsonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Net Payable",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                        Text(
                            text = formatInr(summary.netPayableToVendor),
                            style = MaterialTheme.typography.titleMedium,
                            color = FreshGreenDark,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Mark Settled / Paid Toggle Button
            if (hasDues) {
                if (summary.isSettled) {
                    OutlinedButton(
                        onClick = onToggleSettled,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FreshGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("mark_settled_${summary.shop.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Settled",
                            tint = FreshGreen,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = summary.settledAt?.let {
                                "Settled ${formatRelativeTime(it)} • Tap to Mark Pending"
                            } ?: "Settled • Tap to Mark Pending",
                            style = MaterialTheme.typography.labelLarge,
                            color = FreshGreenDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onToggleSettled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FreshGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("mark_settled_${summary.shop.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = "Mark Settled",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark Settled (${formatInr(summary.netPayableToVendor)})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementStatusBadge(
    hasDues: Boolean,
    isSettled: Boolean
) {
    val bgColor = when {
        !hasDues -> SurfaceMuted
        isSettled -> FreshGreenSoft
        else -> WarmAmberSoft
    }
    val fgColor = when {
        !hasDues -> InkMuted
        isSettled -> FreshGreenDark
        else -> Color(0xFFB45309)
    }
    val dotColor = when {
        !hasDues -> InkMuted
        isSettled -> FreshGreen
        else -> WarmAmber
    }
    val text = when {
        !hasDues -> "No Orders"
        isSettled -> "Settled"
        else -> "Pending"
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RiderCashTrackerCard(
    summary: RiderHisabSummary
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hisab_rider_card_${summary.partner.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.partner.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.partner.vehicleNumber} • ${summary.deliveredOrderCount} delivered",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (summary.netCashToAdmin > 0) WarmAmberSoft else SurfaceMuted
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (summary.netCashToAdmin > 0) Icons.Filled.Schedule else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (summary.netCashToAdmin > 0) Color(0xFFB45309) else FreshGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (summary.netCashToAdmin > 0) {
                                "Collect ${formatInr(summary.netCashToAdmin)}"
                            } else {
                                "All Clear"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (summary.netCashToAdmin > 0) Color(0xFFB45309) else FreshGreenDark,
                            fontWeight = FontWeight.Bold
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
                Column {
                    Text(
                        text = "Cash Collected",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted
                    )
                    Text(
                        text = formatInr(summary.cashCollected),
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Minus Rider Fee",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted
                    )
                    Text(
                        text = "-${formatInr(summary.deliveryFeeEarned)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = FreshGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Cash Due",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted
                    )
                    Text(
                        text = formatInr(summary.netCashToAdmin),
                        style = MaterialTheme.typography.titleMedium,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
