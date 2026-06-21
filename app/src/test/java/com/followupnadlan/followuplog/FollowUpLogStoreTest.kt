package com.followupnadlan.followuplog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpLogStoreTest {
    @Test
    fun appendAndDecodeRoundTripsEntryFields() {
        val entry = entry(
            messagePreview = "Hebrew preview text",
            actionType = FollowUpActionType.WHATSAPP_OPENED,
            timestampEpochMs = 123456789L,
            phone = "972501234567",
            source = "manual"
        )

        val raw = FollowUpLogStorage.append("", entry)
        val decoded = FollowUpLogStorage.decode(raw)

        assertEquals(listOf(entry), decoded)
    }

    @Test
    fun appendTrimsOldEntriesWhenMaxSizeIsReached() {
        val raw = (1L..101L).fold("") { current, timestamp ->
            FollowUpLogStorage.append(current, entry(timestampEpochMs = timestamp))
        }

        val decoded = FollowUpLogStorage.decode(raw)

        assertEquals(100, decoded.size)
        assertEquals(2L, decoded.first().timestampEpochMs)
        assertEquals(101L, decoded.last().timestampEpochMs)
    }

    @Test
    fun emptyLoadReturnsEmptyList() {
        assertEquals(emptyList<FollowUpLogEntry>(), FollowUpLogStorage.decode(""))
    }

    @Test
    fun actionLabelsStayTruthfulAndDoNotClaimDelivery() {
        val labels = FollowUpActionType.entries.map { it.name }

        assertTrue(labels.contains("AUTO_SMS_SENT"))
        assertTrue(labels.contains("AUTO_SMS_FAILED"))
        assertTrue(labels.contains("WHATSAPP_REPLY_PREPARED"))
        assertTrue(labels.contains("WHATSAPP_REPLY_OPENED"))
        assertTrue(labels.contains("WHATSAPP_REPLY_FAILED"))
        assertTrue(labels.contains("WHATSAPP_AUTO_SEND_ATTEMPTED"))
        assertTrue(labels.contains("WHATSAPP_AUTO_SENT"))
        assertTrue(labels.contains("WHATSAPP_AUTO_FAILED"))
        assertTrue(labels.contains("WHATSAPP_ACCESSIBILITY_NOT_ENABLED"))
        assertTrue(labels.none { it == "WHATSAPP_REPLY_SENT" })
        assertTrue(labels.contains("FALLBACK_SMS_SENT"))
        assertTrue(labels.contains("FALLBACK_SMS_OPENED"))
        assertTrue(labels.contains("FALLBACK_SMS_FAILED"))
        assertTrue(labels.contains("AUTO_SMS_SKIPPED_NO_PERMISSION"))
        assertTrue(labels.contains("AUTO_SMS_SKIPPED_DISABLED"))
        assertTrue(labels.contains("AUTO_SMS_SKIPPED_DUPLICATE"))
        assertTrue(labels.contains("AUTO_SMS_SKIPPED_NO_NUMBER"))
        assertTrue(labels.contains("MISSED_CALL_DETECTED"))
        assertTrue(labels.contains("MANUAL_REPLY_OPENED"))
        assertTrue(labels.none { it.contains("DELIVERED") })
    }

    @Test
    fun manualWhatsAppReplyLabelsNeverClaimSent() {
        val labels = FollowUpActionType.entries.map { it.name }
        val manualWhatsAppReplyLabels = labels.filter { it.startsWith("WHATSAPP_REPLY_") }

        assertEquals(
            listOf("WHATSAPP_REPLY_PREPARED", "WHATSAPP_REPLY_OPENED", "WHATSAPP_REPLY_FAILED"),
            manualWhatsAppReplyLabels
        )
    }

    @Test
    fun messagePreviewCompactsWhitespaceAndTrimsToEightyCharacters() {
        val preview = FollowUpLogStorage.messagePreview("a".repeat(90))

        assertEquals(80, preview.length)
    }

    @Test
    fun messagePreviewCompactsWhitespace() {
        val preview = FollowUpLogStorage.messagePreview(
            "  hello\n\nthere,   compact   this please  ",
            maxChars = 20
        )

        assertEquals("hello there, compact", preview)
    }

    @Test
    fun decodeSkipsMalformedRows() {
        val raw = listOf(
            "not-a-valid-row",
            FollowUpLogStorage.append("", entry(actionType = FollowUpActionType.COPY_USED))
        ).joinToString("\n")

        val decoded = FollowUpLogStorage.decode(raw)

        assertEquals(1, decoded.size)
        assertEquals(FollowUpActionType.COPY_USED, decoded.first().actionType)
    }

    @Test
    fun hebrewPreviewRoundTripsThroughBase64() {
        val preview = "שלום יוסי, זה טקסט בעברית לבדיקת שמירה מקומית"

        val decoded = FollowUpLogStorage.decode(FollowUpLogStorage.append("", entry(messagePreview = preview)))

        assertEquals(preview, decoded.single().messagePreview)
    }

    @Test
    fun legacyThreeFieldRowsStillDecode() {
        val legacy = "123|COPY_USED|bGVnYWN5"

        val decoded = FollowUpLogStorage.decode(legacy)

        assertEquals(1, decoded.size)
        assertEquals(FollowUpActionType.COPY_USED, decoded.single().actionType)
        assertEquals("legacy", decoded.single().messagePreview)
        assertEquals("", decoded.single().phone)
        assertEquals("", decoded.single().source)
    }

    @Test
    fun sprint15LogStoresSourceAndPhoneButOnlyPreview() {
        val fullMessage = "a".repeat(120)
        val raw = FollowUpLogStorage.append(
            "",
            entry(
                actionType = FollowUpActionType.AUTO_SMS_SENT,
                messagePreview = fullMessage,
                phone = "972501234567",
                source = "missed_call_auto_response"
            )
        )

        val decoded = FollowUpLogStorage.decode(raw).single()

        assertEquals(FollowUpActionType.AUTO_SMS_SENT, decoded.actionType)
        assertEquals("972501234567", decoded.phone)
        assertEquals("missed_call_auto_response", decoded.source)
        assertEquals(80, decoded.messagePreview.length)
    }

    private fun entry(
        messagePreview: String = "",
        actionType: FollowUpActionType = FollowUpActionType.SHARE_OPENED,
        timestampEpochMs: Long = 1L,
        phone: String = "",
        source: String = ""
    ) = FollowUpLogEntry(
        actionType = actionType,
        timestampEpochMs = timestampEpochMs,
        messagePreview = messagePreview,
        phone = phone,
        source = source
    )
}
