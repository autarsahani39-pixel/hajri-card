package com.example.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

/**
 * Firebase Email & Password Authentication Provider.
 */
class EmailPasswordAuthProvider(private val context: Context) : AuthProvider {

    override val providerType: AuthType = AuthType.EMAIL_PASSWORD

    override val displayName: String = "Email & Password"

    override val isReady: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

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

    override suspend fun authenticate(credentials: AuthCredentials): AuthResult {
        if (credentials !is AuthCredentials.EmailPassword) {
            return AuthResult.Error("Invalid credentials passed to EmailPasswordAuthProvider.")
        }

        val cleanEmail = credentials.email.trim()
        val cleanPass = credentials.password.trim()

        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return AuthResult.Error("Please enter a valid email address (e.g. yourname@gmail.com)")
        }

        if (cleanPass.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters long!")
        }

        val auth = getFirebaseAuth()
            ?: return AuthResult.Error("Firebase Authentication is not available. Please check google-services.json.")

        return try {
            val authResult = if (credentials.isRegistration) {
                auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
            } else {
                auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
            }

            val user = authResult.user
            val profile = UserProfile(
                isLoggedIn = true,
                email = user?.email ?: cleanEmail,
                ownerName = credentials.ownerName,
                businessName = credentials.businessName,
                loginTimestamp = System.currentTimeMillis(),
                firebaseUid = user?.uid ?: ""
            )
            AuthResult.Success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            val mappedMessage = mapAuthException(e, credentials.isRegistration)
            AuthResult.Error(mappedMessage, e)
        }
    }

    override suspend fun signOut() {
        try {
            getFirebaseAuth()?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out", e)
        }
    }

    private fun mapAuthException(e: Exception?, isRegister: Boolean): String {
        if (e == null) return "❌ Authentication failed. Please try again."
        val msg = e.message ?: ""
        val errorCode = (e as? FirebaseAuthException)?.errorCode ?: ""

        return when {
            e is FirebaseAuthInvalidUserException || msg.contains("user-not-found", true) || errorCode == "ERROR_USER_NOT_FOUND" ->
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

    companion object {
        private const val TAG = "EmailPasswordAuthProvider"
    }
}
