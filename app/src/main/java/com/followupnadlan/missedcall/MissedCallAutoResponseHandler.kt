package com.followupnadlan.missedcall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.followuplog.FollowUpLogStorage
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.accessibility.ExclusionsStore
import com.followupnadlan.accessibility.AllowedRecipientsStore
import com.followupnadlan.accessibility.BlockedRecipientGroup
import com.followupnadlan.accessibility.EffectiveRecipientScope
import com.followupnadlan.accessibility.GeneralRecipientScopeSettings
import com.followupnadlan.accessibility.LocalMomentResolver
import com.followupnadlan.accessibility.RecipientScope
import com.followupnadlan.accessibility.RecipientScopeSettings
import com.followupnadlan.accessibility.WorkingHoursDecider
import com.followupnadlan.accessibility.WorkingHoursSettings
import com.followupnadlan.notifications.FollowUpFailureNotificationHelper
import com.followupnadlan.notifications.MissedCallManualReplyNotificationHelper
import com.followupnadlan.postcall.CallLogReader
import com.followupnadlan.postcall.FollowUpCallType
import com.followupnadlan.postcall.LatestCallLogEntry
import com.followupnadlan.profile.ContactCard
import com.followupnadlan.profile.MyDetailsStore
import com.followupnadlan.profile.SignatureLine
import com.followupnadlan.templates.MessageComposition
import com.followupnadlan.templates.MessageTemplate
import com.followupnadlan.templates.TemplateRole
import com.followupnadlan.templates.TemplateRoleSelector
import com.followupnadlan.templates.TemplateStore
import com.followupnadlan.templates.TemplateTagRenderer
import com.followupnadlan.templates.TemplateTagValues
import com.followupnadlan.whatsapp.PhoneNumberNormalizer

class MissedCallAutoResponseHandler(private val context: Context) {
    private val settings = MissedCallAutoResponseSettings(context)
    private val cooldownStore = MissedCallCooldownStore(context)
    private val logStore = FollowUpLogStore(context)
    private val templateStore = TemplateStore(context)
    private val profileStore = MyDetailsStore(context)
    private val missedCardSettings = com.followupnadlan.accessibility.MissedCardSettings(context)
    private val smsSender = SmsSender(context)
    private val manualReplyNotificationHelper = MissedCallManualReplyNotificationHelper(context)
    private val whatsAppPackageResolver = WhatsAppPackageResolver(context)
    private val whatsAppReplySender = WhatsAppReplySender(context)
    private val whatsAppAutoSendController = WhatsAppAutoSendController(context)
    private val recipientScopeSettings = RecipientScopeSettings(context)
    private val generalRecipientScopeSettings = GeneralRecipientScopeSettings(context)
    private val generalCooldownSettings = com.followupnadlan.accessibility.FollowUpCooldownSettings(context)
    private val missedCooldownOverride = com.followupnadlan.accessibility.MomentCooldownOverrideSettings.forMissed(context)
    private val exclusionsStore = ExclusionsStore(context)
    private val allowedRecipientsStore = AllowedRecipientsStore(context)
    private val contactVerifier = ContactVerifier(context)
    private val workingHoursSettings = WorkingHoursSettings(context)

    fun handleMissedIncomingCandidate(): MissedCallAutoResponseAction {
        val latestCall = CallLogReader(context).readLatestCall()
        return handleMissedIncomingCandidate(
            MissedCallCandidate(
                phoneNumber = latestCall?.phoneNumber.orEmpty(),
                direction = directionFor(latestCall),
                wasAnswered = latestCall?.let(::wasAnswered) ?: false
            )
        )
    }

    fun handleConfirmedMissedIncomingCandidate(phoneNumber: String?): MissedCallAutoResponseAction =
        if (phoneNumber.isNullOrBlank()) {
            handleMissedIncomingCandidate()
        } else {
            handleMissedIncomingCandidate(
                MissedCallCandidate(
                    phoneNumber = phoneNumber,
                    direction = MissedCallDirection.INCOMING,
                    wasAnswered = false
                )
            )
        }

    fun handleMissedIncomingCandidate(candidate: MissedCallCandidate): MissedCallAutoResponseAction {
        val now = System.currentTimeMillis()
        val evaluation = evaluateCandidate(candidate, now)
        val normalizedPhone = evaluation.normalizedPhone
        val phone = evaluation.rawPhone
        val template = evaluation.template
        val message = evaluation.message

        appendLog(
            actionType = FollowUpActionType.MISSED_CALL_DETECTED,
            timestampEpochMs = now,
            phone = normalizedPhone.orEmpty(),
            messagePreview = message,
            source = candidate.source
        )

        val whatsappPackages = evaluation.whatsappPackages
        val decision = evaluation.decisionInput.let(MissedCallAutoResponseDecision::decide)

        when (decision) {
            MissedCallAutoResponseAction.ATTEMPT_WHATSAPP_AUTO_SEND -> attemptWhatsAppAutoSend(
                normalizedPhone = normalizedPhone.orEmpty(),
                message = message,
                packageName = whatsappPackages.selectedPackage,
                input = decisionInput(
                    candidate = candidate,
                    normalizedPhone = normalizedPhone,
                    phone = phone,
                    now = now,
                    templateAvailable = template != null,
                    whatsappPackages = whatsappPackages,
                    message = message
                ),
                now = now,
                source = candidate.source
            )
            MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP -> openPreparedWhatsApp(
                normalizedPhone = normalizedPhone.orEmpty(),
                message = message,
                packageName = whatsappPackages.selectedPackage,
                input = decisionInput(
                    candidate = candidate,
                    normalizedPhone = normalizedPhone,
                    phone = phone,
                    now = now,
                    templateAvailable = template != null,
                    whatsappPackages = whatsappPackages,
                    message = message
                ),
                now = now,
                source = candidate.source
            )
            MissedCallAutoResponseAction.OPEN_PREPARED_WHATSAPP_ACCESSIBILITY_MISSING -> {
                appendLog(
                    FollowUpActionType.WHATSAPP_ACCESSIBILITY_NOT_ENABLED,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
                openPreparedWhatsApp(
                    normalizedPhone = normalizedPhone.orEmpty(),
                    message = message,
                    packageName = whatsappPackages.selectedPackage,
                    input = decisionInput(
                        candidate = candidate,
                        normalizedPhone = normalizedPhone,
                        phone = phone,
                        now = now,
                        templateAvailable = template != null,
                        whatsappPackages = whatsappPackages,
                        message = message
                    ),
                    now = now,
                    source = candidate.source
                )
                // §2: the user asked for messages to go out without their approval, but without
                // the accessibility service nothing can press send — the chat just sits open. They
                // believe the client was answered, so they have to be told they were not.
                notifySendFailed(normalizedPhone.orEmpty(), message)
            }
            MissedCallAutoResponseAction.SHOW_MANUAL_REPLY_PROMPT -> {
                appendLog(
                    FollowUpActionType.MANUAL_REPLY_PENDING,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
                showManualReplyPrompt(normalizedPhone ?: phone, message)
            }
            MissedCallAutoResponseAction.SEND_AUTOMATIC_SMS -> sendAutomaticSms(
                phone = phone,
                normalizedPhone = normalizedPhone.orEmpty(),
                message = message,
                now = now,
                fallback = settings.primaryChannel == MissedCallResponsePrimaryChannel.WHATSAPP_FIRST,
                source = candidate.source
            )
            MissedCallAutoResponseAction.OPEN_MANUAL_FALLBACK -> {
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_NO_PERMISSION,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
                showManualFallback(normalizedPhone ?: phone, message)
            }
            MissedCallAutoResponseAction.SKIP_DISABLED -> {
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_DISABLED,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            }
            MissedCallAutoResponseAction.SKIP_DUPLICATE ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_DUPLICATE,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_NO_NUMBER ->
                appendLog(FollowUpActionType.AUTO_SMS_SKIPPED_NO_NUMBER, now, "", message, candidate.source)
            MissedCallAutoResponseAction.SKIP_EXCLUDED ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_EXCLUDED,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_BLOCKED_CONTACT ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_BLOCKED_CONTACT,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_BLOCKED_NON_CONTACT ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_BLOCKED_NON_CONTACT,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_FIRST_TIME_NUMBER ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_FIRST_TIME,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_CONTACT_TYPE_UNVERIFIED ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_CONTACT_TYPE_UNVERIFIED,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_CONTACTS_ONLY_UNVERIFIED ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_CONTACTS_ONLY,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_NOT_ALLOWED ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_NOT_ALLOWED,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_NO_PERMISSION ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_NO_PERMISSION,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_OUTSIDE_WORKING_HOURS ->
                appendLog(
                    FollowUpActionType.AUTO_SMS_SKIPPED_OUTSIDE_WORKING_HOURS,
                    now,
                    normalizedPhone.orEmpty(),
                    message,
                    candidate.source
                )
            MissedCallAutoResponseAction.SKIP_NOT_MISSED_CALL -> Unit
            MissedCallAutoResponseAction.SKIP_NO_TEMPLATE ->
                appendLog(FollowUpActionType.AUTO_SMS_FAILED, now, normalizedPhone.orEmpty(), "", candidate.source)
        }
        return decision
    }

    fun previewMissedIncomingCandidate(candidate: MissedCallCandidate): MissedCallAutoResponseAction {
        val now = System.currentTimeMillis()
        return evaluateCandidate(candidate, now).decisionInput.let(MissedCallAutoResponseDecision::decide)
    }

    /**
     * The exact missed-call message text that WOULD be sent for [phoneNumber] — the same composition
     * the engine uses ([evaluateCandidate]), so §2 holds: what "נסה על עצמך" opens in WhatsApp is what
     * a real client would receive. This is composition ONLY — it deliberately does NOT run the
     * recipient/cooldown/working-hours filters, because "try on yourself" is a user-initiated test on a
     * number they typed, not a real incoming call. Returns "" if no template is available.
     */
    fun composeMissedMessageFor(phoneNumber: String): String {
        val candidate = MissedCallCandidate(
            phoneNumber = phoneNumber,
            direction = MissedCallDirection.INCOMING,
            wasAnswered = false
        )
        return evaluateCandidate(candidate, System.currentTimeMillis()).message
    }

    private fun attemptWhatsAppAutoSend(
        normalizedPhone: String,
        message: String,
        packageName: String?,
        input: MissedCallAutoResponseInput,
        now: Long,
        source: String
    ) {
        appendLog(FollowUpActionType.WHATSAPP_AUTO_SEND_ATTEMPTED, now, normalizedPhone, message, source)
        val selectedPackage = packageName
        if (selectedPackage == null) {
            fallbackAfterWhatsAppFailure(input, normalizedPhone, message, now, source)
            return
        }

        when (whatsAppReplySender.openPreparedReply(normalizedPhone, message, selectedPackage)) {
            WhatsAppReplyOpenResult.OPENED -> {
                whatsAppAutoSendController.enqueuePendingSend(normalizedPhone, message, selectedPackage, now, source)
                appendLog(FollowUpActionType.WHATSAPP_REPLY_PREPARED, now, normalizedPhone, message, source)
                appendLog(FollowUpActionType.WHATSAPP_REPLY_OPENED, now, normalizedPhone, message, source)
            }
            WhatsAppReplyOpenResult.FAILED -> {
                appendLog(FollowUpActionType.WHATSAPP_AUTO_FAILED, now, normalizedPhone, message, source)
                fallbackAfterWhatsAppFailure(input, normalizedPhone, message, now, source)
            }
        }
    }

    private fun openPreparedWhatsApp(
        normalizedPhone: String,
        message: String,
        packageName: String?,
        input: MissedCallAutoResponseInput,
        now: Long,
        source: String
    ) {
        appendLog(FollowUpActionType.WHATSAPP_REPLY_PREPARED, now, normalizedPhone, message, source)
        when (whatsAppReplySender.openPreparedReply(normalizedPhone, message, packageName)) {
            WhatsAppReplyOpenResult.OPENED -> {
                if (!isTestSource(source)) {
                    cooldownStore.markAutoReplyAttempt(normalizedPhone, now)
                }
                appendLog(FollowUpActionType.WHATSAPP_REPLY_OPENED, now, normalizedPhone, message, source)
            }
            WhatsAppReplyOpenResult.FAILED -> {
                appendLog(FollowUpActionType.WHATSAPP_REPLY_FAILED, now, normalizedPhone, message, source)
                fallbackAfterWhatsAppFailure(input, normalizedPhone, message, now, source)
            }
        }
    }

    private fun fallbackAfterWhatsAppFailure(
        input: MissedCallAutoResponseInput,
        normalizedPhone: String,
        message: String,
        now: Long,
        source: String
    ) {
        when (MissedCallAutoResponseDecision.decideAfterWhatsAppFailure(input)) {
            MissedCallAutoResponseAction.SEND_AUTOMATIC_SMS -> sendAutomaticSms(
                phone = normalizedPhone,
                normalizedPhone = normalizedPhone,
                message = message,
                now = now,
                fallback = true,
                source = source
            )
            MissedCallAutoResponseAction.OPEN_MANUAL_FALLBACK -> {
                showManualFallback(normalizedPhone, message)
            }
            else -> {
                // Nothing was sent and no manual path was offered — the client got nothing, so
                // the user has to hear about it (§2).
                appendLog(FollowUpActionType.FALLBACK_SMS_FAILED, now, normalizedPhone, message, source)
                notifySendFailed(normalizedPhone, message)
            }
        }
    }

    private fun sendAutomaticSms(
        phone: String,
        normalizedPhone: String,
        message: String,
        now: Long,
        fallback: Boolean,
        source: String
    ) {
        when (smsSender.send(phone, message)) {
            SmsSendResult.ATTEMPTED -> {
                if (!isTestSource(source)) {
                    cooldownStore.markAutoReplyAttempt(normalizedPhone, now)
                }
                appendLog(
                    if (fallback) FollowUpActionType.FALLBACK_SMS_SENT else FollowUpActionType.AUTO_SMS_SENT,
                    now,
                    normalizedPhone,
                    message,
                    source
                )
            }
            SmsSendResult.NO_PERMISSION -> {
                appendLog(
                    if (fallback) FollowUpActionType.FALLBACK_SMS_FAILED else FollowUpActionType.AUTO_SMS_SKIPPED_NO_PERMISSION,
                    now,
                    normalizedPhone,
                    message,
                    source
                )
                showManualFallback(normalizedPhone, message)
            }
            SmsSendResult.FAILED -> {
                appendLog(
                    if (fallback) FollowUpActionType.FALLBACK_SMS_FAILED else FollowUpActionType.AUTO_SMS_FAILED,
                    now,
                    normalizedPhone,
                    message,
                    source
                )
                // The send failed outright: tell the user, and offer the manual path as well.
                notifySendFailed(normalizedPhone, message)
                showManualFallback(normalizedPhone, message)
            }
        }
    }

    private fun decisionInput(
        candidate: MissedCallCandidate,
        normalizedPhone: String?,
        phone: String,
        now: Long,
        templateAvailable: Boolean,
        whatsappPackages: WhatsAppPackageAvailability,
        message: String
    ): MissedCallAutoResponseInput = MissedCallAutoResponseInput(
        direction = candidate.direction,
        wasAnswered = candidate.wasAnswered,
        phoneNumber = normalizedPhone ?: phone,
        featureEnabled = settings.isEnabled,
        primaryChannel = settings.primaryChannel,
        whatsappInstalled = whatsappPackages.messengerInstalled,
        whatsappBusinessInstalled = whatsappPackages.businessInstalled,
        whatsappMode = settings.whatsappMode,
        whatsappAutomationEnabled = settings.whatsappAutomationEnabled,
        whatsappAccessibilityEnabled = whatsAppAutoSendController.isAccessibilityServiceEnabled(),
        smsFallbackEnabled = settings.smsFallbackEnabled,
        smsPermissionGranted = hasSmsPermission(),
        lastAutoReplyAtEpochMs = if (isTestSource(candidate.source)) {
            null
        } else {
            normalizedPhone?.let(cooldownStore::lastAutoReplyAt)
        },
        nowEpochMs = now,
        templateAvailable = templateAvailable,
        manualFallbackAvailable = settings.manualSmsFallbackEnabled && canShowManualFallback(normalizedPhone ?: phone, message),
        // Override-on-default per-number brake for the MISSED moment: resolve the missed override
        // against the general same-number default. Inherit ⇒ the general value (today's behavior);
        // Off ⇒ no brake (0L, so no send is ever suppressed as a duplicate); Value ⇒ that interval.
        // The decider is unchanged — it still consumes a single cooldownMillis.
        cooldownMillis = com.followupnadlan.accessibility.EffectiveCooldown.resolve(
            missedCooldownOverride.sameNumberOverride,
            generalCooldownSettings.sameNumberCooldownMillis
        ) ?: 0L,
        excluded = exclusionsStore.isExcluded(normalizedPhone ?: phone),
        // Override-on-default: the missed moment's per-moment override wins, else the general
        // default. Resolved here at the call site; the decision logic is unchanged.
        recipientMode = when (
            EffectiveRecipientScope.of(
                recipientScopeSettings.scopeOverride,
                generalRecipientScopeSettings.scope
            )
        ) {
            RecipientScope.ANY_NUMBER -> MissedCallRecipientMode.ANY_NUMBER
            RecipientScope.CONTACTS_ONLY -> MissedCallRecipientMode.CONTACTS_ONLY
            RecipientScope.NON_CONTACTS_ONLY -> MissedCallRecipientMode.NON_CONTACTS_ONLY
            RecipientScope.ONLY_SELECTED -> MissedCallRecipientMode.ONLY_SELECTED
        },
        allowedRecipientNumbers = allowedRecipientsStore.load().map { it.number },
        blockSavedContacts = exclusionsStore.loadBlockedGroups().contains(BlockedRecipientGroup.CONTACTS),
        blockNonContacts = exclusionsStore.loadBlockedGroups().contains(BlockedRecipientGroup.NON_CONTACTS),
        blockFirstTimeNumbers = exclusionsStore.loadBlockedGroups().contains(BlockedRecipientGroup.FIRST_TIME),
        isFirstTimeNumber = FollowUpNumberHistory.isFirstTimeNumber(logStore.load(), normalizedPhone ?: phone),
        contactsPermissionGranted = contactVerifier.hasContactsPermission(),
        isSavedContact = contactVerifier.isSavedContact(normalizedPhone ?: phone),
        withinWorkingHours = WorkingHoursDecider.isWithinWorkingHours(
            LocalMomentResolver.resolve(now),
            workingHoursSettings.snapshot()
        )
    )

    /**
     * Renders the template and appends the signature line — §5: the signature closes *both* the
     * missed and the ended message, with no skip logic. On an empty/partial profile
     * [SignatureLine] drops what it cannot fill, so a placeholder never reaches a client.
     *
     * [attachCard] is honored so the ended journey's "מצורף" switch changes the message that is
     * actually sent, not just its preview. It never applies to missed: there the signature is how
     * the client knows who is answering them.
     */
    private fun renderMessage(templateBody: String, attachCard: Boolean = true): String {
        val profile = profileStore.load()
        val businessName = profile.officeName.ifBlank { profile.agentName }
        val rendered = TemplateTagRenderer.render(
            templateBody,
            TemplateTagValues(
                agentName = profile.agentName,
                officeName = profile.officeName,
                businessName = businessName,
                phone = profile.phone,
                website = profile.website,
                businessCard = profile.businessCard,
                signature = profile.signature
            )
        )
        return if (attachCard) {
            SignatureLine.append(rendered, ContactCard.fromProfile(profile))
        } else {
            rendered.trim()
        }
    }

    /**
     * The full outgoing missed-call message. When the missed moment's card toggle is ON, the
     * formatted TEXT business card REPLACES the one-line signature (name/role/phone live in the
     * card, so the separate signature line is dropped to avoid saying it twice), and the template's
     * own website/card link lines are suppressed so the website is not shown twice — the card owns
     * them. When the card is OFF, the one-line signature closes the body as before (§5). The
     * composed string is exactly what the preview shows (§2).
     */
    private fun renderMissedMessage(template: MessageTemplate): String {
        val profile = profileStore.load()
        val card = ContactCard.fromProfile(profile)
        return if (missedCardSettings.cardAttached) {
            // Card on: raw body (no link lines), tag-rendered, NO signature — the text card replaces it.
            val rawBody = renderMessage(template.body, attachCard = false)
            com.followupnadlan.templates.ContactTextCard.append(rawBody, card)
        } else {
            // Card off: unchanged — body + link lines + signature.
            renderMessage(MessageComposition.build(template), attachCard = true)
        }
    }

    /**
     * A missed-call reply that did not go out. §2: without this the user believes their client
     * was answered when they were not — the most damaging false belief the app can create, since
     * the whole promise is "nobody is left without a reply".
     */
    private fun notifySendFailed(phone: String, message: String) {
        if (phone.isBlank()) return
        FollowUpFailureNotificationHelper(context).showSendFailed(phone, message)
    }

    private fun showManualFallback(phone: String, message: String) {
        if (!canShowManualFallback(phone, message)) return
        manualReplyNotificationHelper.showManualSmsReply(phone, message)
    }

    private fun showManualReplyPrompt(phone: String, message: String) {
        if (!canShowManualFallback(phone, message)) return
        manualReplyNotificationHelper.showManualReplyPrompt(phone, message)
    }

    private fun canShowManualFallback(phone: String, message: String): Boolean =
        phone.isNotBlank() && message.isNotBlank()

    private fun directionFor(call: LatestCallLogEntry?): MissedCallDirection =
        when (call?.type) {
            FollowUpCallType.Missed,
            FollowUpCallType.Incoming -> MissedCallDirection.INCOMING
            FollowUpCallType.Outgoing -> MissedCallDirection.OUTGOING
            null -> MissedCallDirection.INCOMING
        }

    private fun wasAnswered(call: LatestCallLogEntry): Boolean =
        call.type == FollowUpCallType.Incoming && call.durationSeconds > 0L

    private fun hasSmsPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    private fun isTestSource(source: String): Boolean =
        source == DebugMissedCallSimulator.SOURCE

    private fun appendLog(
        actionType: FollowUpActionType,
        timestampEpochMs: Long,
        phone: String,
        messagePreview: String,
        source: String
    ) {
        logStore.append(
            FollowUpLogEntry(
                actionType = actionType,
                timestampEpochMs = timestampEpochMs,
                messagePreview = FollowUpLogStorage.messagePreview(messagePreview),
                phone = phone,
                source = source
            )
        )
    }

    private fun evaluateCandidate(candidate: MissedCallCandidate, now: Long): MissedCallEvaluation {
        val phone = candidate.phoneNumber.orEmpty()
        val normalizedPhone = PhoneNumberNormalizer.normalizeForWhatsApp(phone)
        // The engine only fires on missed incoming calls, so it always sends the missed-call
        // wording (falls back to any card if the user has none of that role — see selector).
        val template = TemplateRoleSelector.forRole(
            templates = templateStore.loadTemplates(),
            role = TemplateRole.MISSED_CALL,
            selectedIdForRole = settings.selectedMissedTemplateId
        )
        val message = template?.let { renderMissedMessage(it) }.orEmpty()
        val whatsappPackages = whatsAppPackageResolver.resolve(settings.preferredWhatsAppPackage)
        return MissedCallEvaluation(
            rawPhone = phone,
            normalizedPhone = normalizedPhone,
            template = template,
            message = message,
            whatsappPackages = whatsappPackages,
            decisionInput = decisionInput(
                candidate = candidate,
                normalizedPhone = normalizedPhone,
                phone = phone,
                now = now,
                templateAvailable = template != null,
                whatsappPackages = whatsappPackages,
                message = message
            )
        )
    }

    private data class MissedCallEvaluation(
        val rawPhone: String,
        val normalizedPhone: String?,
        val template: MessageTemplate?,
        val message: String,
        val whatsappPackages: WhatsAppPackageAvailability,
        val decisionInput: MissedCallAutoResponseInput
    )
}

internal object MissedCallMessageResolver {
    // Falls back to the first template when the saved id is orphaned (e.g. its card was
    // deleted) so the engine never renders an empty message on a real missed call.
    fun selectedTemplate(templates: List<MessageTemplate>, selectedTemplateId: String): MessageTemplate? =
        templates.firstOrNull { it.id == selectedTemplateId }
            ?: templates.firstOrNull()

    fun renderTemplate(template: MessageTemplate?, renderBody: (String) -> String): String =
        template?.let { renderBody(MessageComposition.build(it)) }.orEmpty()
}

internal object FollowUpNumberHistory {
    fun isFirstTimeNumber(entries: List<FollowUpLogEntry>, phoneNumber: String): Boolean {
        val normalized = PhoneNumberNormalizer.normalizeForWhatsApp(phoneNumber) ?: return true
        return entries.none { entry ->
            PhoneNumberNormalizer.normalizeForWhatsApp(entry.phone) == normalized
        }
    }
}
