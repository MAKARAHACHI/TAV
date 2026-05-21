package com.followupnadlan.data.followup

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface FollowUpTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: FollowUpTaskEntity): Long

    @Update
    suspend fun update(task: FollowUpTaskEntity)

    @Query("SELECT * FROM follow_up_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FollowUpTaskEntity?

    @Query("SELECT * FROM follow_up_tasks WHERE status = :status ORDER BY updatedAtEpochMs DESC")
    suspend fun listByStatus(status: String): List<FollowUpTaskEntity>

    @Delete
    suspend fun delete(task: FollowUpTaskEntity)
}
