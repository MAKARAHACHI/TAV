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
            timestampEpochMs = 123456789L
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

        assertEquals(listOf("WHATSAPP_OPENED", "SHARE_OPENED", "COPY_USED"), labels)
        assertTrue(labels.none { it.contains("SENT") || it.contains("DELIVERED") })
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

    private fun entry(
        messagePreview: String = "",
        actionType: FollowUpActionType = FollowUpActionType.SHARE_OPENED,
        timestampEpochMs: Long = 1L
    ) = FollowUpLogEntry(
        actionType = actionType,
        timestampEpochMs = timestampEpochMs,
        messagePreview = messagePreview
    )
}
