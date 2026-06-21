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
    val cooldownMillis: Long = MissedCallAutoResponseDecision.DEFAULT_COOLDOWN_MILLIS
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
    OPEN_MANUAL_FALLBACK
}

object MissedCallAutoResponseDecision {
    const val DEFAULT_COOLDOWN_MILLIS: Long = 6 * 60 * 60 * 1000L

    fun decide(input: MissedCallAutoResponseInput): MissedCallAutoResponseAction {
        val phone = input.phoneNumber.orEmpty().trim()

        if (input.direction != MissedCallDirection.INCOMING || input.wasAnswered) {
            return MissedCallAutoResponseAction.SKIP_NOT_MISSED_CALL
        }
        if (phone.isBlank() || isPrivateOrUnknown(phone)) {
            return MissedCallAutoResponseAction.SKIP_NO_NUMBER
        }
        if (!input.featureEnabled) {
            return MissedCallAutoResponseAction.SKIP_DISABLED
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

    private fun isPrivateOrUnknown(phone: String): Boolean {
        val normalized = phone.trim().lowercase()
        return normalized in setOf("unknown", "private", "anonymous", "restricted", "unavailable")
    }
}
