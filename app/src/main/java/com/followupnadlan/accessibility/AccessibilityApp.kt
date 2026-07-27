package com.followupnadlan.accessibility

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel
import com.followupnadlan.missedcall.MissedCallWhatsAppMode
import com.followupnadlan.missedcall.WhatsAppAutoSendController
import com.followupnadlan.missedcall.WhatsAppPackageResolver
import com.followupnadlan.postcall.CallDetectionDiagnostics
import com.followupnadlan.postcall.CallDetectionDiagnosticsSnapshot
import com.followupnadlan.postcall.CallDetectionPreferences
import com.followupnadlan.postcall.CallDetectionService
import com.followupnadlan.postcall.CallDetectionServiceAction
import com.followupnadlan.postcall.CallDetectionServiceLifecycle
import com.followupnadlan.profile.ContactCard
import com.followupnadlan.profile.MyDetailsProfile
import com.followupnadlan.profile.MyDetailsStore
import com.followupnadlan.profile.SignatureLine
import com.followupnadlan.setup.PermissionSnapshot
import com.followupnadlan.setup.PermissionStatusLogic
import com.followupnadlan.sharing.ContactCardShareResult
import com.followupnadlan.sharing.PrepareAndShareContactCard
import com.followupnadlan.templates.MessageComposition
import com.followupnadlan.templates.MessageTemplate
import com.followupnadlan.templates.TemplateRole
import com.followupnadlan.templates.TemplateRoleSelector
import com.followupnadlan.templates.TemplateStore
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import com.followupnadlan.whatsapp.WhatsAppLinkBuilder

/** Top-level tabs in the accessibility app: בית / היום / הגדרות. */
internal enum class AccessibilityTab { HOME, ACTIVITY, SETTINGS }

/** Modal screens layered above the tabs (consent, templates, exclusions, missed-call prompt). */
internal enum class AccessibilityModal {
    NONE,
    SETUP,
    TEMPLATES,
    EXCLUSIONS,
    SMART_RULES,
    ALLOWED_RECIPIENTS,
    CONTACT_CARD,
    MISSED_CALL_PROMPT,
    // Design Pass 2/3 — the two trust-moment journey pages, opened from the home cards.
    MISSED_JOURNEY,
    ENDED_JOURNEY,
    // System maintenance, reached only from the small ⚙️ on Home — never from navigation.
    SYSTEM_SETTINGS,
    // Reached from a row inside SYSTEM_SETTINGS (⚙️ list navigation — plan v2 §"ניווט").
    HISTORY,
    SUPPORT
}

/** Candidate source for the multi-picker overlay. */
internal enum class PickerSource { CONTACTS, RECENT }

/** Which recipient store the multi-picker writes to on confirm. */
internal enum class PickerTarget { EXCLUSIONS, ALLOWED }

/** Full-screen multi-picker request, layered above the current tab/modal when non-null. */
internal data class ActivePicker(
    val title: String,
    val source: PickerSource,
    val target: PickerTarget
)

internal data class AccessibilityBackState(
    val tab: AccessibilityTab,
    val modal: AccessibilityModal,
    val homeMessageEditorOpen: Boolean
)

internal object AccessibilityBackNavigation {
    fun handleBack(state: AccessibilityBackState): AccessibilityBackState? =
        when {
            state.homeMessageEditorOpen -> state.copy(homeMessageEditorOpen = false)
            state.modal != AccessibilityModal.NONE -> state.copy(modal = AccessibilityModal.NONE)
            state.tab != AccessibilityTab.HOME -> state.copy(tab = AccessibilityTab.HOME)
            else -> null
        }
}

internal object HomeMessagePreviewLogic {
    /**
     * The full message that will actually be sent for the selected card — body plus the
     * appended link lines — so Home shows exactly what the recipient receives. Falls back to
     * the first card when the saved id is orphaned (matches the engine/UI fallback).
     */
    fun preview(templates: List<MessageTemplate>, selectedTemplateId: String): String {
        val template = templates.firstOrNull { it.id == selectedTemplateId }
            ?: templates.firstOrNull()
            ?: return ""
        return MessageComposition.build(template)
    }
}

/** Carries the missed-call context when the app is opened from a follow-up notification. */
data class MissedCallLaunch(
    val phone: String = "",
    val message: String = "",
    val fromNotification: Boolean = false,
    /** Raw notification call-type extra ("missed"/"incoming"/"outgoing"); decides the prompt wording. */
    val callType: String? = null
)

/**
 * Native Compose accessibility app — the "missed-call text bridge" UI.
 * It binds the new screens to the existing stores and decision settings; it does not
 * change missed-call detection, the WhatsApp-first/SMS-fallback decision, cooldown, or logs.
 */
@Composable
fun AccessibilityApp(missedCallLaunch: MissedCallLaunch = MissedCallLaunch()) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val settings = remember(context) { MissedCallAutoResponseSettings(appContext) }
    val callDetectionPreferences = remember(context) { CallDetectionPreferences(appContext) }
    val callDetectionDiagnostics = remember(context) { CallDetectionDiagnostics(appContext) }
    val templateStore = remember(context) { TemplateStore(appContext) }
    val logStore = remember(context) { FollowUpLogStore(appContext) }
    val recipientScopeSettings = remember(context) { RecipientScopeSettings(appContext) }
    val endedScopeSettings = remember(context) { EndedScopeSettings(appContext) }
    val endedCardSettings = remember(context) { EndedCardSettings(appContext) }
    val cooldownSettings = remember(context) { FollowUpCooldownSettings(appContext) }
    val exclusionsStore = remember(context) { ExclusionsStore(appContext) }
    val workingHoursSettings = remember(context) { WorkingHoursSettings(appContext) }
    val allowedRecipientsStore = remember(context) { AllowedRecipientsStore(appContext) }
    val myDetailsStore = remember(context) { MyDetailsStore(appContext) }
    val whatsAppAutoSendController = remember(context) { WhatsAppAutoSendController(appContext) }
    val setupPreferences = remember(context) { com.followupnadlan.setup.SetupPreferences(appContext) }

    var onboardingDone by remember { mutableStateOf(setupPreferences.isSetupCompleted()) }

    var tab by remember { mutableStateOf(AccessibilityTab.HOME) }
    var modal by remember {
        mutableStateOf(if (missedCallLaunch.fromNotification) AccessibilityModal.MISSED_CALL_PROMPT else AccessibilityModal.NONE)
    }
    // Full-screen recipient multi-picker overlay (above the current tab/modal when non-null).
    var activePicker by remember { mutableStateOf<ActivePicker?>(null) }
    // Wiring Pass step 1: which journey's message the "שנה את ההודעה/התזכורת" editor is open for
    // (missed vs. ended, saved to separate templates). null = closed.
    var messageEditorTarget by remember { mutableStateOf<TemplateRole?>(null) }
    // Wiring Pass: journey-screen picker overlays (missed journey) and the ended journey's
    // in-page contact-card editor.
    var recipientPickerOpen by remember { mutableStateOf(false) }
    var askPickerOpen by remember { mutableStateOf(false) }
    var cardEditorOpen by remember { mutableStateOf(false) }
    var channelPickerOpen by remember { mutableStateOf(false) }
    var endedScopePickerOpen by remember { mutableStateOf(false) }
    // Bumped after the contact card is edited from the ended journey so the preview reloads.
    var profileRefresh by remember { mutableStateOf(0) }
    // Bumped whenever a recipient store is mutated, so Settings previews recompute.
    var recipientsRefresh by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val undoScope = rememberCoroutineScope()

    // Bridging is "active" when the missed-call auto response feature is enabled.
    var bridgingEnabled by remember { mutableStateOf(settings.isEnabled) }
    var askBeforeSend by remember {
        mutableStateOf(settings.whatsappMode == MissedCallWhatsAppMode.PREPARED_MANUAL)
    }
    var recipientScope by remember { mutableStateOf(recipientScopeSettings.scope) }
    // The ended moment's own scope — a separate decision from missed (different jobs, §4).
    var endedScope by remember { mutableStateOf(endedScopeSettings.scope) }
    var cardAttached by remember { mutableStateOf(endedCardSettings.cardAttached) }
    // The single explicit channel choice, read back from the engine's flag pair.
    var selectedChannel by remember { mutableStateOf(FollowUpChannelSettings.current(settings)) }
    var smsFallback by remember { mutableStateOf(settings.smsFallbackEnabled) }
    // The signature exactly as it will close every sent message; recomputed after a profile edit.
    val signaturePreview = remember(profileRefresh) { SignatureLine.render(myDetailsStore.load()) }
    var preferredWhatsApp by remember {
        mutableStateOf(
            if (settings.preferredWhatsAppPackage == WhatsAppPackageResolver.WHATSAPP_BUSINESS_PACKAGE) {
                WhatsAppChoice.BUSINESS
            } else {
                WhatsAppChoice.REGULAR
            }
        )
    }
    // Default card per call scenario. Missed calls send the "missed" wording; completed
    // calls (and manual home sends) the "call ended" wording.
    var selectedEndedId by remember { mutableStateOf(settings.selectedEndedTemplateId) }
    var selectedMissedId by remember { mutableStateOf(settings.selectedMissedTemplateId) }
    var templates by remember { mutableStateOf(templateStore.loadTemplates()) }
    var phoneStateGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_PHONE_STATE)) }
    var callLogGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_CALL_LOG)) }
    var diagnosticsSnapshot by remember { mutableStateOf(callDetectionDiagnostics.snapshot()) }
    val preferredWhatsAppPackage = when (preferredWhatsApp) {
        WhatsAppChoice.BUSINESS -> WhatsAppPackageResolver.WHATSAPP_BUSINESS_PACKAGE
        WhatsAppChoice.REGULAR -> WhatsAppPackageResolver.WHATSAPP_MESSENGER_PACKAGE
    }
    val whatsappAvailability = remember(context, preferredWhatsAppPackage) {
        WhatsAppPackageResolver(context.applicationContext).resolve(preferredWhatsAppPackage)
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        settings.smsFallbackEnabled = granted
        smsFallback = granted
    }
    val callDetectionPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        phoneStateGranted = context.hasPermission(Manifest.permission.READ_PHONE_STATE)
        callLogGranted = context.hasPermission(Manifest.permission.READ_CALL_LOG)
        settings.isEnabled = true
        callDetectionPreferences.setEnabled(true)
        bridgingEnabled = true
        applyCallDetectionServiceState(appContext, bridgeEnabled = true, phoneStateGranted, callLogGranted)
        diagnosticsSnapshot = callDetectionDiagnostics.snapshot()
    }

    // Fresh install shows "WhatsApp רגיל" as the default; persist it so storage matches the UI
    // (an empty stored preference would otherwise resolve to Business at the tie-break).
    LaunchedEffect(Unit) {
        if (settings.preferredWhatsAppPackage.isBlank()) {
            settings.preferredWhatsAppPackage = WhatsAppPackageResolver.WHATSAPP_MESSENGER_PACKAGE
        }
    }

    if (!onboardingDone) {
        Surface(modifier = Modifier.fillMaxSize(), color = AccessibilityColors.ScreenBackground) {
            OnboardingScreen(
                myDetailsStore = myDetailsStore,
                setupPreferences = setupPreferences,
                onRequestCallPermissions = {
                    phoneStateGranted = context.hasPermission(Manifest.permission.READ_PHONE_STATE)
                    callLogGranted = context.hasPermission(Manifest.permission.READ_CALL_LOG)
                    if (phoneStateGranted && callLogGranted) {
                        settings.isEnabled = true
                        callDetectionPreferences.setEnabled(true)
                        bridgingEnabled = true
                        applyCallDetectionServiceState(appContext, bridgeEnabled = true, phoneStateGranted, callLogGranted)
                        diagnosticsSnapshot = callDetectionDiagnostics.snapshot()
                    }
                },
                onFinish = { onboardingDone = true }
            )
        }
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = AccessibilityColors.ScreenBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                val picker = activePicker
                if (picker != null) {
                    ContactMultiPickerScreen(
                        title = picker.title,
                        needsContactsPermission = picker.source == PickerSource.CONTACTS,
                        loadCandidates = {
                            when (picker.source) {
                                PickerSource.CONTACTS -> ContactsPickerRepository(appContext).load()
                                PickerSource.RECENT -> RecentCallersRepository(appContext).load()
                            }
                        },
                        savedKeys = when (picker.target) {
                            PickerTarget.EXCLUSIONS -> exclusionsStore.load()
                                .map { recipientKey(it.number) }.filter { it.isNotEmpty() }.toSet()
                            PickerTarget.ALLOWED -> allowedRecipientsStore.load()
                                .map { recipientKey(it.number) }.filter { it.isNotEmpty() }.toSet()
                        },
                        onConfirm = { added, removedKeys ->
                            val mutation = applyRecipientDiff(
                                target = picker.target,
                                added = added,
                                removedKeys = removedKeys,
                                exclusionsStore = exclusionsStore,
                                allowedRecipientsStore = allowedRecipientsStore
                            )
                            activePicker = null
                            recipientsRefresh++
                            recipientChangeMessage(mutation.addedCount, mutation.removedCount)?.let { message ->
                                undoScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = message,
                                        actionLabel = "ביטול",
                                        withDismissAction = true
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        mutation.undo()
                                        recipientsRefresh++
                                    }
                                }
                            }
                        },
                        onBack = { activePicker = null }
                    )
                } else {
                when (modal) {
                    AccessibilityModal.NONE -> when (tab) {
                        AccessibilityTab.HOME -> {
                        val homeStatusInput = HomeServiceStatusInput(
                            bridgeEnabled = bridgingEnabled,
                            whatsappMode = if (askBeforeSend) {
                                MissedCallWhatsAppMode.PREPARED_MANUAL
                            } else {
                                MissedCallWhatsAppMode.ACCESSIBILITY_AUTO
                            },
                            primaryChannel = settings.primaryChannel,
                            whatsappAvailable = whatsappAvailability.selectedPackage != null,
                            smsFallbackEnabled = smsFallback,
                            readPhoneStateGranted = phoneStateGranted,
                            readCallLogGranted = callLogGranted,
                            detectorEnabled = callDetectionPreferences.isEnabled(),
                            autoSendConsentGranted = whatsAppAutoSendController.isAccessibilityServiceEnabled(),
                            hasTemplate = templates.isNotEmpty()
                        )
                        val missedTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.MISSED_CALL, selectedMissedId
                        )
                        val endedTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.CALL_ENDED, selectedEndedId
                        )
                        val homeWarning = HomeWarningLogic.warningFor(
                            HomeWarningState(
                                serviceEnabled = bridgingEnabled && callDetectionPreferences.isEnabled(),
                                phoneStatePermissionGranted = phoneStateGranted,
                                callLogPermissionGranted = callLogGranted,
                                profileEmpty = signaturePreview.isBlank()
                            )
                        )
                        HomeScreen(
                            warning = homeWarning,
                            missedEnabled = bridgingEnabled,
                            missedBody = missedTemplate?.let { MessageComposition.build(it) }.orEmpty(),
                            endedBody = endedTemplate?.let { MessageComposition.build(it) }.orEmpty(),
                            signature = signaturePreview,
                            cardAttached = cardAttached,
                            cardInitials = ContactCard.fromProfile(myDetailsStore.load()).let { card ->
                                card.fullName.trim().split(" ").filter { it.isNotBlank() }.take(2)
                                    .joinToString("") { it.take(1) }.ifBlank { "דל" }
                            },
                            cardLine1 = ContactCard.fromProfile(myDetailsStore.load()).fullName.ifBlank { "השם שלך" },
                            onToggleEnabled = {
                                if (bridgingEnabled) {
                                    settings.isEnabled = false
                                    callDetectionPreferences.setEnabled(false)
                                    bridgingEnabled = false
                                    applyCallDetectionServiceState(appContext, bridgeEnabled = false, phoneStateGranted, callLogGranted)
                                    diagnosticsSnapshot = callDetectionDiagnostics.snapshot()
                                } else {
                                    modal = AccessibilityModal.SETUP
                                }
                            },
                            onOpenMissedJourney = { modal = AccessibilityModal.MISSED_JOURNEY },
                            onOpenEndedJourney = { modal = AccessibilityModal.ENDED_JOURNEY },
                            // The ⚠️ leads to whichever fix it is about: an empty profile opens the
                            // card editor, anything else opens the enable/permissions flow.
                            onResolveWarning = {
                                if (homeWarning == HomeWarning.EMPTY_PROFILE) {
                                    cardEditorOpen = true
                                } else {
                                    modal = AccessibilityModal.SETUP
                                }
                            },
                            onOpenSystemSettings = { modal = AccessibilityModal.SYSTEM_SETTINGS }
                        )
                        }
                        AccessibilityTab.ACTIVITY -> ActivityScreen(logStore = logStore)
                        AccessibilityTab.SETTINGS -> {
                        val allowedPreview = remember(recipientsRefresh) {
                            RecipientPreviewLogic.summary(allowedRecipientsStore.load().map { it.label })
                        }
                        val exclusionsPreview = remember(recipientsRefresh) {
                            RecipientPreviewLogic.summary(exclusionsStore.load().map { it.label })
                        }
                        SettingsScreen(
                            askBeforeSend = askBeforeSend,
                            recipientScope = recipientScope,
                            preferredWhatsApp = preferredWhatsApp,
                            // Only offer the regular/Business choice when Business is actually
                            // installed; otherwise there's nothing to choose and it just confuses users.
                            showWhatsAppChoice = whatsappAvailability.businessInstalled,
                            smsFallback = smsFallback,
                            allowedPreview = allowedPreview,
                            exclusionsPreview = exclusionsPreview,
                            onSelectAskBeforeSend = { ask ->
                                askBeforeSend = ask
                                settings.whatsappMode = if (ask) {
                                    MissedCallWhatsAppMode.PREPARED_MANUAL
                                } else {
                                    MissedCallWhatsAppMode.ACCESSIBILITY_AUTO
                                }
                            },
                            onSelectRecipientScope = { scope ->
                                recipientScope = scope
                                recipientScopeSettings.scope = scope
                            },
                            onSelectWhatsApp = { choice ->
                                preferredWhatsApp = choice
                                settings.preferredWhatsAppPackage = when (choice) {
                                    WhatsAppChoice.BUSINESS -> WhatsAppPackageResolver.WHATSAPP_BUSINESS_PACKAGE
                                    WhatsAppChoice.REGULAR -> WhatsAppPackageResolver.WHATSAPP_MESSENGER_PACKAGE
                                }
                            },
                            onToggleSmsFallback = {
                                val next = !smsFallback
                                if (next && context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                } else {
                                    settings.smsFallbackEnabled = next
                                    smsFallback = next
                                }
                            },
                            onOpenExclusions = { modal = AccessibilityModal.EXCLUSIONS },
                            onOpenAllowedRecipients = { modal = AccessibilityModal.ALLOWED_RECIPIENTS },
                            onOpenTemplates = { modal = AccessibilityModal.TEMPLATES },
                            onDeleteHistory = { logStore.clear() },
                            myDetailsStore = myDetailsStore,
                            diagnosticsSnapshot = diagnosticsSnapshot
                        )
                        }
                    }

                    AccessibilityModal.SETUP -> SetupConsentScreen(
                        onEnable = {
                            phoneStateGranted = context.hasPermission(Manifest.permission.READ_PHONE_STATE)
                            callLogGranted = context.hasPermission(Manifest.permission.READ_CALL_LOG)
                            settings.isEnabled = true
                            callDetectionPreferences.setEnabled(true)
                            bridgingEnabled = true
                            if (!phoneStateGranted || !callLogGranted) {
                                callDetectionPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_PHONE_STATE,
                                        Manifest.permission.READ_CALL_LOG
                                    )
                                )
                            } else {
                                applyCallDetectionServiceState(appContext, bridgeEnabled = true, phoneStateGranted, callLogGranted)
                            }
                            diagnosticsSnapshot = callDetectionDiagnostics.snapshot()
                            modal = AccessibilityModal.NONE
                        },
                        onDismiss = { modal = AccessibilityModal.NONE }
                    )

                    AccessibilityModal.TEMPLATES -> TemplatesScreen(
                        templates = templates,
                        selectedEndedId = selectedEndedId,
                        selectedMissedId = selectedMissedId,
                        // Choosing a card as default sets the default for that card's own role.
                        onSelectTemplate = { template ->
                            when (template.role) {
                                TemplateRole.CALL_ENDED -> {
                                    selectedEndedId = template.id
                                    settings.selectedEndedTemplateId = template.id
                                }
                                TemplateRole.MISSED_CALL -> {
                                    selectedMissedId = template.id
                                    settings.selectedMissedTemplateId = template.id
                                }
                            }
                        },
                        onSaveTemplate = { template ->
                            templateStore.saveTemplate(template)
                            templates = templateStore.loadTemplates()
                        },
                        onAddTemplate = { title, body, cardLink, websiteLink, role ->
                            templateStore.addTemplate(title, body, cardLink, websiteLink, role)
                            templates = templateStore.loadTemplates()
                        },
                        onDeleteTemplate = { id ->
                            templateStore.deleteTemplate(id)
                            val reloaded = templateStore.loadTemplates()
                            templates = reloaded
                            // If a role's default was deleted, fall back to the first card of that role.
                            if (reloaded.none { it.id == selectedEndedId }) {
                                (reloaded.firstOrNull { it.role == TemplateRole.CALL_ENDED } ?: reloaded.firstOrNull())?.let {
                                    selectedEndedId = it.id
                                    settings.selectedEndedTemplateId = it.id
                                }
                            }
                            if (reloaded.none { it.id == selectedMissedId }) {
                                (reloaded.firstOrNull { it.role == TemplateRole.MISSED_CALL } ?: reloaded.firstOrNull())?.let {
                                    selectedMissedId = it.id
                                    settings.selectedMissedTemplateId = it.id
                                }
                            }
                        },
                        onBack = { modal = AccessibilityModal.NONE }
                    )

                    AccessibilityModal.EXCLUSIONS -> ExclusionsScreen(
                        store = exclusionsStore,
                        recipientScope = recipientScope,
                        onAddFromContacts = {
                            activePicker = ActivePicker("הוסף מאנשי קשר", PickerSource.CONTACTS, PickerTarget.EXCLUSIONS)
                        },
                        onAddFromRecent = {
                            activePicker = ActivePicker("הוסף מהמתקשרים האחרונים", PickerSource.RECENT, PickerTarget.EXCLUSIONS)
                        },
                        onBack = { modal = AccessibilityModal.NONE; recipientsRefresh++ }
                    )

                    AccessibilityModal.ALLOWED_RECIPIENTS -> AllowedRecipientsScreen(
                        store = allowedRecipientsStore,
                        onAddFromContacts = {
                            activePicker = ActivePicker("הוסף מאנשי קשר", PickerSource.CONTACTS, PickerTarget.ALLOWED)
                        },
                        onAddFromRecent = {
                            activePicker = ActivePicker("הוסף מהמתקשרים האחרונים", PickerSource.RECENT, PickerTarget.ALLOWED)
                        },
                        onBack = { modal = AccessibilityModal.NONE; recipientsRefresh++ }
                    )

                    AccessibilityModal.CONTACT_CARD -> ContactCardScreen(
                        store = myDetailsStore,
                        onBack = { modal = AccessibilityModal.NONE }
                    )

                    AccessibilityModal.MISSED_JOURNEY -> {
                        val missedTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.MISSED_CALL, selectedMissedId
                        )
                        MissedJourneyScreen(
                            isEnabled = bridgingEnabled,
                            missedBody = missedTemplate?.let { MessageComposition.build(it) }.orEmpty(),
                            signature = signaturePreview,
                            recipientLabel = recipientScopeLabel(recipientScope),
                            askBeforeSend = askBeforeSend,
                            channelLabel = channelLabel(selectedChannel),
                            onToggleEnabled = {
                                if (bridgingEnabled) {
                                    settings.isEnabled = false
                                    callDetectionPreferences.setEnabled(false)
                                    bridgingEnabled = false
                                    applyCallDetectionServiceState(appContext, bridgeEnabled = false, phoneStateGranted, callLogGranted)
                                    diagnosticsSnapshot = callDetectionDiagnostics.snapshot()
                                } else {
                                    modal = AccessibilityModal.SETUP
                                }
                            },
                            onEditMessage = { messageEditorTarget = TemplateRole.MISSED_CALL },
                            onEditRecipient = { recipientPickerOpen = true },
                            onEditAskBeforeSend = { askPickerOpen = true },
                            onEditChannel = { channelPickerOpen = true },
                            onBack = { modal = AccessibilityModal.NONE }
                        )
                    }

                    AccessibilityModal.ENDED_JOURNEY -> {
                        val endedTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.CALL_ENDED, selectedEndedId
                        )
                        val endedCard = remember(profileRefresh) {
                            ContactCard.fromProfile(myDetailsStore.load())
                        }
                        EndedJourneyScreen(
                            reminderBody = endedTemplate?.let { MessageComposition.build(it) }.orEmpty(),
                            card = endedCard,
                            cardAttached = cardAttached,
                            scopeLabel = endedScopeLabel(endedScope),
                            channelLabel = channelLabel(selectedChannel),
                            onEditMessage = { messageEditorTarget = TemplateRole.CALL_ENDED },
                            onEditCard = { cardEditorOpen = true },
                            onToggleCardAttached = {
                                cardAttached = !cardAttached
                                endedCardSettings.cardAttached = cardAttached
                            },
                            onEditScope = { endedScopePickerOpen = true },
                            onEditChannel = { channelPickerOpen = true },
                            onBack = { modal = AccessibilityModal.NONE }
                        )
                    }

                    AccessibilityModal.SYSTEM_SETTINGS -> SystemSettingsScreen(
                        permissions = PermissionSnapshot(
                            phoneStateGranted = phoneStateGranted,
                            callLogGranted = callLogGranted,
                            contactsGranted = context.hasPermission(Manifest.permission.READ_CONTACTS)
                        ),
                        exclusionsPreview = remember(recipientsRefresh) {
                            RecipientPreviewLogic.summary(exclusionsStore.load().map { it.label })
                        },
                        diagnosticsSnapshot = diagnosticsSnapshot,
                        cooldownSettings = cooldownSettings,
                        onRequestPermissions = {
                            callDetectionPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_CONTACTS
                                )
                            )
                        },
                        onOpenSmartRules = { modal = AccessibilityModal.SMART_RULES },
                        onOpenHistory = { modal = AccessibilityModal.HISTORY },
                        onOpenSupport = { modal = AccessibilityModal.SUPPORT },
                        onDeleteHistory = { logStore.clear() },
                        onBack = { modal = AccessibilityModal.NONE }
                    )

                    AccessibilityModal.HISTORY -> HistoryScreen(
                        logStore = logStore,
                        onBack = { modal = AccessibilityModal.SYSTEM_SETTINGS }
                    )

                    AccessibilityModal.SUPPORT -> SupportScreen(
                        onBack = { modal = AccessibilityModal.SYSTEM_SETTINGS }
                    )

                    AccessibilityModal.SMART_RULES -> SmartRulesScreen(
                        workingHoursSettings = workingHoursSettings,
                        exclusionsStore = exclusionsStore,
                        recipientScope = recipientScope,
                        exclusionsPreview = remember(recipientsRefresh) {
                            RecipientPreviewLogic.summary(exclusionsStore.load().map { it.label })
                        },
                        onOpenExclusions = { modal = AccessibilityModal.EXCLUSIONS },
                        onBack = { modal = AccessibilityModal.NONE; recipientsRefresh++ }
                    )

                    AccessibilityModal.MISSED_CALL_PROMPT -> MissedCallPromptScreen(
                        phone = missedCallLaunch.phone,
                        message = missedCallLaunch.message,
                        mode = FollowUpPromptModeLogic.fromCallType(missedCallLaunch.callType),
                        templates = templates,
                        selectedEndedId = selectedEndedId,
                        selectedMissedId = selectedMissedId,
                        preferredWhatsAppPackage = preferredWhatsAppPackage,
                        primaryChannel = settings.primaryChannel,
                        whatsappAvailable = whatsappAvailability.selectedPackage != null,
                        askBeforeSend = askBeforeSend,
                        onOpenContactCardSettings = { modal = AccessibilityModal.CONTACT_CARD },
                        onDone = { modal = AccessibilityModal.NONE }
                    )
                }
                }

                // Wiring Pass step 1: the message/reminder editor, opened from a journey screen.
                // Edits the template *body* only — links (card/website) stay in their own fields, so
                // MessageComposition keeps appending them once (editing the composed text would bake
                // the links into the body and duplicate them on the next send).
                messageEditorTarget?.let { role ->
                    val editingId = when (role) {
                        TemplateRole.MISSED_CALL -> selectedMissedId
                        TemplateRole.CALL_ENDED -> selectedEndedId
                    }
                    val editingTemplate = TemplateRoleSelector.forRole(templates, role, editingId)
                    if (editingTemplate == null) {
                        messageEditorTarget = null
                    } else {
                        val isReminder = role == TemplateRole.CALL_ENDED
                        MessageEditorScreen(
                            isEnded = isReminder,
                            body = editingTemplate.body,
                            signature = signaturePreview,
                            cardAttached = cardAttached,
                            onToggleCardAttached = {
                                cardAttached = !cardAttached
                                endedCardSettings.cardAttached = cardAttached
                            },
                            onSave = { newBody ->
                                templateStore.saveTemplate(editingTemplate.copy(body = newBody))
                                templates = templateStore.loadTemplates()
                                messageEditorTarget = null
                            },
                            onBack = { messageEditorTarget = null }
                        )
                    }
                }

                // Wiring Pass: "מי יקבל את ההודעה?" — the 4-state recipient scope. Every state is
                // honored by the engine's decide() (ANY_NUMBER / CONTACTS_ONLY / NON_CONTACTS_ONLY /
                // ONLY_SELECTED), so exposing all four opens no §2 gap.
                if (recipientPickerOpen) {
                    JourneyOptionPickerDialog(
                        title = "מי יקבל את ההודעה?",
                        options = RecipientScope.entries.map { it to recipientScopeLabel(it) },
                        selected = recipientScope,
                        onSelect = { scope ->
                            recipientScope = scope
                            recipientScopeSettings.scope = scope
                            recipientPickerOpen = false
                            // "רק אנשים שאבחר" is meaningless until a list exists, so choosing it
                            // goes straight to building that list rather than silently matching no one.
                            if (scope == RecipientScope.ONLY_SELECTED) {
                                modal = AccessibilityModal.ALLOWED_RECIPIENTS
                            }
                        },
                        onDismiss = { recipientPickerOpen = false }
                    )
                }

                // "באיזה ערוץ?" — WhatsApp Business is offered only when installed, and each
                // choice is stored with fallback OFF, so the picked channel is the one used (§2).
                if (channelPickerOpen) {
                    JourneyOptionPickerDialog(
                        title = "באיזה ערוץ?",
                        options = FollowUpChannelSettings
                            .available(whatsappAvailability.businessInstalled)
                            .map { it to channelLabel(it) },
                        selected = selectedChannel,
                        onSelect = { channel ->
                            selectedChannel = channel
                            FollowUpChannelSettings.apply(settings, channel)
                            smsFallback = settings.smsFallbackEnabled
                            preferredWhatsApp = if (channel == FollowUpChannel.WHATSAPP_BUSINESS) {
                                WhatsAppChoice.BUSINESS
                            } else {
                                WhatsAppChoice.REGULAR
                            }
                            channelPickerOpen = false
                        },
                        onDismiss = { channelPickerOpen = false }
                    )
                }

                // "אחרי אילו שיחות להציע לשלוח?" — the ended moment's own scope, stored separately.
                if (endedScopePickerOpen) {
                    JourneyOptionPickerDialog(
                        title = "אחרי אילו שיחות להציע לשלוח?",
                        options = RecipientScope.entries.map { it to endedScopeLabel(it) },
                        selected = endedScope,
                        onSelect = { scope ->
                            endedScope = scope
                            endedScopeSettings.scope = scope
                            endedScopePickerOpen = false
                            if (scope == RecipientScope.ONLY_SELECTED) {
                                modal = AccessibilityModal.ALLOWED_RECIPIENTS
                            }
                        },
                        onDismiss = { endedScopePickerOpen = false }
                    )
                }

                // Wiring Pass: "האם לאשר לפני שליחה?" — maps to whatsappMode (PREPARED_MANUAL = ask,
                // ACCESSIBILITY_AUTO = don't). Same mapping the Settings screen already persists.
                if (askPickerOpen) {
                    JourneyOptionPickerDialog(
                        title = "האם לאשר לפני שליחה?",
                        options = listOf(
                            true to "כן, אאשר כל הודעה",
                            false to "לא, תישלח גם בלי אישורי"
                        ),
                        selected = askBeforeSend,
                        onSelect = { ask ->
                            askBeforeSend = ask
                            settings.whatsappMode = if (ask) {
                                MissedCallWhatsAppMode.PREPARED_MANUAL
                            } else {
                                MissedCallWhatsAppMode.ACCESSIBILITY_AUTO
                            }
                            // §2: choosing "תישלח גם בלי אישורי" *is* the request to send without
                            // asking. The engine gates auto-send on this second flag as well, so
                            // leaving it false would show the user a promise the engine ignores —
                            // it would quietly open the chat and wait instead.
                            settings.whatsappAutomationEnabled = !ask
                            askPickerOpen = false
                        },
                        onDismiss = { askPickerOpen = false }
                    )
                }

                // Tapping the card in the ended preview edits its three fields in place. This is a
                // dedicated editor rather than the older ContactCardScreen, whose copy is about
                // sharing a vCard — a flow MVP-1 does not use.
                if (cardEditorOpen) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AccessibilityColors.ScreenBackground
                    ) {
                        SignatureCardEditorScreen(
                            store = myDetailsStore,
                            onBack = {
                                cardEditorOpen = false
                                profileRefresh++
                            }
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // MVP-1 has no bottom navigation: the product is Home plus the two journeys, and
            // ⚙️ is reached from the small icon on Home rather than from a tab. "היום" belongs to
            // the deferred assistant layer. Both screens and their tabs remain in the code,
            // simply not surfaced — see AccessibilityTab.
        }
    }
}

enum class WhatsAppChoice { REGULAR, BUSINESS }

@Composable
private fun BottomNav(selected: AccessibilityTab, onSelect: (AccessibilityTab) -> Unit) {
    Surface(color = AccessibilityColors.Surface, shadowElevation = 10.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem("בית", AccessibilityIcons.Home, selected == AccessibilityTab.HOME) { onSelect(AccessibilityTab.HOME) }
            NavItem("היום", AccessibilityIcons.Today, selected == AccessibilityTab.ACTIVITY) { onSelect(AccessibilityTab.ACTIVITY) }
            NavItem("הגדרות", AccessibilityIcons.Settings, selected == AccessibilityTab.SETTINGS) { onSelect(AccessibilityTab.SETTINGS) }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) AccessibilityColors.Primary else AccessibilityColors.TextFaint
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(26.dp))
        Text(label, color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp)
    }
}

/**
 * One journey row (Design Pass 2/3): a question/label on top, the current *result* below, and a
 * trailing chevron when [onClick] is set. The value is result-language, never a mechanism name —
 * the caller passes "לכל מי שמתקשר", not "Recipient Scope". Display-only until the wiring pass:
 * [onClick] is null in these passes, so the chevron is shown but the row is inert.
 */
@Composable
private fun JourneyResultRow(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    AppCard(cornerRadius = 18) {
        Row(
            modifier = rowModifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = AccessibilityColors.TextMuted
                )
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AccessibilityColors.TextStrong
                )
            }
            if (onClick != null) {
                Icon(
                    AccessibilityIcons.ChevronStart,
                    contentDescription = null,
                    tint = AccessibilityColors.TextFaint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** A quiet, borderless journey line — e.g. the cooldown statement. No picker, no chevron. */
@Composable
private fun JourneyQuietLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            AccessibilityIcons.Schedule,
            contentDescription = null,
            tint = AccessibilityColors.TextFaint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = AccessibilityColors.TextMuted
        )
    }
}

/** Section label above a journey row group ("ההודעה", "התזכורת", "כרטיס הביקור"). */
@Composable
private fun JourneySectionLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        color = AccessibilityColors.Heading,
        modifier = Modifier.fillMaxWidth()
    )
}

// Wiring Pass: a minimal single-choice picker used by the journey rows (recipient scope, ask-
// before-send). Options are (value, label) pairs; picking one calls onSelect and closes. Design is
// intentionally throwaway — the UI is scheduled to be re-skinned; this just makes the choice work.
@Composable
private fun <T> JourneyOptionPickerDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = AccessibilityColors.Heading) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    RadioRow(
                        label = label,
                        selected = value == selected,
                        onClick = { onSelect(value) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("סגור", color = AccessibilityColors.Primary)
            }
        }
    )
}

// ===================== SCREEN 2 — "אם לא עניתי" (Design Pass 2) =====================
// A stand-alone journey page (deep-link safe). Each row is a result that stands on its own —
// no summary, no chips. Result-language only (Golden Rule): "מי יקבל את ההודעה?" answers with
// "לכל מי שמתקשר", the send-confirm row asks a decision, not "how it's sent". Display-only:
// values are read from the existing stores; the chevrons don't open pickers until the wiring pass.
@Composable
private fun MissedJourneyScreen(
    isEnabled: Boolean,
    missedBody: String,
    signature: String,
    recipientLabel: String,
    askBeforeSend: Boolean,
    channelLabel: String,
    onToggleEnabled: () -> Unit,
    onEditMessage: () -> Unit,
    onEditRecipient: () -> Unit,
    onEditAskBeforeSend: () -> Unit,
    onEditChannel: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModalHeader(title = "אם לא עניתי", onBack = onBack)

        // 1 — isEnabled exists in the engine, so it must be visible (§2). Off means clients who
        // don't reach you get nothing; hiding that would be the exact false belief §2 forbids.
        JourneySwitchRow(
            label = "פועל",
            checked = isEnabled,
            onToggle = onToggleEnabled
        )

        // 2 — the message itself, dimmed when off so the screen shows what is actually happening.
        JourneySectionLabel("ההודעה")
        MessageWithSignaturePreview(
            body = missedBody,
            signature = signature,
            dimmed = !isEnabled,
            onEditBody = onEditMessage
        )
        if (!isEnabled) {
            JourneyQuietLine("לקוחות שלא נענו לא יקבלו הודעה.")
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 3 — who receives it (result, not "recipient scope").
        JourneyResultRow(title = "מי יקבל את ההודעה?", value = recipientLabel, onClick = onEditRecipient)
        // 4 — the confirm-before-send decision, phrased as a result.
        JourneyResultRow(
            title = "האם לאשר לפני שליחה?",
            value = if (askBeforeSend) "כן, אאשר כל הודעה" else "לא, תישלח גם בלי אישורי",
            onClick = onEditAskBeforeSend
        )
        // 5 — the channel, now an explicit choice. See FollowUpChannelSettings: each option
        // stores a flag pair with fallback OFF, so the chosen channel is the channel used.
        JourneyResultRow(title = "באיזה ערוץ?", value = channelLabel, onClick = onEditChannel)

        Spacer(modifier = Modifier.height(2.dp))

        // 6 — cooldown, stated quietly. No picker (⚪ — not a decision the user should carry).
        JourneyQuietLine("לא נשלח שוב לאותו אדם במשך יממה.")
    }
}

/**
 * The message exactly as the client will receive it: an editable body plus the signature line,
 * always visible and always locked.
 *
 * Locked-but-visible is deliberate (§2): the signature is part of what the client gets, so hiding
 * it during editing would show the user less than the truth. It is not editable here because it is
 * identity, not wording — it is changed by editing the profile, in one place.
 */
@Composable
private fun MessageWithSignaturePreview(
    body: String,
    signature: String,
    dimmed: Boolean,
    onEditBody: () -> Unit
) {
    val colors = AccessibilityExtra.colors
    val alpha = if (dimmed) 0.45f else 1f
    Surface(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
        color = colors.bubbleGreen.copy(alpha = alpha),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = body.ifBlank { " " },
                color = colors.textStrong.copy(alpha = alpha),
                fontSize = 18.sp,
                lineHeight = 29.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditBody)
            )
            if (signature.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = signature,
                        color = colors.textMuted.copy(alpha = alpha),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        AccessibilityIcons.Lock,
                        contentDescription = "שורת החתימה נקבעת מהפרטים שלך",
                        tint = colors.textFaint.copy(alpha = alpha),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/** A journey row carrying a real on/off switch — used for the missed journey's "פועל". */
@Composable
private fun JourneySwitchRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    AppCard(cornerRadius = 18) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = AccessibilityColors.TextStrong
            )
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

// ===================== SCREEN 3 — "אחרי שדיברנו" (Design Pass 3) =====================
// Configure only — never Act. The ended send happens in a notification 5-10s after a call, not
// here. So this page has no "שלח", no client picker, no CTA. It only defines what will be sent:
// the reminder wording and the business card. "הודעה" becomes "תזכורת" in the display lexicon.
// Display-only: "שנה"/"ערוך" are inert until the wiring pass.
@Composable
private fun EndedJourneyScreen(
    reminderBody: String,
    card: ContactCard,
    cardAttached: Boolean,
    scopeLabel: String,
    channelLabel: String,
    onEditMessage: () -> Unit,
    onEditCard: () -> Unit,
    onToggleCardAttached: () -> Unit,
    onEditScope: () -> Unit,
    onEditChannel: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModalHeader(title = "אחרי שדיברנו", onBack = onBack)

        // 1+2 — the follow-up message with the card sitting inside it, exactly as it will arrive.
        // The card is part of the message, so it is shown inside the same bubble, not beside it.
        JourneySectionLabel("הודעת ההמשך")
        EndedMessagePreview(
            body = reminderBody,
            card = card,
            cardAttached = cardAttached,
            onEditBody = onEditMessage,
            onEditCard = onEditCard,
            onToggleCardAttached = onToggleCardAttached
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 3 — which conversations get an offer. Not "who receives" — nobody receives anything
        // automatically here; this only controls when the suggestion appears.
        JourneyResultRow(
            title = "אחרי אילו שיחות להציע לשלוח?",
            value = scopeLabel,
            onClick = onEditScope
        )
        // 4 — the channel, a separate setting from the missed moment's.
        JourneyResultRow(title = "באיזה ערוץ?", value = channelLabel, onClick = onEditChannel)
    }
}

/**
 * The follow-up message as the client receives it: an editable body, and the business card woven
 * in as the closing line.
 *
 * The "מצורף" switch sits *on the card, inside the bubble* — flipping it removes the card from the
 * preview live, so the user watches the message become what will actually be sent. A separate
 * "האם לצרף כרטיס?" row would describe that instead of showing it.
 */
@Composable
private fun EndedMessagePreview(
    body: String,
    card: ContactCard,
    cardAttached: Boolean,
    onEditBody: () -> Unit,
    onEditCard: () -> Unit,
    onToggleCardAttached: () -> Unit
) {
    val colors = AccessibilityExtra.colors
    Surface(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
        color = colors.bubbleGreen,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = body.ifBlank { " " },
                color = colors.textStrong,
                fontSize = 18.sp,
                lineHeight = 29.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditBody)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // The card itself, tappable to edit its fields. When detached it disappears
            // from the message and only the switch line remains, so the toggle stays reachable.
            if (cardAttached) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEditCard)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = card.fullName.ifBlank { "הוסף את שמך" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (card.fullName.isBlank()) {
                                colors.textFaint
                            } else {
                                colors.textStrong
                            }
                        )
                        if (card.org.isNotBlank()) {
                            Text(
                                text = card.org,
                                fontSize = 14.sp,
                                color = colors.textMuted
                            )
                        }
                        if (card.phone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    AccessibilityIcons.Call,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = card.phone,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "מצורף",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.textMuted
                )
                Switch(checked = cardAttached, onCheckedChange = { onToggleCardAttached() })
            }
        }
    }
}

// ===================== SCREEN 1 — HOME =====================
// Design Pass 1 — "גרסה C": the home no longer tells a product/status story. It carries
// identity: the two trust moments ("אם לא עניתי" / "אחרי שדיברנו") in result-language. No badge
// and no state label — "automatic" is a mechanism, not a result (Golden Rule), and the
// automatic/manual distinction is learned on entering the journey, not on Home (rule 3). The old
// status/quick-send blocks are gone; the bridging-enable line is kept as a single quiet line at
// the bottom, because a disabled bridge the user is unaware of is a false belief that messages
// are being sent (§2).
@Composable
private fun HomeScreen(
    warning: HomeWarning?,
    missedEnabled: Boolean,
    missedBody: String,
    endedBody: String,
    signature: String,
    cardAttached: Boolean,
    cardInitials: String,
    cardLine1: String,
    onToggleEnabled: () -> Unit,
    onOpenMissedJourney: () -> Unit,
    onOpenEndedJourney: () -> Unit,
    onResolveWarning: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    val colors = AccessibilityExtra.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header — greeting + ⚙️ (HOME.html .header). ⚙️ is present but not part of
        // navigation — 95% of the time nobody needs it.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("שלום, דני 👋", fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = colors.heading)
                Text(
                    "מערכת הפולואפ פעילה",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAEAEA)),
                shadowElevation = 2.dp
            ) {
                Icon(
                    AccessibilityIcons.Settings,
                    contentDescription = "הגדרות מערכת",
                    tint = colors.textMuted,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onOpenSystemSettings)
                        .padding(11.dp)
                )
            }
        }

        // Shown only when something is actually broken; otherwise Home stays silent.
        warning?.let {
            HomeWarningRow(text = HomeWarningLogic.message(it), onClick = onResolveWarning)
        }

        // Master toggle card (HOME.html .master-card) — the real bridging on/off switch.
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.heading,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("מצב אוטומטי", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        if (missedEnabled) "האפליקציה עובדת ברקע" else "כבוי — לקוחות לא יקבלו הודעה",
                        color = colors.green,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
                Switch(checked = missedEnabled, onCheckedChange = { onToggleEnabled() })
            }
        }

        // Stats — not-wired example numbers (plan v2 §3: no DB-backed counters yet).
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HomeStatBox(value = "4", label = "לקוחות ניצלו היום", modifier = Modifier.weight(1f))
            HomeStatBox(value = "12", label = "הודעות נשלחו השבוע", modifier = Modifier.weight(1f))
        }

        Text("ההודעות שלך", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = colors.heading)

        // Home is a mirror: each card shows the real message, not a sentence about it.
        HomeMomentCard(
            emoji = "📞",
            title = "אם לא עניתי",
            body = missedBody,
            signature = signature,
            dimmed = !missedEnabled,
            footnote = if (missedEnabled) null else "לקוחות שלא נענו לא יקבלו הודעה",
            card = null,
            onOpen = onOpenMissedJourney
        )

        HomeMomentCard(
            emoji = "🤝",
            title = "אחרי שדיברנו",
            body = endedBody,
            signature = "",
            dimmed = false,
            footnote = null,
            card = if (cardAttached) HomeCardPreview(initials = cardInitials, line1 = cardLine1) else null,
            onOpen = onOpenEndedJourney
        )
    }
}

private data class HomeCardPreview(val initials: String, val line1: String)

@Composable
private fun HomeStatBox(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = AccessibilityExtra.colors
    AppCard(modifier = modifier, cornerRadius = 20) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = colors.primary)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textMuted)
        }
    }
}

/** The ⚠️ line: what the client is experiencing, and the way to fix it. */
@Composable
private fun HomeWarningRow(text: String, onClick: () -> Unit) {
    val colors = AccessibilityExtra.colors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.dangerBg,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "⚠️", fontSize = 16.sp)
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = colors.danger,
                modifier = Modifier.weight(1f)
            )
            Icon(
                AccessibilityIcons.ChevronStart,
                contentDescription = null,
                tint = colors.danger,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * A home moment card: the moment's name, and beneath it the actual message a client receives.
 *
 * Deliberately carries no explanatory subtitle, no state label and no counters — the message is
 * the thing itself, and a sentence describing it would only compete with it. Tapping zooms into
 * the same artifact, larger and editable.
 */
@Composable
private fun HomeMomentCard(
    emoji: String,
    title: String,
    body: String,
    signature: String,
    dimmed: Boolean,
    footnote: String?,
    card: HomeCardPreview?,
    onOpen: () -> Unit
) {
    val colors = AccessibilityExtra.colors
    val alpha = if (dimmed) 0.45f else 1f
    AppCard(cornerRadius = 24, background = colors.surface) {
        Column {
            // config-header: icon + title + "עריכה" (HOME.html .config-header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = emoji, fontSize = 19.sp)
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textStrong)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer,
                    modifier = Modifier.clickable(onClick = onOpen)
                ) {
                    Text(
                        "עריכה",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F0F0)))

            // chat-preview-zone: realistic bubble, matching the WhatsApp look exactly.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFEAE2))
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                    color = colors.bubbleGreen.copy(alpha = alpha),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = body.ifBlank { " " },
                            color = colors.textStrong.copy(alpha = alpha),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            maxLines = 4
                        )
                        if (signature.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = signature,
                                color = colors.textMuted.copy(alpha = alpha),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                maxLines = 1
                            )
                        }

                        // vCard-attachment element inside the bubble (HOME.html .vcard-attachment).
                        // Drawn exactly as designed but not-wired: WhatsApp blocks file-share to an
                        // unsaved number's chat, so the real send stays text-only (plan v2 §3).
                        card?.let {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surface)
                                    .border(1.dp, Color(0x0D000000), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFF17B3A3), Color(0xFF128C7E)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(it.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(it.line1, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textStrong)
                                    Text("איש קשר (.vcf)", fontSize = 12.sp, color = colors.textMuted)
                                }
                                Icon(
                                    AccessibilityIcons.ChevronStart,
                                    contentDescription = null,
                                    tint = Color(0xFF128C7E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("10:42", fontSize = 10.sp, color = Color(0xFF667781))
                            // ✓✓ static — no WhatsApp delivery-receipt access (plan v2 §3).
                            Text("✓✓", fontSize = 10.sp, color = AccessibilityColors.WaCheck)
                        }
                    }
                }
            }

            footnote?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = colors.textMuted,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Two-way toggle for the home quick-send: which call scenario's wording to prepare
 * (missed call vs. completed call). Mirrors the segmented look used elsewhere.
 */
@Composable
private fun TemplateRoleToggle(
    selected: TemplateRole,
    onSelect: (TemplateRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AccessibilityExtra.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.fieldGrey)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TemplateRole.entries.forEach { role ->
            val isSelected = role == selected
            val label = when (role) {
                TemplateRole.MISSED_CALL -> "שיחה שלא נענתה"
                TemplateRole.CALL_ENDED -> "סיום שיחה"
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) colors.primary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(role) }
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else colors.textBody,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WhatsAppMessagePreview(message: String, maxLines: Int = 4) {
    val colors = AccessibilityExtra.colors
    Surface(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
        color = colors.bubbleGreen,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message.ifBlank { " " },
            color = colors.textStrong,
            fontSize = 18.sp,
            lineHeight = 29.sp,
            maxLines = maxLines,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        )
    }
}

private fun homeServiceStatusIcon(kind: HomeServiceStatusKind): ImageVector =
    when (kind) {
        HomeServiceStatusKind.BRIDGE -> AccessibilityIcons.PhoneInTalk
        HomeServiceStatusKind.MODE -> AccessibilityIcons.CheckCircle
        HomeServiceStatusKind.WHATSAPP -> AccessibilityIcons.Chat
        HomeServiceStatusKind.SMS -> AccessibilityIcons.Sms
        HomeServiceStatusKind.SAFETY -> AccessibilityIcons.Lock
        HomeServiceStatusKind.DUPLICATE_PROTECTION -> AccessibilityIcons.Shield
    }

private fun homeServiceStatusIconTint(tone: HomeServiceStatusTone): Color =
    when (tone) {
        HomeServiceStatusTone.ACTIVE -> AccessibilityColors.GreenBright
        HomeServiceStatusTone.NEUTRAL -> AccessibilityColors.Primary
        HomeServiceStatusTone.DISABLED -> AccessibilityColors.TextMuted
    }

private fun homeServiceStatusTrailingIcon(tone: HomeServiceStatusTone): ImageVector =
    if (tone == HomeServiceStatusTone.DISABLED) AccessibilityIcons.Block else AccessibilityIcons.CheckCircle

private fun homeServiceStatusTrailingTint(tone: HomeServiceStatusTone): Color =
    when (tone) {
        HomeServiceStatusTone.ACTIVE -> AccessibilityColors.GreenBright
        HomeServiceStatusTone.NEUTRAL -> AccessibilityColors.Primary
        HomeServiceStatusTone.DISABLED -> AccessibilityColors.UnselectedIcon
    }

// The single status line's color: green when healthy, amber when a blocking problem
// needs the user's attention (plan §1 — one clear state, ranked by severity).
private fun homeServiceStatusLineColor(tone: HomeServiceStatusTone): Color =
    when (tone) {
        HomeServiceStatusTone.ACTIVE -> AccessibilityColors.GreenBright
        HomeServiceStatusTone.NEUTRAL -> AccessibilityColors.TextStrong
        HomeServiceStatusTone.DISABLED -> AccessibilityColors.Warning
    }

// ===================== SCREEN 2 — SETUP / CONSENT =====================
@Composable
private fun SetupConsentScreen(onEnable: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconBadge(
            icon = AccessibilityIcons.GppGood,
            background = AccessibilityColors.GreenContainer,
            tint = AccessibilityColors.Green,
            boxSize = 78,
            cornerRadius = 24,
            iconSize = 42
        )
        Text(
            text = "הפעלת גישור לשיחות",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 23.sp,
            color = AccessibilityColors.Heading
        )
        Text(
            text = "כדי להעביר שיחות שלא נענו לערוץ כתוב, צריך לאשר שליחה ב־WhatsApp או SMS.",
            fontSize = 16.sp,
            color = AccessibilityColors.TextBody,
            modifier = Modifier.fillMaxWidth()
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = AccessibilityColors.PrimaryTint,
            border = androidx.compose.foundation.BorderStroke(1.dp, AccessibilityColors.PrimaryTintBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(AccessibilityIcons.Lock, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(24.dp))
                Text(
                    text = "האפליקציה תשלח הודעה אוטומטית רק לאחר שיחה שלא נענתה, ורק לפי נוסח שבחרת מראש. אפשר לכבות את זה בכל רגע.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccessibilityColors.TextMuted
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        PillButton(text = "הפעל", onClick = onEnable)
        OutlinePillButton(text = "לא עכשיו", onClick = onDismiss)
    }
}

// ===================== SCREEN 3 — TEMPLATES =====================
/** A short one-line body preview for the template list. */
internal object TemplateCardSummary {
    fun bodyPreview(body: String): String =
        body.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .let { if (it.length > 70) it.take(70).trimEnd() + "…" else it }
}

@Composable
private fun TemplatesScreen(
    templates: List<MessageTemplate>,
    selectedEndedId: String,
    selectedMissedId: String,
    onSelectTemplate: (MessageTemplate) -> Unit,
    onSaveTemplate: (MessageTemplate) -> Unit,
    onAddTemplate: (String, String, String, String, TemplateRole) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onBack: () -> Unit
) {
    // null + creatingNew=false → list; editing an existing template → editing != null; new card → creatingNew.
    var editing by remember { mutableStateOf<MessageTemplate?>(null) }
    var creatingNew by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModalHeader(title = "הודעות ברירת מחדל", onBack = onBack)

        when {
            creatingNew || editing != null -> {
                val target = editing
                TemplateCardEditor(
                    template = target,
                    onSave = { title, body, cardLink, websiteLink, role ->
                        if (target == null) {
                            onAddTemplate(title, body, cardLink, websiteLink, role)
                        } else {
                            onSaveTemplate(
                                target.copy(
                                    title = title,
                                    body = body,
                                    cardLink = cardLink,
                                    websiteLink = websiteLink,
                                    role = role
                                )
                            )
                        }
                        creatingNew = false
                        editing = null
                    },
                    onCancel = {
                        creatingNew = false
                        editing = null
                    }
                )
            }
            else -> {
                Text(
                    text = "לכל תרחיש שיחה יש הודעת ברירת מחדל. בחר/י את הכרטיסייה שתישלח, או צור/י חדשה.",
                    fontSize = 14.sp,
                    color = AccessibilityColors.TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )
                // Grouped by scenario so the two defaults (missed / ended) are clearly separate.
                TemplateRole.entries.forEach { role ->
                    val group = templates.filter { it.role == role }
                    if (group.isEmpty()) return@forEach
                    val selectedIdForRole = if (role == TemplateRole.MISSED_CALL) selectedMissedId else selectedEndedId
                    Text(
                        text = templateRoleLabel(role),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccessibilityColors.Heading,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    group.forEach { template ->
                        TemplateCard(
                            template = template,
                            selected = template.id == selectedIdForRole,
                            canDelete = templates.size > 1,
                            onClick = { onSelectTemplate(template) },
                            onEdit = { editing = template },
                            onDelete = { onDeleteTemplate(template.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinePillButton(
                    text = "כרטיסייה חדשה",
                    onClick = { creatingNew = true },
                    borderColor = AccessibilityColors.Primary,
                    contentColor = AccessibilityColors.Primary,
                    leadingIcon = AccessibilityIcons.Add
                )
            }
        }
    }
}

/** Hebrew label for a template's call scenario. */
private fun templateRoleLabel(role: TemplateRole): String = when (role) {
    TemplateRole.MISSED_CALL -> "שיחה שלא נענתה"
    TemplateRole.CALL_ENDED -> "סיום שיחה"
}

@Composable
private fun TemplateCard(
    template: MessageTemplate,
    selected: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val hasLinks = template.cardLink.isNotBlank() || template.websiteLink.isNotBlank()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AccessibilityColors.GreenSurface else AccessibilityColors.SubtleSurface,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.5.dp,
            if (selected) AccessibilityColors.GreenCheck else AccessibilityColors.CardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(template.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccessibilityColors.TextStrong)
                    Text(
                        TemplateCardSummary.bodyPreview(template.body),
                        fontSize = 13.sp,
                        color = AccessibilityColors.TextMuted
                    )
                    if (hasLinks) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                AccessibilityIcons.Link,
                                contentDescription = null,
                                tint = AccessibilityColors.Primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text("כולל קישורים", fontSize = 12.sp, color = AccessibilityColors.Primary)
                        }
                    }
                }
                Icon(
                    if (selected) AccessibilityIcons.RadioChecked else AccessibilityIcons.RadioUnchecked,
                    contentDescription = null,
                    tint = if (selected) AccessibilityColors.GreenCheck else AccessibilityColors.UnselectedIcon,
                    modifier = Modifier.size(23.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.clickable(onClick = onEdit)
                ) {
                    Icon(AccessibilityIcons.Edit, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(17.dp))
                    Text("ערוך", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AccessibilityColors.Primary)
                }
                if (canDelete) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.clickable(onClick = onDelete)
                    ) {
                        Icon(AccessibilityIcons.Delete, contentDescription = null, tint = AccessibilityColors.Danger, modifier = Modifier.size(17.dp))
                        Text("מחק", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AccessibilityColors.Danger)
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCardEditor(
    template: MessageTemplate?,
    onSave: (String, String, String, String, TemplateRole) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember(template?.id) { mutableStateOf(template?.title.orEmpty()) }
    var body by remember(template?.id) { mutableStateOf(template?.body.orEmpty()) }
    var cardLink by remember(template?.id) { mutableStateOf(template?.cardLink.orEmpty()) }
    var websiteLink by remember(template?.id) { mutableStateOf(template?.websiteLink.orEmpty()) }
    var role by remember(template?.id) { mutableStateOf(template?.role ?: TemplateRole.CALL_ENDED) }
    var error by remember(template?.id) { mutableStateOf<String?>(null) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (template == null) "כרטיסייה חדשה" else "עריכת כרטיסייה",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AccessibilityColors.Heading
            )
            Text("מתי לשלוח את ההודעה?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccessibilityColors.TextMuted)
            TemplateRoleToggle(
                selected = role,
                onSelect = { role = it },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("כותרת") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("נוסח ההודעה") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cardLink,
                onValueChange = { cardLink = it },
                label = { Text("קישור לכרטיס דיגיטלי (לא חובה)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = websiteLink,
                onValueChange = { websiteLink = it },
                label = { Text("קישור לאתר (לא חובה)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            Text("תצוגה מקדימה", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccessibilityColors.TextMuted)
            WhatsAppMessagePreview(
                message = com.followupnadlan.templates.MessageComposition.build(body, cardLink, websiteLink),
                maxLines = 12
            )

            error?.let { Text(it, color = AccessibilityColors.Danger, fontSize = 13.sp) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(
                    text = "שמור",
                    onClick = {
                        val trimmedTitle = title.trim()
                        if (trimmedTitle.isBlank() || body.isBlank()) {
                            error = "כותרת ונוסח הם שדות חובה"
                            return@PillButton
                        }
                        onSave(trimmedTitle, body, cardLink.trim(), websiteLink.trim(), role)
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinePillButton(text = "ביטול", onClick = onCancel, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ===================== SCREEN 4 — MISSED CALL PROMPT =====================
@Composable
private fun MissedCallPromptScreen(
    phone: String,
    message: String,
    mode: FollowUpPromptMode,
    templates: List<MessageTemplate>,
    selectedEndedId: String,
    selectedMissedId: String,
    preferredWhatsAppPackage: String,
    primaryChannel: MissedCallResponsePrimaryChannel,
    whatsappAvailable: Boolean,
    askBeforeSend: Boolean,
    onOpenContactCardSettings: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    // Contact-card share is independent of the message send: it reuses the saved "my details"
    // and opens WhatsApp with a .vcf attached; the user picks the chat and taps send themselves.
    val shareContactCard = remember(context) { PrepareAndShareContactCard(context) }
    val contactCardComplete = remember(context) {
        ContactCard.fromProfile(MyDetailsStore(context.applicationContext).load()).isComplete
    }
    // Wording follows the call scenario: a missed call uses the "missed" cards, a completed
    // call the "call ended" cards.
    val role = if (mode == FollowUpPromptMode.MISSED_CALL) TemplateRole.MISSED_CALL else TemplateRole.CALL_ENDED
    val roleTemplates = templates.filter { it.role == role }
    val selectedIdForRole = if (role == TemplateRole.MISSED_CALL) selectedMissedId else selectedEndedId
    // Manual "ask me" mode with more than one card of this role lets the user pick a different
    // card for this send only (it does not change the saved default).
    val showSelector = askBeforeSend && roleTemplates.size > 1
    val defaultTemplate = TemplateRoleSelector.forRole(templates, role, selectedIdForRole)
    var activeId by remember(selectedIdForRole, role) { mutableStateOf(defaultTemplate?.id.orEmpty()) }
    val activeTemplate = roleTemplates.firstOrNull { it.id == activeId }
        ?: defaultTemplate
    val templateMessage = if (showSelector) {
        activeTemplate?.let { MessageComposition.build(it) }.orEmpty()
    } else {
        message.ifBlank { activeTemplate?.let { MessageComposition.build(it) }.orEmpty() }
    }
    // A per-call, in-place edit that is NOT saved to the store — it only tailors this one
    // send. It resets whenever the underlying template message changes (e.g. picking a chip).
    var localEdit by remember(templateMessage) { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    val resolvedMessage = localEdit ?: templateMessage
    val normalizedPhone = remember(phone) { PhoneNumberNormalizer.normalizeForWhatsApp(phone) }
    var status by remember { mutableStateOf<String?>(null) }

    // The primary button + header channel are one and the same (plan §1): resolve once, use
    // the same name in both. The card add-on is a completed-call action only (§4 / §3ה) — the
    // missed-call screen never shows it. In a completed call the message field is the main
    // action, so it opens for editing immediately (§3ד).
    val channelResolution = remember(primaryChannel, whatsappAvailable) {
        FollowUpChannelResolver.resolve(primaryChannel, whatsappAvailable)
    }
    val isMissed = mode == FollowUpPromptMode.MISSED_CALL
    val showContactCard = !isMissed
    val missingNumber = isMissed && normalizedPhone == null
    // "אפשרויות נוספות" offers the other channel for this one send.
    var showMoreOptions by remember { mutableStateOf(false) }

    fun openWhatsApp() {
        if (normalizedPhone == null || resolvedMessage.isBlank()) {
            status = "חסר מספר תקין או נוסח הודעה."
            return
        }
        val result = AccessibilityActions.openWhatsApp(
            context,
            WhatsAppLinkBuilder.build(normalizedPhone, resolvedMessage),
            preferredWhatsAppPackage
        )
        if (result == null) {
            AccessibilityActions.logEntry(context, com.followupnadlan.followuplog.FollowUpActionType.WHATSAPP_OPENED, resolvedMessage, phone)
            onDone()
        } else {
            status = result
        }
    }

    fun openSms() {
        if (phone.isBlank() || resolvedMessage.isBlank()) {
            status = "חסר מספר או נוסח הודעה."
            return
        }
        val result = AccessibilityActions.openSmsComposer(context, phone, resolvedMessage)
        if (result == null) {
            onDone()
        } else {
            status = result
        }
    }

    fun runPrimary() {
        when (channelResolution.channel) {
            FollowUpSendChannel.WHATSAPP -> openWhatsApp()
            FollowUpSendChannel.SMS -> openSms()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(
            icon = if (mode == FollowUpPromptMode.MISSED_CALL) AccessibilityIcons.PhoneMissed else AccessibilityIcons.PhoneInTalk,
            background = AccessibilityColors.PrimaryContainer,
            tint = AccessibilityColors.Primary,
            boxSize = 92,
            cornerRadius = 46,
            iconSize = 48
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(FollowUpPromptModeLogic.title(mode), fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = AccessibilityColors.Heading)

        // "רואים למי + באיזה ערוץ" (plan §1) — the recipient and the channel are the first
        // thing on the screen, and the channel name here is identical to the primary button.
        Spacer(modifier = Modifier.height(12.dp))
        Text("אל:", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AccessibilityColors.TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = phone.ifBlank { "מספר לא ידוע" },
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = AccessibilityColors.TextStrong
        )
        if (!missingNumber) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = if (channelResolution.channel == FollowUpSendChannel.WHATSAPP) AccessibilityIcons.Chat else AccessibilityIcons.Sms,
                    contentDescription = null,
                    tint = AccessibilityColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = channelResolution.channelName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AccessibilityColors.Primary
                )
            }
        }

        if (missingNumber) {
            // Missed call with no usable number → nothing can be sent; offer only to close (§4).
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "מספר לא ידוע — אי אפשר לשלוח",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AccessibilityColors.Danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinePillButton(text = "ביטול", onClick = onDone)
            return@Column
        }

        if (showSelector) {
            Spacer(modifier = Modifier.height(16.dp))
            TemplateChipRow(
                templates = roleTemplates,
                selectedId = activeId,
                onSelect = { activeId = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (isMissed) {
            // Missed call: the user wasn't in a conversation, so a generic message is enough —
            // the bubble shows it and editing stays a secondary action (§3ד distinction).
            WhatsAppMessagePreview(message = resolvedMessage, maxLines = 8)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinePillButton(
                text = "ערוך הודעה",
                onClick = { editing = true },
                borderColor = AccessibilityColors.Primary,
                contentColor = AccessibilityColors.Primary,
                leadingIcon = AccessibilityIcons.Edit
            )
        } else {
            // Completed call: the user just spoke and has context, so editing is the main action —
            // the field is open for immediate editing, not hidden behind a button (§3ד).
            OutlinedTextField(
                value = resolvedMessage,
                onValueChange = { localEdit = it },
                label = { Text("ההודעה שתישלח") },
                minLines = 4,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "עריכה זו מתאימה את ההודעה לשיחה הזו בלבד ואינה נשמרת כברירת מחדל.",
            fontSize = 12.sp,
            color = AccessibilityColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // One primary button matching the chosen channel (plan §1) — its label is identical to
        // the channel name shown in the header above.
        PillButton(
            text = channelResolution.primaryButtonText,
            onClick = { runPrimary() },
            background = if (channelResolution.channel == FollowUpSendChannel.WHATSAPP) AccessibilityColors.Green else AccessibilityColors.Primary,
            leadingIcon = if (channelResolution.channel == FollowUpSendChannel.WHATSAPP) AccessibilityIcons.Chat else AccessibilityIcons.Sms
        )

        // "אפשרויות נוספות" — the other channel for this one send only.
        Spacer(modifier = Modifier.height(11.dp))
        if (!showMoreOptions) {
            OutlinePillButton(
                text = "אפשרויות נוספות",
                onClick = { showMoreOptions = true },
                borderColor = AccessibilityColors.Primary,
                contentColor = AccessibilityColors.Primary
            )
        } else {
            val otherIsWhatsApp = channelResolution.channel == FollowUpSendChannel.SMS
            PillButton(
                text = if (otherIsWhatsApp) "פתח ${FollowUpChannelResolver.WHATSAPP_NAME}" else "פתח ${FollowUpChannelResolver.SMS_NAME}",
                onClick = { if (otherIsWhatsApp) openWhatsApp() else openSms() },
                background = AccessibilityColors.Primary,
                leadingIcon = if (otherIsWhatsApp) AccessibilityIcons.Chat else AccessibilityIcons.Sms
            )
        }

        // Contact-card share — completed-call flow only (§4 / §3ה): a separate clean vCard share.
        if (showContactCard) {
            Spacer(modifier = Modifier.height(11.dp))
            PillButton(
                text = "צרף את הכרטיס שלי",
                enabled = contactCardComplete,
                onClick = {
                    when (val result = shareContactCard(preferredWhatsAppPackage)) {
                        is ContactCardShareResult.Opened -> status = null
                        ContactCardShareResult.IncompleteProfile -> onOpenContactCardSettings()
                        is ContactCardShareResult.Failed -> status = result.userMessage
                    }
                },
                background = AccessibilityColors.Primary,
                leadingIcon = AccessibilityIcons.PersonAdd
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (contactCardComplete) {
                Text(
                    text = "ייפתח WhatsApp עם הכרטיס — בחר/י את השיחה ולחץ/י שלח.",
                    fontSize = 12.sp,
                    color = AccessibilityColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "מלא/י שם וטלפון בהפרטים שלי כדי לצרף כרטיס.",
                    fontSize = 12.sp,
                    color = AccessibilityColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinePillButton(
                    text = "מילוי פרטי הכרטיס",
                    onClick = onOpenContactCardSettings,
                    borderColor = AccessibilityColors.Primary,
                    contentColor = AccessibilityColors.Primary,
                    leadingIcon = AccessibilityIcons.PersonAdd
                )
            }
        }

        Spacer(modifier = Modifier.height(11.dp))
        OutlinePillButton(text = if (isMissed) "ביטול" else "לא הפעם", onClick = onDone)
        status?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(it, color = AccessibilityColors.Danger, fontSize = 14.sp)
        }
    }

    if (editing) {
        FollowUpMessageEditorDialog(
            message = resolvedMessage,
            onSave = { edited ->
                localEdit = edited
                editing = false
            },
            onCancel = { editing = false }
        )
    }
}

/**
 * Quick, per-call message editor for the follow-up prompt. Unlike the Home editor, the result
 * is NOT persisted — it only tailors the current send, so the copy makes that explicit.
 */
@Composable
private fun FollowUpMessageEditorDialog(
    message: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var draft by remember(message) { mutableStateOf(message) }
    var error by remember { mutableStateOf<String?>(null) }

    fun saveDraft() {
        if (draft.isBlank()) {
            error = "ההודעה לא יכולה להיות ריקה"
            return
        }
        onSave(draft)
    }

    AlertDialog(
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding(),
        onDismissRequest = onCancel,
        title = { Text("עריכת ההודעה לשיחה זו", fontWeight = FontWeight.Bold, color = AccessibilityColors.Heading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "השינוי מתאים את ההודעה לשיחה הנוכחית בלבד ואינו נשמר כברירת מחדל.",
                    color = AccessibilityColors.TextMuted,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        error = null
                    },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 310.dp)
                )
                error?.let { Text(it, color = AccessibilityColors.Danger, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = { saveDraft() }) {
                Text("שמור לשיחה זו", fontWeight = FontWeight.Bold, color = AccessibilityColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("ביטול", color = AccessibilityColors.TextBody)
            }
        }
    )
}

/** Horizontal, scrollable row of selectable template chips (by title). */
@Composable
private fun TemplateChipRow(
    templates: List<MessageTemplate>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        templates.forEach { template ->
            val selected = template.id == selectedId
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selected) AccessibilityColors.Primary else AccessibilityColors.SubtleSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (selected) AccessibilityColors.Primary else AccessibilityColors.CardBorder
                ),
                modifier = Modifier.clickable { onSelect(template.id) }
            ) {
                Text(
                    text = template.title,
                    color = if (selected) Color.White else AccessibilityColors.TextBody,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}

// ===================== SCREEN 5 — ACTIVITY =====================
@Composable
private fun ActivityScreen(logStore: FollowUpLogStore) {
    val context = LocalContext.current
    val nameByDigits = remember(context) {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ContactsPickerRepository(context.applicationContext).load()
                .associate { it.phone.filter(Char::isDigit).takeLast(9) to it.name }
        } else {
            emptyMap()
        }
    }
    val rows = ActivityFeed.rows(logStore.load()) { phone ->
        nameByDigits[phone.filter(Char::isDigit).takeLast(9)]
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        ScreenHeader(
            title = "מה קרה היום",
            trailingIcon = AccessibilityIcons.Today,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (rows.isEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "עדיין אין פעילות להיום. כאן יופיעו שיחות שלא נענו והודעות שנשלחו.",
                    fontSize = 14.sp,
                    color = AccessibilityColors.TextMuted,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                rows.forEach { row -> ActivityRowItem(row) }
            }
        }
    }
}

@Composable
private fun ActivityRowItem(row: ActivityRow) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEBF0F6))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Icon(row.icon, contentDescription = null, tint = row.iconTint, modifier = Modifier.size(23.dp))
                Column {
                    Text(row.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
                    if (row.contactName != null) {
                        Text(row.contactName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AccessibilityColors.TextMuted)
                    }
                    if (row.phone.isNotBlank()) {
                        Text(row.phone, fontSize = 12.sp, color = AccessibilityColors.TextFaint)
                    }
                }
            }
            Text(row.time, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AccessibilityColors.TextFaint)
        }
    }
}

// ===================== SCREEN 6 — SETTINGS =====================
@Composable
private fun SettingsScreen(
    askBeforeSend: Boolean,
    recipientScope: RecipientScope,
    preferredWhatsApp: WhatsAppChoice,
    showWhatsAppChoice: Boolean,
    smsFallback: Boolean,
    allowedPreview: RecipientPreview,
    exclusionsPreview: RecipientPreview,
    onSelectAskBeforeSend: (Boolean) -> Unit,
    onSelectRecipientScope: (RecipientScope) -> Unit,
    onSelectWhatsApp: (WhatsAppChoice) -> Unit,
    onToggleSmsFallback: () -> Unit,
    onOpenExclusions: () -> Unit,
    onOpenAllowedRecipients: () -> Unit,
    onOpenTemplates: () -> Unit,
    onDeleteHistory: () -> Unit,
    myDetailsStore: MyDetailsStore,
    diagnosticsSnapshot: CallDetectionDiagnosticsSnapshot
) {
    var deleted by remember { mutableStateOf(false) }
    var showDeleteHistoryDialog by remember { mutableStateOf(false) }
    if (showDeleteHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteHistoryDialog = false },
            title = { Text("למחוק את ההיסטוריה?") },
            text = {
                Text(
                    "הפעולה תמחק לצמיתות את היסטוריית השיחות והפעולות שנשמרו באפליקציה.\n\n" +
                        "לאחר המחיקה, מספרים שכבר הופיעו בעבר עשויים להיחשב שוב כמספרים חדשים."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteHistory()
                        deleted = true
                        showDeleteHistoryDialog = false
                    }
                ) {
                    Text("מחק היסטוריה", color = AccessibilityColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteHistoryDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(title = "הגדרות", trailingIcon = AccessibilityIcons.Settings, modifier = Modifier.padding(horizontal = 4.dp))

        // §4/§6: "הפרטים שלי" — first card, inline edit-in-place (replaces the diagnostics card,
        // which moves to "עזרה ותמיכה" at the bottom). Single source of truth: MyDetailsStore.
        MyDetailsInlineCard(store = myDetailsStore)

        // איך לשלוח?
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                SettingsSectionTitle("איך לשלוח?")
                RadioRow("שליחה אוטומטית", selected = !askBeforeSend, onClick = { onSelectAskBeforeSend(false) })
                RadioRow("שאל אותי לפני שליחה", selected = askBeforeSend, onClick = { onSelectAskBeforeSend(true) })
            }
        }

        // למי לשלוח?
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSectionTitle("למי לשלוח?", bottomPadding = 0)
                    Icon(AccessibilityIcons.Group, contentDescription = null, tint = AccessibilityColors.TextMuted, modifier = Modifier.size(21.dp))
                }
                RecipientScope.values().forEach { scope ->
                    RadioRow(
                        recipientScopeLabel(scope),
                        selected = recipientScope == scope,
                        onClick = { onSelectRecipientScope(scope) }
                    )
                }
                if (recipientScope == RecipientScope.ONLY_SELECTED) {
                    if (allowedPreview.count == 0) {
                        RecipientEmptyRow(
                            title = "רק למי שבחרתי",
                            prompt = "לא בחרת עדיין אנשי קשר.",
                            actionLabel = "בחר עכשיו",
                            onClick = onOpenAllowedRecipients
                        )
                    } else {
                        RecipientSummaryRow(
                            title = "רק למי שבחרתי",
                            preview = allowedPreview,
                            onClick = onOpenAllowedRecipients
                        )
                    }
                }
                if (exclusionsPreview.count == 0) {
                    NavigationRow(label = "למי לא לשלוח?", onClick = onOpenExclusions, topDivider = true)
                } else {
                    RecipientSummaryRow(
                        title = "למי לא לשלוח?",
                        preview = exclusionsPreview,
                        onClick = onOpenExclusions
                    )
                }
            }
        }

        // WhatsApp regular / business — only shown when both apps are installed.
        if (showWhatsAppChoice) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    RadioRow(
                        "WhatsApp רגיל",
                        selected = preferredWhatsApp == WhatsAppChoice.REGULAR,
                        onClick = { onSelectWhatsApp(WhatsAppChoice.REGULAR) },
                        leadingIcon = AccessibilityIcons.Chat,
                        leadingTint = AccessibilityColors.GreenBright
                    )
                    RadioRow(
                        "WhatsApp Business",
                        selected = preferredWhatsApp == WhatsAppChoice.BUSINESS,
                        onClick = { onSelectWhatsApp(WhatsAppChoice.BUSINESS) },
                        leadingIcon = AccessibilityIcons.Storefront,
                        leadingTint = AccessibilityColors.Green
                    )
                }
            }
        }

        // SMS fallback toggle
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleSmsFallback).padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(AccessibilityIcons.Sms, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(22.dp))
                    Text("SMS כגיבוי", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
                }
                Icon(
                    if (smsFallback) AccessibilityIcons.RadioChecked else AccessibilityIcons.RadioUnchecked,
                    contentDescription = null,
                    tint = if (smsFallback) AccessibilityColors.Primary else AccessibilityColors.UnselectedIcon,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Default message templates entry
        AppCard(modifier = Modifier.fillMaxWidth()) {
            NavigationRowContent(
                label = "הודעת ברירת מחדל",
                leadingIcon = AccessibilityIcons.Edit,
                leadingTint = AccessibilityColors.Primary,
                onClick = onOpenTemplates
            )
        }

        // (Contact-card editing lives inline in "הפרטים שלי" at the top of Settings — no separate
        // entry here. ContactCardScreen still exists as the IncompleteProfile fallback from the
        // post-call share flow, reached via onOpenContactCardSettings, not from Settings.)

        // Delete history
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showDeleteHistoryDialog = true }.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(AccessibilityIcons.Delete, contentDescription = null, tint = AccessibilityColors.Danger, modifier = Modifier.size(22.dp))
                    Text("מחק היסטוריה", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
                }
                Icon(AccessibilityIcons.ChevronStart, contentDescription = null, tint = AccessibilityColors.UnselectedIcon, modifier = Modifier.size(20.dp))
            }
        }
        if (deleted) {
            Text("ההיסטוריה נמחקה.", color = AccessibilityColors.Primary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }

        // §4/§6: diagnostics is the ONLY place technical data lives — moved from the top of the
        // page down to "עזרה ותמיכה". Same rows, verbatim; nothing deleted.
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SettingsSectionTitle("עזרה ותמיכה", bottomPadding = 4)
                DiagnosticRow(
                    "אירוע אחרון",
                    CallDetectionDiagnosticsLabels
                        .event(diagnosticsSnapshot.lastPhoneStateEvent.ifBlank { diagnosticsSnapshot.lastEvent })
                        .ifBlank { "אין עדיין" }
                )
                DiagnosticRow("מספר אחרון", diagnosticsSnapshot.lastIncomingNumber.ifBlank { "לא זוהה" })
                DiagnosticRow("שיחה שלא נענתה", diagnosticsSnapshot.lastMissedCallDetectedAtMillis.takeIf { it > 0L }?.toString() ?: "אין עדיין")
                DiagnosticRow("מקלט", if (diagnosticsSnapshot.receiverActive) "פעיל" else "לא נרשם")
                DiagnosticRow("שירות רקע", if (diagnosticsSnapshot.serviceActive) "פעיל" else "כבוי")
                DiagnosticRow("הרשאת מצב טלפון", if (diagnosticsSnapshot.readPhoneStateGranted) "מאושר" else "חסר")
                DiagnosticRow("הרשאת יומן שיחות", if (diagnosticsSnapshot.readCallLogGranted) "מאושר" else "חסר")
            }
        }
    }
}

// ===================== SETTINGS — MY DETAILS (inline edit) =====================
/**
 * §4/§6: "הפרטים שלי" at the top of Settings. Shows name / office / phone as plain values; tapping
 * any row opens edit-in-place (no navigation to another screen). Single source of truth is
 * [MyDetailsStore] — no separate store. Undo (§6): after a save, the previous profile is offered
 * back for one action ("בטל שינוי").
 */
@Composable
private fun MyDetailsInlineCard(store: MyDetailsStore) {
    var profile by remember { mutableStateOf(store.load()) }
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(profile.agentName) }
    var org by remember { mutableStateOf(profile.officeName) }
    var phone by remember { mutableStateOf(profile.phone) }
    // Undo target: the profile as it was before the last save (null = nothing to undo).
    var undoProfile by remember { mutableStateOf<MyDetailsProfile?>(null) }

    val isEmpty = profile.agentName.isBlank() && profile.officeName.isBlank() && profile.phone.isBlank()

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionTitle("הפרטים שלי", bottomPadding = 4)

            if (editing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("שם מלא") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("טלפון") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = org,
                    onValueChange = { org = it },
                    // "עיסוק", not "שם עסק": we want "עו״ד מקרקעין" — what the client needs in
                    // order to place you — not "לוי ושותפים", which means nothing to them.
                    label = { Text("עיסוק") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        text = "שמור",
                        onClick = {
                            undoProfile = profile
                            val saved = profile.copy(
                                agentName = name.trim(),
                                officeName = org.trim(),
                                phone = phone.trim()
                            )
                            store.save(saved)
                            profile = saved
                            name = saved.agentName; org = saved.officeName; phone = saved.phone
                            editing = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinePillButton(
                        text = "ביטול",
                        onClick = {
                            name = profile.agentName; org = profile.officeName; phone = profile.phone
                            editing = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (isEmpty) {
                Text("עדיין לא מילאת פרטים", fontSize = 14.sp, color = AccessibilityExtra.colors.textMuted)
                NavigationRow(
                    label = "מלא עכשיו",
                    onClick = {
                        name = profile.agentName; org = profile.officeName; phone = profile.phone
                        editing = true
                    },
                    topDivider = false
                )
            } else {
                MyDetailRow("שם", profile.agentName.ifBlank { "—" }) { editing = true }
                MyDetailRow("טלפון", profile.phone.ifBlank { "—" }) { editing = true }
                MyDetailRow("עיסוק", profile.officeName.ifBlank { "—" }) { editing = true }

                // Preview of the card as it will be shared, so editing and preview live in one place.
                val previewCard = ContactCard.fromProfile(profile)
                if (previewCard.isComplete) {
                    Text("תצוגה מקדימה", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccessibilityColors.TextMuted)
                    ContactCardPreview(previewCard)
                }
            }

            // Undo (§6): available for one action right after a save, unless we're mid-edit again.
            if (!editing) {
                undoProfile?.let { previous ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            store.save(previous)
                            profile = previous
                            name = previous.agentName; org = previous.officeName; phone = previous.phone
                            undoProfile = null
                        },
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AccessibilityIcons.Block, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(18.dp))
                        Text("בטל שינוי", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AccessibilityColors.Primary)
                    }
                }
            }
        }
    }
}

/** One read-only "label: value" row in "הפרטים שלי"; the whole row is tappable to edit in place. */
@Composable
private fun MyDetailRow(label: String, value: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = AccessibilityColors.TextMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AccessibilityColors.TextStrong, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
        Icon(AccessibilityIcons.Edit, contentDescription = "ערוך", tint = AccessibilityColors.Primary, modifier = Modifier.size(18.dp).padding(start = 6.dp))
    }
}

// ===================== SCREEN — ⚙️ SYSTEM SETTINGS (hidden) =====================
/**
 * System maintenance, reached only from the small ⚙️ on Home.
 *
 * Everything that *is* a journey decision (the message, who receives it, whether to confirm, which
 * channel) lives inside its journey — this screen deliberately does not repeat any of it. What
 * remains here is upkeep: permissions, privacy, the exclusion list, data, and diagnostics.
 *
 * System vocabulary is allowed here, because this is the one place a user comes looking for it.
 * There is no licence row and no language picker: MVP-1 is Hebrew-only, and offering a switch
 * that changes nothing would be a promise the app does not keep.
 */
@Composable
private fun SystemSettingsScreen(
    permissions: PermissionSnapshot,
    exclusionsPreview: RecipientPreview,
    diagnosticsSnapshot: CallDetectionDiagnosticsSnapshot,
    cooldownSettings: FollowUpCooldownSettings,
    onRequestPermissions: () -> Unit,
    onOpenSmartRules: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSupport: () -> Unit,
    onDeleteHistory: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    var sameNumberCooldown by remember { mutableStateOf(cooldownSettings.sameNumberCooldownMillis) }
    var globalQuiet by remember { mutableStateOf(cooldownSettings.globalQuietMillis) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AccessibilityExtra.colors.listBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModalHeader(title = "הגדרות", onBack = onBack)

        // Permissions — live state. This is the engine behind the ⚠️ line and the fault
        // notification, so it shows the real count, not a stored value.
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "הרשאות",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AccessibilityColors.Heading
                    )
                    Text(
                        PermissionStatusLogic.summary(permissions),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (PermissionStatusLogic.canAnswerCalls(permissions)) {
                            AccessibilityColors.Green
                        } else {
                            AccessibilityColors.Warning
                        }
                    )
                }

                // One row per permission, each stating what the user gets from it.
                PermissionStatusLogic.all.forEach { permission ->
                    val granted = permissions.isGranted(permission)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Icon(
                            if (granted) AccessibilityIcons.CheckCircle else AccessibilityIcons.Block,
                            contentDescription = null,
                            tint = if (granted) AccessibilityColors.Green else AccessibilityColors.TextFaint,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            PermissionStatusLogic.outcome(permission),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = AccessibilityColors.TextBody,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!PermissionStatusLogic.canAnswerCalls(permissions)) {
                    PillButton(text = "אפשר הרשאות", onClick = onRequestPermissions)
                }
            }
        }

        // Working hours + who never gets a message — gathered on one screen.
        AppCard(modifier = Modifier.fillMaxWidth()) {
            NavigationRowContent(
                label = if (exclusionsPreview.count == 0) {
                    "הגדרות חכמות"
                } else {
                    "הגדרות חכמות · ${exclusionsPreview.count} חסומים"
                },
                leadingIcon = AccessibilityIcons.Block,
                leadingTint = AccessibilityColors.Primary,
                onClick = onOpenSmartRules
            )
        }

        // ⚙️-list navigation to the two remaining screens (plan v2): history and support.
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                NavigationRowContent(
                    label = "יומן פעילות",
                    leadingIcon = AccessibilityIcons.Today,
                    leadingTint = AccessibilityColors.Primary,
                    onClick = onOpenHistory
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AccessibilityColors.CardBorder))
                NavigationRowContent(
                    label = "תמיכה ומערכת",
                    leadingIcon = AccessibilityIcons.WhatsApp,
                    leadingTint = AccessibilityColors.Green,
                    onClick = onOpenSupport
                )
            }
        }

        // Privacy, stated plainly: there is no server to talk about.
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingsSectionTitle("פרטיות", bottomPadding = 4)
                Text(
                    "כל הנתונים נשמרים במכשיר שלך בלבד. אין שרת, אין חשבון, " +
                        "ואין העברת מידע לשום גורם.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = AccessibilityColors.TextBody
                )
            }
        }

        // Timing of the follow-up suggestion. This is upkeep rather than a journey decision — the
        // journey decides *what* to send and to whom; this decides how often the app is allowed to
        // ask. Both brakes can be switched off entirely: a user who wants a suggestion after every
        // call is choosing more noise on purpose, and the suggestion is silent either way.
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingsSectionTitle("תזמון ההצעות", bottomPadding = 4)

                Text(
                    "כל כמה זמן להציע שוב על אותו מספר",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccessibilityColors.TextStrong
                )
                FollowUpCooldownOptions.sameNumber.forEach { choice ->
                    RadioRow(
                        label = choice.label,
                        selected = sameNumberCooldown == choice.millis,
                        onClick = {
                            sameNumberCooldown = choice.millis
                            cooldownSettings.sameNumberCooldownMillis = choice.millis
                        }
                    )
                }

                Text(
                    "מרווח מינימלי בין הצעות",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccessibilityColors.TextStrong,
                    modifier = Modifier.padding(top = 8.dp)
                )
                FollowUpCooldownOptions.globalQuiet.forEach { choice ->
                    RadioRow(
                        label = choice.label,
                        selected = globalQuiet == choice.millis,
                        onClick = {
                            globalQuiet = choice.millis
                            cooldownSettings.globalQuietMillis = choice.millis
                        }
                    )
                }

                if (sameNumberCooldown == null && globalQuiet == null) {
                    Text(
                        "כל שיחה תייצר הצעה — כולל טעויות חיוג ומוקדים.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = AccessibilityColors.TextFaint,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteDialog = true }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(
                        AccessibilityIcons.Delete,
                        contentDescription = null,
                        tint = AccessibilityColors.Danger,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "מחק היסטוריה",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AccessibilityColors.TextStrong
                    )
                }
                Icon(
                    AccessibilityIcons.ChevronStart,
                    contentDescription = null,
                    tint = AccessibilityColors.UnselectedIcon,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (deleted) {
            Text(
                "ההיסטוריה נמחקה.",
                color = AccessibilityColors.Primary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Diagnostics: the only place technical detail belongs.
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SettingsSectionTitle("עזרה ותמיכה", bottomPadding = 4)
                DiagnosticRow(
                    "אירוע אחרון",
                    CallDetectionDiagnosticsLabels
                        .event(diagnosticsSnapshot.lastPhoneStateEvent.ifBlank { diagnosticsSnapshot.lastEvent })
                        .ifBlank { "אין עדיין" }
                )
                DiagnosticRow("מספר אחרון", diagnosticsSnapshot.lastIncomingNumber.ifBlank { "לא זוהה" })
                DiagnosticRow("מקלט", if (diagnosticsSnapshot.receiverActive) "פעיל" else "לא נרשם")
                DiagnosticRow("שירות רקע", if (diagnosticsSnapshot.serviceActive) "פעיל" else "כבוי")
            }
        }

        Text(
            "FollowUp · גרסה ${com.followupnadlan.BuildConfig.VERSION_NAME}",
            fontSize = 12.sp,
            color = AccessibilityColors.TextFaint,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("למחוק את ההיסטוריה?", fontWeight = FontWeight.Bold) },
            text = { Text("הפעולה אינה הפיכה.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteHistory()
                    deleted = true
                    showDeleteDialog = false
                }) {
                    Text("מחק", fontWeight = FontWeight.Bold, color = AccessibilityColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("ביטול", color = AccessibilityColors.TextBody)
                }
            }
        )
    }
}

// ===================== SCREEN — THE BUSINESS CARD (3 fields) =====================
/**
 * Edits the three fields that make up the signature line: name · occupation · phone.
 *
 * Saves to [MyDetailsStore] — the same single source of truth the rest of the app reads, so a
 * change here updates the missed message, the ended message and Home at once. It shows a live
 * preview of the resulting line rather than describing it.
 *
 * "עיסוק" (not "שם עסק") is the deliberate wording: the client needs "עו״ד מקרקעין" to place who
 * called them, not the firm's registered name.
 */
@Composable
private fun SignatureCardEditorScreen(
    store: MyDetailsStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(store.load()) }
    var fullName by remember { mutableStateOf(profile.agentName) }
    var occupation by remember { mutableStateOf(profile.officeName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var website by remember { mutableStateOf(profile.website) }
    var contactsGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_CONTACTS)) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> contactsGranted = granted }

    fun persist() {
        val trimmed = profile.copy(
            agentName = fullName.trim(),
            officeName = occupation.trim(),
            phone = phone.trim(),
            website = website.trim()
        )
        store.save(trimmed)
        profile = trimmed
    }

    val initials = fullName.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.take(1) }.ifBlank { "?" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModalHeader(title = "הגדרות פרופיל", onBack = { persist(); onBack() })

        // Avatar header (profile.html .profile-header) — initials update live as you type.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF17B3A3), Color(0xFF128C7E)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
                }
                // Photo edit — drawn as designed, not wired (no camera/avatar-upload feature).
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AccessibilityColors.Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("הפרטים שלך", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AccessibilityColors.Heading)
            Text("יופיעו בכרטיס הביקור ובהודעות", fontSize = 13.sp, color = AccessibilityColors.TextMuted)
        }

        AppCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("📝 פרטים אישיים", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AccessibilityColors.Heading)
                LabeledField(label = "שם מלא (יופיע ב-[השם שלי])", value = fullName, onChange = { fullName = it })
                LabeledField(label = "תפקיד / מקצוע (יופיע ב-[תפקיד] ו-[שם העסק])", value = occupation, onChange = { occupation = it })
                LabeledField(
                    label = "מספר טלפון לכרטיס הביקור",
                    value = phone,
                    onChange = { phone = it },
                    keyboardType = KeyboardType.Phone
                )
                LabeledField(
                    label = "אתר (אופציונלי)",
                    value = website,
                    onChange = { website = it },
                    keyboardType = KeyboardType.Uri
                )
            }
        }

        // Permissions card (profile.html §"הרשאות מערכת") — live-granted status, real request.
        AppCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🛡️ הרשאות מערכת", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AccessibilityColors.Heading)
                Text(
                    "כדי שהאפליקציה תוכל לזהות שיחות ולשלוח הודעות, עליך לאשר את ההרשאות הבאות:",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = AccessibilityColors.TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ProfilePermissionRow(
                    emoji = "📞",
                    title = "זיהוי שיחות",
                    description = "מזהה מתי פספסת שיחה",
                    granted = context.hasPermission(Manifest.permission.READ_PHONE_STATE),
                    onRequest = null
                )
                ProfilePermissionRow(
                    emoji = "💬",
                    title = "שליחת SMS",
                    description = "לשליחת הודעת ההמשך",
                    granted = context.hasPermission(Manifest.permission.SEND_SMS),
                    onRequest = null
                )
                ProfilePermissionRow(
                    emoji = "📇",
                    title = "אנשי קשר (מומלץ)",
                    description = "כדי לא לשלוח לאנשים שכבר שמורים",
                    granted = contactsGranted,
                    onRequest = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                )
            }
        }

        PillButton(text = "שמור שינויים", onClick = { persist(); onBack() })
        Text(
            "FollowUp · גרסה ${com.followupnadlan.BuildConfig.VERSION_NAME}",
            fontSize = 11.sp,
            color = AccessibilityColors.TextFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccessibilityColors.TextMuted)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProfilePermissionRow(emoji: String, title: String, description: String, granted: Boolean, onRequest: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AccessibilityColors.FieldGrey),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 16.sp)
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
                Text(description, fontSize = 12.sp, color = AccessibilityColors.TextMuted)
            }
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = if (granted) AccessibilityExtra.colors.bubbleGreen else Color(0xFFFFEAE8),
            modifier = if (!granted && onRequest != null) Modifier.clickable(onClick = onRequest) else Modifier
        ) {
            Text(
                if (granted) "✔ מאושר" else "הענק הרשאה",
                color = if (granted) Color(0xFF2A5A15) else AccessibilityColors.Danger,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// ===================== SCREEN — MY CONTACT CARD =====================
@Composable
private fun ContactCardScreen(
    store: MyDetailsStore,
    onBack: () -> Unit
) {
    // Base profile; copy() preserves the other MyDetails fields on save/clear. Mutable so a save
    // can refresh it before we return to the caller (post-call flow).
    var profile by remember { mutableStateOf(store.load()) }
    var fullName by remember { mutableStateOf(profile.agentName) }
    var org by remember { mutableStateOf(profile.officeName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    val card = ContactCard(fullName = fullName, org = org, phone = phone)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModalHeader(title = "כרטיס איש הקשר שלי", onBack = onBack)
        Text(
            text = "מלא/י את הפרטים פעם אחת כדי שאפשר יהיה לשלוח אותם ככרטיס איש קשר אחרי שיחה. " +
                "הפרטים נשמרים במכשיר בלבד.",
            fontSize = 14.sp,
            color = AccessibilityExtra.colors.textMuted,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; savedMessage = null },
            label = { Text("שם מלא") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; savedMessage = null },
            label = { Text("טלפון") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = org,
            onValueChange = { org = it; savedMessage = null },
            label = { Text("עיסוק") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("תצוגה מקדימה", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccessibilityColors.TextMuted)
        ContactCardPreview(card)
        Text(
            text = "כך ייראה הכרטיס אצל מי שיקבל אותו.",
            fontSize = 12.sp,
            color = AccessibilityColors.TextMuted,
            modifier = Modifier.fillMaxWidth()
        )

        if (!card.isComplete) {
            CaptionWithIcon(
                text = "כדי לשלוח כרטיס צריך לפחות שם וטלפון.",
                icon = AccessibilityIcons.Block,
                color = AccessibilityColors.TextMuted
            )
        }

        PillButton(
            text = "שמור",
            onClick = {
                val trimmed = profile.copy(
                    agentName = fullName.trim(),
                    officeName = org.trim(),
                    phone = phone.trim()
                )
                store.save(trimmed)
                profile = trimmed
                fullName = trimmed.agentName
                org = trimmed.officeName
                phone = trimmed.phone
                // When we got here to complete a missing profile (post-call share), a complete
                // save returns to the flow that sent us; otherwise just confirm in place.
                if (ContactCard.fromProfile(trimmed).isComplete) onBack() else savedMessage = "פרטי הכרטיס נשמרו"
            }
        )
        OutlinePillButton(
            text = "מחק פרטי כרטיס",
            onClick = {
                store.save(profile.copy(agentName = "", officeName = "", phone = ""))
                fullName = ""
                org = ""
                phone = ""
                savedMessage = "פרטי הכרטיס נמחקו"
            },
            borderColor = AccessibilityColors.Danger,
            contentColor = AccessibilityColors.Danger,
            leadingIcon = AccessibilityIcons.Delete
        )
        savedMessage?.let {
            Text(it, color = AccessibilityColors.Primary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** A recipient's-eye preview of the contact card (name / occupation / phone / optional website). */
@Composable
private fun ContactCardPreview(card: ContactCard) {
    val colors = AccessibilityExtra.colors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.subtleSurface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircleAvatar(
                background = colors.primaryContainer,
                icon = AccessibilityIcons.Person,
                iconTint = colors.primary,
                boxSize = 46
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = card.fullName.ifBlank { "שם מלא" },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (card.fullName.isBlank()) colors.textFaint else colors.textStrong
                )
                if (card.org.isNotBlank()) {
                    Text(card.org, fontSize = 13.sp, color = colors.textMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(AccessibilityIcons.Call, contentDescription = null, tint = colors.primary, modifier = Modifier.size(13.dp))
                    Text(
                        text = card.phone.ifBlank { "טלפון" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (card.phone.isBlank()) colors.textFaint else colors.primary
                    )
                }
                if (card.website.isNotBlank()) {
                    Text(card.website, fontSize = 13.sp, color = colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = AccessibilityColors.TextMuted, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            color = AccessibilityColors.TextStrong,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.15f)
        )
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun applyCallDetectionServiceState(
    context: Context,
    bridgeEnabled: Boolean,
    readPhoneStateGranted: Boolean,
    readCallLogGranted: Boolean
) {
    val action = CallDetectionServiceLifecycle.actionFor(
        bridgeEnabled = bridgeEnabled,
        readPhoneStateGranted = readPhoneStateGranted,
        readCallLogGranted = readCallLogGranted
    )
    runCatching {
        when (action) {
            CallDetectionServiceAction.START -> CallDetectionService.start(context)
            CallDetectionServiceAction.STOP -> CallDetectionService.stop(context)
        }
    }
}

/** Records how many recipients were added/removed by a picker confirm, plus an exact undo. */
internal class RecipientMutation(
    val addedCount: Int,
    val removedCount: Int,
    val undo: () -> Unit
)

/**
 * Applies a multi-picker confirm to the target store: adds the newly-selected candidates and removes
 * the un-checked, currently-listed ones (by last-9-digits key). Returns a [RecipientMutation] whose
 * [RecipientMutation.undo] reverses exactly this change.
 */
private fun applyRecipientDiff(
    target: PickerTarget,
    added: List<ContactCandidate>,
    removedKeys: Set<String>,
    exclusionsStore: ExclusionsStore,
    allowedRecipientsStore: AllowedRecipientsStore
): RecipientMutation = when (target) {
    PickerTarget.EXCLUSIONS -> {
        val addedEntries = added.map { ExcludedRecipient(label = it.name, number = it.phone) }
        val removedEntries = exclusionsStore.load()
            .filter { it.number.isNotBlank() && removedKeys.contains(recipientKey(it.number)) }
        addedEntries.forEach { exclusionsStore.add(it) }
        removedEntries.forEach { exclusionsStore.remove(it) }
        RecipientMutation(addedEntries.size, removedEntries.size) {
            addedEntries.forEach { exclusionsStore.remove(it) }
            removedEntries.forEach { exclusionsStore.add(it) }
        }
    }
    PickerTarget.ALLOWED -> {
        val addedEntries = added.map { AllowedRecipient(label = it.name, number = it.phone) }
        val removedEntries = allowedRecipientsStore.load()
            .filter { it.number.isNotBlank() && removedKeys.contains(recipientKey(it.number)) }
        addedEntries.forEach { allowedRecipientsStore.add(it) }
        removedEntries.forEach { allowedRecipientsStore.remove(it) }
        RecipientMutation(addedEntries.size, removedEntries.size) {
            addedEntries.forEach { allowedRecipientsStore.remove(it) }
            removedEntries.forEach { allowedRecipientsStore.add(it) }
        }
    }
}

/** Undo snackbar text for a recipient change; null when nothing changed. */
private fun recipientChangeMessage(added: Int, removed: Int): String? = when {
    added > 0 && removed == 0 -> if (added == 1) "נוסף איש קשר אחד" else "נוספו $added אנשי קשר"
    added == 0 && removed > 0 -> if (removed == 1) "הוסר איש קשר אחד" else "הוסרו $removed אנשי קשר"
    added > 0 && removed > 0 -> "הרשימה עודכנה"
    else -> null
}

@Composable
private fun SettingsSectionTitle(title: String, bottomPadding: Int = 10) {
    Text(
        title,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = AccessibilityColors.Heading,
        modifier = Modifier.padding(bottom = bottomPadding.dp)
    )
}

@Composable
private fun NavigationRow(label: String, onClick: () -> Unit, topDivider: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (topDivider) {
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider(color = Color(0xFFEEF2F7))
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccessibilityColors.Primary)
            Icon(AccessibilityIcons.ChevronStart, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RecipientRowDivider() {
    Spacer(modifier = Modifier.height(8.dp))
    androidx.compose.material3.HorizontalDivider(color = Color(0xFFEEF2F7))
    Spacer(modifier = Modifier.height(8.dp))
}

/** Settings row showing a recipient list summary: "title (N)" + names + "+N נוספים" + [ערוך]. */
@Composable
private fun RecipientSummaryRow(title: String, preview: RecipientPreview, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RecipientRowDivider()
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$title (${preview.count})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccessibilityColors.Primary
                )
                if (preview.names.isNotEmpty()) {
                    Text(
                        preview.names.joinToString(" • "),
                        fontSize = 13.sp,
                        color = AccessibilityColors.TextMuted
                    )
                }
                if (preview.moreCount > 0) {
                    Text(
                        "+${preview.moreCount} נוספים",
                        fontSize = 12.sp,
                        color = AccessibilityColors.TextFaint
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ערוך", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccessibilityColors.Primary)
                Icon(AccessibilityIcons.ChevronStart, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Settings row for the "ONLY_SELECTED but empty" state: prompt + [בחר עכשיו]. */
@Composable
private fun RecipientEmptyRow(title: String, prompt: String, actionLabel: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        RecipientRowDivider()
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
        Text(prompt, fontSize = 13.sp, color = AccessibilityColors.TextMuted, modifier = Modifier.padding(top = 2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text(actionLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccessibilityColors.Primary)
            Icon(AccessibilityIcons.ChevronStart, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun NavigationRowContent(label: String, leadingIcon: ImageVector, leadingTint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(leadingIcon, contentDescription = null, tint = leadingTint, modifier = Modifier.size(22.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
        }
        Icon(AccessibilityIcons.ChevronStart, contentDescription = null, tint = AccessibilityColors.UnselectedIcon, modifier = Modifier.size(20.dp))
    }
}

// Result-language, in the user's voice — these are answers to "מי יקבל את ההודעה?", so they
// read as outcomes ("רק מי שלא שמור אצלי"), never as a mechanism name ("NON_CONTACTS_ONLY").
private fun recipientScopeLabel(scope: RecipientScope): String =
    when (scope) {
        RecipientScope.ANY_NUMBER -> "לכל מי שמתקשר"
        RecipientScope.CONTACTS_ONLY -> "רק אנשי הקשר שלי"
        RecipientScope.NON_CONTACTS_ONLY -> "רק מי שלא שמור אצלי"
        RecipientScope.ONLY_SELECTED -> "רק אנשים שאבחר"
    }

/** The same four choices phrased for the ended moment, which offers rather than sends. */
private fun endedScopeLabel(scope: RecipientScope): String =
    when (scope) {
        RecipientScope.ANY_NUMBER -> "אחרי כל שיחה"
        RecipientScope.CONTACTS_ONLY -> "רק אנשי הקשר שלי"
        RecipientScope.NON_CONTACTS_ONLY -> "רק מי שלא שמור אצלי"
        RecipientScope.ONLY_SELECTED -> "רק אנשים שאבחר"
    }

/** The delivery channel, named as the client experiences it. */
private fun channelLabel(channel: FollowUpChannel): String =
    when (channel) {
        FollowUpChannel.WHATSAPP -> "WhatsApp"
        FollowUpChannel.WHATSAPP_BUSINESS -> "WhatsApp Business"
        FollowUpChannel.SMS -> "SMS"
    }

private fun blockedGroupLabel(group: BlockedRecipientGroup): String =
    when (group) {
        BlockedRecipientGroup.CONTACTS -> "אנשי קשר"
        BlockedRecipientGroup.NON_CONTACTS -> "מספרים לא שמורים"
        BlockedRecipientGroup.FIRST_TIME -> "מספרים חדשים — פעם ראשונה"
    }

// ===================== SCREEN 7 — EXCLUSIONS =====================
@Composable
private fun ExclusionsScreen(
    store: ExclusionsStore,
    recipientScope: RecipientScope,
    onAddFromContacts: () -> Unit,
    onAddFromRecent: () -> Unit,
    onBack: () -> Unit
) {
    var entries by remember { mutableStateOf(store.load()) }
    var blockedGroups by remember { mutableStateOf(store.loadBlockedGroups()) }
    var addingNumber by remember { mutableStateOf(false) }
    var numberDraft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModalHeader(title = "למי לא לשלוח?", onBack = onBack)
        Text(
            text = "בחר/י אנשים או מספרים שהאפליקציה לא תשלח להם הודעה אוטומטית.",
            fontSize = 14.sp,
            color = AccessibilityColors.TextMuted,
            modifier = Modifier.fillMaxWidth()
        )

        val visibleGroups = RecipientRulesUi.visibleBlockGroups(recipientScope)
        if (visibleGroups.isNotEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsSectionTitle("קבוצות חסומות", bottomPadding = 4)
                    visibleGroups.forEach { group ->
                        RadioRow(
                            blockedGroupLabel(group),
                            selected = blockedGroups.contains(group),
                            onClick = {
                                val next = !blockedGroups.contains(group)
                                store.setBlockedGroup(group, next)
                                blockedGroups = store.loadBlockedGroups()
                            }
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                label = "הוסף מאנשי קשר",
                icon = AccessibilityIcons.PersonAdd,
                onClick = { onAddFromContacts(); addingNumber = false },
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                label = "הוסף מהמתקשרים האחרונים",
                icon = AccessibilityIcons.Call,
                onClick = { onAddFromRecent(); addingNumber = false },
                modifier = Modifier.weight(1f)
            )
        }
        ActionTile(
            label = "הוסף מספר ידנית",
            icon = AccessibilityIcons.Dialpad,
            onClick = { addingNumber = true },
            modifier = Modifier.fillMaxWidth()
        )

        if (addingNumber) {
            InlineAddField(
                value = numberDraft,
                onValueChange = { numberDraft = it },
                label = "מספר טלפון",
                phoneKeyboard = true,
                onAdd = {
                    if (numberDraft.isNotBlank()) {
                        store.add(ExcludedRecipient(label = numberDraft.trim(), number = numberDraft.trim()))
                        entries = store.load()
                        numberDraft = ""
                        addingNumber = false
                    }
                }
            )
        }

        entries.forEach { entry ->
            ExclusionRow(entry = entry, onRemove = {
                store.remove(entry)
                entries = store.load()
            })
        }
    }
}

// One-letter Hebrew day initials, Sunday-first — matches the HTML reference's day-circle picker.
private fun weekdayInitial(dayOfWeek: Int): String = when (dayOfWeek) {
    java.util.Calendar.SUNDAY -> "א'"
    java.util.Calendar.MONDAY -> "ב'"
    java.util.Calendar.TUESDAY -> "ג'"
    java.util.Calendar.WEDNESDAY -> "ד'"
    java.util.Calendar.THURSDAY -> "ה'"
    java.util.Calendar.FRIDAY -> "ו'"
    else -> "ש'"
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    return "%02d:%02d".format(hour, minute)
}

/** A single day-of-week toggle circle in the working-hours picker. */
@Composable
private fun DayCircle(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (active) AccessibilityColors.Primary else AccessibilityColors.FieldGrey)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (active) Color.White else AccessibilityColors.TextMuted
        )
    }
}

/**
 * "הגדרות חכמות" — the working-hours brake plus the existing recipient rules (blocked groups,
 * blacklist) gathered on one screen, as in the design reference. Blocked-groups and blacklist
 * still read/write [ExclusionsStore]; only the layout is new here.
 */
@Composable
private fun SmartRulesScreen(
    workingHoursSettings: WorkingHoursSettings,
    exclusionsStore: ExclusionsStore,
    recipientScope: RecipientScope,
    exclusionsPreview: RecipientPreview,
    onOpenExclusions: () -> Unit,
    onBack: () -> Unit
) {
    var enabled by remember { mutableStateOf(workingHoursSettings.enabled) }
    var activeDays by remember { mutableStateOf(workingHoursSettings.activeDays) }
    var startMinute by remember { mutableStateOf(workingHoursSettings.startMinuteOfDay) }
    var endMinute by remember { mutableStateOf(workingHoursSettings.endMinuteOfDay) }
    var blockedGroups by remember { mutableStateOf(exclusionsStore.loadBlockedGroups()) }

    val context = LocalContext.current

    fun openTimePicker(current: Int, onPicked: (Int) -> Unit) {
        android.app.TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            current / 60,
            current % 60,
            true
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModalHeader(title = "הגדרות חכמות", onBack = onBack)

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(AccessibilityIcons.Schedule, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(20.dp))
                        Text("שעות פעילות", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccessibilityColors.Heading)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            workingHoursSettings.enabled = it
                        }
                    )
                }
                Text(
                    if (enabled) {
                        "המערכת תשלח הודעות רק בשעות ובימים שנבחרו למטה. מחוץ לשעות אלו, האפליקציה תנוח."
                    } else {
                        "המערכת פעילה בכל שעה וכל יום."
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = AccessibilityColors.TextMuted
                )

                if (enabled) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(
                            java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY, java.util.Calendar.TUESDAY,
                            java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY, java.util.Calendar.FRIDAY,
                            java.util.Calendar.SATURDAY
                        ).forEach { day ->
                            DayCircle(
                                label = weekdayInitial(day),
                                active = activeDays.contains(day),
                                onClick = {
                                    val next = if (activeDays.contains(day)) activeDays - day else activeDays + day
                                    activeDays = next
                                    workingHoursSettings.activeDays = next
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccessibilityColors.FieldGrey)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatMinuteOfDay(startMinute),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AccessibilityColors.TextStrong,
                            modifier = Modifier.clickable {
                                openTimePicker(startMinute) {
                                    startMinute = it
                                    workingHoursSettings.startMinuteOfDay = it
                                }
                            }
                        )
                        Text("עד", fontSize = 13.sp, color = AccessibilityColors.TextMuted)
                        Text(
                            formatMinuteOfDay(endMinute),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AccessibilityColors.TextStrong,
                            modifier = Modifier.clickable {
                                openTimePicker(endMinute) {
                                    endMinute = it
                                    workingHoursSettings.endMinuteOfDay = it
                                }
                            }
                        )
                    }
                }
            }
        }

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(AccessibilityIcons.Shield, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(20.dp))
                    Text("למי לא לשלוח?", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccessibilityColors.Heading)
                }
                Text(
                    "הגדרות למניעת שליחה לאנשים שכבר מכירים אותך.",
                    fontSize = 13.sp,
                    color = AccessibilityColors.TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val visibleGroups = RecipientRulesUi.visibleBlockGroups(recipientScope)
                visibleGroups.forEach { group ->
                    SmartRuleToggleRow(
                        title = blockedGroupLabel(group),
                        description = blockedGroupDescription(group),
                        checked = blockedGroups.contains(group),
                        onToggle = {
                            val next = !blockedGroups.contains(group)
                            exclusionsStore.setBlockedGroup(group, next)
                            blockedGroups = exclusionsStore.loadBlockedGroups()
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, AccessibilityColors.CardBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenExclusions)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🚫 רשימה שחורה אישית", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
                    Text(
                        "${exclusionsPreview.count} מספרים",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccessibilityColors.Danger
                    )
                }
            }
        }
    }
}

/** A "rule row" with a title, description, and a real on/off switch — matching the design's exclusion rows. */
@Composable
private fun SmartRuleToggleRow(title: String, description: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
            Text(description, fontSize = 12.sp, lineHeight = 17.sp, color = AccessibilityColors.TextMuted)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

private fun blockedGroupDescription(group: BlockedRecipientGroup): String =
    when (group) {
        BlockedRecipientGroup.CONTACTS -> "אל תשלח הודעות לאנשים ששמורים אצלי בטלפון."
        BlockedRecipientGroup.NON_CONTACTS -> "אל תשלח הודעות למספרים שלא שמורים אצלי."
        BlockedRecipientGroup.FIRST_TIME -> "אל תשלח בפעם הראשונה שמספר מתקשר — רק החל מהשנייה."
    }

@Composable
private fun AllowedRecipientsScreen(
    store: AllowedRecipientsStore,
    onAddFromContacts: () -> Unit,
    onAddFromRecent: () -> Unit,
    onBack: () -> Unit
) {
    var entries by remember { mutableStateOf(store.load()) }
    var addingNumber by remember { mutableStateOf(false) }
    var numberDraft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun saveDraft(): Boolean {
        if (!addingNumber || numberDraft.isBlank()) return true
        val entry = AllowedRecipientValidator.validEntry(
            AllowedRecipient(label = numberDraft.trim(), number = numberDraft.trim())
        )
        if (entry == null) {
            error = AllowedRecipientsUiSpec.INVALID_NUMBER
            return false
        }
        store.add(entry)
        entries = store.load()
        numberDraft = ""
        addingNumber = false
        error = null
        return true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModalHeader(title = AllowedRecipientsUiSpec.TITLE, onBack = onBack)
        Text(
            text = AllowedRecipientsUiSpec.DESCRIPTION,
            fontSize = 14.sp,
            color = AccessibilityColors.TextMuted,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                label = "הוסף מאנשי קשר",
                icon = AccessibilityIcons.PersonAdd,
                onClick = { onAddFromContacts(); addingNumber = false; error = null },
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                label = "הוסף מהמתקשרים האחרונים",
                icon = AccessibilityIcons.Call,
                onClick = { onAddFromRecent(); addingNumber = false; error = null },
                modifier = Modifier.weight(1f)
            )
        }
        ActionTile(
            label = "הוסף מספר ידנית",
            icon = AccessibilityIcons.Dialpad,
            onClick = { addingNumber = true; error = null },
            modifier = Modifier.fillMaxWidth()
        )
        if (addingNumber) {
            InlineAddField(
                value = numberDraft,
                onValueChange = { numberDraft = it; error = null },
                label = "מספר טלפון",
                phoneKeyboard = true,
                buttonText = "שמור",
                onAdd = { saveDraft() }
            )
        }
        error?.let { Text(it, color = AccessibilityColors.Danger, fontSize = 13.sp) }
        if (entries.isEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = AllowedRecipientsUiSpec.EMPTY_STATE,
                    fontSize = 14.sp,
                    color = AccessibilityColors.TextMuted,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            entries.forEach { entry ->
                AllowedRecipientRow(entry = entry, onRemove = {
                    store.remove(entry)
                    entries = store.load()
                })
            }
        }
        PillButton(text = "שמור", onClick = { if (saveDraft()) onBack() })
    }
}

@Composable
private fun ActionTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AccessibilityColors.PrimaryTint,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccessibilityColors.PrimaryTintBorder),
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AccessibilityColors.Primary)
        }
    }
}

@Composable
private fun InlineAddField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onAdd: () -> Unit,
    buttonText: String = "הוסף",
    phoneKeyboard: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (phoneKeyboard) KeyboardType.Phone else KeyboardType.Text),
            textStyle = if (phoneKeyboard) {
                androidx.compose.material3.LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
            } else {
                androidx.compose.material3.LocalTextStyle.current
            },
            modifier = Modifier.weight(1f)
        )
        PillButton(text = buttonText, onClick = onAdd, modifier = Modifier.width(90.dp))
    }
}

@Composable
private fun AllowedRecipientRow(entry: AllowedRecipient, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AccessibilityColors.SubtleSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccessibilityColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                CircleAvatar(background = Color(0xFFDBE7FB), initial = entry.label.take(1))
                Column {
                    Text(entry.label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccessibilityColors.TextStrong)
                    Text(AllowedRecipientsUiSpec.ITEM_SUBTITLE, fontSize = 12.sp, color = AccessibilityColors.TextFaint)
                }
            }
            Icon(
                AccessibilityIcons.Close,
                contentDescription = "הסר",
                tint = AccessibilityColors.UnselectedIcon,
                modifier = Modifier.size(21.dp).clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun ExclusionRow(entry: ExcludedRecipient, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AccessibilityColors.SubtleSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccessibilityColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                if (entry.number.isBlank()) {
                    CircleAvatar(background = Color(0xFFDBE7FB), initial = entry.label.take(1))
                } else {
                    CircleAvatar(background = Color(0xFFDBE7FB), icon = AccessibilityIcons.Call)
                }
                Column {
                    Text(entry.label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccessibilityColors.TextStrong)
                    Text("לא תישלח הודעה אוטומטית", fontSize = 12.sp, color = AccessibilityColors.TextFaint)
                }
            }
            Icon(
                AccessibilityIcons.Close,
                contentDescription = "הסר",
                tint = AccessibilityColors.UnselectedIcon,
                modifier = Modifier.size(21.dp).clickable(onClick = onRemove)
            )
        }
    }
}

// ===================== SHARED =====================
@Composable
private fun ModalHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            AccessibilityIcons.ChevronStart,
            contentDescription = "חזור",
            tint = AccessibilityColors.IconDark,
            modifier = Modifier.size(26.dp).clickable(onClick = onBack)
        )
        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = AccessibilityColors.Heading)
    }
}

private val recipientKeySetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() }
)

/**
 * Full-screen multi-select recipient picker. Contacts or recent callers ([loadCandidates]);
 * writes nothing until [onConfirm] — Back/ביטול leave the store unchanged. [savedKeys] (last-9-digits)
 * start checked. Search bar is pinned above the scrolling list. Reuses [ContactsPickerSearch.filter].
 */
@Composable
private fun ContactMultiPickerScreen(
    title: String,
    needsContactsPermission: Boolean,
    loadCandidates: () -> List<ContactCandidate>,
    savedKeys: Set<String>,
    onConfirm: (added: List<ContactCandidate>, removedKeys: Set<String>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var permissionDenied by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<ContactCandidate>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var checkedKeys by rememberSaveable(stateSaver = recipientKeySetSaver) { mutableStateOf(savedKeys) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            candidates = loadCandidates()
            permissionDenied = false
        } else {
            permissionDenied = true
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        val granted = !needsContactsPermission ||
            context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            candidates = loadCandidates()
            loading = false
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val filtered = remember(candidates, query) { ContactsPickerSearch.filter(candidates, query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        ModalHeader(title = title, onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))

        if (permissionDenied) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Text(
                    ContactsPickerPermissionState.DENIED_MESSAGE,
                    fontSize = 14.sp,
                    color = AccessibilityColors.TextMuted
                )
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("חיפוש לפי שם או מספר") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> Text("טוען אנשי קשר…", fontSize = 14.sp, color = AccessibilityColors.TextMuted)
                    candidates.isEmpty() -> Text(
                        "לא נמצאו אנשי קשר. אפשר להוסיף מספר ידנית.",
                        fontSize = 14.sp,
                        color = AccessibilityColors.TextMuted
                    )
                    filtered.isEmpty() -> Text("לא נמצאו תוצאות", fontSize = 14.sp, color = AccessibilityColors.TextMuted)
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filtered) { candidate ->
                            val key = recipientKey(candidate.phone)
                            val checked = checkedKeys.contains(key)
                            ContactMultiPickerRow(
                                candidate = candidate,
                                checked = checked,
                                onToggle = {
                                    checkedKeys = if (checked) checkedKeys - key else checkedKeys + key
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "נבחרו ${checkedKeys.size} אנשי קשר",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccessibilityColors.TextStrong
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("ביטול", color = AccessibilityColors.TextMuted) }
                PillButton(
                    text = "הוסף",
                    onClick = {
                        val contactKeys = candidates.map { recipientKey(it.phone) }.toSet()
                        val diff = ContactSelectionDiff.compute(savedKeys, checkedKeys, contactKeys)
                        val addedCandidates = candidates.filter { recipientKey(it.phone) in diff.added }
                        onConfirm(addedCandidates, diff.removed)
                    },
                    modifier = Modifier.width(96.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactMultiPickerRow(candidate: ContactCandidate, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                candidate.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AccessibilityColors.TextStrong
            )
            Text(
                PhoneNumberNormalizer.toLocalIsraeliDisplay(candidate.phone),
                fontSize = 12.sp,
                color = AccessibilityColors.TextFaint
            )
        }
        Icon(
            if (checked) AccessibilityIcons.RadioChecked else AccessibilityIcons.RadioUnchecked,
            contentDescription = if (checked) "נבחר" else "לא נבחר",
            tint = if (checked) AccessibilityColors.Primary else AccessibilityColors.UnselectedIcon,
            modifier = Modifier.size(24.dp)
        )
    }
}
