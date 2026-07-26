package com.followupnadlan.accessibility

import android.content.Context

/**
 * Whether the business card is attached to the follow-up message ("מצורף").
 *
 * The toggle lives *on* the card inside the preview, and turning it off removes the card from the
 * preview live — the user sees the message shrink to exactly what the client will receive. That is
 * the artifact-not-description rule: no separate "האם לצרף?" row describing a thing you could
 * instead just show.
 *
 * "The card" here is the signature line rendered as text at the end of the message — there is no
 * vCard in MVP-1 (WhatsApp blocks file sharing to unsaved numbers, and most clients are unsaved).
 */
class EndedCardSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var cardAttached: Boolean
        get() = preferences.getBoolean(KEY_CARD_ATTACHED, true)
        set(value) {
            preferences.edit().putBoolean(KEY_CARD_ATTACHED, value).apply()
        }

    private companion object {
        const val PREFERENCES_NAME = "ended_card_settings"
        const val KEY_CARD_ATTACHED = "card_attached"
    }
}
