package com.example

import android.app.Application
import android.util.Log
import com.example.data.ads.AdManager
import com.example.data.language.LanguageManager
import com.google.firebase.FirebaseApp

/**
 * Main Application class for Hajri Card.
 * Ensures early and reliable initialization of FirebaseApp, Localization, and AdManager.
 */
class HajriApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                Log.i(TAG, "FirebaseApp initialized successfully: ${app?.name}")
            } else {
                Log.i(TAG, "FirebaseApp already initialized with ${FirebaseApp.getApps(this).size} app(s).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FirebaseApp during Application startup", e)
        }

        // Initialize Language and Ad systems
        LanguageManager.initialize(this)
        AdManager.initialize(this)
    }

    companion object {
        private const val TAG = "HajriApplication"
    }
}
