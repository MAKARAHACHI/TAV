package com.followupnadlan.accessibility

import android.content.Context

/**
 * Who the app is allowed to message automatically.
 * Storage only — this records the user's choice. No detection or sending behavior
 * lives here; the missed-call decision logic is unchanged.
 */
enum class RecipientScope {
    ANY_NUMBER,
    CONTACTS_ONLY,
    NON_CONTACTS_ONLY,
    ONLY_SELECTED
}

class RecipientScopeSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var scope: RecipientScope
        get() = preferences.getString(KEY_SCOPE, null)
            ?.let { runCatching { RecipientScope.valueOf(it) }.getOrNull() }
            ?: RecipientScope.ANY_NUMBER
        set(value) {
            preferences.edit().putString(KEY_SCOPE, value.name).apply()
        }

    private companion object {
        const val PREFERENCES_NAME = "recipient_scope_settings"
        const val KEY_SCOPE = "scope"
    }
}

object RecipientRulesUi {
    fun visibleBlockGroups(scope: RecipientScope): Set<BlockedRecipientGroup> =
        when (scope) {
            RecipientScope.ANY_NUMBER -> BlockedRecipientGroup.values().toSet()
            RecipientScope.CONTACTS_ONLY -> setOf(BlockedRecipientGroup.NON_CONTACTS, BlockedRecipientGroup.FIRST_TIME)
            RecipientScope.NON_CONTACTS_ONLY -> setOf(BlockedRecipientGroup.CONTACTS, BlockedRecipientGroup.FIRST_TIME)
            RecipientScope.ONLY_SELECTED -> emptySet()
        }
}
