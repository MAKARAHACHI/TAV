package com.followupnadlan.postcall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndedMomentClassifierTest {
    @Test
    fun outgoingWithZeroDurationIsNoAnswer() {
        assertEquals(
            EndedMoment.NO_ANSWER_OUTGOING,
            EndedMomentClassifier.classify(FollowUpCallType.Outgoing, durationSeconds = 0L)
        )
    }

    @Test
    fun outgoingWithNegativeDurationIsNoAnswer() {
        // A defensive guard: a bogus negative duration is still "did not connect".
        assertEquals(
            EndedMoment.NO_ANSWER_OUTGOING,
            EndedMomentClassifier.classify(FollowUpCallType.Outgoing, durationSeconds = -1L)
        )
    }

    @Test
    fun answeredOutgoingIsEnded() {
        // An outgoing call that connected keeps the verified ENDED classification.
        assertEquals(
            EndedMoment.ENDED,
            EndedMomentClassifier.classify(FollowUpCallType.Outgoing, durationSeconds = 12L)
        )
    }

    @Test
    fun incomingCallsAreAlwaysEndedRegardlessOfDuration() {
        // The missed-incoming moment is detected live elsewhere; on this after-call path an
        // incoming call — answered or not — is never reclassified as NO_ANSWER.
        assertEquals(EndedMoment.ENDED, EndedMomentClassifier.classify(FollowUpCallType.Incoming, 0L))
        assertEquals(EndedMoment.ENDED, EndedMomentClassifier.classify(FollowUpCallType.Incoming, 30L))
        assertEquals(EndedMoment.ENDED, EndedMomentClassifier.classify(FollowUpCallType.Missed, 0L))
    }

    // WAVE G: the ENDED moment's notification TITLE must not mislabel a 0-duration answered call.
    // Duration never enters this decision — only the platform call-log TYPE.

    @Test
    fun zeroDurationIncomingCallStillTitledAsAnsweredForEnded() {
        // A connected incoming call the platform logs with a (rare/rounded) 0 duration must still
        // read as a real conversation, not "שיחה שלא נענתה".
        assertTrue(EndedMomentClassifier.wasAnsweredForEndedTitle(FollowUpCallType.Incoming))
    }

    @Test
    fun zeroDurationOutgoingCallThatReachedEndedIsStillTitledAsAnswered() {
        // An outgoing call only reaches ENDED (not NO_ANSWER_OUTGOING) once it connected, so it is
        // always titled as answered here regardless of duration.
        assertTrue(EndedMomentClassifier.wasAnsweredForEndedTitle(FollowUpCallType.Outgoing))
    }

    @Test
    fun missedTypeIsNeverTitledAsAnswered() {
        // A genuinely missed call — the NO_ANSWER_OUTGOING wording's sibling case on the incoming
        // side — must keep the "שיחה שלא נענתה" title.
        assertFalse(EndedMomentClassifier.wasAnsweredForEndedTitle(FollowUpCallType.Missed))
    }
}
