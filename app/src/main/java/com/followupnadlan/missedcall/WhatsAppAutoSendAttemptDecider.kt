package com.followupnadlan.missedcall

enum class WhatsAppAutoSendAttemptOutcome {
    WAIT_FOR_MORE_EVENTS,
    FAIL_PENDING
}

object WhatsAppAutoSendAttemptDecider {
    private const val FAIL_AFTER_MILLIS = 15_000L

    fun whenSendButtonMissing(
        pendingCreatedAtEpochMs: Long,
        nowEpochMs: Long
    ): WhatsAppAutoSendAttemptOutcome {
        val ageMillis = nowEpochMs - pendingCreatedAtEpochMs
        return if (ageMillis >= FAIL_AFTER_MILLIS) {
            WhatsAppAutoSendAttemptOutcome.FAIL_PENDING
        } else {
            WhatsAppAutoSendAttemptOutcome.WAIT_FOR_MORE_EVENTS
        }
    }
}
