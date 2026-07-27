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
            ?: DEFAULT_SCOPE
        set(value) {
            preferences.edit().putString(KEY_SCOPE, value.name).apply()
        }

    /**
     * The per-moment override under the "override-on-default" model: `null` = "כמו הכללי" (follow
     * the general default), a concrete [RecipientScope] = this moment overrides the general default.
     * Absent storage reads as `null`, so an untouched install now follows the general default.
     * Backed by the same key as [scope]; the codec turns a concrete choice into its enum name.
     */
    var scopeOverride: RecipientScope?
        get() = RecipientScopeCodec.decode(preferences.getString(KEY_SCOPE, null))
        set(value) {
            preferences.edit().apply {
                val encoded = RecipientScopeCodec.encode(value)
                if (encoded == null) remove(KEY_SCOPE) else putString(KEY_SCOPE, encoded)
            }.apply()
        }

    companion object {
        /**
         * MVP-1 default: only people who aren't saved in the user's contacts. Auto-answering a
         * spouse or a friend with "תודה שהתקשרת, אני כרגע לא פנוי" plus a business card is the
         * embarrassment this default exists to prevent.
         */
        val DEFAULT_SCOPE = RecipientScope.NON_CONTACTS_ONLY
        private const val PREFERENCES_NAME = "recipient_scope_settings"
        private const val KEY_SCOPE = "scope"
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
