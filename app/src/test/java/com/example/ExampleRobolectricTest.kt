package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Hajri Card", appName)
  }

  @Test
  fun `verify AuthManager initialization and registered providers`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = AuthManager.getInstance(context)
    assertNotNull(authManager)
    assertNotNull(authManager.getProvider(AuthType.EMAIL_PASSWORD))
    assertNotNull(authManager.getProvider(AuthType.WHATSAPP))
    assertNotNull(authManager.getProvider(AuthType.PHONE_OTP))
  }
}
