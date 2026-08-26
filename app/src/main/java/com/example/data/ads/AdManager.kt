package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Extension to safely resolve Activity from any ContextWrapper in Jetpack Compose
 */
fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

/**
 * Production-ready Google AdMob Manager for Hajri Card
 *
 * Implements:
 * - Safe background initialization
 * - Interstitial frequency cooldown to prevent aggressive ad spamming
 * - Verified completion check for Rewarded Ads
 * - Auto pre-loading & graceful fallbacks (zero app crashes)
 */
object AdManager {

    private const val TAG = "HajriAdManager"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val isInterstitialLoading = AtomicBoolean(false)
    private val isRewardedLoading = AtomicBoolean(false)

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Frequency capping: Timestamp when last interstitial ad was displayed
    private var lastInterstitialShownTime: Long = 0L

    /**
     * Initializes Google Mobile Ads SDK on App Startup
     */
    fun initialize(context: Context) {
        if (_isInitialized.value) return

        try {
            VipRewardManager.init(context)

            // Configure test devices if needed (policy compliant)
            val requestConfiguration = RequestConfiguration.Builder()
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "Google Mobile Ads SDK initialized: $status")
                _isInitialized.value = true
                loadInterstitialAd(context)
                loadRewardedAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization error", e)
        }
    }

    /**
     * Preloads Interstitial Ad with duplicate load prevention
     */
    fun loadInterstitialAd(context: Context) {
        if (isInterstitialLoading.get() || interstitialAd != null) return

        isInterstitialLoading.set(true)
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.getInterstitialAdUnitId()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad preloaded successfully")
                    interstitialAd = ad
                    _isInterstitialReady.value = true
                    isInterstitialLoading.set(false)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Interstitial Ad load failed: ${loadAdError.message} (code: ${loadAdError.code})")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    isInterstitialLoading.set(false)
                }
            }
        )
    }

    /**
     * Context-aware wrapper for showing Interstitial Ad
     */
    fun showInterstitialAd(
        context: Context,
        forceShow: Boolean = false,
        onComplete: () -> Unit
    ) {
        val activity = context.findActivity()
        if (activity != null) {
            showInterstitialAd(activity, forceShow, onComplete)
        } else {
            onComplete()
        }
    }

    /**
     * Shows Interstitial Ad with strict Frequency Cooldown.
     * If on cooldown, ad not ready, or user has VIP pass, calls onComplete() immediately.
     */
    fun showInterstitialAd(
        activity: Activity,
        forceShow: Boolean = false,
        onComplete: () -> Unit
    ) {
        // If user has VIP Pass, skip interstitial ad completely
        if (VipRewardManager.checkVipStatus(activity)) {
            Log.d(TAG, "VIP Pass active: Skipping Interstitial Ad")
            onComplete()
            return
        }

        val currentTime = System.currentTimeMillis()
        val elapsedSinceLast = currentTime - lastInterstitialShownTime

        // Frequency cooldown check
        if (!forceShow && elapsedSinceLast < AdConfig.INTERSTITIAL_COOLDOWN_MILLIS) {
            Log.d(TAG, "Interstitial on cooldown ($elapsedSinceLast ms < ${AdConfig.INTERSTITIAL_COOLDOWN_MILLIS} ms). Skipping.")
            onComplete()
            return
        }

        val ad = interstitialAd
        if (ad != null && !activity.isFinishing && !activity.isDestroyed) {
            try {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial Ad shown to user")
                        lastInterstitialShownTime = System.currentTimeMillis()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial Ad dismissed by user")
                        interstitialAd = null
                        _isInterstitialReady.value = false
                        loadInterstitialAd(activity)
                        activity.runOnUiThread { onComplete() }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "Interstitial Ad failed to show: ${adError.message}")
                        interstitialAd = null
                        _isInterstitialReady.value = false
                        loadInterstitialAd(activity)
                        activity.runOnUiThread { onComplete() }
                    }
                }
                ad.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Exception showing Interstitial Ad", e)
                interstitialAd = null
                _isInterstitialReady.value = false
                onComplete()
            }
        } else {
            Log.d(TAG, "Interstitial Ad not ready. Executing user action directly.")
            loadInterstitialAd(activity)
            onComplete()
        }
    }

    /**
     * Preloads Rewarded Ad with duplicate load prevention
     */
    fun loadRewardedAd(context: Context) {
        if (isRewardedLoading.get() || rewardedAd != null) return

        isRewardedLoading.set(true)
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.getRewardedAdUnitId()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad preloaded successfully")
                    rewardedAd = ad
                    _isRewardedReady.value = true
                    isRewardedLoading.set(false)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Rewarded Ad load failed: ${loadAdError.message} (code: ${loadAdError.code})")
                    rewardedAd = null
                    _isRewardedReady.value = false
                    isRewardedLoading.set(false)
                }
            }
        )
    }

    /**
     * Context-aware wrapper for showing Rewarded Ad
     */
    fun showRewardedAd(
        context: Context,
        onUserEarnedReward: () -> Unit,
        onAdUnavailable: () -> Unit = {},
        onAdClosedWithoutReward: () -> Unit = {}
    ) {
        val activity = context.findActivity()
        if (activity != null) {
            showRewardedAd(activity, onUserEarnedReward, onAdUnavailable, onAdClosedWithoutReward)
        } else {
            onAdUnavailable()
        }
    }

    /**
     * Shows Rewarded Ad.
     * REWARD POLICY COMPLIANCE:
     * Only invokes onUserEarnedReward if the user genuinely completes the ad
     * and Google AdMob executes the reward callback.
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdUnavailable: () -> Unit = {},
        onAdClosedWithoutReward: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded Ad is not ready yet")
            loadRewardedAd(activity)
            onAdUnavailable()
            return
        }

        var hasUserEarnedReward = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded Ad started playback")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded Ad closed. User completed: $hasUserEarnedReward")
                rewardedAd = null
                _isRewardedReady.value = false
                loadRewardedAd(activity)

                if (hasUserEarnedReward) {
                    // Successfully earned
                    VipRewardManager.grantVipReward(activity)
                    onUserEarnedReward()
                } else {
                    // Closed prematurely
                    onAdClosedWithoutReward()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded Ad failed to show: ${adError.message}")
                rewardedAd = null
                _isRewardedReady.value = false
                loadRewardedAd(activity)
                onAdUnavailable()
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "Google AdMob verified user earned reward: ${rewardItem.amount} ${rewardItem.type}")
            hasUserEarnedReward = true
        }
    }
}
