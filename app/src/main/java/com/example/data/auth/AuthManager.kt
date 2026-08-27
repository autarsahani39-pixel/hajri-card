package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val mobileNumber: String = "",
    val ownerName: String = "",
    val businessName: String = "",
    val loginTimestamp: Long = 0L,
    val firebaseUid: String = ""
)

/**
 * Modular Authentication Manager for Hajri Card.
 *
 * Architecture:
 * AuthManager
 *  ├── Email/Password Provider (Firebase Auth - Active)
 *  ├── WhatsApp Provider (Future Meta Business API Backend - Staged)
 *  ├── Extensible for Google & Phone OTP
 */
class AuthManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("hajri_auth_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    // Pluggable Auth Providers
    private val emailPasswordProvider: EmailPasswordAuthProvider = EmailPasswordAuthProvider(context)
    private val whatsAppProvider: WhatsAppAuthProvider = WhatsAppAuthProvider(context)
    private val phoneAuthProvider: PhoneAuthProviderImpl = PhoneAuthProviderImpl(context)

    private val registeredProviders: Map<AuthType, AuthProvider> = mapOf(
        AuthType.EMAIL_PASSWORD to emailPasswordProvider,
        AuthType.WHATSAPP to whatsAppProvider,
        AuthType.PHONE_OTP to phoneAuthProvider
    )

    init {
        setupFirebaseAuthListener()
    }

    /**
     * Retrieve any registered authentication provider.
     */
    fun getProvider(type: AuthType): AuthProvider? = registeredProviders[type]

    /**
     * Unified authentication entrypoint for any credentials type.
     */
    suspend fun authenticate(credentials: AuthCredentials): AuthResult {
        val provider = when (credentials) {
            is AuthCredentials.EmailPassword -> emailPasswordProvider
            is AuthCredentials.WhatsApp,
            is AuthCredentials.FirebaseCustomToken -> whatsAppProvider
            is AuthCredentials.Google -> null
            is AuthCredentials.PhoneOtp -> phoneAuthProvider
        } ?: return AuthResult.Error("No provider registered for given credentials.")

        val result = provider.authenticate(credentials)
        if (result is AuthResult.Success) {
            saveSession(result.profile)
        }
        return result
    }

    private fun getFirebaseAuth(): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp initialization / FirebaseAuth error: ${e.message}")
            try {
                FirebaseAuth.getInstance()
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun setupFirebaseAuthListener() {
        try {
            val auth = getFirebaseAuth() ?: return
            authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                val isFirebaseLoggedIn = currentUser != null
                val email = currentUser?.email ?: prefs.getString(KEY_EMAIL, "") ?: ""
                val mobile = currentUser?.phoneNumber ?: prefs.getString(KEY_MOBILE, "") ?: ""
                val name = prefs.getString(KEY_OWNER_NAME, "") ?: ""
                val business = prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
                val timestamp = prefs.getLong(KEY_LOGIN_TIME, 0L)
                val uid = currentUser?.uid ?: ""

                _userProfile.value = UserProfile(
                    isLoggedIn = isFirebaseLoggedIn,
                    email = if (isFirebaseLoggedIn) email else "",
                    mobileNumber = if (isFirebaseLoggedIn) mobile else "",
                    ownerName = name,
                    businessName = business,
                    loginTimestamp = if (isFirebaseLoggedIn) timestamp else 0L,
                    firebaseUid = uid
                )
            }
            auth.addAuthStateListener(authStateListener!!)
        } catch (e: Exception) {
            Log.w(TAG, "Could not attach AuthStateListener: ${e.message}")
        }
    }

    private fun loadProfile(): UserProfile {
        val auth = getFirebaseAuth()
        val currentUser = auth?.currentUser
        val isFirebaseLoggedIn = currentUser != null

        val email = currentUser?.email ?: prefs.getString(KEY_EMAIL, "") ?: ""
        val mobile = currentUser?.phoneNumber ?: prefs.getString(KEY_MOBILE, "") ?: ""
        val name = prefs.getString(KEY_OWNER_NAME, "") ?: ""
        val business = prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
        val timestamp = prefs.getLong(KEY_LOGIN_TIME, 0L)
        val uid = currentUser?.uid ?: ""

        return UserProfile(
            isLoggedIn = isFirebaseLoggedIn,
            email = if (isFirebaseLoggedIn) email else "",
            mobileNumber = if (isFirebaseLoggedIn) mobile else "",
            ownerName = name,
            businessName = business,
            loginTimestamp = if (isFirebaseLoggedIn) timestamp else 0L,
            firebaseUid = uid
        )
    }

    private fun saveSession(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_MOBILE, profile.mobileNumber)
            .putString(KEY_OWNER_NAME, profile.ownerName)
            .putString(KEY_BUSINESS_NAME, profile.businessName)
            .putLong(KEY_LOGIN_TIME, profile.loginTimestamp)
            .apply()

        _userProfile.value = profile
    }

    /**
     * Sign In with Firebase Email & Password
     */
    fun signInWithEmail(
        email: String,
        pass: String,
        ownerName: String = "",
        businessName: String = "",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onFailure("Please enter a valid email address (e.g. yourname@gmail.com)")
            return
        }

        if (cleanPass.length < 6) {
            onFailure("Password must be at least 6 characters long!")
            return
        }

        val auth = getFirebaseAuth()
        if (auth == null) {
            onFailure("Firebase Authentication is not available. Please check google-services.json.")
            return
        }

        auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val uid = firebaseUser?.uid ?: ""
                    val finalEmail = firebaseUser?.email ?: cleanEmail
                    val finalName = if (ownerName.isNotBlank()) ownerName.trim() else prefs.getString(KEY_OWNER_NAME, "") ?: ""
                    val finalBusiness = if (businessName.isNotBlank()) businessName.trim() else prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
                    val timestamp = System.currentTimeMillis()

                    val profile = UserProfile(
                        isLoggedIn = true,
                        email = finalEmail,
                        mobileNumber = "",
                        ownerName = finalName,
                        businessName = finalBusiness,
                        loginTimestamp = timestamp,
                        firebaseUid = uid
                    )
                    saveSession(profile)

                    Log.d(TAG, "Firebase user logged in successfully: $uid")
                    onSuccess()
                } else {
                    val exception = task.exception
                    Log.e(TAG, "Sign in with email failed", exception)
                    val errorMsg = mapAuthException(exception, isRegister = false)
                    onFailure(errorMsg)
                }
            }
    }

    /**
     * Create / Register a new Firebase Account with Email & Password
     */
    fun registerWithEmail(
        email: String,
        pass: String,
        ownerName: String,
        businessName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onFailure("Please enter a valid email address (e.g. yourname@gmail.com)")
            return
        }

        if (cleanPass.length < 6) {
            onFailure("Password must be at least 6 characters long!")
            return
        }

        val auth = getFirebaseAuth()
        if (auth == null) {
            onFailure("Firebase Authentication is not available.")
            return
        }

        auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val uid = firebaseUser?.uid ?: ""
                    val finalEmail = firebaseUser?.email ?: cleanEmail
                    val finalName = if (ownerName.isNotBlank()) ownerName.trim() else ""
                    val finalBusiness = if (businessName.isNotBlank()) businessName.trim() else ""
                    val timestamp = System.currentTimeMillis()

                    val profile = UserProfile(
                        isLoggedIn = true,
                        email = finalEmail,
                        mobileNumber = "",
                        ownerName = finalName,
                        businessName = finalBusiness,
                        loginTimestamp = timestamp,
                        firebaseUid = uid
                    )
                    saveSession(profile)

                    Log.d(TAG, "Firebase user created successfully: $uid")
                    onSuccess()
                } else {
                    val exception = task.exception
                    Log.e(TAG, "User registration failed", exception)
                    val errorMsg = mapAuthException(exception, isRegister = true)
                    onFailure(errorMsg)
                }
            }
    }

    /**
     * Send Password Reset Email Link
     */
    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onFailure("Please enter a valid email address (e.g. yourname@gmail.com)")
            return
        }

        val auth = getFirebaseAuth()
        if (auth == null) {
            val err = "Firebase Authentication is not initialized or unavailable."
            Log.e(TAG, "sendPasswordResetEmail: $err")
            onFailure(err)
            return
        }

        Log.i(TAG, "Dispatching Firebase sendPasswordResetEmail to: '$cleanEmail' (App: ${auth.app.name})")

        auth.sendPasswordResetEmail(cleanEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Firebase sendPasswordResetEmail SUCCEEDED for: '$cleanEmail'")
                    onSuccess()
                } else {
                    val exception = task.exception
                    Log.e(TAG, "Firebase sendPasswordResetEmail FAILED for: '$cleanEmail'", exception)

                    val errorCode = (exception as? FirebaseAuthException)?.errorCode ?: ""
                    val rawMsg = exception?.localizedMessage ?: exception?.message ?: "Unknown Firebase error"

                    val errorMsg = when {
                        exception is FirebaseAuthInvalidUserException || rawMsg.contains("user-not-found", ignoreCase = true) ->
                            "❌ [User Not Found]: No Firebase account registered with '$cleanEmail'. Please select 'Create Account' to sign up first."
                        rawMsg.contains("operation-not-allowed", ignoreCase = true) || errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
                            "❌ [Provider Disabled]: Email/Password provider is disabled in Firebase Console. Please enable Email/Password under Authentication > Sign-in method in Firebase Console."
                        rawMsg.contains("invalid-email", ignoreCase = true) || errorCode == "ERROR_INVALID_EMAIL" ->
                            "❌ [Invalid Email]: The email address format is invalid."
                        rawMsg.contains("too-many-requests", ignoreCase = true) || errorCode == "ERROR_TOO_MANY_REQUESTS" ->
                            "❌ [Rate Limited]: We have blocked all requests from this device due to unusual activity. Try again later."
                        rawMsg.contains("network", ignoreCase = true) || rawMsg.contains("timeout", ignoreCase = true) ->
                            "❌ [Network Error]: Could not reach Firebase servers. Please verify your internet connection."
                        else ->
                            "❌ Firebase Auth Error (${errorCode.ifBlank { exception?.javaClass?.simpleName ?: "Error" }}): $rawMsg"
                    }
                    onFailure(errorMsg)
                }
            }
    }

    private fun mapAuthException(e: Exception?, isRegister: Boolean): String {
        if (e == null) return "❌ Authentication failed. Please try again."
        val msg = e.message ?: ""

        return when {
            e is FirebaseAuthInvalidUserException || msg.contains("user-not-found", true) ->
                "❌ No account found with this email. Please select 'Register' to create a new account."
            e is FirebaseAuthInvalidCredentialsException || msg.contains("wrong-password", true) || msg.contains("invalid-credential", true) ->
                "❌ Incorrect password or invalid credentials! Please verify your password."
            e is FirebaseAuthUserCollisionException || msg.contains("email-already-in-use", true) ->
                "❌ This email is already registered! Please sign in using the 'Login' tab."
            e is FirebaseAuthWeakPasswordException || msg.contains("weak-password", true) ->
                "❌ Password is too weak. Please use at least 6 characters with letters and numbers."
            msg.contains("invalid-email", true) || msg.contains("badly formatted", true) ->
                "❌ Invalid email address format! Please enter a valid email (e.g. owner@gmail.com)."
            msg.contains("user-disabled", true) ->
                "❌ This user account has been disabled by the administrator."
            msg.contains("too-many-requests", true) ->
                "❌ Too many failed attempts. For security reasons, please wait a few minutes before trying again."
            msg.contains("network", true) || msg.contains("timeout", true) ->
                "❌ Network connection issue! Please check your internet connection and retry."
            else -> "❌ Error: ${e.localizedMessage ?: "Authentication failed"}"
        }
    }

    /**
     * Check if WhatsApp authentication backend is actively configured.
     */
    fun isWhatsAppConfigured(): Boolean {
        return whatsAppProvider.isReady
    }

    /**
     * Get configured WhatsApp backend URL.
     */
    fun getWhatsAppBackendUrl(): String {
        return whatsAppProvider.backendUrl
    }

    /**
     * Set or update WhatsApp backend gateway URL.
     */
    fun setWhatsAppBackendUrl(url: String) {
        prefs.edit().putString(WhatsAppAuthProvider.KEY_WHATSAPP_BACKEND_URL, url.trim()).apply()
    }

    /**
     * Sign in to Firebase Authentication using a Custom Token minted by the secure backend.
     */
    fun signInWithCustomToken(
        customToken: String,
        phoneNumber: String = "",
        ownerName: String = "",
        businessName: String = "",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val auth = getFirebaseAuth()
        if (auth == null) {
            onFailure("Firebase Authentication is not available on this device.")
            return
        }

        if (customToken.isBlank()) {
            onFailure("Invalid custom token received from backend.")
            return
        }

        auth.signInWithCustomToken(customToken)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val uid = firebaseUser?.uid ?: ""
                    val finalEmail = firebaseUser?.email ?: ""
                    val finalMobile = phoneNumber.ifBlank { firebaseUser?.phoneNumber ?: "" }
                    val finalName = if (ownerName.isNotBlank()) ownerName.trim() else prefs.getString(KEY_OWNER_NAME, "") ?: ""
                    val finalBusiness = if (businessName.isNotBlank()) businessName.trim() else prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
                    val timestamp = System.currentTimeMillis()

                    val profile = UserProfile(
                        isLoggedIn = true,
                        email = finalEmail,
                        mobileNumber = finalMobile,
                        ownerName = finalName,
                        businessName = finalBusiness,
                        loginTimestamp = timestamp,
                        firebaseUid = uid
                    )
                    saveSession(profile)

                    Log.d(TAG, "Firebase custom token sign in successful: $uid (Phone: $finalMobile)")
                    onSuccess()
                } else {
                    val exception = task.exception
                    Log.e(TAG, "Sign in with custom token failed", exception)
                    val errorMsg = exception?.localizedMessage ?: "Failed to authenticate with Firebase custom token"
                    onFailure("❌ Firebase Auth Error: $errorMsg")
                }
            }
    }

    fun updateProfile(ownerName: String, businessName: String) {
        val current = _userProfile.value
        val finalName = ownerName.trim()
        val finalBusiness = businessName.trim()

        prefs.edit()
            .putString(KEY_OWNER_NAME, finalName)
            .putString(KEY_BUSINESS_NAME, finalBusiness)
            .apply()

        _userProfile.value = current.copy(
            ownerName = finalName,
            businessName = finalBusiness
        )
    }

    /**
     * Send Phone OTP using Firebase PhoneAuthProvider.
     */
    fun sendPhoneOtp(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        timeoutSeconds: Long = 60L
    ) {
        phoneAuthProvider.sendVerificationCode(activity, phoneNumber, callbacks, timeoutSeconds)
    }

    /**
     * Verify Phone OTP and sign in to Firebase.
     */
    fun verifyPhoneOtp(
        verificationId: String,
        code: String,
        ownerName: String = "",
        businessName: String = "",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        phoneAuthProvider.signInWithVerificationCode(
            verificationId = verificationId,
            code = code,
            ownerName = ownerName,
            businessName = businessName,
            onSuccess = { profile ->
                saveSession(profile)
                onSuccess()
            },
            onFailure = onFailure
        )
    }

    fun logout() {
        try {
            getFirebaseAuth()?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out of Firebase", e)
        }

        _userProfile.value = _userProfile.value.copy(
            isLoggedIn = false,
            firebaseUid = "",
            email = "",
            mobileNumber = ""
        )
    }

    companion object {
        private const val TAG = "HajriAuthManager"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_MOBILE = "user_mobile"
        private const val KEY_OWNER_NAME = "owner_name"
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_LOGIN_TIME = "login_timestamp"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
