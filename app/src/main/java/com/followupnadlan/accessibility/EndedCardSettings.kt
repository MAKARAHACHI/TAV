package com.followupnadlan.accessibility

import android.content.Context

/**
 * Whether the contact card is attached to a moment's follow-up message ("צירוף כרטיס ביקור").
 *
 * The card is now an opt-in ADD/REMOVE control per moment (missed / ended / no-answer), not a fixed
 * decoration: the user turns it ON to attach the card element and OFF to remove it. The flag only
 * governs whether the card ELEMENT is DRAWN in the message artifact — the card stays a drawn element
 * only (WhatsApp blocks file-share to an unsaved number's chat, so no real .vcf is sent, §2).
 *
 * Default is OFF for all three moments: no card unless the user chose to add it.
 *
 * [EndedCardSettings] backs the ended moment; [MissedCardSettings] / [NoAnswerCardSettings] are the
 * parallel per-moment stores (same shape, own prefs file). Defined here so the shape lives once.
 */
class EndedCardSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var cardAttached: Boolean
        get() = preferences.getBoolean(KEY_CARD_ATTACHED, DEFAULT_CARD_ATTACHED)
        set(value) {
            preferences.edit().putBoolean(KEY_CARD_ATTACHED, value).apply()
        }

    private companion object {
        const val PREFERENCES_NAME = "ended_card_settings"
        const val KEY_CARD_ATTACHED = "card_attached"
    }
}

/** Default OFF: the card is drawn only when the user opts in per moment. Shared by the three stores. */
internal const val DEFAULT_CARD_ATTACHED = false

/** Per-moment card-attached flag for the MISSED moment. Same shape as [EndedCardSettings]. */
class MissedCardSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var cardAttached: Boolean
        get() = preferences.getBoolean(KEY_CARD_ATTACHED, DEFAULT_CARD_ATTACHED)
        set(value) {
            preferences.edit().putBoolean(KEY_CARD_ATTACHED, value).apply()
        }

    private companion object {
        const val PREFERENCES_NAME = "missed_card_settings"
        const val KEY_CARD_ATTACHED = "card_attached"
    }
}

/** Per-moment card-attached flag for the NO_ANSWER (outgoing not answered) moment. */
class NoAnswerCardSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var cardAttached: Boolean
        get() = preferences.getBoolean(KEY_CARD_ATTACHED, DEFAULT_CARD_ATTACHED)
        set(value) {
            preferences.edit().putBoolean(KEY_CARD_ATTACHED, value).apply()
        }

    private companion object {
        const val PREFERENCES_NAME = "no_answer_card_settings"
        const val KEY_CARD_ATTACHED = "card_attached"
    }
}
