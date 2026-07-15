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
    fun titlesMatchEachMode() {
        assertEquals("שיחה שלא נענתה", FollowUpPromptModeLogic.title(FollowUpPromptMode.MISSED_CALL))
        assertEquals("סיום שיחה", FollowUpPromptModeLogic.title(FollowUpPromptMode.CALL_ENDED))
    }
}
