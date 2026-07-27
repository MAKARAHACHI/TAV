package com.followupnadlan.accessibility

/**
 * Whether the follow-up prompt was reached after a missed call or after a call that
 * actually took place (answered / outgoing). Both open the same action screen with the
 * same send functions — only the wording differs.
 */
enum class FollowUpPromptMode {
    /** Incoming call that was not answered. */
    MISSED_CALL,

    /** Call that ended normally (answered incoming or outgoing). */
    CALL_ENDED,

    /** Outgoing call I made that the client did not answer (Part A). */
    NO_ANSWER_OUTGOING
}

object FollowUpPromptModeLogic {
    /**
     * Maps the notification's `callType` extra ("missed" / "no_answer" / "incoming" / "outgoing",
     * or null) to the prompt mode. An explicit "missed" is a missed incoming call and "no_answer"
     * is an outgoing call the client did not pick up; anything else — including an unknown/blank
     * type — is treated as a completed call.
     */
    fun fromCallType(callType: String?): FollowUpPromptMode =
        when (callType?.trim()?.lowercase()) {
            CALL_TYPE_MISSED -> FollowUpPromptMode.MISSED_CALL
            CALL_TYPE_NO_ANSWER -> FollowUpPromptMode.NO_ANSWER_OUTGOING
            else -> FollowUpPromptMode.CALL_ENDED
        }

    fun title(mode: FollowUpPromptMode): String = when (mode) {
        FollowUpPromptMode.MISSED_CALL -> "שיחה שלא נענתה"
        FollowUpPromptMode.CALL_ENDED -> "סיום שיחה"
        FollowUpPromptMode.NO_ANSWER_OUTGOING -> "לא ענו לך"
    }

    const val CALL_TYPE_MISSED = "missed"
    const val CALL_TYPE_NO_ANSWER = "no_answer"
}
