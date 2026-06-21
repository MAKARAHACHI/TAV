package com.followupnadlan.missedcall

import org.junit.Assert.assertEquals
import org.junit.Test

class MissedCallAutoResponseDecisionTest {
    @Test
    fun missedCallWithWhatsAppInstalledAndManualModeOpensPreparedWhatsApp() {
        val action = MissedCallAutoResponseDecision.decide(defaultInput())

        assertEquals(MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP, action)
    }

    @Test
    fun missedCallWithAutoModeAndAccessibilityEnabledAttemptsWhatsAppAutoSend() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                whatsappAutomationEnabled = true,
                whatsappAccessibilityEnabled = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.ATTEMPT_WHATSAPP_AUTO_SEND, action)
    }

    @Test
    fun missedCallWithAutoModeAndAccessibilityDisabledOpensPreparedWhatsAppWithMissingState() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                whatsappAutomationEnabled = true,
                whatsappAccessibilityEnabled = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP_ACCESSIBILITY_MISSING, action)
    }

    @Test
    fun autoModeWithoutUserEnabledAutomationDoesNotAttemptAutoClick() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                whatsappAutomationEnabled = false,
                whatsappAccessibilityEnabled = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP, action)
    }

    @Test
    fun whatsAppUnavailableWithSmsFallbackAndPermissionSendsFallbackSms() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(whatsappInstalled = false, smsFallbackEnabled = true, smsPermissionGranted = true)
        )

        assertEquals(MissedCallAutoResponseAction.SEND_AUTOMATIC_SMS, action)
    }

    @Test
    fun whatsAppUnavailableWithSmsFallbackAndNoPermissionOpensManualSmsFallback() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(whatsappInstalled = false, smsFallbackEnabled = true, smsPermissionGranted = false)
        )

        assertEquals(MissedCallAutoResponseAction.OPEN_MANUAL_FALLBACK, action)
    }

    @Test
    fun smsPermissionMissingWithoutManualFallbackSkipsSafely() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                whatsappInstalled = false,
                smsFallbackEnabled = true,
                smsPermissionGranted = false,
                manualFallbackAvailable = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_NO_PERMISSION, action)
    }

    @Test
    fun whatsAppOpenedSuccessfullyMeansInitialDecisionIsNotSms() {
        val action = MissedCallAutoResponseDecision.decide(defaultInput())

        assertEquals(MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP, action)
    }

    @Test
    fun whatsAppAutoSendSuccessMeansInitialDecisionIsNotSms() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                whatsappAutomationEnabled = true,
                whatsappAccessibilityEnabled = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.ATTEMPT_WHATSAPP_AUTO_SEND, action)
    }

    @Test
    fun whatsAppAutoSendFailureSelectsFallbackSmsPath() {
        val input = defaultInput(whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO)
        val action = MissedCallAutoResponseDecision.decideAfterWhatsAppFailure(input)

        assertEquals(MissedCallAutoResponseAction.SEND_AUTOMATIC_SMS, action)
    }

    @Test
    fun duplicateWithinSixHoursSkipsAllChannels() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(lastAutoReplyAtEpochMs = 1_000, nowEpochMs = 1_000 + 5 * 60 * 60 * 1000L)
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_DUPLICATE, action)
    }

    @Test
    fun sameNumberAfterCooldownAllowsWhatsAppAgain() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(lastAutoReplyAtEpochMs = 1_000, nowEpochMs = 1_000 + 6 * 60 * 60 * 1000L)
        )

        assertEquals(MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP, action)
    }

    @Test
    fun unknownOrPrivateNumberSkipsNoNumber() {
        listOf("", "unknown", "private", "restricted").forEach { phone ->
            val action = MissedCallAutoResponseDecision.decide(defaultInput(phoneNumber = phone))

            assertEquals(MissedCallAutoResponseAction.SKIP_NO_NUMBER, action)
        }
    }

    @Test
    fun outgoingCallDoesNotRespond() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(direction = MissedCallDirection.OUTGOING)
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_NOT_MISSED_CALL, action)
    }

    @Test
    fun answeredIncomingCallDoesNotRespond() {
        val action = MissedCallAutoResponseDecision.decide(defaultInput(wasAnswered = true))

        assertEquals(MissedCallAutoResponseAction.SKIP_NOT_MISSED_CALL, action)
    }

    @Test
    fun smsOnlyModePreservesSprint15AutomaticSmsDecision() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(primaryChannel = MissedCallResponsePrimaryChannel.SMS_ONLY)
        )

        assertEquals(MissedCallAutoResponseAction.SEND_AUTOMATIC_SMS, action)
    }

    @Test
    fun missingTemplateSkipsNoTemplate() {
        val action = MissedCallAutoResponseDecision.decide(defaultInput(templateAvailable = false))

        assertEquals(MissedCallAutoResponseAction.SKIP_NO_TEMPLATE, action)
    }

    @Test
    fun disabledFeatureSkipsDisabled() {
        val action = MissedCallAutoResponseDecision.decide(defaultInput(featureEnabled = false))

        assertEquals(MissedCallAutoResponseAction.SKIP_DISABLED, action)
    }

    private fun defaultInput(
        direction: MissedCallDirection = MissedCallDirection.INCOMING,
        wasAnswered: Boolean = false,
        phoneNumber: String? = "0501234567",
        featureEnabled: Boolean = true,
        primaryChannel: MissedCallResponsePrimaryChannel = MissedCallResponsePrimaryChannel.WHATSAPP_FIRST,
        whatsappInstalled: Boolean = true,
        whatsappBusinessInstalled: Boolean = false,
        whatsappMode: MissedCallWhatsAppMode = MissedCallWhatsAppMode.PREPARED_MANUAL,
        whatsappAutomationEnabled: Boolean = false,
        whatsappAccessibilityEnabled: Boolean = false,
        smsFallbackEnabled: Boolean = true,
        smsPermissionGranted: Boolean = true,
        lastAutoReplyAtEpochMs: Long? = null,
        nowEpochMs: Long = 10_000,
        templateAvailable: Boolean = true,
        manualFallbackAvailable: Boolean = true
    ): MissedCallAutoResponseInput = MissedCallAutoResponseInput(
        direction = direction,
        wasAnswered = wasAnswered,
        phoneNumber = phoneNumber,
        featureEnabled = featureEnabled,
        primaryChannel = primaryChannel,
        whatsappInstalled = whatsappInstalled,
        whatsappBusinessInstalled = whatsappBusinessInstalled,
        whatsappMode = whatsappMode,
        whatsappAutomationEnabled = whatsappAutomationEnabled,
        whatsappAccessibilityEnabled = whatsappAccessibilityEnabled,
        smsFallbackEnabled = smsFallbackEnabled,
        smsPermissionGranted = smsPermissionGranted,
        lastAutoReplyAtEpochMs = lastAutoReplyAtEpochMs,
        nowEpochMs = nowEpochMs,
        templateAvailable = templateAvailable,
        manualFallbackAvailable = manualFallbackAvailable
    )
}
