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
 * tag [HomeTodayCount] and [HistoryFeed] key off, so a no-answer send shows as a NO_ANSWER row. WAVE
 * H (§2): the immediate marker [FollowUpPromptSender] logs on OPENED is WHATSAPP_REPLY_OPENED — an
 * open, not a proven send — so it now shows honestly as "נפתח" in history and is NOT tallied in the
 * today-count (only WHATSAPP_AUTO_SENT/AUTO_SMS_SENT/FALLBACK_SMS_SENT are). The accessibility
 * service adds WHATSAPP_AUTO_SENT separately once it actually clicks send; that is covered by the
 * plain "sent types count" tests elsewhere and is unaffected here.
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
    fun `an opened-but-not-yet-sent no-answer entry is NOT counted by HomeTodayCount (§2)`() {
        val entries = listOf(noAnswerSend("972500000001", hour = 10))
        assertEquals(0, HomeTodayCount.of(entries, zoneId = zone, today = today))
    }

    @Test
    fun `a no-answer send is classified NO_ANSWER by HistoryFeed, worded as opened not sent`() {
        val rows = HistoryFeed.rows(listOf(noAnswerSend("972500000001", hour = 10)), zoneId = zone, today = today)
        assertEquals(1, rows.size)
        assertEquals(HistoryMoment.NO_ANSWER, rows[0].moment)
        assertEquals("WhatsApp נפתח (לחץ שלח)", rows[0].summary)
    }
}
