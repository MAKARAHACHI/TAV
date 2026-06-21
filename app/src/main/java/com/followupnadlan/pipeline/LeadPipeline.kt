package com.followupnadlan.pipeline

import com.followupnadlan.data.followup.FollowUpTaskEntity
import com.followupnadlan.data.lead.LeadEntity

object FollowUpSource {
    const val POST_CALL_AUTO = "POST_CALL_AUTO"
    const val MANUAL_COMPOSER = "MANUAL_COMPOSER"
    const val SNOOZE_REMINDER = "SNOOZE_REMINDER"
    const val MISSED_CALL = "MISSED_CALL"
}

object FollowUpTaskStatus {
    const val PENDING_RESPONSE = "PENDING_RESPONSE"
    const val OPENED = "OPENED"
    const val SNOOZED = "SNOOZED"
    const val WHATSAPP_OPENED = "WHATSAPP_OPENED"
    const val SAVED_AS_LEAD = "SAVED_AS_LEAD"
    const val CLOSED = "CLOSED"
    const val DISMISSED = "DISMISSED"

    val active: Set<String> = setOf(PENDING_RESPONSE, OPENED, SNOOZED)
    val terminal: Set<String> = setOf(WHATSAPP_OPENED, SAVED_AS_LEAD, CLOSED, DISMISSED)
}

object LeadStatus {
    const val NEW = "NEW"
    const val FOLLOW_UP_NEEDED = "FOLLOW_UP_NEEDED"
    const val MEETING_SET = "MEETING_SET"
    const val SENT_DETAILS = "SENT_DETAILS"
    const val NOT_RELEVANT = "NOT_RELEVANT"
    const val CLOSED = "CLOSED"
}

object LeadType {
    const val UNKNOWN = "UNKNOWN"
    const val BUYER = "BUYER"
    const val SELLER = "SELLER"
}

object LeadPipeline {
    fun createPendingPostCallTask(
        phone: String?,
        contactName: String?,
        selectedTemplateId: String?,
        draftText: String?,
        callEndedAtEpochMs: Long?,
        callDurationSeconds: Long?,
        leadType: String?,
        propertyLink: String?,
        nowEpochMs: Long,
        source: String = FollowUpSource.POST_CALL_AUTO
    ): FollowUpTaskEntity =
        FollowUpTaskEntity(
            phone = phone,
            contactName = contactName,
            callEndedAtEpochMs = callEndedAtEpochMs,
            callDurationSeconds = callDurationSeconds,
            source = source,
            selectedTemplateId = selectedTemplateId,
            draftText = draftText,
            leadType = leadType,
            propertyLink = propertyLink,
            reminderAtEpochMs = null,
            status = FollowUpTaskStatus.PENDING_RESPONSE,
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs
        )

    fun snoozeTask(
        task: FollowUpTaskEntity,
        reminderAtEpochMs: Long,
        nowEpochMs: Long
    ): FollowUpTaskEntity =
        task.copy(
            reminderAtEpochMs = reminderAtEpochMs,
            status = FollowUpTaskStatus.SNOOZED,
            updatedAtEpochMs = nowEpochMs
        )

    fun mergeCurrentCardState(
        task: FollowUpTaskEntity,
        phone: String?,
        contactName: String?,
        selectedTemplateId: String?,
        draftText: String?,
        callEndedAtEpochMs: Long?,
        callDurationSeconds: Long?,
        leadType: String?,
        propertyLink: String?,
        source: String?,
        nowEpochMs: Long
    ): FollowUpTaskEntity =
        task.copy(
            phone = phone,
            contactName = contactName,
            callEndedAtEpochMs = callEndedAtEpochMs,
            callDurationSeconds = callDurationSeconds,
            source = source ?: task.source,
            selectedTemplateId = selectedTemplateId,
            draftText = draftText,
            leadType = leadType ?: task.leadType,
            propertyLink = propertyLink,
            updatedAtEpochMs = nowEpochMs
        )

    fun markWhatsAppOpened(
        task: FollowUpTaskEntity,
        nowEpochMs: Long
    ): FollowUpTaskEntity =
        task.copy(
            reminderAtEpochMs = null,
            status = FollowUpTaskStatus.WHATSAPP_OPENED,
            updatedAtEpochMs = nowEpochMs
        )

    fun markSavedAsLead(
        task: FollowUpTaskEntity,
        nowEpochMs: Long
    ): FollowUpTaskEntity =
        task.copy(
            reminderAtEpochMs = null,
            status = FollowUpTaskStatus.SAVED_AS_LEAD,
            updatedAtEpochMs = nowEpochMs
        )

    fun closeTask(
        task: FollowUpTaskEntity,
        nowEpochMs: Long
    ): FollowUpTaskEntity =
        task.copy(
            reminderAtEpochMs = null,
            status = FollowUpTaskStatus.CLOSED,
            updatedAtEpochMs = nowEpochMs
        )

    fun leadFromTask(
        task: FollowUpTaskEntity,
        nowEpochMs: Long,
        existingLead: LeadEntity? = null
    ): LeadEntity =
        LeadEntity(
            id = existingLead?.id ?: 0L,
            fullName = task.contactName?.takeIf { it.isNotBlank() } ?: existingLead?.fullName,
            phone = task.phone.orEmpty(),
            type = task.leadType ?: existingLead?.type ?: LeadType.UNKNOWN,
            status = existingLead?.status ?: LeadStatus.NEW,
            notes = existingLead?.notes,
            lastCallAtEpochMs = task.callEndedAtEpochMs ?: existingLead?.lastCallAtEpochMs,
            lastFollowUpAtEpochMs = nowEpochMs,
            createdAtEpochMs = existingLead?.createdAtEpochMs ?: nowEpochMs,
            updatedAtEpochMs = nowEpochMs
        )
}
