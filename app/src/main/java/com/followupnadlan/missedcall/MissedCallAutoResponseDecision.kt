package com.followupnadlan.missedcall

enum class MissedCallDirection {
    INCOMING,
    OUTGOING,
    UNKNOWN
}

data class MissedCallAutoResponseInput(
    val direction: MissedCallDirection,
    val wasAnswered: Boolean,
    val phoneNumber: String?,
    val featureEnabled: Boolean,
    val primaryChannel: MissedCallResponsePrimaryChannel = MissedCallResponsePrimaryChannel.SMS_ONLY,
    val whatsappInstalled: Boolean = false,
    val whatsappBusinessInstalled: Boolean = false,
    val whatsappMode: MissedCallWhatsAppMode = MissedCallWhatsAppMode.PREPARED_MANUAL,
    val whatsappAutomationEnabled: Boolean = false,
    val whatsappAccessibilityEnabled: Boolean = false,
    val smsFallbackEnabled: Boolean = true,
    val smsPermissionGranted: Boolean,
    val lastAutoReplyAtEpochMs: Long?,
    val nowEpochMs: Long,
    val templateAvailable: Boolean,
    val manualFallbackAvailable: Boolean,
    val cooldownMillis: Long = MissedCallAutoResponseDecision.DEFAULT_COOLDOWN_MILLIS,
    // Recipient rules. Defaults are permissive ("any number", not excluded) so that
    // callers which do not set them keep the pre-recipient-rules behavior.
    val excluded: Boolean = false,
    val recipientContactsOnly: Boolean = false,
    val contactsPermissionGranted: Boolean = true,
    val isSavedContact: Boolean = false
)

enum class MissedCallResponsePrimaryChannel {
    WHATSAPP_FIRST,
    SMS_ONLY
}

enum class MissedCallWhatsAppMode {
    PREPARED_MANUAL,
    ACCESSIBILITY_AUTO
}

enum class MissedCallAutoResponseAction {
    ATTEMPT_WHATSAPP_AUTO_SEND,
    OPEN_PREPARED_WHATSAPP,
    OPEN_PREPARED_WHATSAPP_ACCESSIBILITY_MISSING,
    SEND_AUTOMATIC_SMS,
    SKIP_DISABLED,
    SKIP_NO_PERMISSION,
    SKIP_DUPLICATE,
    SKIP_NO_NUMBER,
    SKIP_NOT_MISSED_CALL,
    SKIP_NO_TEMPLATE,
    SKIP_EXCLUDED,
    SKIP_CONTACTS_ONLY_UNVERIFIED,
    OPEN_MANUAL_FALLBACK
}

object MissedCallAutoResponseDecision {
    const val DEFAULT_COOLDOWN_MILLIS: Long = 6 * 60 * 60 * 1000L

    fun decide(input: MissedCallAutoResponseInput): MissedCallAutoResponseAction {
        val phone = input.phoneNumber.orEmpty().trim()

        // Decision order is fixed and must hold before any WhatsApp/SMS send:
        // missed call → bridge enabled → usable number → excluded → recipient mode
        // → template → cooldown → channel routing.
        if (input.direction != MissedCallDirection.INCOMING || input.wasAnswered) {
            return MissedCallAutoResponseAction.SKIP_NOT_MISSED_CALL
        }
        if (!input.featureEnabled) {
            return MissedCallAutoResponseAction.SKIP_DISABLED
        }
        if (phone.isBlank() || !isUsableNumber(phone)) {
            return MissedCallAutoResponseAction.SKIP_NO_NUMBER
        }
        if (input.excluded) {
            return MissedCallAutoResponseAction.SKIP_EXCLUDED
        }
        if (input.recipientContactsOnly && !(input.contactsPermissionGranted && input.isSavedContact)) {
            // Contacts-only: never silently fall back to "any number".
            return MissedCallAutoResponseAction.SKIP_CONTACTS_ONLY_UNVERIFIED
        }
        if (!input.templateAvailable) {
            return MissedCallAutoResponseAction.SKIP_NO_TEMPLATE
        }
        if (isDuplicate(input)) {
            return MissedCallAutoResponseAction.SKIP_DUPLICATE
        }

        if (input.primaryChannel == MissedCallResponsePrimaryChannel.WHATSAPP_FIRST) {
            if (input.whatsappInstalled || input.whatsappBusinessInstalled) {
                return when {
                    input.whatsappMode == MissedCallWhatsAppMode.ACCESSIBILITY_AUTO &&
                        input.whatsappAutomationEnabled &&
                        input.whatsappAccessibilityEnabled ->
                        MissedCallAutoResponseAction.ATTEMPT_WHATSAPP_AUTO_SEND
                    input.whatsappMode == MissedCallWhatsAppMode.ACCESSIBILITY_AUTO &&
                        input.whatsappAutomationEnabled ->
                        MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP_ACCESSIBILITY_MISSING
                    else ->
                        MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP
                }
            }
            return smsFallbackDecision(input, smsAllowedByMode = input.smsFallbackEnabled)
        }

        return smsFallbackDecision(input, smsAllowedByMode = true)
    }

    fun decideAfterWhatsAppFailure(input: MissedCallAutoResponseInput): MissedCallAutoResponseAction =
        smsFallbackDecision(input, smsAllowedByMode = input.smsFallbackEnabled)

    private fun smsFallbackDecision(
        input: MissedCallAutoResponseInput,
        smsAllowedByMode: Boolean
    ): MissedCallAutoResponseAction {
        if (!smsAllowedByMode) {
            return if (input.manualFallbackAvailable) {
                MissedCallAutoResponseAction.OPEN_MANUAL_FALLBACK
            } else {
                MissedCallAutoResponseAction.SKIP_NO_PERMISSION
            }
        }
        if (!input.smsPermissionGranted) {
            return if (input.manualFallbackAvailable) {
                MissedCallAutoResponseAction.OPEN_MANUAL_FALLBACK
            } else {
                MissedCallAutoResponseAction.SKIP_NO_PERMISSION
            }
        }

        return MissedCallAutoResponseAction.SEND_AUTOMATIC_SMS
    }

    private fun isDuplicate(input: MissedCallAutoResponseInput): Boolean {
        val last = input.lastAutoReplyAtEpochMs ?: return false
        if (last <= 0L) return false
        val elapsed = input.nowEpochMs - last
        return elapsed in 0 until input.cooldownMillis
    }

    /**
     * A number is usable only if it is a real, dialable subscriber number: not a
     * private/unknown/withheld label, not an emergency/service short code, and long
     * enough to be a genuine phone number.
     */
    private fun isUsableNumber(phone: String): Boolean {
        val trimmed = phone.trim()
        if (trimmed.lowercase() in PRIVATE_OR_UNKNOWN_LABELS) return false

        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return false
        if (digits.length < MIN_SUBSCRIBER_DIGITS) return false
        if (digits in EMERGENCY_OR_SERVICE_NUMBERS) return false

        return true
    }

    private val PRIVATE_OR_UNKNOWN_LABELS =
        setOf("unknown", "private", "anonymous", "restricted", "unavailable", "withheld", "blocked")

    private val EMERGENCY_OR_SERVICE_NUMBERS =
        setOf("100", "101", "102", "110", "112", "911", "999", "000")

    private const val MIN_SUBSCRIBER_DIGITS = 7
}
