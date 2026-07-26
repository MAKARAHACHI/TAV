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

    private companion object {
        const val PREFERENCES_NAME = "call_detection_preferences"
        const val KEY_ENABLED = "call_detection_enabled"
    }
}
