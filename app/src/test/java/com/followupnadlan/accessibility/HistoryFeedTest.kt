package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFeedTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 7, 27)

    private fun epochMsAt(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `missed-call send groups under today with missed summary`() {
        val entry = FollowUpLogEntry(
            actionType = FollowUpActionType.WHATSAPP_AUTO_SENT,
            timestampEpochMs = epochMsAt(today, 14, 30),
            messagePreview = "שלום",
            phone = "972549876543"
        )

        val rows = HistoryFeed.rows(listOf(entry), zoneId = zone, today = today)

        assertEquals(1, rows.size)
        assertEquals(HistoryMoment.MISSED, rows[0].moment)
        assertEquals("היום", rows[0].dateGroup)
        assertEquals("14:30", rows[0].time)
        assertEquals("נשלחה הודעת \"לא עניתי\"", rows[0].summary)
    }

    @Test
    fun `contact card open groups as ended moment`() {
        val entry = FollowUpLogEntry(
            actionType = FollowUpActionType.CONTACT_CARD_OPENED,
            timestampEpochMs = epochMsAt(today, 11, 15),
            messagePreview = "",
            phone = "972521112223"
        )

        val rows = HistoryFeed.rows(listOf(entry), zoneId = zone, today = today)

        assertEquals(HistoryMoment.ENDED, rows[0].moment)
    }

    @Test
    fun `yesterday date groups separately from today`() {
        val entry = FollowUpLogEntry(
            actionType = FollowUpActionType.AUTO_SMS_SENT,
            timestampEpochMs = epochMsAt(today.minusDays(1), 16, 40),
            messagePreview = "",
            phone = "972521112223"
        )

        val rows = HistoryFeed.rows(listOf(entry), zoneId = zone, today = today)

        assertEquals("אתמול", rows[0].dateGroup)
    }

    @Test
    fun `internal diagnostic entries are filtered out`() {
        val entry = FollowUpLogEntry(
            actionType = FollowUpActionType.MANUAL_REPLY_PENDING,
            timestampEpochMs = epochMsAt(today, 9, 0),
            messagePreview = "",
            phone = "972500000000"
        )

        val rows = HistoryFeed.rows(listOf(entry), zoneId = zone, today = today)

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `rows sorted newest first`() {
        val earlier = FollowUpLogEntry(
            actionType = FollowUpActionType.AUTO_SMS_SENT,
            timestampEpochMs = epochMsAt(today, 9, 0),
            messagePreview = "",
            phone = "972500000001"
        )
        val later = FollowUpLogEntry(
            actionType = FollowUpActionType.AUTO_SMS_SENT,
            timestampEpochMs = epochMsAt(today, 17, 0),
            messagePreview = "",
            phone = "972500000002"
        )

        val rows = HistoryFeed.rows(listOf(earlier, later), zoneId = zone, today = today)

        assertEquals("17:00", rows[0].time)
        assertEquals("09:00", rows[1].time)
    }
}
