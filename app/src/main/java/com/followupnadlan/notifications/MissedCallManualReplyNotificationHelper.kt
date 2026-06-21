package com.followupnadlan.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.followupnadlan.missedcall.ManualSmsReplyActivity
import com.followupnadlan.R

class MissedCallManualReplyNotificationHelper(private val context: Context) {
    fun showManualSmsReply(phone: String, message: String) {
        if (phone.isBlank() || message.isBlank()) return
        createChannel()

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("תגובה ידנית לשיחה שלא נענתה")
            .setContentText("אפשר לפתוח הודעת SMS מוכנה לשליחה ידנית.")
            .setStyle(Notification.BigTextStyle().bigText("אפשר לפתוח הודעת SMS מוכנה לשליחה ידנית. השליחה תתבצע רק אחרי אישור שלך."))
            .setContentIntent(createSmsIntent(phone, message))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createSmsIntent(phone: String, message: String): PendingIntent {
        val intent = android.content.Intent(context, ManualSmsReplyActivity::class.java).apply {
            putExtra(ManualSmsReplyActivity.EXTRA_PHONE, phone)
            putExtra(ManualSmsReplyActivity.EXTRA_MESSAGE, message)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_MANUAL_SMS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "תגובה ידנית לשיחה שלא נענתה",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "פתיחת הודעת SMS מוכנה כאשר שליחה אוטומטית אינה זמינה"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    private companion object {
        const val CHANNEL_ID = "missed_call_manual_reply"
        const val NOTIFICATION_ID = 8101
        const val REQUEST_CODE_MANUAL_SMS = 8101
    }
}
