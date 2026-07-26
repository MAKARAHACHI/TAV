package com.followupnadlan.postcall

import android.content.Context
import com.followupnadlan.whatsapp.PhoneNumberNormalizer

/**
 * Remembers when a follow-up was last suggested, so the same person is not asked about twice in a
 * day and notifications cannot arrive in a burst.
 *
 * Deliberately *not* a history: one timestamp per number, overwritten each time, plus one global
 * timestamp. There is no list, no count and nothing to browse — this is a cooldown ledger, not a
 * record of who was called. The device's own contacts remain the only real memory.
 */
class EndedSuggestionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun lastSuggestedAt(phone: String): Long? =
        keyFor(phone)?.let { key ->
            preferences.getLong(key, 0L).takeIf { it > 0L }
        }

    fun lastAnyNotificationAt(): Long? =
        preferences.getLong(KEY_LAST_ANY, 0L).takeIf { it > 0L }

    fun markSuggested(phone: String, nowEpochMs: Long) {
        val editor = preferences.edit().putLong(KEY_LAST_ANY, nowEpochMs)
        keyFor(phone)?.let { editor.putLong(it, nowEpochMs) }
        editor.apply()
    }

    fun clear() = preferences.edit().clear().apply()

    private fun keyFor(phone: String): String? =
        PhoneNumberNormalizer.normalizeForWhatsApp(phone)?.takeIf { it.isNotBlank() }?.let { "$KEY_PREFIX$it" }

    private companion object {
        const val PREFERENCES_NAME = "ended_suggestion_store"
        const val KEY_PREFIX = "last_suggested_"
        const val KEY_LAST_ANY = "last_any_notification"
    }
}
