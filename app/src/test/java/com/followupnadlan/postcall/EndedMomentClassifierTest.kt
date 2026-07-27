package com.followupnadlan.postcall

import org.junit.Assert.assertEquals
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
}
