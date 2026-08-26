package com.example.data.auth

/**
 * Pluggable Authentication Provider Interface.
 *
 * Allows adding new authentication methods (e.g. WhatsApp, Google, Phone)
 * without rewriting UI screens or core database layers.
 */
interface AuthProvider {
    /**
     * Unique identifier and type of the authentication provider.
     */
    val providerType: AuthType

    /**
     * Human-readable label for UI buttons and dialogs.
     */
    val displayName: String

    /**
     * Indicates whether this provider is currently configured and operational in production.
     */
    val isReady: Boolean

    /**
     * Authenticates the user with the given credentials.
     */
    suspend fun authenticate(credentials: AuthCredentials): AuthResult

    /**
     * Signs out the user session for this provider.
     */
    suspend fun signOut()
}
