package com.followupnadlan.missedcall

import org.junit.Assert.assertEquals
import org.junit.Test

class MissedCallAutoResponseDecisionTest {
    @Test
    fun missedCallWithWhatsAppInstalledAndManualModeShowsManualPrompt() {
        val action = MissedCallAutoResponseDecision.decide(defaultInput())

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
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
            defaultInput(
                whatsappInstalled = false,
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                smsFallbackEnabled = true,
                smsPermissionGranted = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SEND_AUTOMATIC_SMS, action)
    }

    @Test
    fun whatsAppUnavailableWithSmsFallbackAndNoPermissionOpensManualSmsFallback() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                whatsappInstalled = false,
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                smsFallbackEnabled = true,
                smsPermissionGranted = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.OPEN_MANUAL_FALLBACK, action)
    }

    @Test
    fun smsPermissionMissingWithoutManualFallbackSkipsSafely() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                whatsappInstalled = false,
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                smsFallbackEnabled = true,
                smsPermissionGranted = false,
                manualFallbackAvailable = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_NO_PERMISSION, action)
    }

    @Test
    fun manualModeInitialDecisionIsPromptAndNotSms() {
        val action = MissedCallAutoResponseDecision.decide(defaultInput())

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
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
    fun sameNumberAfterCooldownAllowsManualPromptAgain() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(lastAutoReplyAtEpochMs = 1_000, nowEpochMs = 1_000 + 6 * 60 * 60 * 1000L)
        )

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
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
            defaultInput(
                primaryChannel = MissedCallResponsePrimaryChannel.SMS_ONLY,
                whatsappMode = MissedCallWhatsAppMode.ACCESSIBILITY_AUTO
            )
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

    @Test
    fun excludedNumberSkipsBeforeAnyWhatsAppOrSms() {
        // WhatsApp installed + SMS permission present: the only reason not to send is the exclusion.
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(excluded = true, whatsappInstalled = true, smsPermissionGranted = true)
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_EXCLUDED, action)
    }

    @Test
    fun excludedNumberSkipsEvenInSmsOnlyMode() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(excluded = true, primaryChannel = MissedCallResponsePrimaryChannel.SMS_ONLY)
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_EXCLUDED, action)
    }

    @Test
    fun recipientModeContactsOnlySavedContactAllowsManualPrompt() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientContactsOnly = true,
                contactsPermissionGranted = true,
                isSavedContact = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
    }

    @Test
    fun contactsOnlyUnknownNumberSkips() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientContactsOnly = true,
                contactsPermissionGranted = true,
                isSavedContact = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_CONTACTS_ONLY_UNVERIFIED, action)
    }

    @Test
    fun contactsOnlyMissingPermissionSkipsAndDoesNotFallBackToAnyNumber() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientContactsOnly = true,
                contactsPermissionGranted = false,
                isSavedContact = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_CONTACT_TYPE_UNVERIFIED, action)
    }

    @Test
    fun anyNumberModeAllowsManualPromptForUsableNumber() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientMode = MissedCallRecipientMode.ANY_NUMBER,
                recipientContactsOnly = false,
                contactsPermissionGranted = false,
                isSavedContact = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
    }

    @Test
    fun cooldownStillSkipsAfterRecipientRulesPass() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientContactsOnly = true,
                contactsPermissionGranted = true,
                isSavedContact = true,
                lastAutoReplyAtEpochMs = 1_000,
                nowEpochMs = 1_000 + 5 * 60 * 60 * 1000L
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_DUPLICATE, action)
    }

    @Test
    fun excludedTakesPrecedenceOverContactsOnly() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                excluded = true,
                recipientContactsOnly = true,
                contactsPermissionGranted = true,
                isSavedContact = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_EXCLUDED, action)
    }

    @Test
    fun askBeforeSendModeNeverAutoSendsWhatsApp() {
        // PREPARED_MANUAL (ask-before-send) must show a prompt, never open or send automatically.
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                whatsappMode = MissedCallWhatsAppMode.PREPARED_MANUAL,
                whatsappAutomationEnabled = true,
                whatsappAccessibilityEnabled = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
    }

    @Test
    fun contactsOnlySavedContactAllowsManualPrompt() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientMode = MissedCallRecipientMode.CONTACTS_ONLY,
                contactsPermissionGranted = true,
                isSavedContact = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
    }

    @Test
    fun contactsOnlyNonContactSkips() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientMode = MissedCallRecipientMode.CONTACTS_ONLY,
                contactsPermissionGranted = true,
                isSavedContact = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_CONTACTS_ONLY_UNVERIFIED, action)
    }

    @Test
    fun nonContactsOnlyNonContactAllowsManualPrompt() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientMode = MissedCallRecipientMode.NON_CONTACTS_ONLY,
                contactsPermissionGranted = true,
                isSavedContact = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
    }

    @Test
    fun nonContactsOnlySavedContactSkips() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientMode = MissedCallRecipientMode.NON_CONTACTS_ONLY,
                contactsPermissionGranted = true,
                isSavedContact = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_CONTACTS_ONLY_UNVERIFIED, action)
    }

    @Test
    fun contactTypeRulesFailClosedWithoutContactsPermission() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientMode = MissedCallRecipientMode.NON_CONTACTS_ONLY,
                contactsPermissionGranted = false,
                isSavedContact = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_CONTACT_TYPE_UNVERIFIED, action)
    }

    @Test
    fun onlySelectedAllowedNumberAllowsManualPrompt() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientMode = MissedCallRecipientMode.ONLY_SELECTED,
                allowedRecipientNumbers = listOf("+972501234567")
            )
        )

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
    }

    @Test
    fun onlySelectedUnlistedNumberSkips() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientMode = MissedCallRecipientMode.ONLY_SELECTED,
                allowedRecipientNumbers = listOf("0527654321")
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_NOT_ALLOWED, action)
    }

    @Test
    fun onlySelectedEmptyListSkipsAll() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                recipientMode = MissedCallRecipientMode.ONLY_SELECTED,
                allowedRecipientNumbers = emptyList()
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_NOT_ALLOWED, action)
    }

    @Test
    fun exclusionOverridesAllowedList() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                excluded = true,
                recipientMode = MissedCallRecipientMode.ONLY_SELECTED,
                allowedRecipientNumbers = listOf("0501234567")
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_EXCLUDED, action)
    }

    @Test
    fun blockContactsSkipsSavedContactBeforeRecipientMode() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                blockSavedContacts = true,
                recipientMode = MissedCallRecipientMode.ANY_NUMBER,
                contactsPermissionGranted = true,
                isSavedContact = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_BLOCKED_CONTACT, action)
    }

    @Test
    fun blockNonContactsSkipsNonContactBeforeRecipientMode() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                blockNonContacts = true,
                recipientMode = MissedCallRecipientMode.ANY_NUMBER,
                contactsPermissionGranted = true,
                isSavedContact = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_BLOCKED_NON_CONTACT, action)
    }

    @Test
    fun blockFirstTimeSkipsFirstTimeNumber() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                blockFirstTimeNumbers = true,
                isFirstTimeNumber = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SKIP_FIRST_TIME_NUMBER, action)
    }

    @Test
    fun repeatCallerIsNotBlockedByFirstTimeRule() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                blockFirstTimeNumbers = true,
                isFirstTimeNumber = false
            )
        )

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
    }

    @Test
    fun askBeforeSendModeNeverAutoSendsSmsEvenInSmsOnlyMode() {
        val action = MissedCallAutoResponseDecision.decide(
            defaultInput(
                primaryChannel = MissedCallResponsePrimaryChannel.SMS_ONLY,
                whatsappMode = MissedCallWhatsAppMode.PREPARED_MANUAL,
                smsPermissionGranted = true,
                smsFallbackEnabled = true
            )
        )

        assertEquals(MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT, action)
    }

    @Test
    fun shortOrEmergencyNumberSkipsNoNumber() {
        listOf("112", "911", "100", "123").forEach { phone ->
            val action = MissedCallAutoResponseDecision.decide(defaultInput(phoneNumber = phone))

            assertEquals("expected skip for $phone", MissedCallAutoResponseAction.SKIP_NO_NUMBER, action)
        }
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
        manualFallbackAvailable: Boolean = true,
        excluded: Boolean = false,
        recipientContactsOnly: Boolean = false,
        recipientMode: MissedCallRecipientMode = MissedCallRecipientMode.ANY_NUMBER,
        allowedRecipientNumbers: List<String> = emptyList(),
        blockSavedContacts: Boolean = false,
        blockNonContacts: Boolean = false,
        blockFirstTimeNumbers: Boolean = false,
        isFirstTimeNumber: Boolean = false,
        contactsPermissionGranted: Boolean = true,
        isSavedContact: Boolean = false
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
        manualFallbackAvailable = manualFallbackAvailable,
        excluded = excluded,
        recipientContactsOnly = recipientContactsOnly,
        recipientMode = recipientMode,
        allowedRecipientNumbers = allowedRecipientNumbers,
        blockSavedContacts = blockSavedContacts,
        blockNonContacts = blockNonContacts,
        blockFirstTimeNumbers = blockFirstTimeNumbers,
        isFirstTimeNumber = isFirstTimeNumber,
        contactsPermissionGranted = contactsPermissionGranted,
        isSavedContact = isSavedContact
    )
}
