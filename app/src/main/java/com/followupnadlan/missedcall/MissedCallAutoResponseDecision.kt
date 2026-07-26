package com.followupnadlan.missedcall

import com.followupnadlan.whatsapp.DialableNumber

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
    val recipientMode: MissedCallRecipientMode = MissedCallRecipientMode.ANY_NUMBER,
    val allowedRecipientNumbers: List<String> = emptyList(),
    val blockSavedContacts: Boolean = false,
    val blockNonContacts: Boolean = false,
    val blockFirstTimeNumbers: Boolean = false,
    val isFirstTimeNumber: Boolean = false,
    val contactsPermissionGranted: Boolean = true,
    val isSavedContact: Boolean = false
)

enum class MissedCallRecipientMode {
    ANY_NUMBER,
    CONTACTS_ONLY,
    NON_CONTACTS_ONLY,
    ONLY_SELECTED
}

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
    SHOW_MANUAL_REPLY_PROMPT,
    SEND_AUTOMATIC_SMS,
    SKIP_DISABLED,
    SKIP_NO_PERMISSION,
    SKIP_DUPLICATE,
    SKIP_NO_NUMBER,
    SKIP_NOT_MISSED_CALL,
    SKIP_NO_TEMPLATE,
    SKIP_EXCLUDED,
    SKIP_BLOCKED_CONTACT,
    SKIP_BLOCKED_NON_CONTACT,
    SKIP_FIRST_TIME_NUMBER,
    SKIP_CONTACT_TYPE_UNVERIFIED,
    SKIP_CONTACTS_ONLY_UNVERIFIED,
    SKIP_NOT_ALLOWED,
    OPEN_MANUAL_FALLBACK
}

object MissedCallAutoResponseDecision {
    /** One message per person per day (MVP-1 §4). Stated to the user, never offered as a control. */
    const val DEFAULT_COOLDOWN_MILLIS: Long = FollowUpConstants.SAME_NUMBER_COOLDOWN_MILLIS

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
        val recipientMode = effectiveRecipientMode(input)
        val contactTypeNeeded = input.blockSavedContacts ||
            input.blockNonContacts ||
            recipientMode == MissedCallRecipientMode.CONTACTS_ONLY ||
            recipientMode == MissedCallRecipientMode.NON_CONTACTS_ONLY
        if (contactTypeNeeded && !input.contactsPermissionGranted) {
            return MissedCallAutoResponseAction.SKIP_CONTACT_TYPE_UNVERIFIED
        }
        if (input.blockSavedContacts && input.isSavedContact) {
            return MissedCallAutoResponseAction.SKIP_BLOCKED_CONTACT
        }
        if (input.blockNonContacts && !input.isSavedContact) {
            return MissedCallAutoResponseAction.SKIP_BLOCKED_NON_CONTACT
        }
        if (input.blockFirstTimeNumbers && input.isFirstTimeNumber) {
            return MissedCallAutoResponseAction.SKIP_FIRST_TIME_NUMBER
        }
        when (recipientMode) {
            MissedCallRecipientMode.CONTACTS_ONLY -> {
                if (!input.isSavedContact) {
                    // Contacts-only: never silently fall back to "any number".
                    return MissedCallAutoResponseAction.SKIP_CONTACTS_ONLY_UNVERIFIED
                }
            }

            MissedCallRecipientMode.NON_CONTACTS_ONLY -> {
                if (input.isSavedContact) {
                    return MissedCallAutoResponseAction.SKIP_CONTACTS_ONLY_UNVERIFIED
                }
            }

            MissedCallRecipientMode.ONLY_SELECTED -> {
                if (!AllowedRecipientDecisionMatcher.isAllowed(input.allowedRecipientNumbers, phone)) {
                    return MissedCallAutoResponseAction.SKIP_NOT_ALLOWED
                }
            }

            MissedCallRecipientMode.ANY_NUMBER -> Unit
        }
        if (!input.templateAvailable) {
            return MissedCallAutoResponseAction.SKIP_NO_TEMPLATE
        }
        if (isDuplicate(input)) {
            return MissedCallAutoResponseAction.SKIP_DUPLICATE
        }
        if (input.whatsappMode == MissedCallWhatsAppMode.PREPARED_MANUAL) {
            return MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT
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

    private fun effectiveRecipientMode(input: MissedCallAutoResponseInput): MissedCallRecipientMode =
        if (input.recipientContactsOnly) {
            MissedCallRecipientMode.CONTACTS_ONLY
        } else {
            input.recipientMode
        }

    /**
     * A number is usable only if a real person can actually receive a message on it. Shared with
     * the ended-suggestion path via [DialableNumber] so both moments filter identically — a
     * toll-free line that gets no missed-call reply must not get a follow-up offer either.
     */
    private fun isUsableNumber(phone: String): Boolean = DialableNumber.isDialable(phone)
}

object AllowedRecipientDecisionMatcher {
    fun isAllowed(allowedNumbers: List<String>, phoneNumber: String): Boolean {
        val target = normalizedDigits(phoneNumber) ?: return false
        return allowedNumbers.any { allowed -> normalizedDigits(allowed) == target }
    }

    private fun normalizedDigits(value: String): String? {
        val compact = value.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
        if (compact.isBlank()) return null

        val withoutPrefix = when {
            compact.startsWith("+") -> compact.drop(1)
            compact.startsWith("00") -> compact.drop(2)
            else -> compact
        }
        if (!withoutPrefix.all { it.isDigit() }) return null
        return when {
            withoutPrefix.startsWith("0") && withoutPrefix.length >= 9 -> "972" + withoutPrefix.drop(1)
            withoutPrefix.startsWith("972") -> withoutPrefix
            withoutPrefix.length in 8..15 -> withoutPrefix
            else -> null
        }
    }
}
