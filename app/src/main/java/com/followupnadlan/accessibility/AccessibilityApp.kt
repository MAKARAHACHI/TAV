package com.followupnadlan.accessibility

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.onFocusChanged
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
import com.followupnadlan.missedcall.WhatsAppPackageResolver
import com.followupnadlan.postcall.CallDetectionDiagnostics
import com.followupnadlan.postcall.CallDetectionDiagnosticsSnapshot
import com.followupnadlan.postcall.CallDetectionPreferences
import com.followupnadlan.postcall.CallDetectionService
import com.followupnadlan.postcall.CallDetectionServiceAction
import com.followupnadlan.postcall.CallDetectionServiceLifecycle
import com.followupnadlan.templates.MessageComposition
import com.followupnadlan.templates.MessageTemplate
import com.followupnadlan.templates.TemplateStore
import com.followupnadlan.templates.TemplateStoreLogic
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
    ALLOWED_RECIPIENTS,
    MISSED_CALL_PROMPT
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
    fun preview(templates: List<MessageTemplate>, selectedTemplateId: String): String =
        templates.firstOrNull { it.id == selectedTemplateId }
            ?.body
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString("\n")
}

/** Carries the missed-call context when the app is opened from a follow-up notification. */
data class MissedCallLaunch(
    val phone: String = "",
    val message: String = "",
    val fromNotification: Boolean = false
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
    val exclusionsStore = remember(context) { ExclusionsStore(appContext) }
    val allowedRecipientsStore = remember(context) { AllowedRecipientsStore(appContext) }

    var tab by remember { mutableStateOf(AccessibilityTab.HOME) }
    var modal by remember {
        mutableStateOf(if (missedCallLaunch.fromNotification) AccessibilityModal.MISSED_CALL_PROMPT else AccessibilityModal.NONE)
    }
    // Full-screen recipient multi-picker overlay (above the current tab/modal when non-null).
    var activePicker by remember { mutableStateOf<ActivePicker?>(null) }
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
    var smsFallback by remember { mutableStateOf(settings.smsFallbackEnabled) }
    var preferredWhatsApp by remember {
        mutableStateOf(
            if (settings.preferredWhatsAppPackage == WhatsAppPackageResolver.WHATSAPP_BUSINESS_PACKAGE) {
                WhatsAppChoice.BUSINESS
            } else {
                WhatsAppChoice.REGULAR
            }
        )
    }
    var selectedTemplateId by remember { mutableStateOf(settings.selectedTemplateId) }
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
                                        actionLabel = "בטל",
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
                        AccessibilityTab.HOME -> HomeScreen(
                            bridgingEnabled = bridgingEnabled,
                            statusRows = HomeServiceStatus.rows(
                                HomeServiceStatusInput(
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
                                    detectorEnabled = callDetectionPreferences.isEnabled()
                                )
                            ),
                            bridgeReady = bridgingEnabled &&
                                phoneStateGranted &&
                                callLogGranted &&
                                callDetectionPreferences.isEnabled(),
                            templates = templates,
                            selectedTemplateId = selectedTemplateId,
                            logStore = logStore,
                            onSaveSelectedMessage = { body ->
                                TemplateStoreLogic.selectedTemplate(templates, selectedTemplateId)?.let { template ->
                                    templateStore.saveTemplate(template.copy(body = body))
                                    templates = templateStore.loadTemplates()
                                }
                            },
                            onToggleBridging = {
                                if (bridgingEnabled) {
                                    settings.isEnabled = false
                                    callDetectionPreferences.setEnabled(false)
                                    bridgingEnabled = false
                                    applyCallDetectionServiceState(appContext, bridgeEnabled = false, phoneStateGranted, callLogGranted)
                                    diagnosticsSnapshot = callDetectionDiagnostics.snapshot()
                                } else {
                                    modal = AccessibilityModal.SETUP
                                }
                            }
                        )
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
                        selectedTemplateId = selectedTemplateId,
                        onSelectTemplate = { id ->
                            selectedTemplateId = id
                            settings.selectedTemplateId = id
                        },
                        onSaveTemplate = { template ->
                            templateStore.saveTemplate(template)
                            templates = templateStore.loadTemplates()
                        },
                        onAddTemplate = { title, body, cardLink, websiteLink ->
                            templateStore.addTemplate(title, body, cardLink, websiteLink)
                            templates = templateStore.loadTemplates()
                        },
                        onDeleteTemplate = { id ->
                            templateStore.deleteTemplate(id)
                            val reloaded = templateStore.loadTemplates()
                            templates = reloaded
                            // If the default was deleted, fall back to the first remaining card.
                            if (reloaded.none { it.id == selectedTemplateId }) {
                                reloaded.firstOrNull()?.let { fallback ->
                                    selectedTemplateId = fallback.id
                                    settings.selectedTemplateId = fallback.id
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

                    AccessibilityModal.MISSED_CALL_PROMPT -> MissedCallPromptScreen(
                        phone = missedCallLaunch.phone,
                        message = missedCallLaunch.message,
                        templates = templates,
                        selectedTemplateId = selectedTemplateId,
                        askBeforeSend = askBeforeSend,
                        onDone = { modal = AccessibilityModal.NONE }
                    )
                }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Bottom navigation is hidden while a full-screen modal or picker is open.
            if (modal == AccessibilityModal.NONE && activePicker == null) {
                BottomNav(selected = tab, onSelect = { tab = it })
            }
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

// ===================== SCREEN 1 — HOME =====================
@Composable
private fun HomeScreen(
    bridgingEnabled: Boolean,
    statusRows: List<HomeServiceStatusRow>,
    bridgeReady: Boolean,
    templates: List<MessageTemplate>,
    selectedTemplateId: String,
    logStore: FollowUpLogStore,
    onSaveSelectedMessage: (String) -> Unit,
    onToggleBridging: () -> Unit
) {
    val context = LocalContext.current
    val selectedMessage = TemplateStoreLogic.selectedTemplate(templates, selectedTemplateId)?.body.orEmpty()
    var editingMessage by remember { mutableStateOf(false) }
    var quickPhone by remember { mutableStateOf("") }
    var quickPhoneEditing by remember { mutableStateOf(false) }
    var lastAppliedMissedPhone by remember { mutableStateOf<String?>(null) }
    var quickStatus by remember { mutableStateOf<String?>(null) }
    var logEntries by remember { mutableStateOf(logStore.load()) }
    val latestMissedCaller = remember(logEntries) { LastMissedCallerLogic.from(logEntries) }

    LaunchedEffect(latestMissedCaller?.phone, quickPhoneEditing) {
        val next = HomeQuickWhatsAppNumberLogic.applyLatestMissedCaller(
            currentText = quickPhone,
            currentLastMissedPhone = lastAppliedMissedPhone,
            latestMissedPhone = latestMissedCaller?.phone?.let { PhoneNumberNormalizer.toLocalIsraeliDisplay(it) },
            isActivelyEditing = quickPhoneEditing
        )
        quickPhone = next.text
        lastAppliedMissedPhone = next.lastMissedPhone
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(title = "אני זמין/ה בכתב")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(AccessibilityIcons.Forum, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(20.dp))
            Text(
                text = "אם אני לא אוכל לענות לשיחה— השיחה תמשיך בכתב.",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AccessibilityColors.TextBody
            )
        }

        AppCard(cornerRadius = 22) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IconBadge(
                        icon = AccessibilityIcons.Forum,
                        background = AccessibilityColors.PrimaryContainer,
                        tint = AccessibilityColors.Primary
                    )
                }
                Text(
                    text = "סטטוס השירות",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = AccessibilityColors.Heading,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "אם לא אענה לשיחה, האפליקציה תבקש מהמתקשר לכתוב לי.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AccessibilityColors.TextStrong,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    statusRows.forEach { row ->
                        StatusChip(
                            icon = homeServiceStatusIcon(row.kind),
                            iconTint = homeServiceStatusIconTint(row.tone),
                            label = row.label,
                            trailingIcon = homeServiceStatusTrailingIcon(row.tone),
                            trailingTint = homeServiceStatusTrailingTint(row.tone)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AppCard(cornerRadius = 22) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ההודעה שתישלח",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = AccessibilityColors.Heading,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                WhatsAppMessagePreview(message = HomeMessagePreviewLogic.preview(templates, selectedTemplateId))
                OutlinePillButton(
                    text = "ערוך הודעה",
                    onClick = { editingMessage = true },
                    borderColor = AccessibilityColors.Primary,
                    contentColor = AccessibilityColors.Primary,
                    leadingIcon = AccessibilityIcons.Edit
                )
            }
        }

        AppCard(cornerRadius = 22) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = HomeQuickWhatsAppUiSpec.TITLE,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = AccessibilityColors.Heading,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillButton(
                        text = HomeQuickWhatsAppUiSpec.BUTTON_TEXT,
                        onClick = {
                            quickStatus = null
                            when (val plan = HomeQuickWhatsAppOpenPlanner.plan(quickPhone, selectedMessage)) {
                                HomeQuickWhatsAppOpenPlan.InvalidNumber -> {
                                    quickStatus = HomeQuickWhatsAppUiSpec.INVALID_NUMBER
                                }
                                is HomeQuickWhatsAppOpenPlan.OpenComposer -> {
                                    val result = AccessibilityActions.openWhatsApp(context, plan.link)
                                    if (result == null) {
                                        AccessibilityActions.logEntry(context, plan.successLogAction, plan.message, plan.normalizedPhone)
                                        logEntries = logStore.load()
                                        quickPhoneEditing = false
                                    } else {
                                        AccessibilityActions.logEntry(context, plan.failureLogAction, plan.message, plan.normalizedPhone)
                                        logEntries = logStore.load()
                                        quickStatus = HomeQuickWhatsAppUiSpec.WHATSAPP_FAILURE
                                    }
                                }
                            }
                        },
                        background = AccessibilityColors.Green,
                        leadingIcon = AccessibilityIcons.Chat,
                        modifier = Modifier.width(146.dp)
                    )
                    OutlinedTextField(
                        value = quickPhone,
                        onValueChange = {
                            quickPhone = it
                            quickStatus = null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = HomeQuickWhatsAppUiSpec.phoneKeyboardType),
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .onFocusChanged { quickPhoneEditing = it.isFocused }
                    )
                }
                quickStatus?.let { Text(it, color = AccessibilityColors.Danger, fontSize = 13.sp, modifier = Modifier.fillMaxWidth()) }
            }
        }

        if (bridgingEnabled) {
            PillButton(
                text = if (bridgeReady) "גישור לשיחות פעיל" else "השלם הרשאות",
                onClick = onToggleBridging,
                background = if (bridgeReady) AccessibilityColors.Green else AccessibilityColors.Warning,
                leadingIcon = AccessibilityIcons.PhoneInTalk
            )
            CaptionWithIcon(
                text = if (bridgeReady) "מוכן לשיחות שלא נענו" else "חסרה הרשאה לזיהוי שיחות",
                icon = if (bridgeReady) AccessibilityIcons.CheckCircle else AccessibilityIcons.Block,
                color = if (bridgeReady) AccessibilityColors.Green else AccessibilityColors.Warning
            )
        } else {
            PillButton(
                text = "הפעל גישור לשיחות",
                onClick = onToggleBridging,
                background = AccessibilityColors.Primary,
                leadingIcon = AccessibilityIcons.PhoneInTalk
            )
            CaptionWithIcon(
                text = "כבוי — לא יישלחו הודעות אוטומטיות",
                icon = AccessibilityIcons.Block,
                color = AccessibilityColors.TextMuted
            )
        }
    }

    if (editingMessage) {
        HomeMessageEditorDialog(
            message = selectedMessage,
            onSave = { body ->
                onSaveSelectedMessage(body)
                editingMessage = false
            },
            onCancel = { editingMessage = false }
        )
    }
}

@Composable
private fun WhatsAppMessagePreview(message: String, maxLines: Int = 4) {
    Surface(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
        color = Color(0xFFE4F8D8),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC9EAB8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message.ifBlank { " " },
            color = AccessibilityColors.TextStrong,
            fontSize = 18.sp,
            lineHeight = 29.sp,
            maxLines = maxLines,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun HomeMessageEditorDialog(
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
        title = { Text("עריכת ההודעה", fontWeight = FontWeight.Bold, color = AccessibilityColors.Heading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "זו ההודעה שתיפתח ב־WhatsApp או SMS לאחר שיחה שלא נענתה.",
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
                    keyboardActions = KeyboardActions(onDone = { saveDraft() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 310.dp)
                )
                error?.let { Text(it, color = AccessibilityColors.Danger, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = { saveDraft() }) {
                Text("שמור", fontWeight = FontWeight.Bold, color = AccessibilityColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("ביטול", color = AccessibilityColors.TextBody)
            }
        }
    )
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
    selectedTemplateId: String,
    onSelectTemplate: (String) -> Unit,
    onSaveTemplate: (MessageTemplate) -> Unit,
    onAddTemplate: (String, String, String, String) -> Unit,
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
        ModalHeader(title = "הודעת ברירת מחדל", onBack = onBack)

        when {
            creatingNew || editing != null -> {
                val target = editing
                TemplateCardEditor(
                    template = target,
                    onSave = { title, body, cardLink, websiteLink ->
                        if (target == null) {
                            onAddTemplate(title, body, cardLink, websiteLink)
                        } else {
                            onSaveTemplate(
                                target.copy(
                                    title = title,
                                    body = body,
                                    cardLink = cardLink,
                                    websiteLink = websiteLink
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
                    text = "בחר/י את הכרטיסייה שתישלח כברירת מחדל, או צור/י חדשה.",
                    fontSize = 14.sp,
                    color = AccessibilityColors.TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )
                templates.forEach { template ->
                    TemplateCard(
                        template = template,
                        selected = template.id == selectedTemplateId,
                        canDelete = templates.size > 1,
                        onClick = { onSelectTemplate(template.id) },
                        onEdit = { editing = template },
                        onDelete = { onDeleteTemplate(template.id) }
                    )
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
    onSave: (String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember(template?.id) { mutableStateOf(template?.title.orEmpty()) }
    var body by remember(template?.id) { mutableStateOf(template?.body.orEmpty()) }
    var cardLink by remember(template?.id) { mutableStateOf(template?.cardLink.orEmpty()) }
    var websiteLink by remember(template?.id) { mutableStateOf(template?.websiteLink.orEmpty()) }
    var error by remember(template?.id) { mutableStateOf<String?>(null) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (template == null) "כרטיסייה חדשה" else "עריכת כרטיסייה",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AccessibilityColors.Heading
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
                        onSave(trimmedTitle, body, cardLink.trim(), websiteLink.trim())
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinePillButton(text = "בטל", onClick = onCancel, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ===================== SCREEN 4 — MISSED CALL PROMPT =====================
@Composable
private fun MissedCallPromptScreen(
    phone: String,
    message: String,
    templates: List<MessageTemplate>,
    selectedTemplateId: String,
    askBeforeSend: Boolean,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    // Manual "ask me" mode with more than one card lets the user pick a different template
    // for this send only (it does not change the saved default).
    val showSelector = askBeforeSend && templates.size > 1
    var activeId by remember(selectedTemplateId) { mutableStateOf(selectedTemplateId) }
    val activeTemplate = templates.firstOrNull { it.id == activeId }
        ?: templates.firstOrNull { it.id == selectedTemplateId }
        ?: templates.firstOrNull()
    val resolvedMessage = if (showSelector) {
        activeTemplate?.let { MessageComposition.build(it) }.orEmpty()
    } else {
        message.ifBlank { activeTemplate?.let { MessageComposition.build(it) }.orEmpty() }
    }
    val normalizedPhone = remember(phone) { PhoneNumberNormalizer.normalizeForWhatsApp(phone) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(
            icon = AccessibilityIcons.PhoneMissed,
            background = AccessibilityColors.PrimaryContainer,
            tint = AccessibilityColors.Primary,
            boxSize = 92,
            cornerRadius = 46,
            iconSize = 48
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text("שיחה שלא נענתה", fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = AccessibilityColors.Heading)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = phone.ifBlank { "מספר לא ידוע" },
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = AccessibilityColors.TextStrong
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text("לשלוח הודעה?", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = AccessibilityColors.TextBody)

        if (showSelector) {
            Spacer(modifier = Modifier.height(16.dp))
            TemplateChipRow(
                templates = templates,
                selectedId = activeId,
                onSelect = { activeId = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            WhatsAppMessagePreview(message = resolvedMessage, maxLines = 8)
        }

        Spacer(modifier = Modifier.weight(1f))

        PillButton(
            text = "שלח ב־WhatsApp",
            onClick = {
                if (normalizedPhone == null || resolvedMessage.isBlank()) {
                    status = "חסר מספר תקין או נוסח הודעה."
                    return@PillButton
                }
                val result = AccessibilityActions.openWhatsApp(
                    context,
                    WhatsAppLinkBuilder.build(normalizedPhone, resolvedMessage)
                )
                if (result == null) {
                    AccessibilityActions.logEntry(context, com.followupnadlan.followuplog.FollowUpActionType.WHATSAPP_OPENED, resolvedMessage, phone)
                    onDone()
                } else {
                    status = result
                }
            },
            background = AccessibilityColors.Green,
            leadingIcon = AccessibilityIcons.Chat
        )
        Spacer(modifier = Modifier.height(11.dp))
        PillButton(
            text = "שלח ב־SMS",
            onClick = {
                if (phone.isBlank() || resolvedMessage.isBlank()) {
                    status = "חסר מספר או נוסח הודעה."
                    return@PillButton
                }
                val result = AccessibilityActions.openSmsComposer(context, phone, resolvedMessage)
                if (result == null) {
                    onDone()
                } else {
                    status = result
                }
            },
            background = AccessibilityColors.Primary,
            leadingIcon = AccessibilityIcons.Sms
        )
        Spacer(modifier = Modifier.height(11.dp))
        OutlinePillButton(text = "בטל", onClick = onDone)
        status?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(it, color = AccessibilityColors.Danger, fontSize = 14.sp)
        }
    }
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

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SettingsSectionTitle("בדיקת זיהוי שיחות", bottomPadding = 4)
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

private fun recipientScopeLabel(scope: RecipientScope): String =
    when (scope) {
        RecipientScope.ANY_NUMBER -> "כל מספר"
        RecipientScope.CONTACTS_ONLY -> "אנשי קשר בלבד"
        RecipientScope.NON_CONTACTS_ONLY -> "מספרים לא שמורים"
        RecipientScope.ONLY_SELECTED -> "רק למי שבחרתי"
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
