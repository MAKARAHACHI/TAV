package com.followupnadlan.accessibility

import android.content.Context

/**
 * The "לא ענו לי" (outgoing-not-answered) moment's own recipient scope, under its own preferences
 * file — parallel to [RecipientScopeSettings] (missed) and [EndedScopeSettings] (ended).
 *
 * This moment had no scope store before; it always shared the ended gate. Under the new
 * "override-on-default" model it gets one: `null` ("כמו הכללי") follows the general default, a
 * concrete [RecipientScope] overrides it. A fresh install has no stored value, so it follows the
 * general default out of the box.
 *
 * Storage only — no detection or sending logic lives here. The scope value is resolved to a
 * concrete [RecipientScope] at the call site (via [EffectiveRecipientScope]) and handed to the
 * unchanged decider.
 */
class NoAnswerScopeSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * The per-moment override: `null` = follow the general default, a concrete [RecipientScope] =
     * override. Absent storage reads as `null`. See [RecipientScopeCodec].
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
        private const val PREFERENCES_NAME = "no_answer_scope_settings"
        private const val KEY_SCOPE = "scope"
    }
}
