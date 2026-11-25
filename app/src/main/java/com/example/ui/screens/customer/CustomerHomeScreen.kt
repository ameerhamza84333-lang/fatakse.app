package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItemDetail
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.ShopCategory
import com.example.data.model.VisualDishShortcut
import com.example.ui.components.ActiveOrderMiniTrackerBanner
import com.example.ui.components.CategoryVisualAvatar
import com.example.ui.components.DeliveringToBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FatakseBrandBadge
import com.example.ui.components.NeighborhoodPickerSheet
import com.example.ui.components.QuantityStepper
import com.example.ui.components.RoleSwitcherPill
import com.example.ui.components.ShopBannerImage
import com.example.ui.components.ShopStatusPill
import com.example.ui.components.VegNonVegIndicator
import com.example.ui.components.formatInr
import com.example.ui.components.openWhatsAppOrder
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
import com.example.ui.theme.WhatsAppGreen
import com.example.viewmodel.KundarkiUiState

@Composable
fun CustomerHomeScreen(
    state: KundarkiUiState,
    onSelectNeighborhood: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleDishShortcut: (VisualDishShortcut) -> Unit,
    onToggleVegOnly: () -> Unit,
    onToggleRating4Plus: () -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onResetFilters: () -> Unit,
    onToggleFavoriteShop: (Shop) -> Unit,
    onOpenShop: (String) -> Unit,
    onAddProduct: (Product, Shop) -> Unit,
    onDecrementProduct: (String) -> Unit,
    onOpenRoleSheet: () -> Unit,
    onViewCart: () -> Unit,
    onTrackActiveOrder: () -> Unit,
    onRefreshFromSupabase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cartCount = state.cartPricing.totalItemCount
    val latestActiveOrder = state.orders.firstOrNull {
        it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED
    }
    var showNeighborhoodSheet by remember { mutableStateOf(false) }

    val quickSuggestions = remember {
        listOf("Momos", "Chicken Biryani", "Oreo Shake", "Masala Chai", "Chakki Atta", "Gulab Jamun")
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        val shopColumns = when {
            maxWidth >= 860.dp -> 3
            maxWidth >= 580.dp -> 2
            else -> 1
        }
        val itemColumns = if (maxWidth >= 600.dp) 2 else 1

        Column(modifier = Modifier.fillMaxSize()) {
            // Spacious Top "Fatakse" Brand Banner (Full Tagline, Never Truncated)
            Surface(
                color = CrimsonPrimary,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CrimsonPrimary)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FatakseBrandBadge(
                            size = 46.dp,
                            cornerRadius = 12.dp,
                            showBorder = true
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Fatakse",
                                style = MaterialTheme.typography.headlineSmall,
                                color = SurfaceWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Biryani ho ya Kirana, Fatak se ghar mangana!",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = SurfaceWhite.copy(alpha = 0.95f),
                                fontWeight = FontWeight.Medium,
                                softWrap = true,
                                modifier = Modifier.testTag("fatakse_brand_tagline")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sleek Green WhatsApp Icon Button
                            Surface(
                                onClick = {
                                    openWhatsAppOrder(
                                        context = context,
                                        phone = state.settings.supportWhatsapp,
                                        message = "Assalam Alaikum! I would like to place a Fatakse order to ${state.selectedNeighborhood}."
                                    )
                                },
                                shape = CircleShape,
                                color = WhatsAppGreen,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("whatsapp_order_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Chat,
                                        contentDescription = "Order on WhatsApp",
                                        tint = SurfaceWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            RoleSwitcherPill(
                                activeRole = state.activeRole,
                                onClick = onOpenRoleSheet
                            )
                        }
                    }
                }
            }

            // Main Scrollable Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = if (cartCount > 0) 104.dp else 28.dp)
            ) {
                // Search Bar + Dedicated "Delivering to" Kundarki Area Selector + Live Suggestions
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceWhite)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = {
                                Text(
                                    text = "Search Biryani, Momos, Shakes, Chai, Atta…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = CrimsonPrimary
                                )
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear search",
                                            tint = InkSecondary
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CanvasLight,
                                unfocusedContainerColor = CanvasLight,
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = HairlineBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_search_input")
                        )

                        // Dedicated "Delivering to: [Selected Area] ▾" Bar right below Search
                        DeliveringToBar(
                            selectedArea = state.selectedNeighborhood,
                            onClick = { showNeighborhoodSheet = true }
                        )

                        // Live Quick Search Suggestions Row (only when live products exist)
                        if (state.products.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(quickSuggestions) { term ->
                                    val active = state.searchQuery.equals(term, ignoreCase = true)
                                    Surface(
                                        onClick = {
                                            onSearchChange(if (active) "" else term)
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (active) CrimsonSoft else SurfaceMuted,
                                        border = BorderStroke(
                                            1.dp,
                                            if (active) CrimsonPrimary else Color.Transparent
                                        )
                                    ) {
                                        Text(
                                            text = term,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (active) CrimsonPrimary else InkSecondary,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = HairlineBorder.copy(alpha = 0.7f))
                }

                // Live Active Order Mini-Tracker Banner (taps directly to Live Step Timeline)
                if (latestActiveOrder != null) {
                    item {
                        ActiveOrderMiniTrackerBanner(
                            order = latestActiveOrder,
                            onTrackClick = onTrackActiveOrder,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // "What's on your mind?" Circular Visual Dish Row
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (latestActiveOrder != null) 6.dp else 14.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "What's on your mind?",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(KundarkiUiState.visualDishShortcuts, key = { it.id }) { shortcut ->
                                val selected = state.selectedCategory == shortcut.category
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .width(80.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onToggleDishShortcut(shortcut) }
                                        .padding(vertical = 2.dp)
                                        .testTag("dish_shortcut_${shortcut.id}")
                                ) {
                                    CategoryVisualAvatar(
                                        category = shortcut.category,
                                        selected = selected
                                    )
                                    Text(
                                        text = shortcut.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) CrimsonPrimary else InkPrimary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Veg Only Toggle Switch + Rating 4.0+ + Favorites Filter Strip
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Veg Only Switch Pill
                        item {
                            Surface(
                                onClick = onToggleVegOnly,
                                shape = RoundedCornerShape(12.dp),
                                color = if (state.vegOnlyFilter) FreshGreenSoft else SurfaceWhite,
                                border = BorderStroke(
                                    1.dp,
                                    if (state.vegOnlyFilter) FreshGreen else HairlineBorder
                                ),
                                modifier = Modifier.testTag("filter_veg_only")
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    VegNonVegIndicator(isVeg = true)
                                    Text(
                                        text = "Veg Only",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (state.vegOnlyFilter) FreshGreenDark else InkPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = state.vegOnlyFilter,
                                        onCheckedChange = { onToggleVegOnly() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SurfaceWhite,
                                            checkedTrackColor = FreshGreen,
                                            uncheckedThumbColor = SurfaceWhite,
                                            uncheckedTrackColor = HairlineBorder
                                        ),
                                        modifier = Modifier.scale(0.72f)
                                    )
                                }
                            }
                        }

                        // Rating 4.0+ Pill
                        item {
                            Surface(
                                onClick = onToggleRating4Plus,
                                shape = RoundedCornerShape(12.dp),
                                color = if (state.rating4PlusFilter) FreshGreenSoft else SurfaceWhite,
                                border = BorderStroke(
                                    1.dp,
                                    if (state.rating4PlusFilter) FreshGreen else HairlineBorder
                                ),
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .testTag("filter_rating_4plus")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (state.rating4PlusFilter) FreshGreen else WarmAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Rating 4.0+",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (state.rating4PlusFilter) FreshGreenDark else InkPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Favorites Pill
                        item {
                            Surface(
                                onClick = onToggleFavoritesOnly,
                                shape = RoundedCornerShape(12.dp),
                                color = if (state.favoritesOnlyFilter) CrimsonSoft else SurfaceWhite,
                                border = BorderStroke(
                                    1.dp,
                                    if (state.favoritesOnlyFilter) CrimsonPrimary else HairlineBorder
                                ),
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .testTag("filter_favorites_only")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.favoritesOnlyFilter) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = null,
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Favorites (${state.favoriteShopIds.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (state.favoritesOnlyFilter) CrimsonPrimary else InkPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Direct Matching Menu Items when searching OR a specific category is selected
                if (state.matchedProductsForHome.isNotEmpty()) {
                    item {
                        Text(
                            text = if (state.searchQuery.isNotBlank()) {
                                "Matching Dishes & Groceries (${state.matchedProductsForHome.size})"
                            } else {
                                "Popular in ${state.selectedCategory.label} (${state.matchedProductsForHome.size})"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    val productRows = state.matchedProductsForHome.chunked(itemColumns)
                    items(
                        items = productRows,
                        key = { row -> "search_row_${row.first().first.id}" }
                    ) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { (product, shop) ->
                                val qty = state.cartQuantities[product.id] ?: 0
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = SurfaceWhite,
                                    shadowElevation = 1.dp,
                                    border = BorderStroke(1.dp, HairlineBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onOpenShop(shop.id) }
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
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                VegNonVegIndicator(isVeg = product.isVeg)
                                                Text(
                                                    text = shop.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (shop.isOpen) CrimsonPrimary else InkMuted,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (!shop.isOpen) {
                                                    Text(
                                                        text = "• CLOSED",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = CrimsonPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = product.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (shop.isOpen) InkPrimary else InkMuted,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${formatInr(product.price)} • ${product.categoryTag}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (shop.isOpen) InkPrimary else InkMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        QuantityStepper(
                                            quantity = qty,
                                            enabled = product.inStock && shop.isOpen,
                                            onAdd = { onAddProduct(product, shop) },
                                            onRemove = { onDecrementProduct(product.id) },
                                            testTagPrefix = "search_${product.id}",
                                            shopClosed = !shop.isOpen
                                        )
                                    }
                                }
                            }
                            repeat(itemColumns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(10.dp)) }
                }

                // Top Shops Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                state.favoritesOnlyFilter -> "Your Favorite Shops"
                                state.selectedCategory == ShopCategory.ALL -> "Top Shops in Kundarki"
                                else -> state.selectedCategory.label
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.filteredShops.size} stores",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary
                        )
                    }
                }

                // Zomato-Style Premium Shop Cards (Responsive 1 / 2 / 3-column grid)
                if (state.filteredShops.isEmpty()) {
                    item {
                        if (state.shops.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Filled.Storefront,
                                title = if (state.isSyncing) "Loading stores from Supabase…" else "No stores available yet",
                                subtitle = if (state.isLiveSupabase) {
                                    "Your Supabase shops table is currently empty. Add real stores in Supabase or the Admin Panel and refresh."
                                } else {
                                    "Configure VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY in AI Studio Secrets to fetch live stores from Supabase."
                                },
                                actionLabel = if (state.isSyncing) null else "Refresh Stores",
                                onAction = if (state.isSyncing) null else onRefreshFromSupabase,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            EmptyStateCard(
                                icon = Icons.Filled.Storefront,
                                title = "No matching shops found",
                                subtitle = "Try resetting filters or exploring all Kundarki stores.",
                                actionLabel = "Reset Filters",
                                onAction = onResetFilters,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    val shopRows = state.filteredShops.chunked(shopColumns)
                    items(
                        items = shopRows,
                        key = { row -> "shop_row_${row.first().id}" }
                    ) { rowShops ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            rowShops.forEach { shop ->
                                val isFav = shop.id in state.favoriteShopIds
                                ZomatoShopCard(
                                    shop = shop,
                                    isFavorite = isFav,
                                    onToggleFavorite = { onToggleFavoriteShop(shop) },
                                    onClick = { onOpenShop(shop.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(shopColumns - rowShops.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Animated Floating Cart Bar with Thumbnails & Pulsing View Cart Button
        AnimatedVisibility(
            visible = cartCount > 0,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 520.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            StickyCartBar(
                cartItems = state.cartItems,
                itemCount = cartCount,
                subtotal = state.cartPricing.grandTotal,
                onClick = onViewCart
            )
        }
    }

    if (showNeighborhoodSheet) {
        NeighborhoodPickerSheet(
            selectedNeighborhood = state.selectedNeighborhood,
            onSelect = {
                onSelectNeighborhood(it)
                showNeighborhoodSheet = false
            },
            onDismiss = { showNeighborhoodSheet = false }
        )
    }
}

@Composable
private fun ZomatoShopCard(
    shop: Shop,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = SurfaceWhite,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("shop_card_${shop.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
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
                                    Color.Black.copy(alpha = if (shop.isOpen) 0.22f else 0.42f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = if (shop.isOpen) 0.72f else 0.82f)
                                )
                            )
                        )
                )

                // Top-Left High-Visibility Glowing Open Now / Closed Status Pill
                ShopStatusPill(
                    isOpen = shop.isOpen,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .testTag("shop_status_${shop.id}")
                )

                // Top-Right Floating Favorite Heart Button
                Surface(
                    onClick = onToggleFavorite,
                    shape = CircleShape,
                    color = SurfaceWhite.copy(alpha = 0.92f),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(38.dp)
                        .testTag("fav_btn_${shop.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Save favorite",
                            tint = if (isFavorite) CrimsonPrimary else InkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom-Left Dark Overlay Delivery Time & Distance
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${shop.deliveryTime} • ${shop.distanceKm}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Bottom-Right Category Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = shop.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = SurfaceWhite,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            // Card Details Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shop.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Rating",
                                tint = SurfaceWhite,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Text(
                    text = shop.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = shop.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StickyCartBar(
    cartItems: List<CartItemDetail>,
    itemCount: Int,
    subtotal: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cart_cta_pulse")
    val ctaScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val uniqueThumbs = cartItems.map { it.product.imageKey }.distinct().take(3)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = InkPrimary,
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .testTag("sticky_cart_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Overlapping Circular Item Thumbnails
                Box(modifier = Modifier.width((28 + (uniqueThumbs.size - 1).coerceAtLeast(0) * 16).dp + 12.dp)) {
                    uniqueThumbs.forEachIndexed { idx, imgKey ->
                        Box(
                            modifier = Modifier
                                .offset(x = (idx * 16).dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(2.dp, SurfaceWhite, CircleShape)
                        ) {
                            ShopBannerImage(
                                imageKey = imgKey,
                                contentDescription = "Cart item",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "$itemCount ${if (itemCount == 1) "ITEM" else "ITEMS"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SurfaceWhite.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatInr(subtotal),
                        style = MaterialTheme.typography.titleLarge,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CrimsonPrimary,
                modifier = Modifier.scale(ctaScale)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "View Cart",
                        style = MaterialTheme.typography.labelLarge,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Cart",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
