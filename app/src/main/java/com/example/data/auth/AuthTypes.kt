package com.example.data.auth

/**
 * Supported and Planned Authentication Providers for Hajri Card.
 */
enum class AuthType(val displayName: String) {
    EMAIL_PASSWORD("Email & Password"),
    WHATSAPP("WhatsApp Authentication"),
    GOOGLE("Google Sign-In"),
    PHONE_OTP("Phone SMS OTP")
}

/**
 * Generic Credentials passed to AuthProviders.
 */
sealed interface AuthCredentials {
    data class EmailPassword(
        val email: String,
        val password: String,
        val ownerName: String = "",
        val businessName: String = "",
        val isRegistration: Boolean = false
    ) : AuthCredentials

    /**
     * Official WhatsApp Credentials.
     * When Meta/WhatsApp Business Cloud API backend is integrated:
     * 1. Android client initiates verification with the phone number.
     * 2. The secure backend calls Meta Cloud API to dispatch official auth template.
     * 3. User verifies, backend mints custom Firebase token, and returns it.
     * Sensitive tokens and Meta secrets are NEVER stored or processed on the Android client.
     */
    data class WhatsApp(
        val phoneNumber: String,
        val verificationCode: String? = null,
        val serverSessionToken: String? = null,
        val ownerName: String = "",
        val businessName: String = ""
    ) : AuthCredentials

    data class FirebaseCustomToken(
        val customToken: String,
        val phoneNumber: String = "",
        val ownerName: String = "",
        val businessName: String = ""
    ) : AuthCredentials

    data class Google(
        val idToken: String
    ) : AuthCredentials

    data class PhoneOtp(
        val phoneNumber: String,
        val otpCode: String
    ) : AuthCredentials
}

/**
 * Result returned by AuthProvider authentication calls.
 */
sealed class AuthResult {
    data class Success(val profile: UserProfile) : AuthResult()
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult()
    data class RequiresVerification(
        val sessionToken: String,
        val phoneNumber: String,
        val instructions: String
    ) : AuthResult()
    data class AccountLinkingRequired(
        val existingEmail: String,
        val customToken: String,
        val message: String
    ) : AuthResult()
    data class ConfigurationRequired(
        val provider: AuthType,
        val title: String,
        val message: String,
        val setupRequirements: List<String> = emptyList()
    ) : AuthResult()
    data class FutureIntegration(
        val provider: AuthType,
        val title: String,
        val message: String,
        val requiresBackendSetup: Boolean = true
    ) : AuthResult()
}

