package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Firebase Phone Authentication Provider for SMS OTP.
 */
class PhoneAuthProviderImpl(private val context: Context) : AuthProvider {

    override val providerType: AuthType = AuthType.PHONE_OTP
    override val displayName: String = "Phone SMS OTP"

    override val isReady: Boolean
        get() = try {
            getFirebaseAuth() != null
        } catch (e: Exception) {
            false
        }

    private fun getFirebaseAuth(): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp not initialized: ${e.message}")
            try {
                FirebaseAuth.getInstance()
            } catch (e2: Exception) {
                null
            }
        }
    }

    override suspend fun authenticate(credentials: AuthCredentials): AuthResult {
        if (credentials !is AuthCredentials.PhoneOtp) {
            return AuthResult.Error("Invalid credentials for Phone Auth")
        }

        val auth = getFirebaseAuth()
            ?: return AuthResult.Error("Firebase Authentication is not available.")

        return try {
            val credential = PhoneAuthProvider.getCredential(credentials.phoneNumber, credentials.otpCode)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            val profile = UserProfile(
                isLoggedIn = true,
                mobileNumber = user?.phoneNumber ?: credentials.phoneNumber,
                loginTimestamp = System.currentTimeMillis(),
                firebaseUid = user?.uid ?: ""
            )
            AuthResult.Success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Phone authentication failed", e)
            AuthResult.Error(e.localizedMessage ?: "Phone authentication failed", e)
        }
    }

    override suspend fun signOut() {
        try {
            getFirebaseAuth()?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out", e)
        }
    }

    /**
     * Send SMS OTP verification code to a phone number.
     */
    fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        timeoutSeconds: Long = 60L
    ) {
        val auth = getFirebaseAuth()
        if (auth == null) {
            callbacks.onVerificationFailed(FirebaseException("Firebase Authentication is not initialized."))
            return
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber.trim())
            .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Sign in with verification ID and received SMS code.
     */
    fun signInWithVerificationCode(
        verificationId: String,
        code: String,
        ownerName: String = "",
        businessName: String = "",
        onSuccess: (UserProfile) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val auth = getFirebaseAuth()
        if (auth == null) {
            onFailure("Firebase Authentication is not initialized.")
            return
        }

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        val profile = UserProfile(
                            isLoggedIn = true,
                            mobileNumber = user?.phoneNumber ?: "",
                            ownerName = ownerName,
                            businessName = businessName,
                            loginTimestamp = System.currentTimeMillis(),
                            firebaseUid = user?.uid ?: ""
                        )
                        onSuccess(profile)
                    } else {
                        onFailure(task.exception?.localizedMessage ?: "Invalid verification code.")
                    }
                }
        } catch (e: Exception) {
            onFailure(e.localizedMessage ?: "Failed to verify code.")
        }
    }

    companion object {
        private const val TAG = "PhoneAuthProvider"
    }
}
