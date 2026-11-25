package com.example.ui.screens.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.UserRole
import com.example.ui.components.ModeBadge
import com.example.ui.components.roleIcon
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
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberSoft
import com.example.viewmodel.KundarkiUiState

@Composable
fun AccountRoleScreen(
    state: KundarkiUiState,
    onSelectRole: (UserRole) -> Unit,
    onUpdateProfile: (fullName: String, phone: String, address: String) -> Unit,
    onLogout: () -> Unit,
    onOpenAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = state.currentUser
    var fullName by remember { mutableStateOf(user?.fullName ?: state.checkoutForm.customerName) }
    var phone by remember { mutableStateOf(user?.phone ?: state.checkoutForm.customerPhone) }
    var savedAddress by remember { mutableStateOf(user?.address ?: state.checkoutForm.deliveryAddress) }

    LaunchedEffect(user?.id, user?.fullName, user?.phone, user?.address) {
        if (user != null) {
            fullName = user.fullName
            phone = user.phone
            savedAddress = user.address.ifBlank { state.checkoutForm.deliveryAddress }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CrimsonPrimary,
        unfocusedBorderColor = HairlineBorder,
        focusedLabelColor = CrimsonPrimary,
        cursorColor = CrimsonPrimary,
        focusedContainerColor = SurfaceWhite,
        unfocusedContainerColor = CanvasLight
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Identity Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, HairlineBorder),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (user != null) {
                    val (roleBg, roleFg) = roleBadgeColors(user.role)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.fullName
                                        .split(" ")
                                        .take(2)
                                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                        .joinToString("")
                                        .ifBlank { "FK" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SurfaceWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = user.fullName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = null,
                                        tint = InkSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = user.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = roleBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = roleIcon(user.role),
                                        contentDescription = null,
                                        tint = roleFg,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${user.role.label} Account",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = roleFg,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            ModeBadge(isLiveSupabase = state.isLiveSupabase)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Browsing as Guest",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Log in or create an account to save your Kundarki address and track orders.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                        }
                        Button(
                            onClick = onOpenAuth,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = SurfaceWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("account_login_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Log In / Create Account",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Editable Profile & Saved Kundarki Delivery Address Form (when logged in)
        if (user != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EditLocationAlt,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Edit Profile & Saved Address",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-filled at checkout for faster Kundarki deliveries",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = InkSecondary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = InkSecondary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_phone_input")
                        )

                        OutlinedTextField(
                            value = savedAddress,
                            onValueChange = { savedAddress = it },
                            label = { Text("Saved Kundarki Delivery Address") },
                            placeholder = { Text("House/Shop No., Mohalla, Landmark, Kundarki") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = CrimsonPrimary
                                )
                            },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_address_input")
                        )

                        // Quick Kundarki neighborhood presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Main Bazar", "Station Road", "Naya Mohalla").forEach { area ->
                                Surface(
                                    onClick = {
                                        savedAddress = "$area, Kundarki, Moradabad, UP 244413"
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = SurfaceMuted,
                                    border = BorderStroke(1.dp, HairlineBorder)
                                ) {
                                    Text(
                                        text = area,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onUpdateProfile(fullName, phone, savedAddress) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = SurfaceWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("save_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Profile & Address",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Prominent Red Log Out Button
            item {
                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, CrimsonPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CrimsonSoft,
                        contentColor = CrimsonPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log out",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.titleSmall,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Backend Mode Info Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (state.isLiveSupabase) FreshGreenSoft else SurfaceWhite,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (state.isLiveSupabase) Icons.Filled.CloudDone else Icons.Filled.Storage,
                        contentDescription = null,
                        tint = if (state.isLiveSupabase) FreshGreen else CrimsonPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (state.isLiveSupabase) "Connected to Live Supabase" else "Supabase Not Configured",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                        Text(
                            text = if (state.isLiveSupabase) {
                                "Fatakse shops, products, orders, profiles, and settlements are synced strictly from your live Supabase database."
                            } else {
                                "Add VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY in AI Studio Secrets to connect Fatakse to your Supabase database."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                }
            }
        }

        // Quick Role Switcher for Demo / Preview Testing
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Quick Panel Switcher",
                style = MaterialTheme.typography.titleLarge,
                color = InkPrimary
            )
            Text(
                text = "Preview any Fatakse role dashboard instantly",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted
            )
        }

        items(UserRole.entries.size) { index ->
            val role = UserRole.entries[index]
            val selected = role == state.activeRole
            Surface(
                onClick = { onSelectRole(role) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) CrimsonSoft else SurfaceWhite,
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) CrimsonPrimary else HairlineBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_role_${role.dbKey}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) CrimsonPrimary else SurfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = roleIcon(role),
                            contentDescription = role.label,
                            tint = if (selected) SurfaceWhite else InkPrimary,
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
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected role",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun roleBadgeColors(role: UserRole): Pair<Color, Color> = when (role) {
    UserRole.CUSTOMER -> CrimsonSoft to CrimsonPrimary
    UserRole.VENDOR -> WarmAmberSoft to WarmAmber
    UserRole.DELIVERY -> RoyalBlueSoft to RoyalBlue
    UserRole.ADMIN -> FreshGreenSoft to FreshGreen
}
