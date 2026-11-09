package com.antigravity.pixelassistant.ai

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ensures Gemini API usage strictly stays within Google's 100% Free Tier limits:
 * - RPM (Requests Per Minute): <= 10 requests / min
 * - RPD (Requests Per Day): <= 1,500 requests / day
 * Automatically falls back to Smart Mock when limits are reached, ensuring zero accidental charges.
 */
class FreeTierQuotaManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("free_tier_quota", Context.MODE_PRIVATE)

    companion object {
        const val MAX_RPM = 10
        const val MAX_RPD = 1500
        private const val KEY_DAILY_COUNT = "daily_count"
        private const val KEY_DATE = "date"
        private const val KEY_MINUTE_TIMESTAMPS = "minute_timestamps"
    }

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    @Synchronized
    fun getDailyUsage(): Int {
        val today = getTodayString()
        val savedDate = prefs.getString(KEY_DATE, "")
        return if (savedDate == today) {
            prefs.getInt(KEY_DAILY_COUNT, 0)
        } else {
            0
        }
    }

    @Synchronized
    fun canMakeRequest(): Boolean {
        val today = getTodayString()
        val savedDate = prefs.getString(KEY_DATE, "")
        var dailyCount = if (savedDate == today) prefs.getInt(KEY_DAILY_COUNT, 0) else 0

        if (dailyCount >= MAX_RPD) {
            return false // Daily free quota reached
        }

        // Clean timestamps older than 60 seconds
        val now = System.currentTimeMillis()
        val rawTimestamps = prefs.getString(KEY_MINUTE_TIMESTAMPS, "") ?: ""
        val validTimestamps = rawTimestamps.split(",")
            .mapNotNull { it.toLongOrNull() }
            .filter { now - it < 60_000 }

        return validTimestamps.size < MAX_RPM
    }

    @Synchronized
    fun recordRequest() {
        val today = getTodayString()
        val savedDate = prefs.getString(KEY_DATE, "")
        val dailyCount = if (savedDate == today) prefs.getInt(KEY_DAILY_COUNT, 0) + 1 else 1

        val now = System.currentTimeMillis()
        val rawTimestamps = prefs.getString(KEY_MINUTE_TIMESTAMPS, "") ?: ""
        val validTimestamps = (rawTimestamps.split(",")
            .mapNotNull { it.toLongOrNull() }
            .filter { now - it < 60_000 } + now)

        prefs.edit()
            .putString(KEY_DATE, today)
            .putInt(KEY_DAILY_COUNT, dailyCount)
            .putString(KEY_MINUTE_TIMESTAMPS, validTimestamps.joinToString(","))
            .apply()
    }
}
