package com.followupnadlan.missedcall

/**
 * The starting values for FollowUp's timing rules.
 *
 * These were fixed constants in MVP-1, on the product rule "FollowUp does not manage preferences".
 * The two below turned out to fail that rule's own test — they *did* change perceived value, and
 * in the losing direction: the quiet window silenced other clients who called in the same minute,
 * and a day-long per-number cooldown is right for one user and far too long for another. They are
 * now defaults for [com.followupnadlan.accessibility.FollowUpCooldownSettings], which lets the user
 * retime or switch off either one. The rest of MVP-1's constants stay unsurfaced.
 */
object FollowUpConstants {
    /**
     * Default per-number cooldown, applied independently to each moment. Missed and ended do NOT
     * block each other: "I didn't answer, so you got a message" and "then we spoke, so here are my
     * details" are two different events, and the second is legitimate news.
     */
    const val SAME_NUMBER_COOLDOWN_MILLIS: Long = 24 * 60 * 60 * 1000L

    /**
     * Default global quiet window between any two follow-up suggestions, so a burst of calls
     * cannot turn into a burst of notifications.
     */
    const val GLOBAL_NOTIFICATION_QUIET_MILLIS: Long = 60 * 1000L
}
