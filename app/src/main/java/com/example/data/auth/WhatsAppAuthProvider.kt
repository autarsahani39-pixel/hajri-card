package com.example.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Official WhatsApp Business Authentication Provider for Hajri Card.
 *
 * ARCHITECTURAL SPECIFICATION & SECURITY MANDATES:
 *
 * 1. Security First:
 *    - WhatsApp Business API Access Tokens and App Secrets MUST NEVER be stored
 *      in the Android app source code or BuildConfig.
 *    - The Android client communicates strictly with your designated secure backend
 *      (e.g., Firebase Cloud Functions, Cloud Run, or Node.js server).
 *
 * 2. Official Backend Verification Flow:
 *    - Step A: Android app sends phone number to backend gateway (`/auth/whatsapp/initiate`).
 *    - Step B: Backend server invokes official Meta WhatsApp Business Cloud API
 *              (`graph.facebook.com/v18.0/{phone_number_id}/messages`) to dispatch an
 *              official authentication template message.
 *    - Step C: User receives official WhatsApp message and inputs OTP / clicks verification link.
 *    - Step D: Backend validates the OTP with Meta, verifies the identity, and mints a
 *              Firebase Custom Auth Token using Firebase Admin SDK (`admin.auth().createCustomToken(uid)`).
 *    - Step E: Android app signs in directly to Firebase Auth using `signInWithCustomToken()`.
 *
 * 3. Configuration Detection:
 *    - When the backend URL or server endpoint is not configured, the provider transparently
 *      reports `AuthResult.ConfigurationRequired` without faking authentication or executing dummy OTPs.
 */
class WhatsAppAuthProvider(private val context: Context) : AuthProvider {

    override val providerType: AuthType = AuthType.WHATSAPP

    override val displayName: String = "WhatsApp Authentication"

    /**
     * Retrieve the backend server URL if configured.
     * Can be customized via SharedPreferences or BuildConfig.
     */
    val backendUrl: String
        get() {
            val prefs = context.getSharedPreferences("hajri_auth_prefs", Context.MODE_PRIVATE)
            return prefs.getString(KEY_WHATSAPP_BACKEND_URL, "") ?: ""
        }

    /**
     * Provider is ready only when a valid backend server URL has been configured.
     */
    override val isReady: Boolean
        get() = backendUrl.isNotBlank() && backendUrl.startsWith("http")

    private fun getFirebaseAuth(): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp not initialized: ${e.message}")
            null
        }
    }

    override suspend fun authenticate(credentials: AuthCredentials): AuthResult = withContext(Dispatchers.IO) {
        when (credentials) {
            is AuthCredentials.WhatsApp -> {
                val phone = credentials.phoneNumber.trim()
                val code = credentials.verificationCode?.trim()
                val session = credentials.serverSessionToken?.trim()

                if (phone.isBlank()) {
                    return@withContext AuthResult.Error("Please enter a valid mobile number with country code.")
                }

                // If not configured with a live backend gateway, return transparent requirement
                if (!isReady) {
                    Log.i(TAG, "WhatsApp backend URL is not configured. Returning ConfigurationRequired.")
                    return@withContext AuthResult.ConfigurationRequired(
                        provider = AuthType.WHATSAPP,
                        title = "WhatsApp Authentication Configuration Required",
                        message = "WhatsApp login requires a secure backend server integrated with the official Meta WhatsApp Business Cloud API and Firebase Admin SDK. Sensitive Meta App Secrets must never reside on the mobile client.",
                        setupRequirements = listOf(
                            "Meta for Developers App ID & System User Access Token",
                            "WhatsApp Business Account (WABA) & Verified Phone Number ID",
                            "Backend Endpoint (Firebase Cloud Function / Cloud Run) to dispatch Meta Authentication Templates",
                            "Firebase Admin SDK Service Account to mint Firebase Custom Auth Tokens"
                        )
                    )
                }

                // If code is not yet provided, initiate verification request with backend
                if (code.isNullOrBlank()) {
                    return@withContext initiateVerification(phone)
                } else {
                    return@withContext verifyCodeAndSignIn(
                        phoneNumber = phone,
                        sessionToken = session ?: "",
                        code = code,
                        ownerName = credentials.ownerName,
                        businessName = credentials.businessName
                    )
                }
            }

            is AuthCredentials.FirebaseCustomToken -> {
                return@withContext signInWithCustomToken(
                    customToken = credentials.customToken,
                    phoneNumber = credentials.phoneNumber,
                    ownerName = credentials.ownerName,
                    businessName = credentials.businessName
                )
            }

            else -> AuthResult.Error("Unsupported credentials passed to WhatsAppAuthProvider.")
        }
    }

    /**
     * Step 1: Call secure backend to dispatch official Meta WhatsApp authentication template.
     */
    private suspend fun initiateVerification(phoneNumber: String): AuthResult = withContext(Dispatchers.IO) {
        val endpoint = "${backendUrl.trimEnd('/')}/auth/whatsapp/initiate"
        try {
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val payload = JSONObject().apply {
                put("phoneNumber", phoneNumber)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val sessionToken = json.optString("sessionToken", "")
                val message = json.optString("message", "Official WhatsApp verification code sent.")

                if (sessionToken.isBlank()) {
                    return@withContext AuthResult.Error("Backend did not return a session token. Please try again.")
                }

                return@withContext AuthResult.RequiresVerification(
                    sessionToken = sessionToken,
                    phoneNumber = phoneNumber,
                    instructions = message
                )
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                val errorMessage = try {
                    JSONObject(errorText).optString("error", "Server returned HTTP $responseCode")
                } catch (_: Exception) {
                    "Backend error (HTTP $responseCode)"
                }
                return@withContext AuthResult.Error("WhatsApp verification failed: $errorMessage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to WhatsApp backend endpoint", e)
            return@withContext AuthResult.Error("Network error contacting WhatsApp authentication server: ${e.localizedMessage}", e)
        }
    }

    /**
     * Step 2: Submit code to backend, receive custom Firebase Auth token, and sign in.
     */
    private suspend fun verifyCodeAndSignIn(
        phoneNumber: String,
        sessionToken: String,
        code: String,
        ownerName: String,
        businessName: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val endpoint = "${backendUrl.trimEnd('/')}/auth/whatsapp/verify"
        try {
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val payload = JSONObject().apply {
                put("phoneNumber", phoneNumber)
                put("sessionToken", sessionToken)
                put("code", code)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val customToken = json.optString("customToken", "")

                if (customToken.isBlank()) {
                    return@withContext AuthResult.Error("Backend validation succeeded, but no Firebase token was received.")
                }

                // Step 3: Firebase Auth Sign In with Custom Token
                return@withContext signInWithCustomToken(
                    customToken = customToken,
                    phoneNumber = phoneNumber,
                    ownerName = ownerName,
                    businessName = businessName
                )
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                val errorMessage = try {
                    JSONObject(errorText).optString("error", "Invalid or expired verification code")
                } catch (_: Exception) {
                    "Verification failed (HTTP $responseCode)"
                }
                return@withContext AuthResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify WhatsApp code with backend", e)
            return@withContext AuthResult.Error("Network error verifying code: ${e.localizedMessage}", e)
        }
    }

    /**
     * Sign in to Firebase Authentication with Custom Token issued by verified backend.
     */
    private suspend fun signInWithCustomToken(
        customToken: String,
        phoneNumber: String,
        ownerName: String,
        businessName: String
    ): AuthResult {
        val auth = getFirebaseAuth()
            ?: return AuthResult.Error("Firebase Authentication is not available on this device.")

        return try {
            val authResult = auth.signInWithCustomToken(customToken).await()
            val user = authResult.user
            val profile = UserProfile(
                isLoggedIn = true,
                email = user?.email ?: "",
                mobileNumber = phoneNumber.ifBlank { user?.phoneNumber ?: "" },
                ownerName = ownerName,
                businessName = businessName,
                loginTimestamp = System.currentTimeMillis(),
                firebaseUid = user?.uid ?: ""
            )
            AuthResult.Success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signInWithCustomToken failed", e)
            val msg = if (e is FirebaseAuthException) {
                "Firebase Authentication rejected custom token: ${e.localizedMessage}"
            } else {
                "Authentication error: ${e.localizedMessage ?: "Failed to sign in"}"
            }
            AuthResult.Error(msg, e)
        }
    }

    override suspend fun signOut() {
        try {
            getFirebaseAuth()?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out", e)
        }
    }

    companion object {
        private const val TAG = "WhatsAppAuthProvider"
        const val KEY_WHATSAPP_BACKEND_URL = "whatsapp_auth_backend_url"
    }
}
