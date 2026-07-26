package com.followupnadlan.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.followupnadlan.R
import com.followupnadlan.postcall.EndedSendReceiver

/**
 * The follow-up suggestion shown after a real conversation.
 *
 * Deliberately silent: no sound, no vibration, collapsed by default. Perfect filtering of "is this
 * a lead?" is impossible, so the design makes a wrong guess *cheap to ignore* rather than trying to
 * be clever — a notification that never interrupts costs nothing to dismiss. This is also what
 * makes a separate "quiet mode" unnecessary.
 *
 * It shows the number (a name only when the contact is saved) and the full message including the
 * signature line, so the user approves what the client will actually receive — not a description
 * of it.
 */
class EndedSuggestionNotificationHelper(private val context: Context) {

    /**
     * [wasAnswered] only changes the wording. Calling "השיחה הסתיימה" after a call that never
     * connected would describe a conversation that did not happen, and the user decides whether to
     * send by reading this line.
     */
    fun showSuggestion(phone: String, displayName: String, message: String, wasAnswered: Boolean = true) {
        if (phone.isBlank() || message.isBlank()) return
        createChannel()

        // The number is the identity; a name appears only when it is genuinely saved.
        val who = displayName.ifBlank { phone }.trim()
        val title = if (wasAnswered) "$who · השיחה הסתיימה" else "$who · שיחה שלא נענתה"

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setContentIntent(sendIntent(phone, message))
            .addAction(0, "שלח", sendIntent(phone, message))
            .addAction(0, "שמור איש קשר", saveContactIntent(phone))
            .setAutoCancel(true)
            // Silence comes from the channel (IMPORTANCE_LOW, no sound, no vibration), which is
            // what actually governs this on every supported version.
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel() = notificationManager.cancel(NOTIFICATION_ID)

    /** "שלח" — hands off to the background sender; the user never leaves what they were doing. */
    private fun sendIntent(phone: String, message: String): PendingIntent {
        val intent = Intent(context, EndedSendReceiver::class.java).apply {
            action = EndedSendReceiver.ACTION_SEND
            putExtra(EndedSendReceiver.EXTRA_PHONE, phone)
            putExtra(EndedSendReceiver.EXTRA_MESSAGE, message)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SEND,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * "שמור איש קשר" — opens the device contacts with the number prefilled. This is the whole
     * learning mechanism: once a number is saved it becomes a contact, and the default scope
     * ("only people not saved with me") stops suggesting it forever. The phone book is the
     * database; there is no list of our own to maintain.
     */
    private fun saveContactIntent(phone: String): PendingIntent {
        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
            type = android.provider.ContactsContract.Contacts.CONTENT_ITEM_TYPE
            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, phone)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_SAVE_CONTACT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // IMPORTANCE_LOW: appears in the shade without sound or heads-up intrusion.
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
            description = CHANNEL_DESCRIPTION
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    companion object {
        const val CHANNEL_ID = "ended_follow_up_suggestion"
        const val NOTIFICATION_ID = 8201
        private const val REQUEST_CODE_SEND = 8201
        private const val REQUEST_CODE_SAVE_CONTACT = 8202
        private const val CHANNEL_NAME = "הצעת המשך אחרי שיחה"
        private const val CHANNEL_DESCRIPTION = "הצעה שקטה לשלוח הודעת המשך אחרי שיחה"
    }
}
