package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTodayCountTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 7, 27)

    private fun epochMsAt(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun send(
        phone: String,
        date: LocalDate = today,
        hour: Int = 10,
        actionType: FollowUpActionType = FollowUpActionType.WHATSAPP_AUTO_SENT
    ) = FollowUpLogEntry(
        actionType = actionType,
        timestampEpochMs = epochMsAt(date, hour, 0),
        messagePreview = "",
        phone = phone
    )

    @Test
    fun `empty log counts zero`() {
        assertEquals(0, HomeTodayCount.of(emptyList(), zoneId = zone, today = today))
    }

    @Test
    fun `distinct client phones count once each`() {
        val entries = listOf(
            send("972500000001"),
            send("972500000002"),
            send("972500000003")
        )
        assertEquals(3, HomeTodayCount.of(entries, zoneId = zone, today = today))
    }

    @Test
    fun `repeat sends to same client count as one client`() {
        val entries = listOf(
            send("972500000001", hour = 9),
            send("972500000001", hour = 11),
            send("972500000001", hour = 15, actionType = FollowUpActionType.CONTACT_CARD_OPENED)
        )
        assertEquals(1, HomeTodayCount.of(entries, zoneId = zone, today = today))
    }

    @Test
    fun `only today counts, not yesterday`() {
        val entries = listOf(
            send("972500000001", date = today),
            send("972500000002", date = today.minusDays(1))
        )
        assertEquals(1, HomeTodayCount.of(entries, zoneId = zone, today = today))
    }

    @Test
    fun `internal and skipped entries do not inflate the count`() {
        val entries = listOf(
            send("972500000001", actionType = FollowUpActionType.MANUAL_REPLY_PENDING),
            send("972500000002", actionType = FollowUpActionType.AUTO_SMS_SKIPPED_DUPLICATE),
            send("972500000003", actionType = FollowUpActionType.WHATSAPP_AUTO_FAILED),
            send("972500000004", actionType = FollowUpActionType.MISSED_CALL_DETECTED)
        )
        assertEquals(0, HomeTodayCount.of(entries, zoneId = zone, today = today))
    }

    @Test
    fun `no-answer and ended and missed sends all count`() {
        val entries = listOf(
            send("972500000001", actionType = FollowUpActionType.WHATSAPP_AUTO_SENT),
            send("972500000002", actionType = FollowUpActionType.CONTACT_CARD_OPENED),
            FollowUpLogEntry(
                actionType = FollowUpActionType.AUTO_SMS_SENT,
                timestampEpochMs = epochMsAt(today, 12, 0),
                messagePreview = "",
                phone = "972500000003",
                source = com.followupnadlan.postcall.NoAnswerFollowUpMessage.SOURCE
            )
        )
        assertEquals(3, HomeTodayCount.of(entries, zoneId = zone, today = today))
    }

    @Test
    fun `opened-but-not-sent WhatsApp does not inflate the today count (§2)`() {
        val entries = listOf(
            send("972500000001", actionType = FollowUpActionType.WHATSAPP_REPLY_OPENED),
            FollowUpLogEntry(
                actionType = FollowUpActionType.WHATSAPP_REPLY_OPENED,
                timestampEpochMs = epochMsAt(today, 12, 0),
                messagePreview = "",
                phone = "972500000002",
                source = com.followupnadlan.postcall.NoAnswerFollowUpMessage.SOURCE
            )
        )
        assertEquals(0, HomeTodayCount.of(entries, zoneId = zone, today = today))
    }

    @Test
    fun `blank-phone sends each count as one`() {
        val entries = listOf(
            send(""),
            send(""),
            send("972500000001")
        )
        assertEquals(3, HomeTodayCount.of(entries, zoneId = zone, today = today))
    }
}
