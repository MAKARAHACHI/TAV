package com.followupnadlan.accessibility

import android.content.Context
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.followuplog.FollowUpLogStorage
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.missedcall.WhatsAppAutoSendController
import com.followupnadlan.missedcall.WhatsAppPackageResolver
import com.followupnadlan.missedcall.WhatsAppReplyOpenResult
import com.followupnadlan.missedcall.WhatsAppReplySender
import com.followupnadlan.postcall.EndedSendReceiver
import com.followupnadlan.postcall.NoAnswerFollowUpMessage
import com.followupnadlan.whatsapp.PhoneNumberNormalizer

/**
 * Sends the follow-up chosen on the pure approval sheet through the SAME verified WhatsApp path the
 * ended one-tap send and the missed auto-reply already use: open the prepared chat, enqueue the
 * pending send so [com.followupnadlan.missedcall.WhatsAppAccessibilityService] presses send in the
 * visible window, and log a client-facing [FollowUpActionType.WHATSAPP_REPLY_OPENED].
 *
 * Crucially, it stamps the log entry with the moment's [source]. For the "לא ענו" moment that source
 * is [NoAnswerFollowUpMessage.SOURCE] — the exact tag [HistoryFeed] and [HomeTodayCount] look for —
 * so a no-answer send finally counts in "היום" and shows in the activity log. Detection, the
 * decider, thresholds and stores are untouched; this only wires the existing send path to carry the
 * right source per moment.
 */
object FollowUpPromptSender {

    /** Which trust moment is being approved, mapped to the log source the counters key off of. */
    fun sourceFor(mode: FollowUpPromptMode): String = when (mode) {
        FollowUpPromptMode.MISSED_CALL -> MissedCallAutoResponseSettings.SOURCE
        FollowUpPromptMode.CALL_ENDED -> EndedSendReceiver.SOURCE
        FollowUpPromptMode.NO_ANSWER_OUTGOING -> NoAnswerFollowUpMessage.SOURCE
    }

    /**
     * Opens WhatsApp on [message] to [phone] and records the send under [mode]'s source.
     * Returns null on success, or a Hebrew error string to show on the sheet.
     */
    fun send(
        context: Context,
        mode: FollowUpPromptMode,
        phone: String,
        message: String,
        preferredWhatsAppPackage: String
    ): String? {
        val appContext = context.applicationContext
        val normalizedPhone = PhoneNumberNormalizer.normalizeForWhatsApp(phone)
        if (normalizedPhone == null || message.isBlank()) {
            return "חסר מספר תקין או נוסח הודעה."
        }

        val targetPackage = WhatsAppPackageResolver(appContext)
            .resolve(preferredWhatsAppPackage)
            .selectedPackage
        if (targetPackage == null) {
            return "לא הצלחנו לפתוח את WhatsApp. אפשר להעתיק את ההודעה ולשלוח ידנית."
        }

        val now = System.currentTimeMillis()
        val source = sourceFor(mode)
        return when (WhatsAppReplySender(appContext).openPreparedReply(normalizedPhone, message, targetPackage)) {
            WhatsAppReplyOpenResult.OPENED -> {
                val controller = WhatsAppAutoSendController(appContext)
                if (controller.isAccessibilityServiceEnabled()) {
                    // Queue the send; the accessibility service completes it in the opened window
                    // and logs WHATSAPP_AUTO_SENT under this same source.
                    controller.enqueuePendingSend(normalizedPhone, message, targetPackage, now, source)
                }
                // Client-facing marker so the send counts even before the service confirms it.
                log(appContext, FollowUpActionType.WHATSAPP_REPLY_OPENED, normalizedPhone, message, source, now)
                null
            }
            WhatsAppReplyOpenResult.FAILED ->
                "לא הצלחנו לפתוח את WhatsApp. אפשר להעתיק את ההודעה ולשלוח ידנית."
        }
    }

    private fun log(
        context: Context,
        actionType: FollowUpActionType,
        phone: String,
        message: String,
        source: String,
        nowEpochMs: Long
    ) {
        FollowUpLogStore(context).append(
            FollowUpLogEntry(
                actionType = actionType,
                timestampEpochMs = nowEpochMs,
                messagePreview = FollowUpLogStorage.messagePreview(message),
                phone = phone,
                source = source
            )
        )
    }
}
