package com.followupnadlan.accessibility

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Pins the per-moment contact-card default: OFF for all three moments (missed / ended / no-answer).
 * The card is an opt-in add/remove control, so a fresh install draws no card until the user adds it.
 * The three per-moment stores ([MissedCardSettings] / [EndedCardSettings] / [NoAnswerCardSettings])
 * all read this shared default; asserting the constant pins the invariant without an Android Context.
 */
class CardAttachedDefaultTest {

    @Test
    fun perMomentCardDefaultsOff() {
        assertFalse(DEFAULT_CARD_ATTACHED)
    }
}
