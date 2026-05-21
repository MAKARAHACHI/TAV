package com.followupnadlan.snooze

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(
    context: Context,
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun schedule(taskId: Long, triggerAtMillis: Long) {
        val initialDelayMillis = (triggerAtMillis - nowMillisProvider()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(ReminderWorker.KEY_TASK_ID, taskId)
                    .build()
            )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            workNameFor(taskId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(taskId: Long) {
        workManager.cancelUniqueWork(workNameFor(taskId))
    }

    companion object {
        fun workNameFor(taskId: Long): String = "followup-reminder-$taskId"
    }
}
