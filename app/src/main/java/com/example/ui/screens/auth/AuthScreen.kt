package com.example.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserRole
import com.example.ui.components.ModeBadge
import com.example.ui.components.roleIcon
import com.example.ui.theme.CanvasLight
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.WarmAmberSoft

private enum class AuthTab { LOGIN, SIGN_UP }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    isLiveSupabase: Boolean,
    isBusy: Boolean,
    promptMessage: String?,
    onLogin: (phone10: String, pin4: String) -> Unit,
    onSignUp: (fullName: String, phone10: String, pin4: String, role: UserRole, passcode: String) -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onContinueAsGuest)

    var activeTab by rememberSaveable { mutableStateOf(AuthTab.LOGIN) }

    // Login state (+91 mobile number + optional 4-digit PIN)
    var loginPhone10 by rememberSaveable { mutableStateOf("") }
    var loginPin4 by rememberSaveable { mutableStateOf("") }
    var showLoginPin by rememberSaveable { mutableStateOf(false) }

    // Create Account state (Name + +91 mobile number + optional 4-digit PIN + Role)
    var fullName by rememberSaveable { mutableStateOf("") }
    var signUpPhone10 by rememberSaveable { mutableStateOf("") }
    var signUpPin4 by rememberSaveable { mutableStateOf("") }
    var showSignUpPin by rememberSaveable { mutableStateOf(false) }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.CUSTOMER) }
    var partnerPasscode by rememberSaveable { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CrimsonPrimary,
        unfocusedBorderColor = HairlineBorder,
        focusedLabelColor = CrimsonPrimary,
        cursorColor = CrimsonPrimary,
        focusedContainerColor = SurfaceWhite,
        unfocusedContainerColor = SurfaceWhite
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Zomato Signature Red (#E23744) Hero Header with seamless rounded sheet overlap
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CrimsonPrimary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_fatakse_logo),
                        contentDescription = "Fatakse Official Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(136.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .testTag("auth_fatakse_logo")
                    )

                    Text(
                        text = "Biryani ho ya Kirana, Fatak se ghar mangana!",
                        style = MaterialTheme.typography.titleMedium,
                        color = SurfaceWhite.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )

                    ModeBadge(isLiveSupabase = isLiveSupabase)
                }

                // Rounded top-sheet cap bridging crimson hero into crisp white form
                Surface(
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    color = SurfaceWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.BottomCenter)
                ) {}
            }

            // Crisp White Form Container (centered max 520.dp on wide screens)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!promptMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = WarmAmberSoft,
                            border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = promptMessage,
                                style = MaterialTheme.typography.labelMedium,
                                color = InkPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Instant Mobile Login Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CrimsonSoft,
                        border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Instant +91 Mobile Login for Customers, Vendors, Riders & Admin — no OTP delays.",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Segmented Login / Create Account Tabs
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AuthModeTabButton(
                                label = "Login",
                                selected = activeTab == AuthTab.LOGIN,
                                onClick = { activeTab = AuthTab.LOGIN },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_tab_login")
                            )
                            AuthModeTabButton(
                                label = "Create Account",
                                selected = activeTab == AuthTab.SIGN_UP,
                                onClick = { activeTab = AuthTab.SIGN_UP },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_tab_signup")
                            )
                        }
                    }

                    if (activeTab == AuthTab.LOGIN) {
                        // LOGIN FORM (+91 MOBILE NUMBER + OPTIONAL 4-DIGIT PIN)
                        OutlinedTextField(
                            value = loginPhone10,
                            onValueChange = { input ->
                                loginPhone10 = input.filter { it.isDigit() }.take(10)
                            },
                            label = { Text("10-Digit Mobile Number") },
                            placeholder = { Text("9876543210") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = CrimsonPrimary
                                )
                            },
                            prefix = {
                                Text(
                                    text = "+91 ",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_login_phone")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = loginPin4,
                                onValueChange = { input ->
                                    loginPin4 = input.filter { it.isDigit() }.take(4)
                                },
                                label = { Text("4-Digit PIN (Optional)") },
                                placeholder = { Text("Leave blank for instant login") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = CrimsonPrimary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showLoginPin = !showLoginPin },
                                        modifier = Modifier.testTag("auth_login_toggle_pin")
                                    ) {
                                        Icon(
                                            imageVector = if (showLoginPin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showLoginPin) "Hide PIN" else "Show PIN",
                                            tint = InkSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (showLoginPin) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_login_pin")
                            )
                            Text(
                                text = "Enter your 4-digit PIN only if you set one during sign-up.",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted
                            )
                        }

                        Button(
                            onClick = { onLogin(loginPhone10, loginPin4) },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = SurfaceWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("auth_login_submit_btn")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    color = SurfaceWhite,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "Log In with Mobile",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "New to Fatakse?",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                            TextButton(
                                onClick = {
                                    if (signUpPhone10.isBlank() && loginPhone10.isNotBlank()) {
                                        signUpPhone10 = loginPhone10
                                    }
                                    activeTab = AuthTab.SIGN_UP
                                },
                                modifier = Modifier.testTag("auth_switch_to_signup_btn")
                            ) {
                                Text(
                                    text = "Create Account",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CrimsonPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // CREATE ACCOUNT FORM (NAME + +91 MOBILE NUMBER + OPTIONAL 4-DIGIT PIN + ROLE)
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("Ameer Hamza") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = CrimsonPrimary
                                )
                            },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_name")
                        )

                        OutlinedTextField(
                            value = signUpPhone10,
                            onValueChange = { input ->
                                signUpPhone10 = input.filter { it.isDigit() }.take(10)
                            },
                            label = { Text("10-Digit Mobile Number") },
                            placeholder = { Text("9876543210") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = CrimsonPrimary
                                )
                            },
                            prefix = {
                                Text(
                                    text = "+91 ",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_phone")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = signUpPin4,
                                onValueChange = { input ->
                                    signUpPin4 = input.filter { it.isDigit() }.take(4)
                                },
                                label = { Text("Set 4-Digit PIN (Optional)") },
                                placeholder = { Text("Optional — leave blank for instant login") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = CrimsonPrimary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showSignUpPin = !showSignUpPin },
                                        modifier = Modifier.testTag("auth_signup_toggle_pin")
                                    ) {
                                        Icon(
                                            imageVector = if (showSignUpPin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showSignUpPin) "Hide PIN" else "Show PIN",
                                            tint = InkSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (showSignUpPin) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_signup_pin")
                            )
                            Text(
                                text = "Leave blank for instant 1-tap mobile login, or set a 4-digit PIN for extra protection.",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted
                            )
                        }

                        // Role Selector Pills
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Select Account Role",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val roleLabels = mapOf(
                                    UserRole.CUSTOMER to "Customer",
                                    UserRole.VENDOR to "Shopkeeper (Vendor)",
                                    UserRole.DELIVERY to "Delivery Partner",
                                    UserRole.ADMIN to "Admin"
                                )
                                UserRole.entries.forEach { role ->
                                    val isSelected = selectedRole == role
                                    Surface(
                                        onClick = { selectedRole = role },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isSelected) CrimsonPrimary else CanvasLight,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) CrimsonPrimary else HairlineBorder
                                        ),
                                        modifier = Modifier.testTag("auth_role_pill_${role.dbKey}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = roleIcon(role),
                                                contentDescription = null,
                                                tint = if (isSelected) SurfaceWhite else InkSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = roleLabels[role] ?: role.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) SurfaceWhite else InkPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Partner / Admin Passcode Check
                        AnimatedVisibility(visible = selectedRole != UserRole.CUSTOMER) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = partnerPasscode,
                                    onValueChange = { partnerPasscode = it.filter { ch -> ch.isDigit() }.take(6) },
                                    label = { Text("${selectedRole.label} Verification Passcode") },
                                    placeholder = { Text("Enter partner verification passcode") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Key,
                                            contentDescription = null,
                                            tint = CrimsonPrimary
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_partner_passcode")
                                )
                                Text(
                                    text = "Passcode protects Vendor, Delivery Partner & Admin dashboards.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkMuted
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onSignUp(
                                    fullName,
                                    signUpPhone10,
                                    signUpPin4,
                                    selectedRole,
                                    partnerPasscode
                                )
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = SurfaceWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("auth_signup_submit_btn")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    color = SurfaceWhite,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "Create Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Divider + Continue as Guest Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = HairlineBorder)
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = HairlineBorder)
                    }

                    OutlinedButton(
                        onClick = onContinueAsGuest,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("auth_continue_guest_btn")
                    ) {
                        Text(
                            text = "Continue as Guest (Browse Menu)",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun AuthModeTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = if (selected) SurfaceWhite else SurfaceMuted,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.heightIn(min = 42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) CrimsonPrimary else InkSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
