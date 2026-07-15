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
    CALL_ENDED
}

object FollowUpPromptModeLogic {
    /**
     * Maps the notification's `callType` extra ("missed" / "incoming" / "outgoing", or null)
     * to the prompt mode. Only an explicit "missed" is treated as a missed call; anything
     * else — including an unknown/blank type — is treated as a completed call.
     */
    fun fromCallType(callType: String?): FollowUpPromptMode =
        if (callType?.trim()?.lowercase() == CALL_TYPE_MISSED) {
            FollowUpPromptMode.MISSED_CALL
        } else {
            FollowUpPromptMode.CALL_ENDED
        }

    fun title(mode: FollowUpPromptMode): String = when (mode) {
        FollowUpPromptMode.MISSED_CALL -> "שיחה שלא נענתה"
        FollowUpPromptMode.CALL_ENDED -> "סיום שיחה"
    }

    const val CALL_TYPE_MISSED = "missed"
}
