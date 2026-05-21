package com.followupnadlan.data.followup

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "follow_up_tasks",
    indices = [
        Index("status"),
        Index("reminderAtEpochMs")
    ]
)
data class FollowUpTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phone: String?,
    val contactName: String?,
    val callEndedAtEpochMs: Long?,
    val callDurationSeconds: Long?,
    val source: String,
    val selectedTemplateId: String?,
    val draftText: String?,
    val leadType: String?,
    val propertyLink: String?,
    val reminderAtEpochMs: Long?,
    val status: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
