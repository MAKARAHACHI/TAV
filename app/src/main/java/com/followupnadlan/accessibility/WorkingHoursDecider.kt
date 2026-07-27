package com.followupnadlan.accessibility

/**
 * A local point in time, stripped to the two fields the working-hours rule needs. The caller
 * resolves epoch millis + device timezone into this — the decider itself stays pure and testable
 * without any Android/Calendar dependency.
 *
 * [dayOfWeek] uses [java.util.Calendar] values (1=Sunday..7=Saturday) since that is what the
 * settings screen's day picker and the store persist.
 */
data class LocalMoment(val dayOfWeek: Int, val minuteOfDay: Int)

/**
 * Decides whether "now" falls inside the user's configured working hours.
 *
 * Off by default: a user who never opens this screen keeps the pre-existing 24/7 behavior.
 * When switched on, a day outside [WorkingHoursSettings.activeDays] or a time outside
 * [start, end) is treated as closed. The range does not wrap past midnight — start must be
 * before end, matching the single time-range picker in the UI.
 */
object WorkingHoursDecider {
    fun isWithinWorkingHours(moment: LocalMoment, settings: WorkingHoursSnapshot): Boolean {
        if (!settings.enabled) return true
        if (!settings.activeDays.contains(moment.dayOfWeek)) return false
        return moment.minuteOfDay in settings.startMinuteOfDay until settings.endMinuteOfDay
    }
}

/** Plain-data snapshot of [WorkingHoursSettings], so the decider doesn't need a Context. */
data class WorkingHoursSnapshot(
    val enabled: Boolean,
    val activeDays: Set<Int>,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int
)
