package com.followupnadlan.missedcall

import android.content.Context

class MissedCallCooldownStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun lastAutoReplyAt(phoneKey: String): Long? =
        preferences.getLong(key(phoneKey), NO_TIMESTAMP).takeIf { it != NO_TIMESTAMP }

    fun markAutoReplyAttempt(phoneKey: String, timestampEpochMs: Long) {
        if (phoneKey.isBlank()) return
        preferences.edit().putLong(key(phoneKey), timestampEpochMs).apply()
    }

    fun isAccessible(): Boolean =
        try {
            preferences.contains(ACCESS_PROBE_KEY)
            true
        } catch (_: RuntimeException) {
            false
        } catch (_: SecurityException) {
            false
        }

    private fun key(phoneKey: String): String =
        "last_auto_reply_${MissedCallCooldownKeys.sanitize(phoneKey)}"

    private companion object {
        const val PREFERENCES_NAME = "missed_call_auto_response_cooldowns"
        const val NO_TIMESTAMP = Long.MIN_VALUE
        const val ACCESS_PROBE_KEY = "__access_probe"
    }
}

object MissedCallCooldownKeys {
    fun sanitize(phone: String): String =
        phone.trim().filter { it.isLetterOrDigit() }.lowercase()
}
