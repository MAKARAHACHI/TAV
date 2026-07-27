package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeHebrewTest {
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour
    private val now = 1_700_000_000_000L

    @Test
    fun `under a minute is now`() {
        assertEquals("עכשיו", RelativeTimeHebrew.of(now - 30_000L, now))
    }

    @Test
    fun `exactly one minute`() {
        assertEquals("לפני דקה", RelativeTimeHebrew.of(now - minute, now))
    }

    @Test
    fun `several minutes`() {
        assertEquals("לפני 3 דקות", RelativeTimeHebrew.of(now - 3 * minute, now))
        assertEquals("לפני 59 דקות", RelativeTimeHebrew.of(now - 59 * minute, now))
    }

    @Test
    fun `exactly one hour`() {
        assertEquals("לפני שעה", RelativeTimeHebrew.of(now - hour, now))
    }

    @Test
    fun `several hours`() {
        assertEquals("לפני 5 שעות", RelativeTimeHebrew.of(now - 5 * hour, now))
        assertEquals("לפני 23 שעות", RelativeTimeHebrew.of(now - 23 * hour, now))
    }

    @Test
    fun `one day is yesterday`() {
        assertEquals("אתמול", RelativeTimeHebrew.of(now - day, now))
    }

    @Test
    fun `several days`() {
        assertEquals("לפני 4 ימים", RelativeTimeHebrew.of(now - 4 * day, now))
    }

    @Test
    fun `future or equal timestamp clamps to now`() {
        assertEquals("עכשיו", RelativeTimeHebrew.of(now, now))
        assertEquals("עכשיו", RelativeTimeHebrew.of(now + hour, now))
    }
}
