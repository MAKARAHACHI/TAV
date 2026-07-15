package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastMissedCallerTest {
    @Test
    fun returnsLatestMissedCaller() {
        val caller = LastMissedCallerLogic.from(
            listOf(
                entry(FollowUpActionType.MISSED_CALL_DETECTED, phone = "0501111111", time = 1),
                entry(FollowUpActionType.MISSED_CALL_DETECTED, phone = "0502222222", time = 2)
            )
        )

        assertEquals("0502222222", caller?.phone)
        assertEquals(LastMissedCallerStatus.PENDING, caller?.status)
    }

    @Test
    fun mapsSentStatusForLatestCaller() {
        val caller = LastMissedCallerLogic.from(
            listOf(
                entry(FollowUpActionType.MISSED_CALL_DETECTED, time = 1),
                entry(FollowUpActionType.AUTO_SMS_SENT, time = 2)
            )
        )

        assertEquals(LastMissedCallerStatus.SENT, caller?.status)
        assertNull(caller?.failureMessage)
    }

    @Test
    fun mapsFailureStatusWithFriendlyMessage() {
        val caller = LastMissedCallerLogic.from(
            listOf(
                entry(FollowUpActionType.MISSED_CALL_DETECTED, time = 1),
                entry(FollowUpActionType.WHATSAPP_REPLY_FAILED, time = 2)
            )
        )

        assertEquals(LastMissedCallerStatus.NOT_SENT, caller?.status)
        assertEquals("לא הצלחנו לשלוח הודעה למתקשר הזה", caller?.failureMessage)
    }

    @Test
    fun ignoresEntriesForOlderCallerWhenPickingLatest() {
        val caller = LastMissedCallerLogic.from(
            listOf(
                entry(FollowUpActionType.MISSED_CALL_DETECTED, phone = "0501111111", time = 1),
                entry(FollowUpActionType.AUTO_SMS_SENT, phone = "0501111111", time = 2),
                entry(FollowUpActionType.MISSED_CALL_DETECTED, phone = "0502222222", time = 3)
            )
        )

        assertEquals("0502222222", caller?.phone)
        assertEquals(LastMissedCallerStatus.PENDING, caller?.status)
    }

    private fun entry(
        type: FollowUpActionType,
        phone: String = "0501234567",
        time: Long
    ) = FollowUpLogEntry(
        actionType = type,
        timestampEpochMs = time,
        messagePreview = "",
        phone = phone,
        source = "test"
    )
}
