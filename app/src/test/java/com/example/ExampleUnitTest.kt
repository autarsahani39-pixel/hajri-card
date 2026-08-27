package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
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
@Config(sdk = [34])
class ExampleUnitTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testAuthTypesAvailable() {
        assertEquals("Email & Password", AuthType.EMAIL_PASSWORD.displayName)
        assertEquals("WhatsApp Authentication", AuthType.WHATSAPP.displayName)
    }

    @Test
    fun testWhatsAppAuthProviderReturnsConfigurationRequiredSafely() = runBlocking {
        val provider = WhatsAppAuthProvider(context)
        assertFalse(provider.isReady)
        assertEquals(AuthType.WHATSAPP, provider.providerType)

        val result = provider.authenticate(AuthCredentials.WhatsApp(phoneNumber = "+919876543210"))
        assertTrue(result is AuthResult.ConfigurationRequired)
    }
}
