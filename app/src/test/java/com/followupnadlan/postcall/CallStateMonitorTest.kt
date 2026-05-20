package com.followupnadlan.postcall

import com.followupnadlan.postcall.CallStateMonitor.CallState
import org.junit.Assert.assertEquals
import org.junit.Test

class CallStateMonitorTest {
    @Test
    fun offhookToIdleTriggersCallEndedWithDuration() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(onCallEnded = endedDurations::add)

        monitor.onStateChanged(CallState.OFFHOOK, 1_000)
        monitor.onStateChanged(CallState.IDLE, 7_000)

        assertEquals(listOf(6L), endedDurations)
    }

    @Test
    fun shortCallBelowDefaultThresholdIsSuppressed() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(onCallEnded = endedDurations::add)

        monitor.onStateChanged(CallState.OFFHOOK, 1_000)
        monitor.onStateChanged(CallState.IDLE, 4_000)

        assertEquals(emptyList<Long>(), endedDurations)
    }

    @Test
    fun ringingToIdleDoesNotTriggerCallEnded() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(onCallEnded = endedDurations::add)

        monitor.onStateChanged(CallState.RINGING, 1_000)
        monitor.onStateChanged(CallState.IDLE, 10_000)

        assertEquals(emptyList<Long>(), endedDurations)
    }

    @Test
    fun duplicateOffhookDoesNotRestartTimer() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(onCallEnded = endedDurations::add)

        monitor.onStateChanged(CallState.OFFHOOK, 1_000)
        monitor.onStateChanged(CallState.OFFHOOK, 4_000)
        monitor.onStateChanged(CallState.IDLE, 7_000)

        assertEquals(listOf(6L), endedDurations)
    }

    @Test
    fun twoConsecutiveEndedCallsTriggerTwice() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(onCallEnded = endedDurations::add)

        monitor.onStateChanged(CallState.OFFHOOK, 1_000)
        monitor.onStateChanged(CallState.IDLE, 7_000)
        monitor.onStateChanged(CallState.OFFHOOK, 10_000)
        monitor.onStateChanged(CallState.IDLE, 18_000)

        assertEquals(listOf(6L, 8L), endedDurations)
    }

    @Test
    fun customThresholdIsHonored() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(
            minCallDurationSeconds = 2,
            onCallEnded = endedDurations::add
        )

        monitor.onStateChanged(CallState.OFFHOOK, 1_000)
        monitor.onStateChanged(CallState.IDLE, 4_000)

        assertEquals(listOf(3L), endedDurations)
    }
}
