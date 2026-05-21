package com.followupnadlan.snooze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SnoozeTimeCalculatorTest {
    private val zone = ZoneId.of("Asia/Jerusalem")

    @Test
    fun snoozeOptionsAreExactlyTheApprovedPresetSet() {
        assertEquals(
            listOf(
                SnoozeOption.IN_15_MIN,
                SnoozeOption.IN_30_MIN,
                SnoozeOption.IN_1_HOUR,
                SnoozeOption.TONIGHT,
                SnoozeOption.TOMORROW_MORNING
            ),
            SnoozeOption.entries.toList()
        )
    }

    @Test
    fun labelsMatchApprovedHebrewCopy() {
        assertEquals("בעוד 15 דקות", SnoozeOption.IN_15_MIN.label)
        assertEquals("בעוד 30 דקות", SnoozeOption.IN_30_MIN.label)
        assertEquals("בעוד שעה", SnoozeOption.IN_1_HOUR.label)
        assertEquals("בערב", SnoozeOption.TONIGHT.label)
        assertEquals("מחר בבוקר", SnoozeOption.TOMORROW_MORNING.label)
    }

    @Test
    fun relativeOptionsAddExactDeltas() {
        val now = millisAt(2026, 5, 21, 10, 0)

        assertEquals(now + 15.minutes, SnoozeTimeCalculator.computeTriggerAt(SnoozeOption.IN_15_MIN, now, zone))
        assertEquals(now + 30.minutes, SnoozeTimeCalculator.computeTriggerAt(SnoozeOption.IN_30_MIN, now, zone))
        assertEquals(now + 60.minutes, SnoozeTimeCalculator.computeTriggerAt(SnoozeOption.IN_1_HOUR, now, zone))
    }

    @Test
    fun tonightBeforeEightPmReturnsTodayAtEightPm() {
        val now = millisAt(2026, 5, 21, 17, 30)
        val expected = millisAt(2026, 5, 21, 20, 0)

        assertEquals(expected, SnoozeTimeCalculator.computeTriggerAt(SnoozeOption.TONIGHT, now, zone))
    }

    @Test
    fun tonightAtEightPmReturnsTomorrowAtEightPm() {
        val now = millisAt(2026, 5, 21, 20, 0)
        val expected = millisAt(2026, 5, 22, 20, 0)

        assertEquals(expected, SnoozeTimeCalculator.computeTriggerAt(SnoozeOption.TONIGHT, now, zone))
    }

    @Test
    fun tonightAfterEightPmReturnsTomorrowAtEightPm() {
        val now = millisAt(2026, 5, 21, 22, 15)
        val expected = millisAt(2026, 5, 22, 20, 0)

        assertEquals(expected, SnoozeTimeCalculator.computeTriggerAt(SnoozeOption.TONIGHT, now, zone))
    }

    @Test
    fun tomorrowMorningReturnsNextDayAtNineAm() {
        val now = millisAt(2026, 5, 21, 10, 0)
        val expected = millisAt(2026, 5, 22, 9, 0)

        assertEquals(expected, SnoozeTimeCalculator.computeTriggerAt(SnoozeOption.TOMORROW_MORNING, now, zone))
    }

    @Test
    fun daylightSavingBoundaryStillReturnsAFutureLocalPresetTime() {
        val beforeDstJump = millisAt(2026, 3, 27, 1, 30)
        val trigger = SnoozeTimeCalculator.computeTriggerAt(SnoozeOption.TOMORROW_MORNING, beforeDstJump, zone)
        val expected = millisAt(2026, 3, 28, 9, 0)

        assertEquals(expected, trigger)
        assertTrue(trigger > beforeDstJump)
    }

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private val Int.minutes: Long
        get() = this * 60_000L
}
