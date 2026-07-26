package com.followupnadlan.accessibility

import android.content.Context

/**
 * The two brakes on follow-up suggestions, both user-adjustable and both switchable off.
 *
 * They answer different questions and are therefore separate values:
 *  - [sameNumberCooldownMillis] — "don't ask me about *this person* again so soon."
 *  - [globalQuietMillis] — "don't let a burst of calls become a burst of notifications."
 *
 * Both shipped as hidden constants, and both caused real losses in that form: the global window in
 * particular silenced *other* clients who happened to call within the same minute, which loses a
 * follow-up permanently rather than delaying it. Rather than pick a better constant, the timing is
 * now the user's — including [OFF], which removes the brake entirely.
 *
 * [OFF] is a genuine option, not a hidden maximum: a user who wants a suggestion after every single
 * call is choosing more noise deliberately, and the suggestion is silent, so nothing protects them
 * from a choice they made on purpose.
 */
class FollowUpCooldownSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Per-number cooldown, or `null` when switched off. */
    var sameNumberCooldownMillis: Long?
        get() = readOption(KEY_SAME_NUMBER, DEFAULT_SAME_NUMBER_MILLIS)
        set(value) = writeOption(KEY_SAME_NUMBER, value)

    /** Global quiet window between any two suggestions, or `null` when switched off. */
    var globalQuietMillis: Long?
        get() = readOption(KEY_GLOBAL_QUIET, DEFAULT_GLOBAL_QUIET_MILLIS)
        set(value) = writeOption(KEY_GLOBAL_QUIET, value)

    /**
     * Stored as milliseconds, with [OFF] as the sentinel for "no brake". A negative or absent value
     * falls back to the default rather than disabling the brake: a corrupt preference should not
     * quietly turn a limit off.
     */
    private fun readOption(key: String, default: Long): Long? {
        val stored = preferences.getLong(key, UNSET)
        return when {
            stored == OFF -> null
            stored <= 0L -> default
            else -> stored
        }
    }

    private fun writeOption(key: String, value: Long?) {
        preferences.edit().putLong(key, value?.takeIf { it > 0L } ?: OFF).apply()
    }

    companion object {
        const val DEFAULT_SAME_NUMBER_MILLIS: Long = 24 * 60 * 60 * 1000L
        const val DEFAULT_GLOBAL_QUIET_MILLIS: Long = 60 * 1000L

        /** Sentinel meaning "switched off"; distinct from [UNSET] so a default can still apply. */
        private const val OFF = -1L
        private const val UNSET = 0L
        private const val PREFERENCES_NAME = "follow_up_cooldown_settings"
        private const val KEY_SAME_NUMBER = "same_number_cooldown_millis"
        private const val KEY_GLOBAL_QUIET = "global_quiet_millis"
    }
}

/**
 * The choices offered for each brake, kept as pure data so the screen renders a list rather than
 * hard-coding rows, and so the labels can be asserted in a test.
 */
object FollowUpCooldownOptions {
    private const val MINUTE = 60 * 1000L
    private const val HOUR = 60 * MINUTE

    data class Choice(val label: String, val millis: Long?)

    /** "כל כמה זמן להציע שוב על אותו מספר" — from a few minutes up to a week, or never wait. */
    val sameNumber: List<Choice> = listOf(
        Choice("בלי המתנה — הצע בכל שיחה", null),
        Choice("שעה", HOUR),
        Choice("6 שעות", 6 * HOUR),
        Choice("24 שעות", 24 * HOUR),
        Choice("3 ימים", 3 * 24 * HOUR),
        Choice("שבוע", 7 * 24 * HOUR)
    )

    /** "מרווח מינימלי בין הצעות" — the anti-burst brake. */
    val globalQuiet: List<Choice> = listOf(
        Choice("בלי מרווח — הצע על כל שיחה", null),
        Choice("30 שניות", 30 * 1000L),
        Choice("דקה", MINUTE),
        Choice("5 דקות", 5 * MINUTE),
        Choice("15 דקות", 15 * MINUTE)
    )

    /**
     * The label for a stored value. An unrecognised value (an older build's constant, a restored
     * backup) still gets a truthful label rather than falling back to a wrong one.
     */
    fun labelFor(choices: List<Choice>, millis: Long?): String =
        choices.firstOrNull { it.millis == millis }?.label
            ?: millis?.let(::describeDuration)
            ?: choices.first().label

    private fun describeDuration(millis: Long): String = when {
        millis >= HOUR && millis % HOUR == 0L -> "${millis / HOUR} שעות"
        millis >= MINUTE && millis % MINUTE == 0L -> "${millis / MINUTE} דקות"
        else -> "${millis / 1000} שניות"
    }
}
