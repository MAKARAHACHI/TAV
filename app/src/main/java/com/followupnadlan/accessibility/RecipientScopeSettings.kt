package com.followupnadlan.accessibility

import android.content.Context

/**
 * Who the app is allowed to message automatically: contacts only, or any number.
 * Storage only — this records the user's choice. No detection or sending behavior
 * lives here; the missed-call decision logic is unchanged.
 */
enum class RecipientScope {
    CONTACTS_ONLY,
    ANY_NUMBER
}

class RecipientScopeSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var scope: RecipientScope
        get() = preferences.getString(KEY_SCOPE, null)
            ?.let { runCatching { RecipientScope.valueOf(it) }.getOrNull() }
            ?: RecipientScope.CONTACTS_ONLY
        set(value) {
            preferences.edit().putString(KEY_SCOPE, value.name).apply()
        }

    private companion object {
        const val PREFERENCES_NAME = "recipient_scope_settings"
        const val KEY_SCOPE = "scope"
    }
}
