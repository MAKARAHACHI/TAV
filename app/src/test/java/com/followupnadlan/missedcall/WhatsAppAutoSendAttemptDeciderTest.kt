package com.followupnadlan.missedcall

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppAutoSendAttemptDeciderTest {
    @Test
    fun missingSendButtonWaitsDuringInitialLoadWindow() {
        val outcome = WhatsAppAutoSendAttemptDecider.whenSendButtonMissing(
            pendingCreatedAtEpochMs = 1_000L,
            nowEpochMs = 10_000L
        )

        assertEquals(WhatsAppAutoSendAttemptOutcome.WAIT_FOR_MORE_EVENTS, outcome)
    }

    @Test
    fun missingSendButtonFailsAfterGraceWindow() {
        val outcome = WhatsAppAutoSendAttemptDecider.whenSendButtonMissing(
            pendingCreatedAtEpochMs = 1_000L,
            nowEpochMs = 16_000L
        )

        assertEquals(WhatsAppAutoSendAttemptOutcome.FAIL_PENDING, outcome)
    }
}
