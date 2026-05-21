package com.followupnadlan.snooze

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulerTest {
    @Test
    fun workNameIsStableAndUniquePerTask() {
        assertEquals("followup-reminder-42", ReminderScheduler.workNameFor(42))
        assertEquals("followup-reminder-43", ReminderScheduler.workNameFor(43))
    }
}
