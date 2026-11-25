package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.KundarkiConfig
import com.example.data.model.OrderStatus
import com.example.data.model.ShopCategory
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
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
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberSoft
import java.text.NumberFormat
import java.util.Locale

private val indianLocale = Locale("en", "IN")

fun formatInr(amount: Int): String {
    val formatter = NumberFormat.getIntegerInstance(indianLocale)
    return "₹${formatter.format(amount)}"
}

fun formatRelativeTime(timestampMs: Long): String {
    val diffMinutes = ((System.currentTimeMillis() - timestampMs) / 60_000L).coerceAtLeast(0L)
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "$diffMinutes min ago"
        diffMinutes < 1440 -> "${diffMinutes / 60} hr ago"
        else -> "${diffMinutes / 1440}d ago"
    }
}

fun openWhatsAppOrder(context: Context, phone: String, message: String) {
    val cleanNumber = phone.filter { it.isDigit() }
    val uri = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

fun dialPhoneNumber(context: Context, phone: String) {
    val uri = Uri.parse("tel:$phone")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
    }
}

fun openGoogleMapsDirections(context: Context, lat: Double, lng: Double) {
    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

@Composable
fun ShopBannerImage(
    imageKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isClosed: Boolean = false
) {
    val closedFilter = if (isClosed) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    } else {
        null
    }
    val imageModifier = if (isClosed) modifier.alpha(0.75f) else modifier

    val localResId = when (imageKey) {
        "img_shop_biryani" -> R.drawable.img_shop_biryani
        "img_shop_chai" -> R.drawable.img_shop_chai
        "img_shop_kirana" -> R.drawable.img_shop_kirana
        "img_shop_sweets" -> R.drawable.img_shop_sweets
        else -> null
    }
    if (localResId != null) {
        Image(
            painter = painterResource(id = localResId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            colorFilter = closedFilter,
            modifier = imageModifier
        )
    } else if (imageKey.startsWith("http")) {
        AsyncImage(
            model = imageKey,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            colorFilter = closedFilter,
            modifier = imageModifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_shop_biryani),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            colorFilter = closedFilter,
            modifier = imageModifier
        )
    }
}

@Composable
fun ShopStatusPill(
    isOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shop_status_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val containerColor = if (isOpen) Color(0xFF16A34A) else Color(0xFF4B5563)
    val borderColor = if (isOpen) Color(0xFF86EFAC) else Color(0xFFFCA5A5).copy(alpha = 0.7f)
    val dotColor = if (isOpen) Color(0xFFBBF7D0) else Color(0xFFF87171)

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isOpen) 6.dp else 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isOpen) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(glowScale)
                            .clip(CircleShape)
                            .background(Color(0xFF86EFAC).copy(alpha = glowAlpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isOpen) SurfaceWhite else dotColor)
                )
            }
            Text(
                text = if (isOpen) "OPEN NOW" else "CLOSED",
                style = MaterialTheme.typography.labelSmall,
                color = SurfaceWhite,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun CategoryVisualAvatar(
    category: ShopCategory,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, gradientColors, iconTint) = when (category) {
        ShopCategory.ALL -> Triple(
            Icons.Filled.GridView,
            listOf(Color(0xFFFFE4E6), Color(0xFFFECDD3)),
            CrimsonPrimary
        )
        ShopCategory.BIRYANI -> Triple(
            Icons.Filled.Restaurant,
            listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
            Color(0xFFB45309)
        )
        ShopCategory.MOMOS_FAST_FOOD -> Triple(
            Icons.Filled.Fastfood,
            listOf(Color(0xFFFFEDD5), Color(0xFFFED7AA)),
            Color(0xFFEA580C)
        )
        ShopCategory.CHAI_COFFEE -> Triple(
            Icons.Filled.LocalCafe,
            listOf(Color(0xFFF3E8FF), Color(0xFFE9D5FF)),
            Color(0xFF7E22CE)
        )
        ShopCategory.COLD_DRINKS_SHAKES -> Triple(
            Icons.Filled.LocalDrink,
            listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD)),
            Color(0xFF0284C7)
        )
        ShopCategory.SWEETS_BAKERY -> Triple(
            Icons.Filled.Cake,
            listOf(Color(0xFFFCE7F3), Color(0xFFFBCFE8)),
            Color(0xFFDB2777)
        )
        ShopCategory.GROCERY_KIRANA -> Triple(
            Icons.Filled.ShoppingBasket,
            listOf(Color(0xFFDCFCE7), Color(0xFFBBF7D0)),
            FreshGreenDark
        )
        ShopCategory.DAIRY_FRUITS -> Triple(
            Icons.Filled.Eco,
            listOf(Color(0xFFECFCCB), Color(0xFFD9F99D)),
            Color(0xFF4D7C0F)
        )
    }

    Box(
        modifier = modifier
            .size(66.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 2.5.dp else 1.5.dp,
                color = if (selected) CrimsonPrimary else HairlineBorder,
                shape = CircleShape
            )
            .then(
                if (selected) {
                    Modifier.background(CrimsonPrimary)
                } else {
                    Modifier.background(Brush.linearGradient(colors = gradientColors))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.label,
            tint = if (selected) SurfaceWhite else iconTint,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun DeliveringToBar(
    selectedArea: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = CrimsonSoft.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("location_selector_btn")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(CrimsonPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Select delivery area",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DELIVERING TO",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedArea,
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelMedium,
                    color = CrimsonPrimary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Choose Kundarki area",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeighborhoodPickerSheet(
    selectedNeighborhood: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Delivering to Kundarki Area",
                style = MaterialTheme.typography.headlineSmall,
                color = InkPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select your mohalla or road — saved automatically to your Checkout address",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            KundarkiConfig.neighborhoods.forEach { area ->
                val isSelected = area == selectedNeighborhood
                Surface(
                    onClick = { onSelect(area) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) CrimsonSoft else CanvasLight,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) CrimsonPrimary else HairlineBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("area_option_$area")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = if (isSelected) CrimsonPrimary else InkSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = area,
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VegNonVegIndicator(
    isVeg: Boolean,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (isVeg) FreshGreen else CrimsonPrimary
    Box(
        modifier = modifier
            .size(16.dp)
            .border(1.5.dp, indicatorColor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
    }
}

@Composable
fun QuantityStepper(
    quantity: Int,
    enabled: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier,
    shopClosed: Boolean = false
) {
    AnimatedContent(
        targetState = quantity > 0,
        label = "stepper_transition",
        modifier = modifier
    ) { inCart ->
        if (!inCart) {
            Surface(
                onClick = onAdd,
                enabled = enabled,
                shape = RoundedCornerShape(10.dp),
                color = if (enabled) CrimsonSoft else SurfaceMuted,
                border = BorderStroke(
                    1.dp,
                    if (enabled) CrimsonPrimary else HairlineBorder
                ),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .testTag("${testTagPrefix}_add_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when {
                            enabled -> "ADD"
                            shopClosed -> "ADD"
                            else -> "OUT"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (enabled) CrimsonPrimary else InkMuted,
                        fontWeight = FontWeight.Bold
                    )
                    if (enabled) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add item",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CrimsonPrimary,
                shadowElevation = 2.dp,
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onRemove)
                            .testTag("${testTagPrefix}_dec_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease quantity",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = enabled, onClick = onAdd)
                            .testTag("${testTagPrefix}_inc_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase quantity",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun roleIcon(role: UserRole): ImageVector = when (role) {
    UserRole.CUSTOMER -> Icons.Filled.Person
    UserRole.VENDOR -> Icons.Filled.Storefront
    UserRole.DELIVERY -> Icons.Filled.DeliveryDining
    UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
}

@Composable
fun RoleSwitcherPill(
    activeRole: UserRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = InkPrimary,
        modifier = modifier
            .heightIn(min = 38.dp)
            .testTag("role_switcher_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = roleIcon(activeRole),
                contentDescription = "Switch app role",
                tint = SurfaceWhite,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = activeRole.label,
                style = MaterialTheme.typography.labelMedium,
                color = SurfaceWhite
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = SurfaceWhite.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePickerBottomSheet(
    currentUser: UserAccount?,
    activeRole: UserRole,
    isLiveSupabase: Boolean,
    onSelectRole: (UserRole) -> Unit,
    onOpenAuth: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    FatakseBrandBadge(size = 40.dp, cornerRadius = 10.dp)
                    Column {
                        Text(
                            text = "Account & Panel Role",
                            style = MaterialTheme.typography.headlineSmall,
                            color = InkPrimary
                        )
                        Text(
                            text = "Manage session or preview any Fatakse panel",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                }
                ModeBadge(isLiveSupabase = isLiveSupabase)
            }

            // Active Account Session Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceMuted,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (currentUser != null) CrimsonPrimary else HairlineBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser != null) {
                            Text(
                                text = currentUser.fullName
                                    .split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                    .ifBlank { "FK" },
                                style = MaterialTheme.typography.labelLarge,
                                color = SurfaceWhite,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = InkSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.fullName ?: "Guest Session",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentUser != null) {
                                "${currentUser.role.label} • ${currentUser.phone}"
                            } else {
                                "Sign in to place & track orders"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    if (currentUser != null) {
                        Surface(
                            onClick = onLogout,
                            shape = RoundedCornerShape(10.dp),
                            color = CrimsonSoft,
                            border = BorderStroke(1.dp, CrimsonPrimary),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("sheet_logout_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Log out",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Log Out",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrimsonPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = onOpenAuth,
                            shape = RoundedCornerShape(10.dp),
                            color = CrimsonPrimary,
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("sheet_login_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = "Log in",
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Log In",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SurfaceWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            UserRole.entries.forEach { role ->
                val isSelected = role == activeRole
                Surface(
                    onClick = { onSelectRole(role) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) CrimsonSoft else SurfaceWhite,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) CrimsonPrimary else HairlineBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("role_option_${role.dbKey}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CrimsonPrimary else SurfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = roleIcon(role),
                                contentDescription = role.label,
                                tint = if (isSelected) SurfaceWhite else InkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${role.label} Panel",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary
                            )
                            Text(
                                text = role.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Active role",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeBadge(isLiveSupabase: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isLiveSupabase) FreshGreenSoft else WarmAmberSoft,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isLiveSupabase) FreshGreen else WarmAmber)
            )
            Text(
                text = if (isLiveSupabase) "Live Supabase" else "Supabase Offline",
                style = MaterialTheme.typography.labelSmall,
                color = if (isLiveSupabase) FreshGreen else InkPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING -> WarmAmberSoft to Color(0xFFB45309)
        OrderStatus.ACCEPTED -> RoyalBlueSoft to RoyalBlue
        OrderStatus.PREPARING -> CrimsonSoft to CrimsonPrimary
        OrderStatus.OUT_FOR_DELIVERY -> RoyalBlueSoft to RoyalBlue
        OrderStatus.DELIVERED -> FreshGreenSoft to FreshGreen
        OrderStatus.CANCELLED -> SurfaceMuted to InkSecondary
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        modifier = modifier
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun OrderStatusTimeline(
    currentStatus: OrderStatus,
    modifier: Modifier = Modifier
) {
    if (currentStatus == OrderStatus.CANCELLED) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceMuted,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = "This order was cancelled by the shop.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                modifier = Modifier.padding(12.dp)
            )
        }
        return
    }

    val steps = listOf(
        OrderStatus.PENDING to "Placed",
        OrderStatus.ACCEPTED to "Accepted",
        OrderStatus.PREPARING to "Preparing",
        OrderStatus.OUT_FOR_DELIVERY to "On Way",
        OrderStatus.DELIVERED to "Delivered"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, (stepStatus, shortLabel) ->
            val reached = currentStatus.stepIndex >= stepStatus.stepIndex
            val isCurrent = currentStatus == stepStatus
            val dotColor by animateColorAsState(
                targetValue = when {
                    reached && currentStatus == OrderStatus.DELIVERED -> FreshGreen
                    reached -> CrimsonPrimary
                    else -> HairlineBorder
                },
                animationSpec = tween(250),
                label = "step_color"
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .background(
                                if (index == 0) Color.Transparent
                                else if (reached) dotColor
                                else HairlineBorder
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 22.dp else 18.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (reached) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = SurfaceWhite,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .background(
                                if (index == steps.lastIndex) Color.Transparent
                                else if (currentStatus.stepIndex > stepStatus.stepIndex) dotColor
                                else HairlineBorder
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reached) InkPrimary else InkMuted,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(CrimsonSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = InkPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    color = CrimsonPrimary,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = SurfaceWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FatakseBrandBadge(
    size: androidx.compose.ui.unit.Dp = 44.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
    showBorder: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = CrimsonPrimary,
        border = if (showBorder) BorderStroke(1.dp, SurfaceWhite.copy(alpha = 0.35f)) else null,
        shadowElevation = if (showBorder) 2.dp else 1.dp,
        modifier = modifier
            .size(size)
            .testTag("fatakse_brand_logo_badge")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_fatakse_logo),
            contentDescription = "Fatakse Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
        )
    }
}

@Composable
fun FatakseSplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CrimsonPrimary)
            .testTag("fatakse_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_fatakse_logo),
                contentDescription = "Fatakse Official Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
            Text(
                text = "Biryani ho ya Kirana, Fatak se ghar mangana!",
                style = MaterialTheme.typography.titleSmall,
                color = SurfaceWhite.copy(alpha = 0.94f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
