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

    /**
     * The monitor reports every ended call, however short. Filtering by length is the decider's
     * job and happens against the call log's duration — a second threshold here would have been a
     * different measurement gating the same rule. See EndedSuggestionDecisionTest for the 60s rule.
     */
    @Test
    fun shortCallIsStillReportedAndLeftForTheDeciderToFilter() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(onCallEnded = endedDurations::add)

        monitor.onStateChanged(CallState.OFFHOOK, 1_000)
        monitor.onStateChanged(CallState.IDLE, 4_000)

        assertEquals(listOf(3L), endedDurations)
    }

    /**
     * A ring nobody answered ends a call too. It reports a duration of zero — no conversation took
     * place — and the decider treats it as a follow-up opportunity like any other.
     */
    @Test
    fun ringingToIdleReportsAnEndedCallWithZeroDuration() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(onCallEnded = endedDurations::add)

        monitor.onStateChanged(CallState.RINGING, 1_000)
        monitor.onStateChanged(CallState.IDLE, 10_000)

        assertEquals(listOf(0L), endedDurations)
    }

    /** Nothing ever happened: IDLE arriving out of nowhere is not a call. */
    @Test
    fun idleWithoutAnyPrecedingCallReportsNothing() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(onCallEnded = endedDurations::add)

        monitor.onStateChanged(CallState.IDLE, 10_000)

        assertEquals(emptyList<Long>(), endedDurations)
    }

    @Test
    fun ringingToIdleTriggersMissedIncomingCall() {
        var missedCalls = 0
        val monitor = CallStateMonitor(
            onCallEnded = {},
            onMissedIncomingCall = { missedCalls += 1 }
        )

        monitor.onStateChanged(CallState.RINGING, 1_000)
        monitor.onStateChanged(CallState.IDLE, 10_000)

        assertEquals(1, missedCalls)
    }

    @Test
    fun answeredIncomingCallDoesNotTriggerMissedIncomingCall() {
        var missedCalls = 0
        val monitor = CallStateMonitor(
            onCallEnded = {},
            onMissedIncomingCall = { missedCalls += 1 }
        )

        monitor.onStateChanged(CallState.RINGING, 1_000)
        monitor.onStateChanged(CallState.OFFHOOK, 2_000)
        monitor.onStateChanged(CallState.IDLE, 10_000)

        assertEquals(0, missedCalls)
    }

    @Test
    fun offhookOnlyCallDoesNotTriggerMissedIncomingCall() {
        var missedCalls = 0
        val monitor = CallStateMonitor(
            onCallEnded = {},
            onMissedIncomingCall = { missedCalls += 1 }
        )

        monitor.onStateChanged(CallState.OFFHOOK, 2_000)
        monitor.onStateChanged(CallState.IDLE, 10_000)

        assertEquals(0, missedCalls)
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

    /** A one-second courtesy call still reaches the decider, which is what rejects it. */
    @Test
    fun veryShortCallIsReportedWithItsRealDuration() {
        val endedDurations = mutableListOf<Long>()
        val monitor = CallStateMonitor(onCallEnded = endedDurations::add)

        monitor.onStateChanged(CallState.OFFHOOK, 1_000)
        monitor.onStateChanged(CallState.IDLE, 2_000)

        assertEquals(listOf(1L), endedDurations)
    }
}
