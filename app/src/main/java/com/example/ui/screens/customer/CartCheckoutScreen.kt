package com.example.ui.screens.customer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.KundarkiConfig
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.ui.components.DeliveringToBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NeighborhoodPickerSheet
import com.example.ui.components.QuantityStepper
import com.example.ui.components.VegNonVegIndicator
import com.example.ui.components.formatInr
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
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberSoft
import com.example.viewmodel.KundarkiUiState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CartCheckoutScreen(
    state: KundarkiUiState,
    onSelectNeighborhood: (String) -> Unit,
    onAddProduct: (Product, Shop) -> Unit,
    onDecrementProduct: (String) -> Unit,
    onClearCart: () -> Unit,
    onBrowseShops: () -> Unit,
    onToggleInstruction: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onGpsDetectStart: () -> Unit,
    onGpsDetected: (String, Double, Double) -> Unit,
    onGpsMessage: (String) -> Unit,
    onPlaceOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pricing = state.cartPricing
    val settings = state.settings
    val form = state.checkoutForm
    var showNeighborhoodSheet by remember { mutableStateOf(false) }

    fun triggerLocationFetch() {
        onGpsDetectStart()
        fetchLastKnownGpsLocation(
            context = context,
            onResult = { address, lat, lng -> onGpsDetected(address, lat, lng) },
            onFallback = { fallbackMsg ->
                onGpsDetected(
                    KundarkiConfig.formatDeliveryAddress(state.selectedNeighborhood),
                    28.6852,
                    78.7793
                )
                onGpsMessage(fallbackMsg)
            },
            launchIo = { block -> scope.launch { block() } }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerLocationFetch()
        } else {
            onGpsMessage("Location permission denied. Enter address manually.")
        }
    }

    if (state.cartItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CanvasLight)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Filled.ShoppingBag,
                title = "Your cart is empty",
                subtitle = "Add steaming biryani, kulhad chai, sweets, or daily groceries from Kundarki's favorite shops.",
                actionLabel = "Explore Shops",
                onAction = onBrowseShops
            )
        }
        return
    }

    val cartShop = state.cartItems.first().shop
    val shopName = cartShop.name
    val isShopOpen = cartShop.isOpen
    val minOrderProgress by animateFloatAsState(
        targetValue = (pricing.itemsTotal.toFloat() / settings.minOrderAmount.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f),
        label = "min_order_progress"
    )
    val freeDeliveryProgress by animateFloatAsState(
        targetValue = (pricing.itemsTotal.toFloat() / settings.freeDeliveryAbove.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f),
        label = "free_delivery_progress"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Checkout",
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ordering from $shopName",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick = onClearCart,
                    modifier = Modifier.testTag("clear_cart_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Clear cart",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", color = CrimsonPrimary)
                }
            }
        }

        // Closed Shop Lock Warning Banner
        if (!isShopOpen) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CrimsonSoft,
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockClock,
                            contentDescription = "Shop closed",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "This shop is currently closed and not accepting orders right now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Minimum Order & Free Delivery Progress Bar
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (pricing.meetsMinOrder) FreshGreenSoft else WarmAmberSoft,
                border = BorderStroke(
                    1.dp,
                    if (pricing.meetsMinOrder) FreshGreen.copy(alpha = 0.35f) else WarmAmber.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (pricing.meetsMinOrder) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = if (pricing.meetsMinOrder) FreshGreen else CrimsonPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = if (pricing.meetsMinOrder) {
                                if (pricing.isFreeDeliveryUnlocked) {
                                    "Minimum order met & FREE delivery unlocked!"
                                } else {
                                    "Add ${formatInr(pricing.freeDeliveryShortfall)} more for FREE delivery!"
                                }
                            } else {
                                "Add ${formatInr(pricing.minOrderShortfall)} more to order (Min ${formatInr(settings.minOrderAmount)})"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (pricing.meetsMinOrder) FreshGreenDark else InkPrimary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (pricing.meetsMinOrder) freeDeliveryProgress else minOrderProgress },
                        color = if (pricing.meetsMinOrder) FreshGreen else CrimsonPrimary,
                        trackColor = SurfaceWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )
                }
            }
        }

        // Cart Items Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(
                        text = "Your Items (${pricing.totalItemCount})",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    state.cartItems.forEachIndexed { idx, detail ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = HairlineBorder,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VegNonVegIndicator(isVeg = detail.product.isVeg)
                                Column {
                                    Text(
                                        text = detail.product.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = "${formatInr(detail.product.price)} × ${detail.quantity} = ${formatInr(detail.lineTotal)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            QuantityStepper(
                                quantity = detail.quantity,
                                enabled = detail.product.inStock && detail.shop.isOpen,
                                onAdd = { onAddProduct(detail.product, detail.shop) },
                                onRemove = { onDecrementProduct(detail.product.id) },
                                testTagPrefix = "cart_${detail.product.id}",
                                shopClosed = !detail.shop.isOpen
                            )
                        }
                    }
                }
            }
        }

        // Delivery Instructions Chips Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Delivery Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KundarkiConfig.deliveryInstructionPresets.forEach { label ->
                            val selected = label in form.selectedInstructions
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleInstruction(label) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonSoft,
                                    selectedLabelColor = CrimsonPrimary
                                ),
                                modifier = Modifier.testTag("instruction_chip_$label")
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Bill Breakdown Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Bill Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    BillRow(label = "Item Total", value = formatInr(pricing.itemsTotal))
                    BillRow(
                        label = "Delivery Fee",
                        value = if (pricing.deliveryFee == 0) "FREE" else formatInr(pricing.deliveryFee),
                        highlightGreen = pricing.deliveryFee == 0
                    )
                    BillRow(label = "Platform Fee", value = formatInr(pricing.platformFee))
                    HorizontalDivider(color = HairlineBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatInr(pricing.grandTotal),
                            style = MaterialTheme.typography.headlineSmall,
                            color = CrimsonPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Delivery Details & GPS Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delivery Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    triggerLocationFetch()
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !form.isDetectingGps,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CrimsonPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("detect_gps_button")
                        ) {
                            if (form.isDetectingGps) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = CrimsonPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = "Detect GPS Location",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Detect GPS",
                                style = MaterialTheme.typography.labelMedium,
                                color = CrimsonPrimary
                            )
                        }
                    }

                    // Kundarki Area / Mohalla Selector Bar synced to Delivery Address
                    DeliveringToBar(
                        selectedArea = state.selectedNeighborhood,
                        onClick = { showNeighborhoodSheet = true }
                    )

                    OutlinedTextField(
                        value = form.customerName,
                        onValueChange = onCustomerNameChange,
                        label = { Text("Customer Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_name_input")
                    )

                    OutlinedTextField(
                        value = form.customerPhone,
                        onValueChange = onCustomerPhoneChange,
                        label = { Text("Mobile Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_phone_input")
                    )

                    OutlinedTextField(
                        value = form.deliveryAddress,
                        onValueChange = onAddressChange,
                        label = { Text("Full Delivery Address (House, Mohalla, Landmark)") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_address_input")
                    )

                    Text(
                        text = "GPS Pin: ${String.format(Locale.US, "%.4f", form.deliveryLat)}, ${String.format(Locale.US, "%.4f", form.deliveryLng)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted
                    )
                }
            }
        }

        // Payment Method Card — Single Cash / UPI QR on Delivery option
        item {
            val method = PaymentMethod.CASH_UPI_QR_ON_DELIVERY
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
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
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = FreshGreenSoft
                        ) {
                            Text(
                                text = "NO GATEWAY FEE",
                                style = MaterialTheme.typography.labelSmall,
                                color = FreshGreenDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CrimsonSoft,
                        border = BorderStroke(1.5.dp, CrimsonPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_${method.dbKey}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceWhite,
                                border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Payments,
                                        contentDescription = "Cash",
                                        tint = FreshGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.QrCode2,
                                        contentDescription = "UPI QR",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = method.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = method.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected payment method",
                                tint = FreshGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Place Order CTA
        item {
            val canPlaceOrder = pricing.meetsMinOrder && isShopOpen
            Button(
                onClick = onPlaceOrder,
                enabled = canPlaceOrder,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPrimary,
                    disabledContainerColor = HairlineBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("place_order_button")
            ) {
                Text(
                    text = when {
                        !isShopOpen -> "Shop Currently Closed"
                        pricing.meetsMinOrder -> "Place Order • ${formatInr(pricing.grandTotal)}"
                        else -> "Add ${formatInr(pricing.minOrderShortfall)} more to place order"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (canPlaceOrder) SurfaceWhite else InkSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showNeighborhoodSheet) {
        NeighborhoodPickerSheet(
            selectedNeighborhood = state.selectedNeighborhood,
            onSelect = { area ->
                onSelectNeighborhood(area)
                showNeighborhoodSheet = false
            },
            onDismiss = { showNeighborhoodSheet = false }
        )
    }
}

@Composable
private fun BillRow(
    label: String,
    value: String,
    highlightGreen: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlightGreen) FreshGreen else InkPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@SuppressLint("MissingPermission")
private fun fetchLastKnownGpsLocation(
    context: Context,
    onResult: (String, Double, Double) -> Unit,
    onFallback: (String) -> Unit,
    launchIo: (suspend () -> Unit) -> Unit
) {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    fusedClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                launchIo {
                    val resolved = withContext(Dispatchers.IO) {
                        runCatching {
                            @Suppress("DEPRECATION")
                            val list = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
                            list?.firstOrNull()?.getAddressLine(0)
                        }.getOrNull()
                    }
                    val addressText = resolved ?: "Detected Location (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
                    onResult(addressText, lat, lng)
                }
            } else {
                onFallback("GPS signal weak — pinned to Kundarki Center")
            }
        }
        .addOnFailureListener {
            onFallback("Unable to read GPS — pinned to Kundarki Center")
        }
}
