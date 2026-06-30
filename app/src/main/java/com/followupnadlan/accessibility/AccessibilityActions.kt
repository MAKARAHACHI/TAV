package com.followupnadlan.accessibility

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.followuplog.FollowUpLogStorage
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings

/**
 * Thin intent helpers shared by the accessibility screens. These only open the user's
 * own messaging apps / SMS composer; they do not change the missed-call decision logic.
 */
object AccessibilityActions {

    /** Opens WhatsApp (or a browser) on the prepared message. Returns null on success, error text otherwise. */
    fun openWhatsApp(context: Context, link: String): String? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            null
        } catch (_: ActivityNotFoundException) {
            "לא הצלחנו לפתוח את WhatsApp. אפשר להעתיק את ההודעה ולשלוח ידנית."
        }
    }

    /** Opens the SMS composer prefilled with [message]. Returns null on success, error text otherwise. */
    fun openSmsComposer(context: Context, phone: String, message: String): String? {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "לא נמצאה אפליקציית SMS לשליחה."
        }
    }

    fun logEntry(context: Context, actionType: FollowUpActionType, message: String, phone: String) {
        FollowUpLogStore(context.applicationContext).append(
            FollowUpLogEntry(
                actionType = actionType,
                timestampEpochMs = System.currentTimeMillis(),
                messagePreview = FollowUpLogStorage.messagePreview(message),
                phone = phone,
                source = MissedCallAutoResponseSettings.SOURCE
            )
        )
    }
}
