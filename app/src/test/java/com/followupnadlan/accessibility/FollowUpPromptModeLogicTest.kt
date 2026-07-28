package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class FollowUpPromptModeLogicTest {
    @Test
    fun missedCallTypeIsMissedMode() {
        assertEquals(FollowUpPromptMode.MISSED_CALL, FollowUpPromptModeLogic.fromCallType("missed"))
        assertEquals(FollowUpPromptMode.MISSED_CALL, FollowUpPromptModeLogic.fromCallType("MISSED"))
        assertEquals(FollowUpPromptMode.MISSED_CALL, FollowUpPromptModeLogic.fromCallType(" missed "))
    }

    @Test
    fun answeredOrOutgoingIsCallEndedMode() {
        assertEquals(FollowUpPromptMode.CALL_ENDED, FollowUpPromptModeLogic.fromCallType("incoming"))
        assertEquals(FollowUpPromptMode.CALL_ENDED, FollowUpPromptModeLogic.fromCallType("outgoing"))
    }

    @Test
    fun unknownOrBlankTypeDefaultsToCallEnded() {
        assertEquals(FollowUpPromptMode.CALL_ENDED, FollowUpPromptModeLogic.fromCallType(null))
        assertEquals(FollowUpPromptMode.CALL_ENDED, FollowUpPromptModeLogic.fromCallType(""))
        assertEquals(FollowUpPromptMode.CALL_ENDED, FollowUpPromptModeLogic.fromCallType("something-else"))
    }

    @Test
    fun noAnswerTypeIsNoAnswerMode() {
        assertEquals(FollowUpPromptMode.NO_ANSWER_OUTGOING, FollowUpPromptModeLogic.fromCallType("no_answer"))
        assertEquals(FollowUpPromptMode.NO_ANSWER_OUTGOING, FollowUpPromptModeLogic.fromCallType(" NO_ANSWER "))
    }

    @Test
    fun manualMissedPromptCallTypeMapsToMissedMode() {
        // FIX 1 / §2: MissedCallManualReplyNotificationHelper tags its prompt intent with
        // CALL_TYPE_MISSED. This asserts that exact value routes the approval sheet to the
        // MISSED moment (not the CALL_ENDED default reached when the extra is absent).
        assertEquals(
            FollowUpPromptMode.MISSED_CALL,
            FollowUpPromptModeLogic.fromCallType(FollowUpPromptModeLogic.CALL_TYPE_MISSED)
        )
    }

    @Test
    fun titlesMatchEachMode() {
        assertEquals("שיחה שלא נענתה", FollowUpPromptModeLogic.title(FollowUpPromptMode.MISSED_CALL))
        assertEquals("סיום שיחה", FollowUpPromptModeLogic.title(FollowUpPromptMode.CALL_ENDED))
        assertEquals("לא ענו לך", FollowUpPromptModeLogic.title(FollowUpPromptMode.NO_ANSWER_OUTGOING))
    }
}
