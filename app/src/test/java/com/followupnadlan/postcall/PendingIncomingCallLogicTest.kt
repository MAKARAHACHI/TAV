package com.followupnadlan.postcall

import com.followupnadlan.postcall.CallStateMonitor.CallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingIncomingCallLogicTest {
    @Test
    fun ringingToIdleWithoutOffhookConfirmsMissedCall() {
        val stored = PendingIncomingCallLogic.transition(
            pending = null,
            newState = CallState.RINGING,
            nowMillis = 1_000L,
            incomingNumber = "0501234567"
        ).nextPending

        val idle = PendingIncomingCallLogic.transition(
            pending = stored,
            newState = CallState.IDLE,
            nowMillis = 3_000L,
            incomingNumber = null
        )

        assertEquals(PendingIncomingCallAction.CONFIRMED_MISSED, idle.action)
        assertEquals("0501234567", idle.confirmedMissedPhoneNumber)
        assertNull(idle.nextPending)
    }

    @Test
    fun ringingOffhookIdleIsAnsweredAndNoMissedCallAction() {
        val stored = PendingIncomingCallLogic.transition(
            pending = null,
            newState = CallState.RINGING,
            nowMillis = 1_000L,
            incomingNumber = "0501234567"
        ).nextPending
        val answered = PendingIncomingCallLogic.transition(
            pending = stored,
            newState = CallState.OFFHOOK,
            nowMillis = 2_000L,
            incomingNumber = null
        ).nextPending

        val idle = PendingIncomingCallLogic.transition(
            pending = answered,
            newState = CallState.IDLE,
            nowMillis = 5_000L,
            incomingNumber = null
        )

        assertEquals(PendingIncomingCallAction.IGNORED_ANSWERED, idle.action)
        assertNull(idle.confirmedMissedPhoneNumber)
        assertNull(idle.nextPending)
    }

    @Test
    fun pendingCallStateCanBeRecreatedBeforeIdle() {
        val recreated = PendingIncomingCall(
            phoneNumber = "0501234567",
            timestampMillis = 1_000L,
            sawOffhook = false
        )

        val idle = PendingIncomingCallLogic.transition(
            pending = recreated,
            newState = CallState.IDLE,
            nowMillis = 2_500L,
            incomingNumber = null
        )

        assertEquals(PendingIncomingCallAction.CONFIRMED_MISSED, idle.action)
        assertEquals("0501234567", idle.confirmedMissedPhoneNumber)
    }

    @Test
    fun stalePendingCallIsIgnored() {
        val stale = PendingIncomingCall(
            phoneNumber = "0501234567",
            timestampMillis = 1_000L,
            sawOffhook = false
        )

        val idle = PendingIncomingCallLogic.transition(
            pending = stale,
            newState = CallState.IDLE,
            nowMillis = 1_000L + PendingIncomingCallLogic.DEFAULT_PENDING_WINDOW_MILLIS + 1L,
            incomingNumber = null
        )

        assertEquals(PendingIncomingCallAction.IGNORED_STALE, idle.action)
        assertNull(idle.confirmedMissedPhoneNumber)
        assertNull(idle.nextPending)
    }
}
