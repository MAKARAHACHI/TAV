package com.followupnadlan.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.followupnadlan.MainActivity
import com.followupnadlan.R

class FollowUpNotificationHelper(private val context: Context) {
    fun showFollowUpNotification(
        phone: String,
        leadName: String,
        templateId: String,
        callDurationSeconds: Long? = null,
        callTimestampMillis: Long? = null,
        callType: String? = null
    ) {
        createChannel()

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_BODY)
            .setStyle(Notification.BigTextStyle().bigText(NOTIFICATION_BODY))
            .setContentIntent(
                createContentIntent(
                    phone = phone,
                    leadName = leadName,
                    templateId = templateId,
                    callDurationSeconds = callDurationSeconds,
                    callTimestampMillis = callTimestampMillis,
                    callType = callType
                )
            )
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createContentIntent(
        phone: String,
        leadName: String,
        templateId: String,
        callDurationSeconds: Long?,
        callTimestampMillis: Long?,
        callType: String?
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_FOLLOW_UP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PHONE, phone)
            putExtra(EXTRA_LEAD_NAME, leadName)
            putExtra(EXTRA_TEMPLATE_ID, templateId)
            callDurationSeconds?.let { putExtra(EXTRA_CALL_DURATION_SECONDS, it) }
            callTimestampMillis?.let { putExtra(EXTRA_CALL_TIMESTAMP_MILLIS, it) }
            callType?.let { putExtra(EXTRA_CALL_TYPE, it) }
        }

        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_FOLLOW_UP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    companion object {
        const val ACTION_OPEN_FOLLOW_UP = "com.followupnadlan.action.OPEN_FOLLOW_UP"
        const val EXTRA_PHONE = "followup_phone"
        const val EXTRA_LEAD_NAME = "followup_lead_name"
        const val EXTRA_TEMPLATE_ID = "followup_template_id"
        const val EXTRA_CALL_DURATION_SECONDS = "followup_call_duration_seconds"
        const val EXTRA_CALL_TIMESTAMP_MILLIS = "followup_call_timestamp_millis"
        const val EXTRA_CALL_TYPE = "followup_call_type"
        const val REQUEST_CODE_OPEN_FOLLOW_UP = 8001

        const val CHANNEL_ID = "follow_up_cards"
        private const val CHANNEL_NAME = "כרטיסי פולואפ"
        private const val CHANNEL_DESCRIPTION = "התראות לפתיחת כרטיס שליחה מהיר לוואטסאפ"
        private const val NOTIFICATION_ID = 8001
        private const val NOTIFICATION_TITLE = "להוציא פולואפ?"
        private const val NOTIFICATION_BODY = "פתח כרטיס שליחה מהיר לוואטסאפ"
    }
}
