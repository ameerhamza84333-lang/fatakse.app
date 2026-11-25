package com.example.ui.screens.delivery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithItems
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FatakseBrandBadge
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.RoleSwitcherPill
import com.example.ui.components.dialPhoneNumber
import com.example.ui.components.formatInr
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.openGoogleMapsDirections
import com.example.ui.theme.CanvasLight
import com.example.ui.theme.CrimsonPrimary
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
import com.example.ui.theme.WarmAmberSoft
import com.example.viewmodel.KundarkiUiState

@Composable
fun DeliveryPanelScreen(
    state: KundarkiUiState,
    onSelectPartner: (String) -> Unit,
    onAcceptDelivery: (OrderWithItems) -> Unit,
    onMarkDelivered: (OrderWithItems) -> Unit,
    onOpenRoleSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeRider = state.selectedDeliveryPartner

    val activeDeliveries = state.orders.filter {
        it.status == OrderStatus.PREPARING ||
            it.status == OrderStatus.OUT_FOR_DELIVERY ||
            it.status == OrderStatus.ACCEPTED
    }
    val completedByRider = state.orders.filter {
        it.status == OrderStatus.DELIVERED &&
            (activeRider == null || it.deliveryBoyId == activeRider.id)
    }
    val riderEarnings = completedByRider.sumOf { it.riderDeliveryFee }
    val todayCashCollected = completedByRider.sumOf { it.grandTotal }
    val netCashToAdmin = (todayCashCollected - riderEarnings).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        // Header + Rider Picker
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
                                text = "Delivery Partner",
                                style = MaterialTheme.typography.headlineSmall,
                                color = InkPrimary
                            )
                            Text(
                                text = activeRider?.let { "${it.name} • ${it.vehicleNumber}" } ?: "Fatakse Rider Fleet",
                                style = MaterialTheme.typography.bodySmall,
                                color = FreshGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    RoleSwitcherPill(
                        activeRole = state.activeRole,
                        onClick = onOpenRoleSheet
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.deliveryPartners, key = { it.id }) { partner ->
                        val selected = partner.id == activeRider?.id
                        Surface(
                            onClick = { onSelectPartner(partner.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) FreshGreen else SurfaceMuted
                        ) {
                            Text(
                                text = partner.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) SurfaceWhite else InkPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Rider Cash Collected & Delivery Earnings Summary Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = InkPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rider_hisab_summary_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Rider Hisab",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SurfaceWhite.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = "${completedByRider.size} delivered • ${activeDeliveries.size} ready/active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FreshGreenSoft
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FreshGreen
                            ) {
                                Text(
                                    text = "ONLINE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SurfaceWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceWhite.copy(alpha = 0.1f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Today's Cash Collected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SurfaceWhite.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = formatInr(todayCashCollected),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = WarmAmberSoft,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceWhite.copy(alpha = 0.1f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "My Delivery Earnings",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SurfaceWhite.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = formatInr(riderEarnings),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = FreshGreenSoft,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WarmAmber.copy(alpha = 0.18f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Net Cash to Submit to Admin",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SurfaceWhite.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = formatInr(netCashToAdmin),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = WarmAmberSoft,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Active & Ready for Pickup (${activeDeliveries.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary
                )
            }

            if (activeDeliveries.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.DeliveryDining,
                        title = "No active deliveries right now",
                        subtitle = "Orders marked Preparing or Out for Delivery appear here immediately."
                    )
                }
            } else {
                items(activeDeliveries, key = { it.id }) { order ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceWhite,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delivery_card_${order.orderCode}")
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${order.orderCode} • ${formatRelativeTime(order.createdAt)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CrimsonPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Collect ${formatInr(order.grandTotal)} • ${order.paymentMethod.label}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = "Rider Delivery Fee: ${formatInr(order.riderDeliveryFee)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FreshGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                OrderStatusChip(status = order.status)
                            }

                            HorizontalDivider(color = HairlineBorder)

                            // Pickup Address
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = "Pickup",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "PICKUP: ${order.shopName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = order.shopAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                            }

                            // Dropoff Address
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Dropoff",
                                    tint = FreshGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "DROP: ${order.customerName} (${order.customerPhone})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = order.deliveryAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                            }

                            // Call + Google Maps Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { dialPhoneNumber(context, order.customerPhone) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, FreshGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("delivery_call_${order.orderCode}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = "Call Customer",
                                        tint = FreshGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Call", color = FreshGreen)
                                }
                                OutlinedButton(
                                    onClick = {
                                        openGoogleMapsDirections(
                                            context = context,
                                            lat = order.deliveryLat,
                                            lng = order.deliveryLng
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, RoyalBlue),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .heightIn(min = 44.dp)
                                        .testTag("delivery_maps_${order.orderCode}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Directions,
                                        contentDescription = "Open in Google Maps",
                                        tint = RoyalBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Google Maps", color = RoyalBlue)
                                }
                            }

                            // Primary Action: Accept Delivery OR Mark Delivered
                            if (order.status != OrderStatus.OUT_FOR_DELIVERY) {
                                Button(
                                    onClick = { onAcceptDelivery(order) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("delivery_accept_${order.orderCode}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeliveryDining,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Accept Delivery & Pick Up")
                                }
                            } else {
                                Button(
                                    onClick = { onMarkDelivered(order) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FreshGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("delivery_complete_${order.orderCode}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Delivered")
                                }
                            }
                        }
                    }
                }
            }

            if (completedByRider.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completed Deliveries (${completedByRider.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkSecondary
                    )
                }
                items(completedByRider, key = { "done_${it.id}" }) { order ->
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
                                    text = "${order.orderCode} • ${order.shopName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkPrimary
                                )
                                Text(
                                    text = "Collected ${formatInr(order.grandTotal)} • ${order.customerName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "+${formatInr(order.riderDeliveryFee)}",
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
}
