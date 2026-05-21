package com.followupnadlan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.followupnadlan.data.followup.FollowUpTaskDao
import com.followupnadlan.data.followup.FollowUpTaskEntity
import com.followupnadlan.data.lead.LeadDao
import com.followupnadlan.data.lead.LeadEntity

@Database(
    entities = [
        FollowUpTaskEntity::class,
        LeadEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followUpTaskDao(): FollowUpTaskDao

    abstract fun leadDao(): LeadDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }

        private const val DATABASE_NAME = "followup-nadlan.db"
    }
}
