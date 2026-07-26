package com.followupnadlan.postcall

import com.followupnadlan.accessibility.RecipientScope
import com.followupnadlan.postcall.CallStateMonitor.CallState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The seam where the ended-call suggestion actually broke.
 *
 * [CallStateMonitorTest] proves the monitor reports transitions and [EndedSuggestionDecisionTest]
 * proves the rules are right, but nothing tied the two together — and the live bug lived precisely
 * in between: the monitor held a duration threshold of its own, measured differently from the
 * decider's, so one half could silently drop a call the other half would have allowed. These tests
 * drive a real monitor into a real decider and assert what the user ends up seeing.
 *
 * Everything Android-side (the service, the call log, the notification) is out of reach in a unit
 * test, so [suggestions] stands in for "showSuggestion was called", and the call log's duration is
 * supplied per call the way [CallLogReader] would report it.
 */
class EndedSuggestionPipelineTest {
    private val phone = "0521234567"
    private val suggestions = mutableListOf<String>()

    /**
     * A conversation, end to end: the monitor observes the state changes, and when it reports the
     * call ended the decider judges it exactly as CallDetectionService does.
     *
     * [loggedDurationSeconds] is what the call log recorded, which is what the real decider reads —
     * separate from the wall-clock the monitor measures.
     */
    private fun conversation(
        loggedDurationSeconds: Long,
        wasAnswered: Boolean = true,
        incoming: Boolean = true,
        offhookMillis: Long = 1_000,
        idleMillis: Long,
        lastSuggestedAtEpochMs: Long? = null,
        lastAnyNotificationAtEpochMs: Long? = null,
        nowEpochMs: Long = 1_000_000L
    ) {
        val monitor = CallStateMonitor(
            onCallEnded = {
                val decision = EndedSuggestionDecider.decide(
                    EndedSuggestionInput(
                        callDurationSeconds = loggedDurationSeconds,
                        phoneNumber = phone,
                        isSavedContact = false,
                        contactsPermissionGranted = true,
                        scope = RecipientScope.NON_CONTACTS_ONLY,
                        lastSuggestedAtEpochMs = lastSuggestedAtEpochMs,
                        lastAnyNotificationAtEpochMs = lastAnyNotificationAtEpochMs,
                        nowEpochMs = nowEpochMs
                    )
                )
                if (decision == EndedSuggestionDecision.SUGGEST) suggestions.add(phone)
            }
        )

        if (incoming) monitor.onStateChanged(CallState.RINGING, offhookMillis - 1_000)
        if (wasAnswered) monitor.onStateChanged(CallState.OFFHOOK, offhookMillis)
        monitor.onStateChanged(CallState.IDLE, idleMillis)
    }

    @Test
    fun answeredIncomingCallOfSixtyFiveSecondsSuggestsExactlyOnce() {
        conversation(loggedDurationSeconds = 65, idleMillis = 66_000)

        assertEquals(listOf(phone), suggestions)
    }

    /** No length rule: a forty-second conversation is still a conversation. */
    @Test
    fun fortySecondCallAlsoSuggests() {
        conversation(loggedDurationSeconds = 40, idleMillis = 41_000)

        assertEquals(listOf(phone), suggestions)
    }

    /** The call you returned is exactly the one that deserves a follow-up. */
    @Test
    fun answeredOutgoingCallOfNinetySecondsSuggests() {
        conversation(loggedDurationSeconds = 90, incoming = false, idleMillis = 91_000)

        assertEquals(listOf(phone), suggestions)
    }

    /** Same number twice inside 24h: the second conversation is silent. */
    @Test
    fun secondConversationWithTheSameNumberWithinTwentyFourHoursStaysSilent() {
        val firstAt = 1_000_000L
        conversation(loggedDurationSeconds = 65, idleMillis = 66_000, nowEpochMs = firstAt)

        val oneHourLater = firstAt + 60 * 60 * 1000L
        conversation(
            loggedDurationSeconds = 65,
            idleMillis = 66_000,
            lastSuggestedAtEpochMs = firstAt,
            nowEpochMs = oneHourLater
        )

        assertEquals(listOf(phone), suggestions)
    }

    /**
     * Two *different* clients inside the same minute. With the quiet window at its default this is
     * the case that got silently dropped on the device, so it is asserted through the real wiring:
     * a suggestion already went out seconds ago, and the second caller must still come through.
     */
    @Test
    fun aDifferentCallerInTheSameMinuteIsNotSilencedWhenTheQuietWindowIsOff() {
        val now = 1_000_000L
        val monitor = CallStateMonitor(
            onCallEnded = {
                val decision = EndedSuggestionDecider.decide(
                    EndedSuggestionInput(
                        callDurationSeconds = 65,
                        phoneNumber = "0529998888",
                        isSavedContact = false,
                        contactsPermissionGranted = true,
                        scope = RecipientScope.NON_CONTACTS_ONLY,
                        lastSuggestedAtEpochMs = null,
                        lastAnyNotificationAtEpochMs = now - 5_000L,
                        globalQuietMillis = null,
                        nowEpochMs = now
                    )
                )
                if (decision == EndedSuggestionDecision.SUGGEST) suggestions.add("0529998888")
            }
        )

        monitor.onStateChanged(CallState.RINGING, 1_000)
        monitor.onStateChanged(CallState.OFFHOOK, 2_000)
        monitor.onStateChanged(CallState.IDLE, 67_000)

        assertEquals(listOf("0529998888"), suggestions)
    }

    /**
     * The missed-call auto-reply is the older feature and shares the same IDLE transition; none of
     * the loosening above may cost it. It stays strictly about *unanswered incoming* calls, so an
     * answered one must still leave it silent.
     */
    @Test
    fun missedCallReplyStillFiresAndStaysLimitedToUnansweredIncomingCalls() {
        var missed = 0
        val monitor = CallStateMonitor(onCallEnded = {}, onMissedIncomingCall = { missed += 1 })

        monitor.onStateChanged(CallState.RINGING, 1_000)
        monitor.onStateChanged(CallState.IDLE, 10_000)
        assertEquals(1, missed)

        monitor.onStateChanged(CallState.RINGING, 20_000)
        monitor.onStateChanged(CallState.OFFHOOK, 21_000)
        monitor.onStateChanged(CallState.IDLE, 30_000)
        assertEquals("an answered call is not a missed one", 1, missed)
    }

    /** A three-second call reaches the notification too — length gates nothing anymore. */
    @Test
    fun briefCallStillSuggests() {
        conversation(loggedDurationSeconds = 3, idleMillis = 4_000)

        assertEquals(listOf(phone), suggestions)
    }

    /**
     * The case that used to be dropped twice over — the monitor never reported RINGING -> IDLE, and
     * the decider rejected a zero duration. A call you could not take now suggests like any other.
     */
    @Test
    fun unansweredIncomingCallAlsoSuggests() {
        conversation(loggedDurationSeconds = 0, wasAnswered = false, idleMillis = 4_000)

        assertEquals(listOf(phone), suggestions)
    }

    /** An unanswered call drives both features at once, and they must not cancel each other. */
    @Test
    fun unansweredCallFiresBothTheSuggestionAndTheMissedCallReply() {
        var missed = 0
        val monitor = CallStateMonitor(
            onCallEnded = { suggestions.add(phone) },
            onMissedIncomingCall = { missed += 1 }
        )

        monitor.onStateChanged(CallState.RINGING, 1_000)
        monitor.onStateChanged(CallState.IDLE, 10_000)

        assertEquals(listOf(phone), suggestions)
        assertEquals(1, missed)
    }

    /** An outgoing call nobody picked up: OFFHOOK with no ring, still a follow-up. */
    @Test
    fun outgoingCallThatWasNotPickedUpSuggests() {
        conversation(loggedDurationSeconds = 0, incoming = false, idleMillis = 4_000)

        assertEquals(listOf(phone), suggestions)
    }
}
