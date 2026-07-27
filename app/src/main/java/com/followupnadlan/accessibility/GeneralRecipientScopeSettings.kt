package com.followupnadlan.accessibility

import android.content.Context

/**
 * The GENERAL default recipient scope — "מי מקבל הודעות המשך (ברירת מחדל)" — set in Smart-Rules.
 *
 * This is the fallback every moment uses while it stays on "כמו הכללי". Each moment can still pin
 * its own scope (a per-moment override); when it does not, [EffectiveRecipientScope.of] resolves to
 * this value.
 *
 * Default is [RecipientScope.NON_CONTACTS_ONLY]: the same value the per-moment stores defaulted to
 * before, so an untouched install (every moment on "כמו הכללי") keeps today's effective behavior.
 *
 * Storage only — reuses the existing [RecipientScope] vocabulary so the engine's matching logic is
 * unchanged.
 */
class GeneralRecipientScopeSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var scope: RecipientScope
        get() = preferences.getString(KEY_SCOPE, null)
            ?.let { runCatching { RecipientScope.valueOf(it) }.getOrNull() }
            ?: DEFAULT_SCOPE
        set(value) {
            preferences.edit().putString(KEY_SCOPE, value.name).apply()
        }

    companion object {
        val DEFAULT_SCOPE = RecipientScope.NON_CONTACTS_ONLY
        private const val PREFERENCES_NAME = "general_recipient_scope_settings"
        private const val KEY_SCOPE = "scope"
    }
}
