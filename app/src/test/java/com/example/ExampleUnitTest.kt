package com.example

import com.example.data.auth.AuthCredentials
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthType
import com.example.data.auth.WhatsAppAuthProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun testAuthTypesAvailable() {
        assertEquals("Email & Password", AuthType.EMAIL_PASSWORD.displayName)
        assertEquals("WhatsApp Quick Login", AuthType.WHATSAPP.displayName)
    }

    @Test
    fun testWhatsAppAuthProviderReturnsFutureIntegrationSafely() = runBlocking {
        val provider = WhatsAppAuthProvider()
        assertFalse(provider.isReady)
        assertEquals(AuthType.WHATSAPP, provider.providerType)

        val result = provider.authenticate(AuthCredentials.WhatsApp(phoneNumber = "+919876543210"))
        assertTrue(result is AuthResult.FutureIntegration)
        val futureResult = result as AuthResult.FutureIntegration
        assertEquals(AuthType.WHATSAPP, futureResult.provider)
        assertTrue(futureResult.requiresBackendSetup)
    }
}
