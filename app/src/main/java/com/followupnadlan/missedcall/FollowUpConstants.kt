package com.followupnadlan.missedcall

/**
 * The hidden constants of MVP-1. None of these is exposed as a picker — per the product rule
 * "FollowUp does not manage preferences": anything that can be decided once, sensibly, and does
 * not change perceived value, should not be a setting. The capability stays in the code
 * (cooldowns are still per-call parameters) — it is simply not surfaced.
 *
 * The only one the user ever sees is [SAME_NUMBER_COOLDOWN_MILLIS], and only as a quiet
 * statement of fact on the missed screen ("לא נשלח שוב לאותו אדם במשך יממה"), never as a control.
 */
object FollowUpConstants {
    /**
     * A conversation must last at least this long before we offer a follow-up. Short calls are
     * wrong numbers, hang-ups and "sorry, driving" — offering a business card there is noise.
     */
    const val ENDED_SUGGESTION_MIN_CALL_SECONDS: Long = 60L

    /**
     * Per-number cooldown, applied independently to each moment. Missed and ended do NOT block
     * each other: "I didn't answer, so you got a message" and "then we spoke, so here are my
     * details" are two different events, and the second is legitimate news.
     */
    const val SAME_NUMBER_COOLDOWN_MILLIS: Long = 24 * 60 * 60 * 1000L

    /**
     * Global quiet window between any two FollowUp notifications, so a burst of calls cannot
     * turn into a burst of notifications.
     */
    const val GLOBAL_NOTIFICATION_QUIET_MILLIS: Long = 60 * 1000L
}
