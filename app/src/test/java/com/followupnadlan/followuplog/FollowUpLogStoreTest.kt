package com.followupnadlan.followuplog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpLogStoreTest {
    @Test
    fun appendAndDecodeRoundTripsEntryFields() {
        val entry = entry(
            leadName = "יוסי כהן",
            phoneNumber = "972501234567",
            templateId = "buyer_property_details",
            templateTitle = "פרטי נכס",
            propertyName = "דירה ברעננה",
            propertyLink = "https://example.com/property",
            messagePreview = "שלום יוסי, בהמשך לשיחה...",
            actionType = FollowUpActionType.WHATSAPP_OPENED,
            timestampEpochMs = 123456789L
        )

        val raw = FollowUpLogStorage.append("", entry)
        val decoded = FollowUpLogStorage.decode(raw)

        assertEquals(listOf(entry), decoded)
    }

    @Test
    fun appendTrimsOldEntriesWhenMaxSizeIsReached() {
        val raw = (1L..4L).fold("") { current, timestamp ->
            FollowUpLogStorage.append(current, entry(timestampEpochMs = timestamp), maxEntries = 3)
        }

        val decoded = FollowUpLogStorage.decode(raw)

        assertEquals(listOf(2L, 3L, 4L), decoded.map { it.timestampEpochMs })
    }

    @Test
    fun actionLabelsStayTruthfulAndDoNotClaimDelivery() {
        val labels = FollowUpActionType.entries.map { it.name }

        assertEquals(listOf("WHATSAPP_OPENED", "SHARE_OPENED", "COPY_USED"), labels)
        assertTrue(labels.none { it.contains("SENT") || it.contains("DELIVERED") })
    }

    @Test
    fun messagePreviewCompactsWhitespaceAndTrimsLength() {
        val preview = FollowUpLogStorage.messagePreview(
            "  שלום\n\nיוסי,   הנה פרטי הנכס והקישור להמשך בדיקה.  ",
            maxChars = 20
        )

        assertEquals("שלום יוסי, הנה פרטי", preview)
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

    private fun entry(
        leadName: String = "",
        phoneNumber: String = "",
        templateId: String = "",
        templateTitle: String = "",
        propertyName: String = "",
        propertyLink: String = "",
        messagePreview: String = "",
        actionType: FollowUpActionType = FollowUpActionType.SHARE_OPENED,
        timestampEpochMs: Long = 1L
    ) = FollowUpLogEntry(
        leadName = leadName,
        phoneNumber = phoneNumber,
        templateId = templateId,
        templateTitle = templateTitle,
        propertyName = propertyName,
        propertyLink = propertyLink,
        messagePreview = messagePreview,
        actionType = actionType,
        timestampEpochMs = timestampEpochMs
    )
}
