package com.followupnadlan.accessibility

import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkingHoursDeciderTest {
    private val sundayToThursday9to17 = WorkingHoursSnapshot(
        enabled = true,
        activeDays = setOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY),
        startMinuteOfDay = 9 * 60,
        endMinuteOfDay = 17 * 60
    )

    @Test
    fun disabledMeansAlwaysOpen() {
        val disabled = sundayToThursday9to17.copy(enabled = false)
        assertTrue(
            WorkingHoursDecider.isWithinWorkingHours(
                LocalMoment(dayOfWeek = Calendar.SATURDAY, minuteOfDay = 3 * 60),
                disabled
            )
        )
    }

    @Test
    fun withinDayAndRangeIsOpen() {
        assertTrue(
            WorkingHoursDecider.isWithinWorkingHours(
                LocalMoment(dayOfWeek = Calendar.MONDAY, minuteOfDay = 12 * 60),
                sundayToThursday9to17
            )
        )
    }

    @Test
    fun beforeStartIsClosed() {
        assertFalse(
            WorkingHoursDecider.isWithinWorkingHours(
                LocalMoment(dayOfWeek = Calendar.MONDAY, minuteOfDay = 8 * 60 + 59),
                sundayToThursday9to17
            )
        )
    }

    @Test
    fun atEndMinuteIsClosed() {
        assertFalse(
            WorkingHoursDecider.isWithinWorkingHours(
                LocalMoment(dayOfWeek = Calendar.MONDAY, minuteOfDay = 17 * 60),
                sundayToThursday9to17
            )
        )
    }

    @Test
    fun oneMinuteBeforeEndIsOpen() {
        assertTrue(
            WorkingHoursDecider.isWithinWorkingHours(
                LocalMoment(dayOfWeek = Calendar.MONDAY, minuteOfDay = 17 * 60 - 1),
                sundayToThursday9to17
            )
        )
    }

    @Test
    fun dayNotInActiveDaysIsClosed() {
        assertFalse(
            WorkingHoursDecider.isWithinWorkingHours(
                LocalMoment(dayOfWeek = Calendar.FRIDAY, minuteOfDay = 12 * 60),
                sundayToThursday9to17
            )
        )
    }
}
