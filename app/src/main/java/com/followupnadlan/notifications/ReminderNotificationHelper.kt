package com.followupnadlan.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.followupnadlan.MainActivity
import com.followupnadlan.R
import com.followupnadlan.data.followup.FollowUpTaskEntity

class ReminderNotificationHelper(private val context: Context) {
    fun show(task: FollowUpTaskEntity): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        createChannel()

        val title = task.contactName
            ?.takeIf { it.isNotBlank() }
            ?.let { "$it מחכה להמשך טיפול" }
            ?: "יש שיחת נדלן שמחכה להמשך טיפול"

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("פתח כרטיס המשך טיפול")
            .setStyle(Notification.BigTextStyle().bigText(title))
            .setContentIntent(createContentIntent(task.id))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationIdFor(task.id), notification)
        return true
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createContentIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_SNOOZED_TASK
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }

        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_SNOOZED_TASK + taskId.toInt().coerceAtLeast(0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    companion object {
        const val ACTION_OPEN_SNOOZED_TASK = "com.followupnadlan.action.OPEN_SNOOZED_TASK"
        const val EXTRA_TASK_ID = "followup_task_id"

        private const val CHANNEL_ID = "snooze_reminders"
        private const val CHANNEL_NAME = "תזכורות פולואפ"
        private const val CHANNEL_DESCRIPTION = "תזכורות להמשך טיפול בשיחות נדלן"
        private const val REQUEST_CODE_OPEN_SNOOZED_TASK = 9100
        private const val NOTIFICATION_ID_BASE = 91000

        private fun notificationIdFor(taskId: Long): Int =
            NOTIFICATION_ID_BASE + (taskId % 10_000).toInt()
    }
}
