package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithItems
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
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class OrderTimelineStep(
    val status: OrderStatus,
    val title: String,
    val activeSubtitle: String,
    val completedSubtitle: String,
    val pendingSubtitle: String,
    val icon: ImageVector,
    val offsetMinutes: Int
)

private fun buildTimelineSteps(order: OrderWithItems): List<OrderTimelineStep> = listOf(
    OrderTimelineStep(
        status = OrderStatus.PENDING,
        title = "Placed",
        activeSubtitle = "Waiting for ${order.shopName} to confirm",
        completedSubtitle = "Order received by ${order.shopName}",
        pendingSubtitle = "Awaiting placement",
        icon = Icons.Filled.ReceiptLong,
        offsetMinutes = 0
    ),
    OrderTimelineStep(
        status = OrderStatus.ACCEPTED,
        title = "Accepted by Shop",
        activeSubtitle = "${order.shopName} confirmed your order",
        completedSubtitle = "Confirmed & queued for kitchen",
        pendingSubtitle = "Shop confirmation pending",
        icon = Icons.Filled.CheckCircle,
        offsetMinutes = 2
    ),
    OrderTimelineStep(
        status = OrderStatus.PREPARING,
        title = "Preparing",
        activeSubtitle = "Freshly preparing ${order.items.sumOf { it.quantity }} item(s) now",
        completedSubtitle = "Packed fresh & sealed for pickup",
        pendingSubtitle = "Starts right after acceptance",
        icon = Icons.Filled.Restaurant,
        offsetMinutes = 6
    ),
    OrderTimelineStep(
        status = OrderStatus.OUT_FOR_DELIVERY,
        title = "Out for Delivery",
        activeSubtitle = "${order.deliveryBoyName ?: "Delivery partner"} is heading to your doorstep",
        completedSubtitle = "Picked up & delivered to your address",
        pendingSubtitle = "Rider will pick up once ready",
        icon = Icons.Filled.DeliveryDining,
        offsetMinutes = 15
    ),
    OrderTimelineStep(
        status = OrderStatus.DELIVERED,
        title = "Delivered",
        activeSubtitle = "Delivered to your doorstep — enjoy!",
        completedSubtitle = "Successfully delivered at doorstep",
        pendingSubtitle = "Final doorstep handoff",
        icon = Icons.Filled.Home,
        offsetMinutes = 23
    )
)

private fun etaHeadlineForStatus(status: OrderStatus): Pair<String, String> = when (status) {
    OrderStatus.PENDING -> "Confirming Order" to "Est. 22–25 mins"
    OrderStatus.ACCEPTED -> "Order Confirmed" to "Est. 18–22 mins"
    OrderStatus.PREPARING -> "Preparing Your Order" to "Est. 14–18 mins"
    OrderStatus.OUT_FOR_DELIVERY -> "Rider On The Way" to "Arriving in 6–10 mins"
    OrderStatus.DELIVERED -> "Order Delivered" to "Completed"
    OrderStatus.CANCELLED -> "Order Cancelled" to "Refund / Voided"
}

private fun nextOrderStatus(current: OrderStatus): OrderStatus? = when (current) {
    OrderStatus.PENDING -> OrderStatus.ACCEPTED
    OrderStatus.ACCEPTED -> OrderStatus.PREPARING
    OrderStatus.PREPARING -> OrderStatus.OUT_FOR_DELIVERY
    OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.DELIVERED
    OrderStatus.DELIVERED, OrderStatus.CANCELLED -> null
}

private fun formatStepTime(baseTimeMs: Long, offsetMinutes: Int): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date(baseTimeMs + offsetMinutes * 60_000L))
}

/**
 * Compact live status banner for the Customer Home screen whenever an active order exists.
 */
@Composable
fun ActiveOrderMiniTrackerBanner(
    order: OrderWithItems,
    onTrackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "mini_live_pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot"
    )
    val progress by animateFloatAsState(
        targetValue = ((order.status.stepIndex + 1).coerceIn(1, 5)) / 5f,
        animationSpec = tween(450),
        label = "mini_progress"
    )
    val (headline, eta) = etaHeadlineForStatus(order.status)

    Surface(
        onClick = onTrackClick,
        shape = RoundedCornerShape(18.dp),
        color = InkPrimary,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_live_order_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(FreshGreen.copy(alpha = 0.35f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(FreshGreen)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIVE • $headline",
                            style = MaterialTheme.typography.titleSmall,
                            color = SurfaceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${order.shopName} • $eta",
                            style = MaterialTheme.typography.bodySmall,
                            color = SurfaceWhite.copy(alpha = 0.78f)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CrimsonPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Track",
                            style = MaterialTheme.typography.labelMedium,
                            color = SurfaceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Track live order",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = FreshGreen,
                trackColor = SurfaceWhite.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
        }
    }
}

/**
 * Full real-time step-based order tracking card for the Customer panel,
 * visualizing progress from 'Placed' to 'Delivered' with animated steps,
 * live pulsing beacon, ETA banner, rider contact, and optional live demo step progression.
 */
@Composable
fun LiveOrderTrackingCard(
    order: OrderWithItems,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isActive = order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED
    val isDelivered = order.status == OrderStatus.DELIVERED
    val (headline, eta) = etaHeadlineForStatus(order.status)

    val progressFraction by animateFloatAsState(
        targetValue = when (order.status) {
            OrderStatus.CANCELLED -> 0f
            else -> ((order.status.stepIndex + 1).coerceIn(1, 5)) / 5f
        },
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "tracker_bar_progress"
    )

    val infinite = rememberInfiniteTransition(label = "live_beacon")
    val beaconScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SurfaceWhite,
        shadowElevation = 3.dp,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) CrimsonPrimary.copy(alpha = 0.35f) else HairlineBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_tracker_${order.orderCode}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Gradient Live Status Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when {
                            isDelivered -> Modifier.background(
                                Brush.linearGradient(colors = listOf(FreshGreen, FreshGreenDark))
                            )
                            order.status == OrderStatus.CANCELLED -> Modifier.background(
                                Brush.linearGradient(colors = listOf(InkSecondary, InkPrimary))
                            )
                            else -> Modifier.background(CrimsonPrimary)
                        }
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier.size(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .scale(beaconScale)
                                            .clip(CircleShape)
                                            .background(SurfaceWhite.copy(alpha = 0.35f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceWhite)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = SurfaceWhite.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isActive) "LIVE TRACKING • ${order.orderCode}" else order.orderCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SurfaceWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceWhite
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = if (isDelivered) FreshGreen else CrimsonPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = eta,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SurfaceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${order.shopName} • Step ${(order.status.stepIndex + 1).coerceIn(1, 5)} of 5",
                            style = MaterialTheme.typography.bodySmall,
                            color = SurfaceWhite.copy(alpha = 0.9f)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = SurfaceWhite,
                        trackColor = SurfaceWhite.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )
                }
            }

            // Step-Based Vertical Timeline Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (order.status == OrderStatus.CANCELLED) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cancel,
                                contentDescription = "Cancelled",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Order Cancelled",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkPrimary
                                )
                                Text(
                                    text = "This order was cancelled before delivery.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                        }
                    }
                } else {
                    StepBasedOrderTimeline(
                        order = order,
                        beaconScale = beaconScale
                    )
                }

                // Assigned Rider Live Card
                if (order.deliveryBoyName != null && isActive) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CanvasLight,
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(FreshGreenSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeliveryDining,
                                        contentDescription = null,
                                        tint = FreshGreen,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = order.deliveryBoyName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Fatakse Verified Delivery Partner",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSecondary
                                    )
                                }
                            }
                            if (!order.deliveryBoyPhone.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { dialPhoneNumber(context, order.deliveryBoyPhone) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, FreshGreen),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = "Call Rider",
                                        tint = FreshGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Call",
                                        style = MaterialTheme.typography.labelMedium,
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
    }
}

@Composable
fun StepBasedOrderTimeline(
    order: OrderWithItems,
    beaconScale: Float,
    modifier: Modifier = Modifier
) {
    val steps = buildTimelineSteps(order)
    val currentIdx = order.status.stepIndex

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        steps.forEachIndexed { index, step ->
            val isCompleted = currentIdx > index || (order.status == OrderStatus.DELIVERED && index == steps.lastIndex)
            val isCurrent = currentIdx == index && order.status != OrderStatus.DELIVERED
            val isLast = index == steps.lastIndex

            val nodeBgColor by animateColorAsState(
                targetValue = when {
                    isCompleted -> FreshGreen
                    isCurrent -> CrimsonPrimary
                    else -> SurfaceMuted
                },
                animationSpec = tween(300),
                label = "node_bg_$index"
            )
            val connectorColor by animateColorAsState(
                targetValue = if (currentIdx > index) FreshGreen else HairlineBorder,
                animationSpec = tween(350),
                label = "line_color_$index"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .testTag("timeline_step_${step.status.dbKey}"),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Vertical Node & Connector Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(34.dp)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(beaconScale.coerceAtMost(1.22f))
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary.copy(alpha = 0.22f))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(nodeBgColor)
                                .then(
                                    if (!isCompleted && !isCurrent) {
                                        Modifier.border(1.5.dp, HairlineBorder, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Filled.Check else step.icon,
                                contentDescription = step.title,
                                tint = if (isCompleted || isCurrent) SurfaceWhite else InkMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 2.dp)
                                .background(connectorColor, RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Right Step Text & Live Timestamp
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (isLast) 2.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = when {
                                    isCurrent -> CrimsonPrimary
                                    isCompleted -> InkPrimary
                                    else -> InkMuted
                                },
                                fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CrimsonSoft
                                ) {
                                    Text(
                                        text = "IN PROGRESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (isCompleted || isCurrent) {
                            Text(
                                text = formatStepTime(order.createdAt, step.offsetMinutes),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) CrimsonPrimary else InkMuted,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Text(
                        text = when {
                            isCurrent -> step.activeSubtitle
                            isCompleted -> step.completedSubtitle
                            else -> step.pendingSubtitle
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrent || isCompleted) InkSecondary else InkMuted
                    )
                }
            }
        }
    }
}
