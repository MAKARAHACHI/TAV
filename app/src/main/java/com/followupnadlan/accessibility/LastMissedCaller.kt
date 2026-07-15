package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry

enum class LastMissedCallerStatus(val label: String) {
    SENT("נשלחה הודעה"),
    NOT_SENT("לא נשלח"),
    PENDING("ממתין לאישור")
}

data class LastMissedCaller(
    val phone: String,
    val contactName: String? = null,
    val status: LastMissedCallerStatus = LastMissedCallerStatus.PENDING,
    val failureMessage: String? = null
)

object LastMissedCallerLogic {
    fun from(entries: List<FollowUpLogEntry>, contactNameFor: (String) -> String? = { null }): LastMissedCaller? {
        val missed = entries
            .filter { it.actionType == FollowUpActionType.MISSED_CALL_DETECTED && it.phone.isNotBlank() }
            .maxByOrNull { it.timestampEpochMs } ?: return null

        val laterForSamePhone = entries
            .filter { it.phone == missed.phone && it.timestampEpochMs >= missed.timestampEpochMs }
            .sortedByDescending { it.timestampEpochMs }

        val statusEntry = laterForSamePhone.firstOrNull { it.actionType.statusOrNull() != null }
        val status = statusEntry?.actionType?.statusOrNull() ?: LastMissedCallerStatus.PENDING
        val failureMessage = if (statusEntry?.actionType.isFailure()) {
            "לא הצלחנו לשלוח הודעה למתקשר הזה"
        } else {
            null
        }

        return LastMissedCaller(
            phone = missed.phone,
            contactName = contactNameFor(missed.phone),
            status = status,
            failureMessage = failureMessage
        )
    }

    private fun FollowUpActionType.statusOrNull(): LastMissedCallerStatus? = when (this) {
        FollowUpActionType.WHATSAPP_AUTO_SENT,
        FollowUpActionType.AUTO_SMS_SENT,
        FollowUpActionType.FALLBACK_SMS_SENT -> LastMissedCallerStatus.SENT

        FollowUpActionType.MANUAL_REPLY_PENDING,
        FollowUpActionType.MANUAL_WHATSAPP_COMPOSER_OPENED,
        FollowUpActionType.MANUAL_SMS_COMPOSER_OPENED,
        FollowUpActionType.WHATSAPP_REPLY_OPENED,
        FollowUpActionType.FALLBACK_SMS_OPENED -> LastMissedCallerStatus.PENDING

        FollowUpActionType.MANUAL_REPLY_CANCELLED,
        FollowUpActionType.AUTO_SMS_SKIPPED_EXCLUDED,
        FollowUpActionType.AUTO_SMS_SKIPPED_CONTACTS_ONLY,
        FollowUpActionType.AUTO_SMS_SKIPPED_NO_NUMBER,
        FollowUpActionType.AUTO_SMS_SKIPPED_NO_PERMISSION,
        FollowUpActionType.AUTO_SMS_SKIPPED_DISABLED,
        FollowUpActionType.AUTO_SMS_SKIPPED_DUPLICATE,
        FollowUpActionType.WHATSAPP_REPLY_FAILED,
        FollowUpActionType.WHATSAPP_AUTO_FAILED,
        FollowUpActionType.AUTO_SMS_FAILED,
        FollowUpActionType.FALLBACK_SMS_FAILED -> LastMissedCallerStatus.NOT_SENT

        else -> null
    }

    private fun FollowUpActionType?.isFailure(): Boolean = when (this) {
        FollowUpActionType.WHATSAPP_REPLY_FAILED,
        FollowUpActionType.WHATSAPP_AUTO_FAILED,
        FollowUpActionType.AUTO_SMS_FAILED,
        FollowUpActionType.FALLBACK_SMS_FAILED -> true
        else -> false
    }
}
