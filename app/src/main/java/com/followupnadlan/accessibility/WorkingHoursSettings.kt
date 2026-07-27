package com.followupnadlan.accessibility

import android.content.Context
import java.util.Calendar

/**
 * "שעות פעילות" — when the app is allowed to send/suggest at all, on top of the existing
 * recipient rules. Off by default (24/7), matching the pre-existing behavior for anyone who
 * never opens this screen.
 */
class WorkingHoursSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_ENABLED, value).apply()

    var activeDays: Set<Int>
        get() = preferences.getStringSet(KEY_DAYS, null)?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: DEFAULT_DAYS
        set(value) = preferences.edit()
            .putStringSet(KEY_DAYS, value.map { it.toString() }.toSet())
            .apply()

    var startMinuteOfDay: Int
        get() = preferences.getInt(KEY_START, DEFAULT_START_MINUTE)
        set(value) = preferences.edit().putInt(KEY_START, value).apply()

    var endMinuteOfDay: Int
        get() = preferences.getInt(KEY_END, DEFAULT_END_MINUTE)
        set(value) = preferences.edit().putInt(KEY_END, value).apply()

    fun snapshot(): WorkingHoursSnapshot = WorkingHoursSnapshot(
        enabled = enabled,
        activeDays = activeDays,
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay
    )

    companion object {
        /** Sunday–Thursday, Calendar.DAY_OF_WEEK values (1=Sunday..7=Saturday) — the Israeli work week. */
        val DEFAULT_DAYS: Set<Int> = setOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY
        )
        const val DEFAULT_START_MINUTE: Int = 8 * 60
        const val DEFAULT_END_MINUTE: Int = 18 * 60

        private const val PREFERENCES_NAME = "working_hours_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_DAYS = "active_days"
        private const val KEY_START = "start_minute"
        private const val KEY_END = "end_minute"
    }
}

/** Resolves epoch millis in the device's default timezone into the fields [WorkingHoursDecider] needs. */
object LocalMomentResolver {
    fun resolve(nowEpochMs: Long): LocalMoment {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return LocalMoment(dayOfWeek = dayOfWeek, minuteOfDay = minuteOfDay)
    }
}
