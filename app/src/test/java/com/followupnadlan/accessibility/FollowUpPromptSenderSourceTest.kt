package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.postcall.EndedSendReceiver
import com.followupnadlan.postcall.NoAnswerFollowUpMessage
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Proves the wiring fix: the source [FollowUpPromptSender] stamps on a no-answer send is exactly the
 * tag [HomeTodayCount] and [HistoryFeed] key off, so a no-answer send now counts in "היום" and shows
 * as a NO_ANSWER row. The send() call itself opens WhatsApp (Android), so this verifies the log
 * entry it produces — built via the same sourceFor mapping — flows through the counters.
 */
class FollowUpPromptSenderSourceTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 7, 27)

    private fun epochMsAt(hour: Int): Long =
        today.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    /** The client-facing entry a no-answer send logs, stamped exactly as FollowUpPromptSender.send does. */
    private fun noAnswerSend(phone: String, hour: Int) = FollowUpLogEntry(
        actionType = FollowUpActionType.WHATSAPP_REPLY_OPENED,
        timestampEpochMs = epochMsAt(hour),
        messagePreview = "שלום",
        phone = phone,
        source = FollowUpPromptSender.sourceFor(FollowUpPromptMode.NO_ANSWER_OUTGOING)
    )

    @Test
    fun `no-answer source is the tag the counters look for`() {
        assertEquals(
            NoAnswerFollowUpMessage.SOURCE,
            FollowUpPromptSender.sourceFor(FollowUpPromptMode.NO_ANSWER_OUTGOING)
        )
        assertEquals(
            MissedCallAutoResponseSettings.SOURCE,
            FollowUpPromptSender.sourceFor(FollowUpPromptMode.MISSED_CALL)
        )
        assertEquals(
            EndedSendReceiver.SOURCE,
            FollowUpPromptSender.sourceFor(FollowUpPromptMode.CALL_ENDED)
        )
    }

    @Test
    fun `a no-answer send is counted by HomeTodayCount`() {
        val entries = listOf(noAnswerSend("972500000001", hour = 10))
        assertEquals(1, HomeTodayCount.of(entries, zoneId = zone, today = today))
    }

    @Test
    fun `a no-answer send is classified NO_ANSWER by HistoryFeed`() {
        val rows = HistoryFeed.rows(listOf(noAnswerSend("972500000001", hour = 10)), zoneId = zone, today = today)
        assertEquals(1, rows.size)
        assertEquals(HistoryMoment.NO_ANSWER, rows[0].moment)
        assertEquals("נשלחה הודעת \"לא ענו\"", rows[0].summary)
    }
}
