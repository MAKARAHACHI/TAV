package com.followupnadlan.data

import com.followupnadlan.data.followup.FollowUpTaskEntity
import com.followupnadlan.data.lead.LeadEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FollowUpRoomContractTest {
    @Test
    fun followUpTaskEntityPreservesContractFields() {
        val task = FollowUpTaskEntity(
            id = 7,
            phone = "",
            contactName = "Yossi",
            callEndedAtEpochMs = 1_000,
            callDurationSeconds = 42,
            source = "POST_CALL_AUTO",
            selectedTemplateId = "buyer_property_details",
            draftText = "draft",
            leadType = "BUYER",
            propertyLink = "https://example.test/property",
            reminderAtEpochMs = 2_000,
            status = "SNOOZED",
            createdAtEpochMs = 500,
            updatedAtEpochMs = 600
        )

        assertEquals(7L, task.id)
        assertEquals("", task.phone)
        assertEquals("Yossi", task.contactName)
        assertEquals(1_000L, task.callEndedAtEpochMs)
        assertEquals(42L, task.callDurationSeconds)
        assertEquals("POST_CALL_AUTO", task.source)
        assertEquals("buyer_property_details", task.selectedTemplateId)
        assertEquals("draft", task.draftText)
        assertEquals("BUYER", task.leadType)
        assertEquals("https://example.test/property", task.propertyLink)
        assertEquals(2_000L, task.reminderAtEpochMs)
        assertEquals("SNOOZED", task.status)
        assertEquals(500L, task.createdAtEpochMs)
        assertEquals(600L, task.updatedAtEpochMs)
    }

    @Test
    fun leadEntityPreservesContractFields() {
        val lead = LeadEntity(
            id = 3,
            fullName = null,
            phone = "972501234567",
            type = "UNKNOWN",
            status = "NEW",
            notes = null,
            lastCallAtEpochMs = null,
            lastFollowUpAtEpochMs = 3_000,
            createdAtEpochMs = 1_000,
            updatedAtEpochMs = 2_000
        )

        assertEquals(3L, lead.id)
        assertNull(lead.fullName)
        assertEquals("972501234567", lead.phone)
        assertEquals("UNKNOWN", lead.type)
        assertEquals("NEW", lead.status)
        assertNull(lead.notes)
        assertNull(lead.lastCallAtEpochMs)
        assertEquals(3_000L, lead.lastFollowUpAtEpochMs)
        assertEquals(1_000L, lead.createdAtEpochMs)
        assertEquals(2_000L, lead.updatedAtEpochMs)
    }
}
