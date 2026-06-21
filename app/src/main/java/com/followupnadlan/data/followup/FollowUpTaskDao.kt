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

    @Query("SELECT * FROM follow_up_tasks WHERE status IN (:statuses) ORDER BY updatedAtEpochMs DESC")
    suspend fun listByStatuses(statuses: List<String>): List<FollowUpTaskEntity>

    @Query(
        """
        SELECT * FROM follow_up_tasks
        WHERE phone = :phone AND status IN (:statuses)
        ORDER BY updatedAtEpochMs DESC
        LIMIT 1
        """
    )
    suspend fun getLatestByPhoneAndStatuses(
        phone: String,
        statuses: List<String>
    ): FollowUpTaskEntity?

    @Query(
        """
        SELECT * FROM follow_up_tasks
        WHERE status = :status
        AND reminderAtEpochMs IS NOT NULL
        AND reminderAtEpochMs <= :nowEpochMs
        ORDER BY reminderAtEpochMs ASC
        """
    )
    suspend fun listDueReminders(
        status: String,
        nowEpochMs: Long
    ): List<FollowUpTaskEntity>

    @Delete
    suspend fun delete(task: FollowUpTaskEntity)
}
