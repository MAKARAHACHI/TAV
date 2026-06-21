package com.followupnadlan.pipeline

import com.followupnadlan.data.followup.FollowUpTaskEntity
import com.followupnadlan.data.lead.LeadEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeadPipelineTest {
    @Test
    fun createPendingPostCallTaskPreservesCardAndCallFields() {
        val task = LeadPipeline.createPendingPostCallTask(
            phone = "050-1234567",
            contactName = "Michael",
            selectedTemplateId = "buyer_property_details",
            draftText = "draft",
            callEndedAtEpochMs = 1_000L,
            callDurationSeconds = 42L,
            leadType = LeadType.BUYER,
            propertyLink = "https://example.test/property",
            nowEpochMs = 2_000L
        )

        assertEquals(0L, task.id)
        assertEquals("050-1234567", task.phone)
        assertEquals("Michael", task.contactName)
        assertEquals("buyer_property_details", task.selectedTemplateId)
        assertEquals("draft", task.draftText)
        assertEquals(1_000L, task.callEndedAtEpochMs)
        assertEquals(42L, task.callDurationSeconds)
        assertEquals(LeadType.BUYER, task.leadType)
        assertEquals("https://example.test/property", task.propertyLink)
        assertNull(task.reminderAtEpochMs)
        assertEquals(FollowUpSource.POST_CALL_AUTO, task.source)
        assertEquals(FollowUpTaskStatus.PENDING_RESPONSE, task.status)
        assertEquals(2_000L, task.createdAtEpochMs)
        assertEquals(2_000L, task.updatedAtEpochMs)
    }

    @Test
    fun snoozeTaskSetsReminderAndKeepsIdentity() {
        val task = baseTask(id = 11L, status = FollowUpTaskStatus.PENDING_RESPONSE)

        val snoozed = LeadPipeline.snoozeTask(
            task = task,
            reminderAtEpochMs = 5_000L,
            nowEpochMs = 3_000L
        )

        assertEquals(11L, snoozed.id)
        assertEquals(FollowUpTaskStatus.SNOOZED, snoozed.status)
        assertEquals(5_000L, snoozed.reminderAtEpochMs)
        assertEquals(task.createdAtEpochMs, snoozed.createdAtEpochMs)
        assertEquals(3_000L, snoozed.updatedAtEpochMs)
    }

    @Test
    fun mergeCurrentCardStateRefreshesReusedActiveTaskBeforeSnooze() {
        val staleTask = baseTask(id = 21L, status = FollowUpTaskStatus.PENDING_RESPONSE).copy(
            contactName = "Old Name",
            selectedTemplateId = "old_template",
            draftText = "old draft",
            callEndedAtEpochMs = 111L,
            callDurationSeconds = 10L,
            propertyLink = "https://example.test/old",
            updatedAtEpochMs = 2_100L
        )

        val merged = LeadPipeline.mergeCurrentCardState(
            task = staleTask,
            phone = "050-1234567",
            contactName = "Current Name",
            selectedTemplateId = "current_template",
            draftText = "current draft",
            callEndedAtEpochMs = 9_000L,
            callDurationSeconds = 77L,
            leadType = LeadType.SELLER,
            propertyLink = "https://example.test/current",
            source = FollowUpSource.POST_CALL_AUTO,
            nowEpochMs = 9_100L
        )
        val snoozed = LeadPipeline.snoozeTask(
            task = merged,
            reminderAtEpochMs = 10_000L,
            nowEpochMs = 9_200L
        )

        assertEquals(21L, snoozed.id)
        assertEquals(staleTask.createdAtEpochMs, snoozed.createdAtEpochMs)
        assertEquals("Current Name", snoozed.contactName)
        assertEquals("current_template", snoozed.selectedTemplateId)
        assertEquals("current draft", snoozed.draftText)
        assertEquals(9_000L, snoozed.callEndedAtEpochMs)
        assertEquals(77L, snoozed.callDurationSeconds)
        assertEquals(LeadType.SELLER, snoozed.leadType)
        assertEquals("https://example.test/current", snoozed.propertyLink)
        assertEquals(FollowUpTaskStatus.SNOOZED, snoozed.status)
        assertEquals(10_000L, snoozed.reminderAtEpochMs)
        assertEquals(9_200L, snoozed.updatedAtEpochMs)
    }

    @Test
    fun mergeCurrentCardStateRefreshesRestoredTaskBeforeSnoozeAndPreservesMissingLeadType() {
        val staleTask = baseTask(id = 22L, status = FollowUpTaskStatus.OPENED).copy(
            selectedTemplateId = "old_restored_template",
            draftText = "old restored draft",
            leadType = LeadType.BUYER,
            source = FollowUpSource.SNOOZE_REMINDER
        )

        val merged = LeadPipeline.mergeCurrentCardState(
            task = staleTask,
            phone = "050-7654321",
            contactName = "Restored Current",
            selectedTemplateId = "restored_current_template",
            draftText = "restored current draft",
            callEndedAtEpochMs = 12_000L,
            callDurationSeconds = 88L,
            leadType = null,
            propertyLink = "https://example.test/restored-current",
            source = FollowUpSource.POST_CALL_AUTO,
            nowEpochMs = 12_100L
        )
        val snoozed = LeadPipeline.snoozeTask(
            task = merged,
            reminderAtEpochMs = 13_000L,
            nowEpochMs = 12_200L
        )

        assertEquals(22L, snoozed.id)
        assertEquals(staleTask.createdAtEpochMs, snoozed.createdAtEpochMs)
        assertEquals("050-7654321", snoozed.phone)
        assertEquals("Restored Current", snoozed.contactName)
        assertEquals("restored_current_template", snoozed.selectedTemplateId)
        assertEquals("restored current draft", snoozed.draftText)
        assertEquals(12_000L, snoozed.callEndedAtEpochMs)
        assertEquals(88L, snoozed.callDurationSeconds)
        assertEquals(LeadType.BUYER, snoozed.leadType)
        assertEquals("https://example.test/restored-current", snoozed.propertyLink)
        assertEquals(FollowUpSource.POST_CALL_AUTO, snoozed.source)
        assertEquals(FollowUpTaskStatus.SNOOZED, snoozed.status)
    }

    @Test
    fun activeStatusesContainOnlyTasksThatCanReturnToTheCard() {
        assertEquals(
            setOf(
                FollowUpTaskStatus.PENDING_RESPONSE,
                FollowUpTaskStatus.OPENED,
                FollowUpTaskStatus.SNOOZED
            ),
            FollowUpTaskStatus.active
        )
        assertTrue(FollowUpTaskStatus.CLOSED !in FollowUpTaskStatus.active)
        assertTrue(FollowUpTaskStatus.WHATSAPP_OPENED !in FollowUpTaskStatus.active)
        assertTrue(FollowUpTaskStatus.SAVED_AS_LEAD !in FollowUpTaskStatus.active)
    }

    @Test
    fun closeTaskSetsTerminalStateAndClearsFutureReminder() {
        val task = baseTask(
            id = 12L,
            status = FollowUpTaskStatus.SNOOZED,
            reminderAtEpochMs = 9_000L
        )

        val closed = LeadPipeline.closeTask(task = task, nowEpochMs = 4_000L)

        assertEquals(12L, closed.id)
        assertEquals(FollowUpTaskStatus.CLOSED, closed.status)
        assertNull(closed.reminderAtEpochMs)
        assertEquals(4_000L, closed.updatedAtEpochMs)
        assertTrue(FollowUpTaskStatus.terminal.contains(closed.status))
    }

    @Test
    fun markWhatsAppOpenedUpdatesOnlyStatusReminderAndUpdatedTime() {
        val task = baseTask(
            id = 13L,
            status = FollowUpTaskStatus.SNOOZED,
            reminderAtEpochMs = 9_000L
        )

        val opened = LeadPipeline.markWhatsAppOpened(task = task, nowEpochMs = 4_500L)

        assertEquals(task.copy(status = FollowUpTaskStatus.WHATSAPP_OPENED, reminderAtEpochMs = null, updatedAtEpochMs = 4_500L), opened)
    }

    @Test
    fun markSavedAsLeadClearsReminderAndLeadFromTaskPreservesExistingLeadIdentity() {
        val task = baseTask(
            id = 14L,
            status = FollowUpTaskStatus.SNOOZED,
            reminderAtEpochMs = 9_000L
        )
        val existingLead = LeadEntity(
            id = 7L,
            fullName = null,
            phone = "050-1234567",
            type = LeadType.SELLER,
            status = LeadStatus.FOLLOW_UP_NEEDED,
            notes = "note",
            lastCallAtEpochMs = null,
            lastFollowUpAtEpochMs = 500L,
            createdAtEpochMs = 400L,
            updatedAtEpochMs = 600L
        )

        val savedTask = LeadPipeline.markSavedAsLead(task = task, nowEpochMs = 5_000L)
        val lead = LeadPipeline.leadFromTask(task = savedTask, nowEpochMs = 5_000L, existingLead = existingLead)

        assertEquals(FollowUpTaskStatus.SAVED_AS_LEAD, savedTask.status)
        assertNull(savedTask.reminderAtEpochMs)
        assertEquals(7L, lead.id)
        assertEquals("Michael", lead.fullName)
        assertEquals("050-1234567", lead.phone)
        assertEquals(LeadType.BUYER, lead.type)
        assertEquals(LeadStatus.FOLLOW_UP_NEEDED, lead.status)
        assertEquals("note", lead.notes)
        assertEquals(1_000L, lead.lastCallAtEpochMs)
        assertEquals(400L, lead.createdAtEpochMs)
        assertEquals(5_000L, lead.updatedAtEpochMs)
    }

    private fun baseTask(
        id: Long = 1L,
        status: String = FollowUpTaskStatus.PENDING_RESPONSE,
        reminderAtEpochMs: Long? = null
    ): FollowUpTaskEntity =
        FollowUpTaskEntity(
            id = id,
            phone = "050-1234567",
            contactName = "Michael",
            callEndedAtEpochMs = 1_000L,
            callDurationSeconds = 42L,
            source = FollowUpSource.POST_CALL_AUTO,
            selectedTemplateId = "buyer_property_details",
            draftText = "draft",
            leadType = LeadType.BUYER,
            propertyLink = "https://example.test/property",
            reminderAtEpochMs = reminderAtEpochMs,
            status = status,
            createdAtEpochMs = 2_000L,
            updatedAtEpochMs = 2_000L
        )
}
