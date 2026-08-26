package com.example.data.ads

/**
 * Google AdMob Centralized Configuration
 *
 * =========================================================================
 * ⚠️ PRODUCTION RELEASE GUIDELINES / प्रोडक्शन निर्देश:
 * 1. Google AdMob Console (https://admob.google.com) से अपने असली Ad Unit IDs प्राप्त करें।
 * 2. `USE_TEST_ADS` को `false` करें या `PROD_*` वाले IDs में अपने वास्तविक Ad Unit IDs डालें।
 * 3. `AndroidManifest.xml` में `com.google.android.gms.ads.APPLICATION_ID` को अपनी असली AdMob App ID से बदलें।
 * =========================================================================
 */
object AdConfig {

    /**
     * Set to true to use Google's official Test Ad Unit IDs.
     * Set to false for live production builds with your real AdMob Ad Unit IDs.
     */
    const val USE_TEST_ADS = false

    // ==========================================
    // 1. Google Official Sample / Test Ad Unit IDs
    // (Always safe for development and testing)
    // ==========================================
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // ==========================================
    // 2. Production Ad Unit IDs
    // ==========================================
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-9389385519426957/7077722817"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9389385519426957/9541852597"
    const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-9389385519426957/3140095777"

    // ==========================================
    // 3. Frequency & Cooldown Controls
    // ==========================================
    /** Minimum cooldown between consecutive full-screen interstitial ads (45 seconds) */
    const val INTERSTITIAL_COOLDOWN_MILLIS: Long = 45_000L

    /** Duration of VIP Ad-Free Pass unlocked via Rewarded Video (24 hours) */
    const val REWARDED_PASS_DURATION_MILLIS: Long = 24 * 60 * 60 * 1000L

    fun getBannerAdUnitId(): String {
        return if (USE_TEST_ADS) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID
    }

    fun getInterstitialAdUnitId(): String {
        return if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID
    }

    fun getRewardedAdUnitId(): String {
        return if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
    }
}
