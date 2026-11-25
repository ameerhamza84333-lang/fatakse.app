package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithItems
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LiveOrderTrackingCard
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.OrderStatusTimeline
import com.example.ui.components.StepBasedOrderTimeline
import com.example.ui.components.formatInr
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.CanvasLight
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.FreshGreenDark
import com.example.ui.theme.FreshGreenSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberSoft

private enum class OrderListFilter(val label: String) {
    ALL("All"),
    ACTIVE("Live Active"),
    DELIVERED("Delivered")
}

@Composable
fun CustomerOrdersScreen(
    orders: List<OrderWithItems>,
    onExploreShops: () -> Unit,
    onReorder: (OrderWithItems) -> Unit,
    onSubmitRating: (String, String, Int, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var ratingOrderTarget by remember { mutableStateOf<OrderWithItems?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf(OrderListFilter.ALL) }
    var expandedOrderId by rememberSaveable { mutableStateOf<String?>(null) }

    val activeOrders = orders.filter {
        it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED
    }
    val pastOrders = orders.filter {
        it.status == OrderStatus.DELIVERED || it.status == OrderStatus.CANCELLED
    }

    val visibleActiveOrders = when (selectedFilter) {
        OrderListFilter.ALL, OrderListFilter.ACTIVE -> activeOrders
        OrderListFilter.DELIVERED -> emptyList()
    }
    val visiblePastOrders = when (selectedFilter) {
        OrderListFilter.ALL, OrderListFilter.DELIVERED -> pastOrders
        OrderListFilter.ACTIVE -> emptyList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Filter Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(
                        text = "Live Order Tracking",
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time step-by-step progress from Placed to Delivered",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderListFilter.entries.forEach { filter ->
                        val selected = filter == selectedFilter
                        val count = when (filter) {
                            OrderListFilter.ALL -> orders.size
                            OrderListFilter.ACTIVE -> activeOrders.size
                            OrderListFilter.DELIVERED -> pastOrders.size
                        }
                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) CrimsonPrimary else SurfaceWhite,
                            border = BorderStroke(
                                1.dp,
                                if (selected) CrimsonPrimary else HairlineBorder
                            ),
                            modifier = Modifier.testTag("orders_filter_${filter.name.lowercase()}")
                        ) {
                            Text(
                                text = "${filter.label} ($count)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) SurfaceWhite else InkPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No orders placed yet",
                    subtitle = "Place your first order to track preparation and doorstep delivery live.",
                    actionLabel = "Order Now",
                    onAction = onExploreShops
                )
            }
        } else {
            // Active Live Orders with full Step-Based Tracking Cards
            if (visibleActiveOrders.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Deliveries (${visibleActiveOrders.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(visibleActiveOrders, key = { "live_${it.id}" }) { order ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveOrderTrackingCard(
                            order = order
                        )
                        OrderSummaryDetailsCard(
                            order = order,
                            isExpanded = expandedOrderId == order.id,
                            onToggleExpand = {
                                expandedOrderId = if (expandedOrderId == order.id) null else order.id
                            }
                        )
                    }
                }
            }

            // Past Delivered / Cancelled Orders
            if (visiblePastOrders.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Past Orders (${visiblePastOrders.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(visiblePastOrders, key = { it.id }) { order ->
                    PastOrderCard(
                        order = order,
                        isTimelineExpanded = expandedOrderId == order.id,
                        onToggleTimeline = {
                            expandedOrderId = if (expandedOrderId == order.id) null else order.id
                        },
                        onRateClick = { ratingOrderTarget = order },
                        onReorder = { onReorder(order) }
                    )
                }
            }

            if (visibleActiveOrders.isEmpty() && visiblePastOrders.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.ReceiptLong,
                        title = "No ${selectedFilter.label.lowercase()} orders",
                        subtitle = "Switch filter tabs or explore Kundarki shops to place a new order.",
                        actionLabel = "Explore Shops",
                        onAction = onExploreShops
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    ratingOrderTarget?.let { target ->
        OrderRatingDialog(
            order = target,
            onDismiss = { ratingOrderTarget = null },
            onSubmit = { shopStars, riderStars, note ->
                onSubmitRating(target.id, target.orderCode, shopStars, riderStars, note)
                ratingOrderTarget = null
            }
        )
    }
}

@Composable
private fun OrderSummaryDetailsCard(
    order: OrderWithItems,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Items (${order.items.sumOf { it.quantity }}) • ${formatInr(order.grandTotal)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide" else "Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle order details",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = HairlineBorder)
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity} × ${item.productName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatInr(item.priceAtOrder * item.quantity),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Column {
                            Text(
                                text = order.deliveryAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                            if (order.deliveryInstructions.isNotBlank()) {
                                Text(
                                    text = "Note: ${order.deliveryInstructions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkMuted
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceMuted
                    ) {
                        Text(
                            text = "Payment: ${order.paymentMethod.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PastOrderCard(
    order: OrderWithItems,
    isTimelineExpanded: Boolean,
    onToggleTimeline: () -> Unit,
    onRateClick: () -> Unit,
    onReorder: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.orderCode}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = order.orderCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = CrimsonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• ${formatRelativeTime(order.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                    }
                    Text(
                        text = order.shopName,
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                OrderStatusChip(status = order.status)
            }

            // Compact 5-step progress strip + expandable full step-based timeline
            OrderStatusTimeline(currentStatus = order.status)

            Surface(
                onClick = onToggleTimeline,
                shape = RoundedCornerShape(10.dp),
                color = CanvasLight,
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
                        text = if (isTimelineExpanded) "Hide Full Step Timeline" else "View Full Step Timeline",
                        style = MaterialTheme.typography.labelMedium,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isTimelineExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isTimelineExpanded && order.status != OrderStatus.CANCELLED) {
                StepBasedOrderTimeline(
                    order = order,
                    beaconScale = 1f,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(color = HairlineBorder)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity} × ${item.productName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatInr(item.priceAtOrder * item.quantity),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceMuted
                ) {
                    Text(
                        text = order.paymentMethod.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "Total: ${formatInr(order.grandTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (order.status == OrderStatus.DELIVERED) {
                if (order.shopRating != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WarmAmberSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = WarmAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Rated ${order.shopRating}★ Shop • ${order.riderRating ?: 5}★ Rider",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (order.shopRating == null) {
                        OutlinedButton(
                            onClick = onRateClick,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, WarmAmber),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("rate_order_${order.orderCode}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Rate Order",
                                tint = WarmAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rate Order",
                                color = InkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = onReorder,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("reorder_${order.orderCode}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = "Reorder",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Reorder", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRatingDialog(
    order: OrderWithItems,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int, String) -> Unit
) {
    var shopStars by remember { mutableIntStateOf(5) }
    var riderStars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Column {
                Text(
                    text = "Rate Your Delivery",
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkPrimary
                )
                Text(
                    text = "${order.shopName} • ${order.orderCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Food & Packaging Quality",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary
                    )
                    StarRatingBar(
                        rating = shopStars,
                        onRatingChange = { shopStars = it }
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Delivery Partner (${order.deliveryBoyName ?: "Rider"})",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary
                    )
                    StarRatingBar(
                        rating = riderStars,
                        onRatingChange = { riderStars = it }
                    )
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Quick feedback (optional)") },
                    placeholder = { Text("Hot food, super fast delivery!") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(shopStars, riderStars, comment) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Submit Rating")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = InkSecondary)
            }
        }
    )
}

@Composable
private fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (star in 1..5) {
            val selected = star <= rating
            Icon(
                imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "$star stars",
                tint = if (selected) WarmAmber else InkMuted,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { onRatingChange(star) }
            )
        }
    }
}
