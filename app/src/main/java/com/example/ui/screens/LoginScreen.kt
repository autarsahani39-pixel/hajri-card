package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.auth.AuthResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.HajriViewModel

@Composable
fun LoginScreen(
    viewModel: HajriViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()

    // 0: Sign In, 1: Register (Create Account)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var isResetLoading by remember { mutableStateOf(false) }
    var resetDialogError by remember { mutableStateOf<String?>(null) }

    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var whatsAppNoticeMessage by remember { mutableStateOf<String?>(null) }
    var whatsAppConfigRequirements by remember { mutableStateOf<List<String>>(emptyList()) }
    var backendUrlInput by remember { mutableStateOf(viewModel.getWhatsAppBackendUrl()) }

    var isWhatsAppLoading by remember { mutableStateOf(false) }
    var showWhatsAppVerifyDialog by remember { mutableStateOf(false) }
    var whatsAppPhoneNumber by remember { mutableStateOf("+91") }
    var whatsAppSessionToken by remember { mutableStateOf("") }
    var whatsAppOtpCode by remember { mutableStateOf("") }
    var whatsAppVerifyError by remember { mutableStateOf<String?>(null) }
    var isWhatsAppVerifying by remember { mutableStateOf(false) }
    var isWhatsAppStep2 by remember { mutableStateOf(false) }


    fun handleSignIn() {
        val cleanEmail = email.trim()
        val cleanPass = password.trim()

        if (cleanEmail.isBlank()) {
            errorMessage = "Please enter your email address."
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            errorMessage = "Please enter a valid email address (e.g. owner@gmail.com)."
            return
        }
        if (cleanPass.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }

        errorMessage = null
        successMessage = null
        isLoading = true
        keyboardController?.hide()

        viewModel.signInWithEmail(
            email = cleanEmail,
            pass = cleanPass,
            ownerName = ownerName,
            businessName = businessName,
            onSuccess = {
                isLoading = false
                Toast.makeText(context, "Login successful! Welcome back.", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            },
            onFailure = { err ->
                isLoading = false
                errorMessage = err
            }
        )
    }

    fun handleRegister() {
        val cleanEmail = email.trim()
        val cleanPass = password.trim()
        val cleanConfirmPass = confirmPassword.trim()

        if (cleanEmail.isBlank()) {
            errorMessage = "Please enter your email address."
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            errorMessage = "Please enter a valid email address (e.g. owner@gmail.com)."
            return
        }
        if (cleanPass.length < 6) {
            errorMessage = "For security, password must be at least 6 characters."
            return
        }
        if (cleanPass != cleanConfirmPass) {
            errorMessage = "Passwords do not match! Please check again."
            return
        }

        errorMessage = null
        successMessage = null
        isLoading = true
        keyboardController?.hide()

        viewModel.registerWithEmail(
            email = cleanEmail,
            pass = cleanPass,
            ownerName = ownerName,
            businessName = businessName,
            onSuccess = {
                isLoading = false
                Toast.makeText(context, "Account created successfully! Welcome.", Toast.LENGTH_LONG).show()
                onLoginSuccess()
            },
            onFailure = { err ->
                isLoading = false
                errorMessage = err
            }
        )
    }

    fun handleSendResetPassword() {
        val cleanResetEmail = resetEmailInput.trim()
        if (cleanResetEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanResetEmail).matches()) {
            resetDialogError = "Please enter a valid email address (e.g. owner@gmail.com)!"
            return
        }

        resetDialogError = null
        isResetLoading = true
        viewModel.sendPasswordReset(
            email = cleanResetEmail,
            onSuccess = {
                isResetLoading = false
                showForgotPasswordDialog = false
                resetDialogError = null
                successMessage = "Password reset request dispatched to $cleanResetEmail."
                Toast.makeText(context, "Firebase: Reset email dispatched!", Toast.LENGTH_LONG).show()
            },
            onFailure = { err ->
                isResetLoading = false
                resetDialogError = err
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Language Switcher Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = { showLanguageDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder(),
                    shadowElevation = 1.dp,
                    modifier = Modifier.testTag("btn_login_language_picker")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = "Change Language",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentLanguage.nativeName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- HERO BRANDING LOGO & TITLE ---
            Image(
                painter = painterResource(id = com.example.R.drawable.ic_brand_logo),
                contentDescription = "Hajri Card Logo",
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(22.dp)
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.app_tagline),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- AUTHENTICATION CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // TAB SELECTOR: SIGN IN / REGISTER
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = {
                                selectedTabIndex = 0
                                errorMessage = null
                                successMessage = null
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.login_sign_in_tab),
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp
                                )
                            },
                            icon = {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.testTag("login_tab_signin")
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = {
                                selectedTabIndex = 1
                                errorMessage = null
                                successMessage = null
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.login_create_account_tab),
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp
                                )
                            },
                            icon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.testTag("login_tab_register")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Error Message Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Success Message Banner
                    AnimatedVisibility(
                        visible = successMessage != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MarkEmailRead,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = successMessage ?: "",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 12.5.sp,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "⚠️ Note: If you don't see it in Primary inbox, please check your Spam / Junk / Promotions folder.\n💡 If you are a new user and haven't created an account yet, please select 'Create Account' above.",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedTabIndex = 1
                                            if (resetEmailInput.isNotBlank()) {
                                                email = resetEmailInput
                                            }
                                        }
                                    ) {
                                        Text("Go to Create Account", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                                    addCategory(Intent.CATEGORY_APP_EMAIL)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com"))
                                                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(webIntent)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Gmail", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // EMAIL FIELD
                    OutlinedTextField(
                        value = email,
                        onValueChange = { input ->
                            email = input
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text(stringResource(R.string.login_email_label)) },
                        placeholder = { Text(stringResource(R.string.login_email_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )

                    // PASSWORD FIELD
                    OutlinedTextField(
                        value = password,
                        onValueChange = { input ->
                            password = input
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text(stringResource(R.string.login_password_label)) },
                        placeholder = { Text(stringResource(R.string.login_password_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (selectedTabIndex == 1) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (selectedTabIndex == 0) handleSignIn()
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    // REGISTER SPECIFIC FIELDS
                    if (selectedTabIndex == 1) {
                        // CONFIRM PASSWORD
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { input ->
                                confirmPassword = input
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text(stringResource(R.string.login_confirm_password_label)) },
                            placeholder = { Text(stringResource(R.string.login_confirm_password_placeholder)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_confirm_password_input")
                        )

                        // OWNER NAME (Optional)
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text(stringResource(R.string.login_owner_name_label)) },
                            placeholder = { Text(stringResource(R.string.login_owner_name_placeholder)) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_owner_name_input")
                        )

                        // FIRM / COMPANY NAME (Optional)
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text(stringResource(R.string.login_business_name_label)) },
                            placeholder = { Text(stringResource(R.string.login_business_name_placeholder)) },
                            leadingIcon = {
                                Icon(Icons.Default.Business, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { handleRegister() }),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_business_name_input")
                        )
                    }

                    // FORGOT PASSWORD (In Login Mode)
                    if (selectedTabIndex == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    resetEmailInput = email
                                    showForgotPasswordDialog = true
                                },
                                modifier = Modifier.testTag("btn_forgot_password")
                            ) {
                                Text(
                                    text = stringResource(R.string.login_forgot_password_btn),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // SUBMIT BUTTON (SIGN IN OR REGISTER)
                    Button(
                        onClick = {
                            if (selectedTabIndex == 0) handleSignIn()
                            else handleRegister()
                        },
                        enabled = !isLoading && email.isNotBlank() && password.length >= 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_auth"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (selectedTabIndex == 0) stringResource(R.string.login_signing_in) else stringResource(R.string.login_creating_account),
                                fontSize = 14.sp
                            )
                        } else {
                            if (selectedTabIndex == 0) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.login_sign_in_btn),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            } else {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.login_create_account_btn),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                )
                            }
                        }
                    }

                    // DIVIDER WITH OR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = stringResource(R.string.login_or_divider),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }

                    // CONTINUE WITH WHATSAPP
                    val isWhatsAppConfigured = viewModel.isWhatsAppConfigured()
                    Surface(
                        onClick = {
                            if (isWhatsAppLoading) return@Surface
                            errorMessage = null
                            successMessage = null
                            keyboardController?.hide()

                            if (isWhatsAppConfigured) {
                                showWhatsAppVerifyDialog = true
                                isWhatsAppStep2 = false
                                whatsAppVerifyError = null
                                whatsAppOtpCode = ""
                            } else {
                                isWhatsAppLoading = true
                                viewModel.requestWhatsAppLogin(whatsAppPhoneNumber) { result ->
                                    isWhatsAppLoading = false
                                    when (result) {
                                        is AuthResult.ConfigurationRequired -> {
                                            whatsAppNoticeMessage = result.message
                                            whatsAppConfigRequirements = result.setupRequirements
                                            showWhatsAppDialog = true
                                        }
                                        is AuthResult.FutureIntegration -> {
                                            whatsAppNoticeMessage = result.message
                                            showWhatsAppDialog = true
                                        }
                                        is AuthResult.RequiresVerification -> {
                                            whatsAppSessionToken = result.sessionToken
                                            isWhatsAppStep2 = true
                                            showWhatsAppVerifyDialog = true
                                        }
                                        is AuthResult.Success -> {
                                            onLoginSuccess()
                                        }
                                        is AuthResult.Error -> {
                                            errorMessage = result.message
                                        }
                                        else -> {
                                            showWhatsAppDialog = true
                                        }
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_continue_whatsapp")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isWhatsAppLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF075E54),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Connecting WhatsApp...",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF075E54)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp),
                                    contentDescription = "WhatsApp",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.login_whatsapp_btn),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF075E54)
                                )
                                if (!isWhatsAppConfigured) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF075E54).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.login_badge_soon),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF075E54),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom info label
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (selectedTabIndex == 0)
                                    "Google Firebase Secure Email Authentication. If you don't have an account, select 'Create Account' above."
                                else
                                    "Once created, you can sign in anytime using this email and password.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer security badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Firebase Secure Authentication • 100% Safe Local & Cloud Data",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // LANGUAGE SELECTION DIALOG FOR LOGIN SCREEN
    if (showLanguageDialog) {
        val languages = viewModel.getSupportedLanguages()
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            icon = {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_select_language_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.forEach { lang ->
                        val isSelected = lang.code == currentLanguage.code
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = viewModel.setLanguage(lang.code)
                                    showLanguageDialog = false
                                    val toastMsg = context.getString(R.string.settings_language_changed_toast, updated.nativeName)
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) CardDefaults.outlinedCardBorder() else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            val updated = viewModel.setLanguage(lang.code)
                                            showLanguageDialog = false
                                            val toastMsg = context.getString(R.string.settings_language_changed_toast, updated.nativeName)
                                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = lang.nativeName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (lang.code != "en") {
                                            Text(
                                                text = lang.englishName,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.common_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // FORGOT PASSWORD DIALOG
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResetLoading) showForgotPasswordDialog = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Password Reset Link", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your registered email address. Firebase will send a password reset link to your inbox:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = {
                            resetEmailInput = it
                            if (resetDialogError != null) resetDialogError = null
                        },
                        label = { Text(stringResource(R.string.login_email_label)) },
                        placeholder = { Text(stringResource(R.string.login_email_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_reset_email")
                    )

                    if (resetDialogError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = resetDialogError ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { handleSendResetPassword() },
                    enabled = !isResetLoading && resetEmailInput.isNotBlank(),
                    modifier = Modifier.testTag("btn_confirm_send_reset")
                ) {
                    if (isResetLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        resetDialogError = null
                    },
                    enabled = !isResetLoading
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // WHATSAPP AUTHENTICATION CONFIGURATION / ARCHITECTURE DIALOG
    if (showWhatsAppDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppDialog = false },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = "WhatsApp",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.login_whatsapp_not_configured_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF075E54),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Official Meta Cloud API • Enterprise Security",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF075E54)
                            )
                        }
                    }

                    Text(
                        text = whatsAppNoticeMessage
                            ?: stringResource(R.string.login_whatsapp_not_configured_desc),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    if (whatsAppConfigRequirements.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Required Components:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                whatsAppConfigRequirements.forEach { req ->
                                    Text(
                                        text = "• $req",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Optional Backend Gateway URL Input
                    OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = { backendUrlInput = it },
                        label = { Text(stringResource(R.string.login_whatsapp_backend_url_label), fontSize = 11.sp) },
                        placeholder = { Text(stringResource(R.string.login_whatsapp_backend_url_placeholder), fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "🔒 Meta client secrets and Firebase Admin credentials stay securely on the backend server.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (backendUrlInput.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setWhatsAppBackendUrl(backendUrlInput)
                                showWhatsAppDialog = false
                                successMessage = "WhatsApp backend endpoint updated."
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(R.string.login_whatsapp_save_backend_btn), fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = { showWhatsAppDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sign In with Email", fontSize = 12.sp)
                    }
                }
            }
        )
    }

    // WHATSAPP LIVE VERIFICATION FLOW DIALOG
    if (showWhatsAppVerifyDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isWhatsAppVerifying) {
                    showWhatsAppVerifyDialog = false
                    whatsAppVerifyError = null
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = "WhatsApp",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.login_whatsapp_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (whatsAppVerifyError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = whatsAppVerifyError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (!isWhatsAppStep2) {
                        // STEP 1: Phone Number Input
                        Text(
                            text = "Enter your WhatsApp phone number to receive an official verification code via Meta Cloud API.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        OutlinedTextField(
                            value = whatsAppPhoneNumber,
                            onValueChange = { whatsAppPhoneNumber = it },
                            label = { Text(stringResource(R.string.login_whatsapp_phone_label)) },
                            placeholder = { Text(stringResource(R.string.login_whatsapp_phone_placeholder)) },
                            leadingIcon = {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF25D366))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // STEP 2: Code Verification
                        Text(
                            text = "Enter the 6-digit verification code sent to your WhatsApp number ($whatsAppPhoneNumber).",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        OutlinedTextField(
                            value = whatsAppOtpCode,
                            onValueChange = { if (it.length <= 6) whatsAppOtpCode = it },
                            label = { Text(stringResource(R.string.login_whatsapp_code_label)) },
                            placeholder = { Text(stringResource(R.string.login_whatsapp_code_placeholder)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF25D366))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isWhatsAppStep2) {
                            // Dispatch Step 1
                            if (whatsAppPhoneNumber.isBlank() || whatsAppPhoneNumber.length < 8) {
                                whatsAppVerifyError = "Please enter a valid phone number with country code (e.g. +91 9876543210)"
                                return@Button
                            }
                            isWhatsAppVerifying = true
                            whatsAppVerifyError = null
                            viewModel.requestWhatsAppLogin(whatsAppPhoneNumber) { result ->
                                isWhatsAppVerifying = false
                                when (result) {
                                    is AuthResult.RequiresVerification -> {
                                        whatsAppSessionToken = result.sessionToken
                                        isWhatsAppStep2 = true
                                    }
                                    is AuthResult.Success -> {
                                        showWhatsAppVerifyDialog = false
                                        onLoginSuccess()
                                    }
                                    is AuthResult.Error -> {
                                        whatsAppVerifyError = result.message
                                    }
                                    else -> {
                                        whatsAppVerifyError = "Verification failed. Please try again."
                                    }
                                }
                            }
                        } else {
                            // Dispatch Step 2
                            if (whatsAppOtpCode.isBlank() || whatsAppOtpCode.length < 4) {
                                whatsAppVerifyError = "Please enter the verification code received on WhatsApp."
                                return@Button
                            }
                            isWhatsAppVerifying = true
                            whatsAppVerifyError = null
                            viewModel.verifyWhatsAppCode(
                                phoneNumber = whatsAppPhoneNumber,
                                sessionToken = whatsAppSessionToken,
                                code = whatsAppOtpCode,
                                ownerName = ownerName,
                                businessName = businessName
                            ) { result ->
                                isWhatsAppVerifying = false
                                when (result) {
                                    is AuthResult.Success -> {
                                        showWhatsAppVerifyDialog = false
                                        onLoginSuccess()
                                    }
                                    is AuthResult.Error -> {
                                        whatsAppVerifyError = result.message
                                    }
                                    else -> {
                                        whatsAppVerifyError = "Could not complete WhatsApp authentication."
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isWhatsAppVerifying,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isWhatsAppVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        if (!isWhatsAppStep2) stringResource(R.string.login_whatsapp_send_code_btn)
                        else stringResource(R.string.login_whatsapp_verify_btn)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWhatsAppVerifyDialog = false
                        whatsAppVerifyError = null
                        isWhatsAppStep2 = false
                    },
                    enabled = !isWhatsAppVerifying
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

