package com.example.ui.screens.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItemDetail
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.ui.components.QuantityStepper
import com.example.ui.components.ShopBannerImage
import com.example.ui.components.ShopStatusPill
import com.example.ui.components.VegNonVegIndicator
import com.example.ui.components.formatInr
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
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite

@Composable
fun ShopMenuScreen(
    shop: Shop,
    isFavorite: Boolean,
    products: List<Product>,
    cartItems: List<CartItemDetail>,
    cartQuantities: Map<String, Int>,
    cartItemCount: Int,
    cartTotal: Int,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddProduct: (Product) -> Unit,
    onDecrementProduct: (String) -> Unit,
    onViewCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var vegOnly by rememberSaveable { mutableStateOf(false) }
    val displayProducts = if (vegOnly) products.filter { it.isVeg } else products

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        val menuColumns = if (maxWidth >= 620.dp) 2 else 1

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (cartItemCount > 0) 104.dp else 28.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                ) {
                    ShopBannerImage(
                        imageKey = shop.imageKey,
                        contentDescription = shop.name,
                        isClosed = !shop.isOpen,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.78f)
                                    )
                                )
                            )
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(SurfaceWhite.copy(alpha = 0.92f), CircleShape)
                            .testTag("shop_menu_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to shops",
                            tint = InkPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShopStatusPill(
                            isOpen = shop.isOpen,
                            modifier = Modifier.testTag("shop_menu_status_badge")
                        )
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .background(SurfaceWhite.copy(alpha = 0.92f), CircleShape)
                                .testTag("shop_menu_fav_btn")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (isFavorite) CrimsonPrimary else InkPrimary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = shop.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = SurfaceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${shop.deliveryTime} • ${shop.distanceKm} • ${shop.tagline}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SurfaceWhite.copy(alpha = 0.92f)
                        )
                    }
                }
            }

            item {
                Surface(
                    color = SurfaceWhite,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = shop.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FreshGreen
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = String.format("%.1f", shop.rating),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SurfaceWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = SurfaceWhite,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = InkSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Express in ${shop.deliveryTime}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkPrimary
                                )
                            }

                            FilterChip(
                                selected = vegOnly,
                                onClick = { vegOnly = !vegOnly },
                                label = {
                                    Text(
                                        text = "Pure Veg Only",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = { VegNonVegIndicator(isVeg = true) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FreshGreenSoft,
                                    selectedLabelColor = FreshGreen
                                )
                            )
                        }

                        if (!shop.isOpen) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CrimsonSoft,
                                border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shop_closed_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
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
                }
            }

            item {
                Text(
                    text = "Menu & Products (${displayProducts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            val productRows = displayProducts.chunked(menuColumns)
            items(
                items = productRows,
                key = { row -> "menu_row_${row.first().id}" }
            ) { rowProducts ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowProducts.forEach { product ->
                        val qty = cartQuantities[product.id] ?: 0
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceWhite,
                            shadowElevation = 1.dp,
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.weight(1f)
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
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        VegNonVegIndicator(isVeg = product.isVeg)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SurfaceMuted
                                        ) {
                                            Text(
                                                text = product.categoryTag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = InkSecondary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (!product.inStock) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = CrimsonSoft
                                            ) {
                                                Text(
                                                    text = "Out of Stock",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CrimsonPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (product.inStock) InkPrimary else InkMuted
                                    )
                                    if (product.description.isNotBlank()) {
                                        Text(
                                            text = product.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkSecondary
                                        )
                                    }
                                    Text(
                                        text = formatInr(product.price),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = InkPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                QuantityStepper(
                                    quantity = qty,
                                    enabled = shop.isOpen && product.inStock,
                                    onAdd = { onAddProduct(product) },
                                    onRemove = { onDecrementProduct(product.id) },
                                    testTagPrefix = "menu_${product.id}",
                                    shopClosed = !shop.isOpen
                                )
                            }
                        }
                    }
                    repeat(menuColumns - rowProducts.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (cartItemCount > 0) {
            StickyCartBar(
                cartItems = cartItems,
                itemCount = cartItemCount,
                subtotal = cartTotal,
                onClick = onViewCart,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            )
        }
    }
}
