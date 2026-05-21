package com.followupnadlan.data.lead

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "leads",
    indices = [Index("phone")]
)
data class LeadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String?,
    val phone: String,
    val type: String?,
    val status: String,
    val notes: String?,
    val lastCallAtEpochMs: Long?,
    val lastFollowUpAtEpochMs: Long?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
