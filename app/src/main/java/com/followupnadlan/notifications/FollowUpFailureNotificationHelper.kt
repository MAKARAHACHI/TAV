package com.followupnadlan.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.followupnadlan.MainActivity
import com.followupnadlan.R
import com.followupnadlan.missedcall.ManualSmsReplyActivity

/**
 * The two notifications that exist because silence about a fault is a lie (§2).
 *
 * "שקט = עובד" holds only while things work. The user may not open this app for weeks, so the ⚠️
 * line on Home cannot carry a fault on its own — if a message did not go out, or the service is
 * not running at all, that has to reach them where they are.
 *
 * Both are worded as the consequence for the client, never as the component that failed.
 */
class FollowUpFailureNotificationHelper(private val context: Context) {

    /**
     * A message that was not delivered. Applies to the missed-call path too: believing a client
     * was answered when they were not is precisely the false belief §2 exists to prevent.
     */
    fun showSendFailed(phone: String, message: String) {
        if (phone.isBlank()) return
        createChannel()

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ההודעה לא נשלחה")
            .setContentText("ההודעה ל-$phone לא נשלחה — שלח ידנית")
            .setStyle(Notification.BigTextStyle().bigText("ההודעה ל-$phone לא נשלחה — שלח ידנית"))
            .setContentIntent(manualSendIntent(phone, message))
            .addAction(0, "שלח ידנית", manualSendIntent(phone, message))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_FAILED, notification)
    }

    /** The service is not running, so nobody is being answered at all. */
    fun showServiceStopped() {
        createChannel()

        val body = "FollowUp הופסק — לקוחות לא מקבלים ממך מענה"
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FollowUp הופסק")
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_STOPPED, notification)
    }

    fun cancelServiceStopped() = notificationManager.cancel(NOTIFICATION_ID_STOPPED)

    private fun manualSendIntent(phone: String, message: String): PendingIntent {
        val intent = Intent(context, ManualSmsReplyActivity::class.java).apply {
            putExtra(ManualSmsReplyActivity.EXTRA_PHONE, phone)
            putExtra(ManualSmsReplyActivity.EXTRA_MESSAGE, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_MANUAL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // A fault is worth a normal-importance notification: unlike a suggestion, ignoring this
        // one has a real cost for the user.
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(channel)
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    companion object {
        const val CHANNEL_ID = "followup_faults"
        const val NOTIFICATION_ID_FAILED = 8301
        const val NOTIFICATION_ID_STOPPED = 8302
        private const val REQUEST_CODE_MANUAL = 8301
        private const val REQUEST_CODE_OPEN = 8302
        private const val CHANNEL_NAME = "תקלות ושליחות שנכשלו"
        private const val CHANNEL_DESCRIPTION = "הודעה שלא נשלחה, או שירות שהופסק"
    }
}
