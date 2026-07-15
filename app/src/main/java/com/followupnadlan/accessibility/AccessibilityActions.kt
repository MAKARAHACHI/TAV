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
import com.followupnadlan.missedcall.WhatsAppPackageResolver

/**
 * Thin intent helpers shared by the accessibility screens. These only open the user's
 * own messaging apps / SMS composer; they do not change the missed-call decision logic.
 */
object AccessibilityActions {

    /**
     * Opens WhatsApp on the prepared message. Targets the user's preferred package (regular /
     * Business) via [setPackage] so it opens the exact app they chose — same behavior the
     * missed-call engine already uses. Resolves against what is actually installed; if the
     * targeted app can't open, retries without a package (any installed WhatsApp / browser)
     * so the user always gets through. Returns null on success, error text otherwise.
     */
    fun openWhatsApp(context: Context, link: String, preferredPackage: String = ""): String? {
        val target = WhatsAppPackageResolver(context.applicationContext)
            .resolve(preferredPackage)
            .selectedPackage
        val uri = Uri.parse(link)

        if (target != null && tryOpen(context, uri, target)) return null
        // Fallback: no explicit package (whichever WhatsApp is installed, or the browser).
        if (tryOpen(context, uri, packageName = null)) return null
        return "לא הצלחנו לפתוח את WhatsApp. אפשר להעתיק את ההודעה ולשלוח ידנית."
    }

    private fun tryOpen(context: Context, uri: Uri, packageName: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        packageName?.let(intent::setPackage)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
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
