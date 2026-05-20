package com.followupnadlan.postcall

import android.content.Context

class CallDetectionPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    fun setEnabled(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, value)
            .apply()
    }

    val minCallDurationSeconds: Int
        get() = preferences.getInt(KEY_MIN_DURATION_SECONDS, DEFAULT_MIN_DURATION_SECONDS)

    private companion object {
        const val PREFERENCES_NAME = "call_detection_preferences"
        const val KEY_ENABLED = "call_detection_enabled"
        const val KEY_MIN_DURATION_SECONDS = "call_detection_min_duration_seconds"
        const val DEFAULT_MIN_DURATION_SECONDS = 5
    }
}
