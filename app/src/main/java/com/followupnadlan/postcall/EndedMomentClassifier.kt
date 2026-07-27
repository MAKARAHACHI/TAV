package com.followupnadlan.postcall

/**
 * Which "after a call ends" moment a finished call belongs to. Pure logic, no Android — see
 * EndedMomentClassifierTest.
 *
 * This runs on the AFTER-CALL path only ([CallDetectionService.offerFollowUpForLatestCall]); the
 * missed-incoming moment is detected LIVE by [PhoneStateEventProcessor] and is untouched here.
 *
 * The split is deliberately additive: the ENDED moment already existed and offered a follow-up for
 * every call that ended, including an outgoing call that never connected. Part A carves that one
 * case out into its own NO_ANSWER moment so it can carry its own wording, its own per-moment
 * enable, and its own default variant. Everything the ended moment used to claim, it still claims —
 * except the outgoing-not-answered call, which now belongs to NO_ANSWER.
 */
enum class EndedMoment {
    /** Outgoing call I placed that the client did not pick up (duration 0 / no connect). */
    NO_ANSWER_OUTGOING,

    /** Any other finished call (answered incoming/outgoing, or an unanswered incoming). */
    ENDED
}

object EndedMomentClassifier {
    /**
     * An outgoing call with zero duration never connected — the client did not answer. Every other
     * finished call keeps its existing ENDED classification, so the verified ended behaviour is
     * unchanged for answered calls.
     */
    fun classify(type: FollowUpCallType, durationSeconds: Long): EndedMoment =
        if (type == FollowUpCallType.Outgoing && durationSeconds <= 0L) {
            EndedMoment.NO_ANSWER_OUTGOING
        } else {
            EndedMoment.ENDED
        }
}
