package com.example.data.ads

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages VIP / Ad-Free pass earned by successfully completing a Rewarded Ad.
 */
object VipRewardManager {

    private const val PREFS_NAME = "hajri_vip_prefs"
    private const val KEY_VIP_UNTIL_TIMESTAMP = "vip_until_timestamp"

    private val _isVipActive = MutableStateFlow(false)
    val isVipActive: StateFlow<Boolean> = _isVipActive.asStateFlow()

    private val _vipRemainingHours = MutableStateFlow(0)
    val vipRemainingHours: StateFlow<Int> = _vipRemainingHours.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        checkVipStatus(context)
    }

    fun checkVipStatus(context: Context): Boolean {
        val prefs = getPrefs(context)
        val untilTimestamp = prefs.getLong(KEY_VIP_UNTIL_TIMESTAMP, 0L)
        val current = System.currentTimeMillis()
        val isActive = current < untilTimestamp

        _isVipActive.value = isActive
        if (isActive) {
            val remainingMillis = untilTimestamp - current
            _vipRemainingHours.value = ((remainingMillis / (1000 * 60 * 60)) + 1).toInt()
        } else {
            _vipRemainingHours.value = 0
        }
        return isActive
    }

    fun grantVipReward(context: Context, durationMillis: Long = AdConfig.REWARDED_PASS_DURATION_MILLIS) {
        val prefs = getPrefs(context)
        val current = System.currentTimeMillis()
        val currentUntil = prefs.getLong(KEY_VIP_UNTIL_TIMESTAMP, 0L)
        val newUntil = if (currentUntil > current) {
            currentUntil + durationMillis // Stack bonus time
        } else {
            current + durationMillis
        }

        prefs.edit().putLong(KEY_VIP_UNTIL_TIMESTAMP, newUntil).apply()
        checkVipStatus(context)
    }

    fun resetVipForTesting(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().remove(KEY_VIP_UNTIL_TIMESTAMP).apply()
        checkVipStatus(context)
    }
}
