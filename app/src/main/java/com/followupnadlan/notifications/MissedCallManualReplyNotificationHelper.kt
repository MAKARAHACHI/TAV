package com.followupnadlan.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.followupnadlan.MainActivity
import com.followupnadlan.missedcall.ManualSmsReplyActivity
import com.followupnadlan.R
import com.followupnadlan.notifications.FollowUpNotificationHelper.Companion.ACTION_OPEN_FOLLOW_UP
import com.followupnadlan.notifications.FollowUpNotificationHelper.Companion.EXTRA_MANUAL_ACTION
import com.followupnadlan.notifications.FollowUpNotificationHelper.Companion.EXTRA_MESSAGE
import com.followupnadlan.notifications.FollowUpNotificationHelper.Companion.EXTRA_PHONE
import com.followupnadlan.notifications.FollowUpNotificationHelper.Companion.MANUAL_ACTION_CANCEL

class MissedCallManualReplyNotificationHelper(private val context: Context) {
    fun showManualReplyPrompt(phone: String, message: String) {
        if (phone.isBlank() || message.isBlank()) return
        createChannel()

        val promptIntent = createPromptIntent(phone, message, REQUEST_CODE_PROMPT)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ממתין לאישור שלך")
            .setContentText("שלח ב־WhatsApp / שלח ב־SMS / בטל")
            .setStyle(Notification.BigTextStyle().bigText("זוהתה שיחה שלא נענתה. אפשר לפתוח הודעה מוכנה ב־WhatsApp, לפתוח SMS ידני או לבטל. שום דבר לא נשלח בלי אישור שלך."))
            .setContentIntent(promptIntent)
            .addAction(0, "שלח ב־WhatsApp", promptIntent)
            .addAction(0, "שלח ב־SMS", promptIntent)
            .addAction(0, "בטל", createPromptIntent(phone, message, REQUEST_CODE_CANCEL, MANUAL_ACTION_CANCEL))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_PROMPT, notification)
    }

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

    private fun createPromptIntent(
        phone: String,
        message: String,
        requestCode: Int,
        manualAction: String? = null
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_FOLLOW_UP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PHONE, phone)
            putExtra(EXTRA_MESSAGE, message)
            manualAction?.let { putExtra(EXTRA_MANUAL_ACTION, it) }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        const val NOTIFICATION_ID_PROMPT = 8102
        const val REQUEST_CODE_MANUAL_SMS = 8101
        const val REQUEST_CODE_PROMPT = 8102
        const val REQUEST_CODE_CANCEL = 8103
    }
}
