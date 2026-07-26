package com.followupnadlan.accessibility

import android.content.Context

/**
 * "אחרי אילו שיחות להציע לשלוח?" — the ended journey's own recipient scope.
 *
 * Deliberately a *separate* store from [RecipientScopeSettings], under its own preferences file:
 * the two moments are different jobs. A user may well auto-answer every missed call but only want
 * a follow-up suggestion for people who aren't in their contacts. Sharing one value would silently
 * couple two unrelated decisions.
 *
 * Note this scope gates a *suggestion*, not a send — nothing is ever sent automatically after a
 * conversation. It reuses the [RecipientScope] vocabulary so the engine's matching logic is shared.
 *
 * Default is [RecipientScope.NON_CONTACTS_ONLY]: suggesting a business card to the user's own
 * spouse or mother, whose number is saved, is the embarrassment §2 exists to prevent.
 */
class EndedScopeSettings(context: Context) {
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
        private const val PREFERENCES_NAME = "ended_scope_settings"
        private const val KEY_SCOPE = "scope"
    }
}
