package com.followupnadlan.data.lead

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LeadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lead: LeadEntity): Long

    @Query("SELECT * FROM leads WHERE phone = :phone ORDER BY updatedAtEpochMs DESC LIMIT 1")
    suspend fun getByPhone(phone: String): LeadEntity?

    @Query("SELECT * FROM leads ORDER BY updatedAtEpochMs DESC")
    suspend fun listLeads(): List<LeadEntity>

    @Update
    suspend fun update(lead: LeadEntity)
}
