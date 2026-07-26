package com.followupnadlan.postcall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.followuplog.FollowUpLogStorage
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.missedcall.WhatsAppAutoSendController
import com.followupnadlan.missedcall.WhatsAppPackageResolver
import com.followupnadlan.missedcall.WhatsAppReplyOpenResult
import com.followupnadlan.missedcall.WhatsAppReplySender
import com.followupnadlan.notifications.EndedSuggestionNotificationHelper
import com.followupnadlan.notifications.FollowUpFailureNotificationHelper

/**
 * Handles "שלח" from the follow-up suggestion: one tap, and the message goes out.
 *
 * How the send actually works, stated plainly because it shapes what we may promise: Android has
 * no API for sending a WhatsApp message without its UI. The only mechanism available is to open the
 * chat with the text prefilled and let [com.followupnadlan.missedcall.WhatsAppAccessibilityService]
 * press send in the visible window. So WhatsApp *does* appear briefly — it is one tap for the user,
 * not an invisible background send, and the UI must not claim otherwise (§2).
 *
 * When the accessibility service is off we cannot press send, so we degrade honestly: the chat
 * opens with the text ready and the user presses send themselves. Either way the user is told what
 * happened — silence about a message that never went out is the failure §2 forbids.
 */
class EndedSendReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SEND) return

        val phone = intent.getStringExtra(EXTRA_PHONE).orEmpty()
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        if (phone.isBlank() || message.isBlank()) return

        val appContext = context.applicationContext
        EndedSuggestionNotificationHelper(appContext).cancel()

        val now = System.currentTimeMillis()
        val settings = MissedCallAutoResponseSettings(appContext)
        val controller = WhatsAppAutoSendController(appContext)
        val packages = WhatsAppPackageResolver(appContext).resolve(settings.preferredWhatsAppPackage)
        val targetPackage = packages.selectedPackage

        if (targetPackage == null) {
            // The chosen channel is not installed. Say so rather than substituting another one.
            log(appContext, FollowUpActionType.WHATSAPP_AUTO_FAILED, phone, message, now)
            FollowUpFailureNotificationHelper(appContext).showSendFailed(phone, message)
            return
        }

        when (WhatsAppReplySender(appContext).openPreparedReply(phone, message, targetPackage)) {
            WhatsAppReplyOpenResult.OPENED -> {
                if (controller.isAccessibilityServiceEnabled()) {
                    // Queue the send; the accessibility service completes it in the opened window.
                    controller.enqueuePendingSend(phone, message, targetPackage, now, SOURCE)
                    log(appContext, FollowUpActionType.WHATSAPP_AUTO_SEND_ATTEMPTED, phone, message, now)
                } else {
                    // Honest degradation: the chat is ready, the user presses send.
                    log(appContext, FollowUpActionType.WHATSAPP_REPLY_PREPARED, phone, message, now)
                }
                EndedSuggestionStore(appContext).markSuggested(phone, now)
            }
            WhatsAppReplyOpenResult.FAILED -> {
                log(appContext, FollowUpActionType.WHATSAPP_AUTO_FAILED, phone, message, now)
                FollowUpFailureNotificationHelper(appContext).showSendFailed(phone, message)
            }
        }
    }

    private fun log(
        context: Context,
        actionType: FollowUpActionType,
        phone: String,
        message: String,
        nowEpochMs: Long
    ) {
        FollowUpLogStore(context).append(
            FollowUpLogEntry(
                actionType = actionType,
                timestampEpochMs = nowEpochMs,
                messagePreview = FollowUpLogStorage.messagePreview(message),
                phone = phone,
                source = SOURCE
            )
        )
    }

    companion object {
        const val ACTION_SEND = "com.followupnadlan.action.ENDED_SEND"
        const val EXTRA_PHONE = "ended_phone"
        const val EXTRA_MESSAGE = "ended_message"
        const val SOURCE = "ended_follow_up"
    }
}
