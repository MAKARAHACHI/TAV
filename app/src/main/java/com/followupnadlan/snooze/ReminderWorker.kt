package com.followupnadlan.snooze

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.followupnadlan.data.AppDatabase
import com.followupnadlan.notifications.ReminderNotificationHelper
import com.followupnadlan.pipeline.FollowUpTaskStatus

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, MISSING_TASK_ID)
        if (taskId == MISSING_TASK_ID) {
            Log.w(TAG, "ReminderWorker fired without task id")
            return Result.success()
        }

        val task = AppDatabase.getInstance(applicationContext)
            .followUpTaskDao()
            .getById(taskId)

        if (task == null) {
            Log.i(TAG, "ReminderWorker fired for missing task")
            return Result.success()
        }

        if (task.status != FollowUpTaskStatus.SNOOZED) {
            Log.i(TAG, "ReminderWorker skipped non-snoozed task")
            return Result.success()
        }

        val shown = ReminderNotificationHelper(applicationContext).show(task)
        Log.i(TAG, if (shown) "ReminderWorker posted reminder" else "ReminderWorker fired without notification permission")
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"

        private const val MISSING_TASK_ID = -1L
        private const val TAG = "ReminderWorker"
    }
}
