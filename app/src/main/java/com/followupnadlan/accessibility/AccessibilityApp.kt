package com.followupnadlan.accessibility

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.missedcall.MissedCallAutoResponseHandler
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel
import com.followupnadlan.missedcall.MissedCallWhatsAppMode
import com.followupnadlan.missedcall.WhatsAppAutoSendController
import com.followupnadlan.missedcall.WhatsAppPackageResolver
import com.followupnadlan.missedcall.WhatsAppReplyOpenResult
import com.followupnadlan.missedcall.WhatsAppReplySender
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
import com.followupnadlan.setup.FollowUpOptionalCapability
import com.followupnadlan.setup.FollowUpPermission
import com.followupnadlan.setup.PermissionSnapshot
import com.followupnadlan.setup.PermissionStatusLogic
import com.followupnadlan.templates.CardEmojiInput
import com.followupnadlan.templates.ContactTextCard
import com.followupnadlan.templates.MessageComposition
import com.followupnadlan.templates.MessageTemplate
import com.followupnadlan.templates.WhatsAppTextStyling
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
    EXCLUSIONS,
    SMART_RULES,
    ALLOWED_RECIPIENTS,
    MISSED_CALL_PROMPT,
    // Design Pass 2/3 — the trust-moment journey pages, opened from the home cards.
    MISSED_JOURNEY,
    ENDED_JOURNEY,
    // Part A — the "לא ענו" outgoing-not-answered moment's edit screen.
    NO_ANSWER_JOURNEY,
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

/**
 * An open message-variant editor (Part B). [variantId] is a specific variant to edit, or blank to
 * create a NEW variant for [role]. [role] drives the wording, tag set, and where a new variant is
 * saved.
 */
internal data class MessageEditorTarget(
    val role: TemplateRole,
    val variantId: String,
    val isNew: Boolean = false
)

/**
 * Part B — the variant data + actions a moment's edit screen needs to show and manage its up-to-5
 * message variants. Bundled so a journey screen takes one parameter instead of six.
 */
internal data class MomentVariants(
    val variants: List<MessageTemplate>,
    val activeId: String,
    val canAdd: Boolean,
    val onSelectActive: (String) -> Unit,
    val onEditVariant: (String) -> Unit,
    val onDeleteVariant: (String) -> Unit,
    val onAddVariant: () -> Unit
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

/** The two approval choices on the moment edit page — send by itself, or wait for my approval. */
internal enum class MomentApprovalMode { AUTOMATIC, MANUAL }

/**
 * Which moment an edit page is showing. Drives two per-moment differences on the page:
 *  - Automatic send is offered ONLY for [MISSED] (the user is away from the phone); [ENDED] and
 *    [NO_ANSWER] happen while holding the phone, so their approval row is a locked "ידני · באישור שלך".
 *  - Each kind writes its "מי יקבל" override to its own store.
 */
internal enum class MomentEditKind { MISSED, ENDED, NO_ANSWER }

/**
 * Pure mapping between the moment edit page's "אישור לפני שליחה" select and the engine's existing
 * automation flags. Kept engine-agnostic (returns the flag pair) so it can be asserted in a test
 * without touching SharedPreferences.
 *
 * MANUAL is the prepared-manual path (the pure approval sheet); AUTOMATIC is the real "phone sends
 * by itself" path that already exists for the missed moment (ACCESSIBILITY_AUTO + automation on).
 * The flags are a single global pair — the ended/no-answer send paths always prompt regardless, so
 * choosing AUTOMATIC there has no effect on those moments (their prompt is unconditional).
 */
internal object MomentApprovalModeMapper {
    data class Flags(val whatsappMode: MissedCallWhatsAppMode, val automationEnabled: Boolean)

    fun toFlags(mode: MomentApprovalMode): Flags = when (mode) {
        MomentApprovalMode.MANUAL -> Flags(MissedCallWhatsAppMode.PREPARED_MANUAL, automationEnabled = false)
        MomentApprovalMode.AUTOMATIC -> Flags(MissedCallWhatsAppMode.ACCESSIBILITY_AUTO, automationEnabled = true)
    }

    fun fromWhatsAppMode(mode: MissedCallWhatsAppMode): MomentApprovalMode =
        if (mode == MissedCallWhatsAppMode.PREPARED_MANUAL) MomentApprovalMode.MANUAL else MomentApprovalMode.AUTOMATIC
}

/** Carries the missed-call context when the app is opened from a follow-up notification. */
data class MissedCallLaunch(
    val phone: String = "",
    val message: String = "",
    val fromNotification: Boolean = false,
    /** Raw notification call-type extra ("missed"/"incoming"/"outgoing"); decides the prompt wording. */
    val callType: String? = null,
    /** Epoch-ms of the call, so the approval sheet can show real relative time ("לפני 3 דקות"). 0 = unknown. */
    val callTimestampMs: Long = 0L,
    /** Contact name when the number is saved; blank ⇒ the sheet falls back to the local-formatted phone. */
    val leadName: String = ""
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
    val noAnswerScopeSettings = remember(context) { NoAnswerScopeSettings(appContext) }
    val generalScopeSettings = remember(context) { GeneralRecipientScopeSettings(appContext) }
    val endedCardSettings = remember(context) { EndedCardSettings(appContext) }
    val missedCardSettings = remember(context) { MissedCardSettings(appContext) }
    val noAnswerCardSettings = remember(context) { NoAnswerCardSettings(appContext) }
    val cooldownSettings = remember(context) { FollowUpCooldownSettings(appContext) }
    val exclusionsStore = remember(context) { ExclusionsStore(appContext) }
    val workingHoursSettings = remember(context) { WorkingHoursSettings(appContext) }
    val allowedRecipientsStore = remember(context) { AllowedRecipientsStore(appContext) }
    val myDetailsStore = remember(context) { MyDetailsStore(appContext) }
    val whatsAppAutoSendController = remember(context) { WhatsAppAutoSendController(appContext) }
    val setupPreferences = remember(context) { com.followupnadlan.setup.SetupPreferences(appContext) }

    var onboardingDone by remember { mutableStateOf(setupPreferences.isSetupCompleted()) }

    // WhatsApp-OTP login gate. Status is derived from the stored token + the clock; LOCKED shows
    // the login screen, NEEDS_RENEW triggers a silent renew (see LaunchedEffect below).
    val authTokenStore = remember(context) { com.followupnadlan.auth.AuthTokenStore(appContext) }
    var authStatus by remember {
        mutableStateOf(
            authTokenStore.load().let { s ->
                com.followupnadlan.auth.OtpGateLogic.evaluate(
                    hasToken = s.hasToken,
                    expiresAtMs = s.expiresAtMs,
                    lastRenewOkMs = s.lastRenewOkMs,
                    nowMs = System.currentTimeMillis()
                )
            }
        )
    }

    var tab by remember { mutableStateOf(AccessibilityTab.HOME) }
    var modal by remember {
        mutableStateOf(if (missedCallLaunch.fromNotification) AccessibilityModal.MISSED_CALL_PROMPT else AccessibilityModal.NONE)
    }
    // Full-screen recipient multi-picker overlay (above the current tab/modal when non-null).
    var activePicker by remember { mutableStateOf<ActivePicker?>(null) }
    // Which message variant the editor is open for (Part B). null = closed. Carries the role (for
    // wording/tags) and the specific variant id being edited — new variants get a blank id.
    var messageEditorTarget by remember { mutableStateOf<MessageEditorTarget?>(null) }
    // The ended journey's in-page contact-card editor.
    var cardEditorOpen by remember { mutableStateOf(false) }
    // The activity log is reachable from BOTH the Home header clock and system settings. Remember
    // which, so Back / onBack returns to the screen the user actually came from (Home vs settings).
    var historyFromHome by remember { mutableStateOf(false) }
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
    // Approval mode for the moment edit page ("אישור לפני שליחה"). Maps to the same global
    // whatsappMode/automation flags as askBeforeSend, via MomentApprovalModeMapper.
    var approvalMode by remember {
        mutableStateOf(MomentApprovalModeMapper.fromWhatsAppMode(settings.whatsappMode))
    }
    // The two GLOBAL timing brakes, now surfaced on every moment's edit page (moved out of Settings,
    // replacing the old 24h on/off toggle). sameNumber = "אל תשלח שוב לאותו אדם" (also mirrored into
    // settings.cooldownMillis for the missed path); globalQuiet = "מרווח מינימלי בין הודעות". null = off.
    var sameNumberCooldown by remember { mutableStateOf(cooldownSettings.sameNumberCooldownMillis) }
    var globalQuiet by remember { mutableStateOf(cooldownSettings.globalQuietMillis) }
    var recipientScope by remember { mutableStateOf(recipientScopeSettings.scope) }
    // Override-on-default model. The general default (Smart-Rules) that every moment falls back to
    // while it stays on "כמו הכללי", plus each moment's per-moment override (null = follow general).
    var generalScope by remember { mutableStateOf(generalScopeSettings.scope) }
    var missedScopeOverride by remember { mutableStateOf(recipientScopeSettings.scopeOverride) }
    var endedScopeOverride by remember { mutableStateOf(endedScopeSettings.scopeOverride) }
    var noAnswerScopeOverride by remember { mutableStateOf(noAnswerScopeSettings.scopeOverride) }
    // Per-moment card-attached flags. The contact card is an opt-in add/remove control on every
    // moment (missed / ended / no-answer), default OFF; the flag only governs whether the card
    // ELEMENT is drawn (still a drawn element only, no real .vcf send — §2).
    var missedCardAttached by remember { mutableStateOf(missedCardSettings.cardAttached) }
    var endedCardAttached by remember { mutableStateOf(endedCardSettings.cardAttached) }
    var noAnswerCardAttached by remember { mutableStateOf(noAnswerCardSettings.cardAttached) }
    // Per-moment cooldown OVERRIDES (override-on-default). Each moment's two brakes are 3-state:
    // Inherit ("לפי הכללי") / Off ("בלי המתנה") / Value. One store per moment; the general default
    // lives in cooldownSettings above. Untouched = Inherit, so nothing changes until overridden.
    val missedCooldownOverride = remember(context) { MomentCooldownOverrideSettings.forMoment(appContext, MomentEditKind.MISSED) }
    val endedCooldownOverride = remember(context) { MomentCooldownOverrideSettings.forMoment(appContext, MomentEditKind.ENDED) }
    val noAnswerCooldownOverride = remember(context) { MomentCooldownOverrideSettings.forMoment(appContext, MomentEditKind.NO_ANSWER) }
    var missedSameNumberChoice by remember { mutableStateOf(missedCooldownOverride.sameNumberOverride) }
    var missedGlobalQuietChoice by remember { mutableStateOf(missedCooldownOverride.globalQuietOverride) }
    var endedSameNumberChoice by remember { mutableStateOf(endedCooldownOverride.sameNumberOverride) }
    var endedGlobalQuietChoice by remember { mutableStateOf(endedCooldownOverride.globalQuietOverride) }
    var noAnswerSameNumberChoice by remember { mutableStateOf(noAnswerCooldownOverride.sameNumberOverride) }
    var noAnswerGlobalQuietChoice by remember { mutableStateOf(noAnswerCooldownOverride.globalQuietOverride) }
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
    // Part A/B: the active variant for the "לא ענו" outgoing-not-answered moment.
    var selectedNoAnswerId by remember { mutableStateOf(settings.selectedNoAnswerTemplateId) }
    var templates by remember { mutableStateOf(templateStore.loadTemplates()) }
    // Part C2: per-moment enable toggles (master gate stays bridgingEnabled/isEnabled).
    var missedMomentEnabled by remember { mutableStateOf(settings.missedMomentEnabled) }
    var endedMomentEnabled by remember { mutableStateOf(settings.endedMomentEnabled) }
    var noAnswerMomentEnabled by remember { mutableStateOf(settings.noAnswerMomentEnabled) }
    var phoneStateGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_PHONE_STATE)) }
    var callLogGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_CALL_LOG)) }
    var contactsGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_CONTACTS)) }
    // Whether our Accessibility service is enabled (the automatic-missed-send capability). Read live
    // from the controller; re-read on resume so returning from the OS accessibility screen updates
    // the Home chip and the Settings row. NOT cached forever (§2: Home must not claim "אוטומטי" when
    // the grant was later revoked).
    var accessibilityEnabled by remember { mutableStateOf(whatsAppAutoSendController.isAccessibilityServiceEnabled()) }
    var diagnosticsSnapshot by remember { mutableStateOf(callDetectionDiagnostics.snapshot()) }
    // Re-read the runtime permissions + the accessibility grant every time the app returns to the
    // foreground. The user flips these in OS screens (app-details / accessibility list) and comes
    // back; on ON_RESUME we refresh so every dependent surface reflects live state.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                phoneStateGranted = context.hasPermission(Manifest.permission.READ_PHONE_STATE)
                callLogGranted = context.hasPermission(Manifest.permission.READ_CALL_LOG)
                contactsGranted = context.hasPermission(Manifest.permission.READ_CONTACTS)
                accessibilityEnabled = whatsAppAutoSendController.isAccessibilityServiceEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
    // The channel to fall back to if the SMS runtime permission is denied when SMS is picked on the
    // moment edit page. Set just before the request; consumed by the launcher below.
    var channelBeforeSmsRequest by remember { mutableStateOf(FollowUpChannel.WHATSAPP) }
    val momentSmsChannelPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            selectedChannel = FollowUpChannel.SMS
            FollowUpChannelSettings.apply(settings, FollowUpChannel.SMS)
            smsFallback = settings.smsFallbackEnabled
        } else {
            // Denied: revert to the previous channel and briefly explain SMS needs the permission.
            selectedChannel = channelBeforeSmsRequest
            FollowUpChannelSettings.apply(settings, channelBeforeSmsRequest)
            smsFallback = settings.smsFallbackEnabled
            preferredWhatsApp = if (channelBeforeSmsRequest == FollowUpChannel.WHATSAPP_BUSINESS) {
                WhatsAppChoice.BUSINESS
            } else {
                WhatsAppChoice.REGULAR
            }
            undoScope.launch {
                snackbarHostState.showSnackbar("כדי לשלוח ב-SMS צריך הרשאת שליחת הודעות", withDismissAction = true)
            }
        }
    }

    // Shared moment-edit handler: pick a delivery channel. SMS is gated on the SEND_SMS runtime
    // permission (request at point of selection; on deny, revert + explain). WhatsApp options apply
    // immediately. Reuses FollowUpChannelSettings so the picked channel is the channel used (§2).
    fun onMomentSelectChannel(channel: FollowUpChannel) {
        if (channel == FollowUpChannel.SMS &&
            context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
        ) {
            channelBeforeSmsRequest = selectedChannel
            momentSmsChannelPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            return
        }
        selectedChannel = channel
        FollowUpChannelSettings.apply(settings, channel)
        smsFallback = settings.smsFallbackEnabled
        preferredWhatsApp = if (channel == FollowUpChannel.WHATSAPP_BUSINESS) {
            WhatsAppChoice.BUSINESS
        } else {
            WhatsAppChoice.REGULAR
        }
    }

    // Shared moment-edit handler: pick the approval mode. Maps to the same global automation flags
    // the Settings screen and missed journey already persist (MomentApprovalModeMapper). Note: the
    // ended/no-answer send paths always prompt regardless, so AUTOMATIC only takes real effect on
    // the missed moment; on the other two it is a no-op under the hood.
    fun onMomentSelectApproval(mode: MomentApprovalMode) {
        approvalMode = mode
        val flags = MomentApprovalModeMapper.toFlags(mode)
        settings.whatsappMode = flags.whatsappMode
        settings.whatsappAutomationEnabled = flags.automationEnabled
        askBeforeSend = mode == MomentApprovalMode.MANUAL
    }

    // Smart-Rules handler: pick the GENERAL same-number cooldown default (or OFF). This is the single
    // fallback every moment inherits while it stays on "לפי הכללי" (override-on-default). Writes only
    // the general store; the missed/ended/no-answer engines resolve it per-moment at decision time via
    // EffectiveCooldown.resolve. No per-moment store is touched here.
    fun onSelectGeneralSameNumberCooldown(millis: Long?) {
        sameNumberCooldown = millis
        cooldownSettings.sameNumberCooldownMillis = millis
    }

    // Smart-Rules handler: pick the GENERAL minimum interval between any two messages (or OFF). The
    // general default for the anti-burst brake; each moment inherits or overrides it. Engine unchanged.
    fun onSelectGeneralGlobalQuiet(millis: Long?) {
        globalQuiet = millis
        cooldownSettings.globalQuietMillis = millis
    }

    // Shared moment-edit handler: pick "מי יקבל את ההודעה?" for a moment. null = "כמו הכללי" (follow
    // the general default); a concrete scope = a per-moment override. Written to the moment's own
    // store (missed/ended/no-answer). Choosing "רק אנשים שאבחר" opens the choose-people flow, matching
    // the existing behavior — the list is meaningless until it exists.
    fun onMomentSelectScope(kind: MomentEditKind, scope: RecipientScope?) {
        when (kind) {
            MomentEditKind.MISSED -> {
                missedScopeOverride = scope
                recipientScopeSettings.scopeOverride = scope
            }
            MomentEditKind.ENDED -> {
                endedScopeOverride = scope
                endedScopeSettings.scopeOverride = scope
            }
            MomentEditKind.NO_ANSWER -> {
                noAnswerScopeOverride = scope
                noAnswerScopeSettings.scopeOverride = scope
            }
        }
        if (scope == RecipientScope.ONLY_SELECTED) {
            modal = AccessibilityModal.ALLOWED_RECIPIENTS
        }
    }

    // Per-moment resolved card-attached flag (missed / ended / no-answer).
    fun cardAttachedFor(kind: MomentEditKind): Boolean = when (kind) {
        MomentEditKind.MISSED -> missedCardAttached
        MomentEditKind.ENDED -> endedCardAttached
        MomentEditKind.NO_ANSWER -> noAnswerCardAttached
    }

    // Shared moment-edit handler: the contact-card add/remove control. Flips the moment's own
    // attach flag and persists it to that moment's store. Governs whether the card ELEMENT is drawn
    // in the artifact only — no real .vcf send is wired (§2).
    fun onMomentToggleCardAttached(kind: MomentEditKind) {
        when (kind) {
            MomentEditKind.MISSED -> {
                missedCardAttached = !missedCardAttached
                missedCardSettings.cardAttached = missedCardAttached
            }
            MomentEditKind.ENDED -> {
                endedCardAttached = !endedCardAttached
                endedCardSettings.cardAttached = endedCardAttached
            }
            MomentEditKind.NO_ANSWER -> {
                noAnswerCardAttached = !noAnswerCardAttached
                noAnswerCardSettings.cardAttached = noAnswerCardAttached
            }
        }
    }

    // Per-moment cooldown-override handlers (override-on-default). Each writes ONLY its own moment's
    // override store — never the general default, never another moment. Inherit ("לפי הכללי") drops
    // the stored key; Off/Value pin an override. The engine resolves these at decision time.
    fun onMomentSelectSameNumberCooldown(kind: MomentEditKind, choice: CooldownChoice) {
        when (kind) {
            MomentEditKind.MISSED -> { missedSameNumberChoice = choice; missedCooldownOverride.sameNumberOverride = choice }
            MomentEditKind.ENDED -> { endedSameNumberChoice = choice; endedCooldownOverride.sameNumberOverride = choice }
            MomentEditKind.NO_ANSWER -> { noAnswerSameNumberChoice = choice; noAnswerCooldownOverride.sameNumberOverride = choice }
        }
    }

    fun onMomentSelectGlobalQuiet(kind: MomentEditKind, choice: CooldownChoice) {
        when (kind) {
            MomentEditKind.MISSED -> { missedGlobalQuietChoice = choice; missedCooldownOverride.globalQuietOverride = choice }
            MomentEditKind.ENDED -> { endedGlobalQuietChoice = choice; endedCooldownOverride.globalQuietOverride = choice }
            MomentEditKind.NO_ANSWER -> { noAnswerGlobalQuietChoice = choice; noAnswerCooldownOverride.globalQuietOverride = choice }
        }
    }

    // Task 3: the inline "רק אנשים שאבחר" preview for a moment. Resolves the effective scope via the
    // existing EffectiveRecipientScope.of (per-moment override, else the general default). Returns a
    // pure RecipientPreview only when the effective scope is ONLY_SELECTED; otherwise null (no list).
    fun selectedPeoplePreviewFor(override: RecipientScope?): RecipientPreview? {
        val effective = EffectiveRecipientScope.of(override, generalScope)
        if (effective != RecipientScope.ONLY_SELECTED) return null
        return RecipientPreviewLogic.summary(allowedRecipientsStore.load().map { it.label })
    }

    val callDetectionPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        phoneStateGranted = context.hasPermission(Manifest.permission.READ_PHONE_STATE)
        callLogGranted = context.hasPermission(Manifest.permission.READ_CALL_LOG)
        contactsGranted = context.hasPermission(Manifest.permission.READ_CONTACTS)
        settings.isEnabled = true
        callDetectionPreferences.setEnabled(true)
        bridgingEnabled = true
        applyCallDetectionServiceState(appContext, bridgeEnabled = true, phoneStateGranted, callLogGranted)
        diagnosticsSnapshot = callDetectionDiagnostics.snapshot()
    }
    // Requests exactly one runtime permission from a Settings row's "אפשר" action, then refreshes
    // the matching state so the row flips to ✓ without leaving the screen.
    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        phoneStateGranted = context.hasPermission(Manifest.permission.READ_PHONE_STATE)
        callLogGranted = context.hasPermission(Manifest.permission.READ_CALL_LOG)
        contactsGranted = context.hasPermission(Manifest.permission.READ_CONTACTS)
    }

    // Fresh install shows "WhatsApp רגיל" as the default; persist it so storage matches the UI
    // (an empty stored preference would otherwise resolve to Business at the tie-break).
    LaunchedEffect(Unit) {
        if (settings.preferredWhatsAppPackage.isBlank()) {
            settings.preferredWhatsAppPackage = WhatsAppPackageResolver.WHATSAPP_MESSENGER_PACKAGE
        }
    }

    // Part B: which stored id is the ACTIVE variant for a role, and how to set it.
    fun activeIdFor(role: TemplateRole): String = when (role) {
        TemplateRole.MISSED_CALL -> selectedMissedId
        TemplateRole.CALL_ENDED -> selectedEndedId
        TemplateRole.NO_ANSWER_OUTGOING -> selectedNoAnswerId
    }

    fun setActiveId(role: TemplateRole, id: String) {
        when (role) {
            TemplateRole.MISSED_CALL -> { selectedMissedId = id; settings.selectedMissedTemplateId = id }
            TemplateRole.CALL_ENDED -> { selectedEndedId = id; settings.selectedEndedTemplateId = id }
            TemplateRole.NO_ANSWER_OUTGOING -> { selectedNoAnswerId = id; settings.selectedNoAnswerTemplateId = id }
        }
    }

    // Part B: build the variant bundle a moment's edit screen needs — the role's variants, the
    // active id, and the add/edit/delete/select actions, all backed by the existing template store.
    fun momentVariantsFor(role: TemplateRole): MomentVariants {
        val roleVariants = templates.filter { it.role == role }
        // Resolve the active id against what actually exists (an orphaned stored id falls back to
        // the first variant of the role, matching the send-path selector).
        val resolvedActive = roleVariants.firstOrNull { it.id == activeIdFor(role) }?.id
            ?: roleVariants.firstOrNull()?.id.orEmpty()
        return MomentVariants(
            variants = roleVariants,
            activeId = resolvedActive,
            canAdd = templates.count { it.role == role } < TemplateStore.MAX_VARIANTS_PER_ROLE,
            onSelectActive = { id -> setActiveId(role, id) },
            onEditVariant = { id -> messageEditorTarget = MessageEditorTarget(role, id) },
            onDeleteVariant = { id ->
                templateStore.deleteVariant(id)
                val reloaded = templateStore.loadTemplates()
                templates = reloaded
                // If the active variant was deleted, fall back to the first remaining of the role.
                if (reloaded.none { it.id == activeIdFor(role) }) {
                    (reloaded.firstOrNull { it.role == role } ?: reloaded.firstOrNull())?.let {
                        setActiveId(role, it.id)
                    }
                }
            },
            onAddVariant = { messageEditorTarget = MessageEditorTarget(role, variantId = "", isNew = true) }
        )
    }

    // Hardware/gesture Back pops the in-app screen stack instead of exiting the app. Topmost
    // overlay first: card editor → message editor → full-screen picker → modal → non-Home tab.
    // Only enabled while something is open; at the Home root it stays disabled so the
    // system default (leave the app) applies. Mirrors the onBack each screen already wires.
    val hasBackStack = cardEditorOpen || messageEditorTarget != null ||
        activePicker != null || modal != AccessibilityModal.NONE || tab != AccessibilityTab.HOME
    BackHandler(enabled = hasBackStack) {
        when {
            cardEditorOpen -> { cardEditorOpen = false; profileRefresh++ }
            messageEditorTarget != null -> messageEditorTarget = null
            activePicker != null -> activePicker = null
            // HISTORY can be opened from the Home header clock OR from SYSTEM_SETTINGS; Back returns
            // to whichever it came from (matches HistoryScreen's own onBack). SUPPORT is settings-only.
            modal == AccessibilityModal.HISTORY -> {
                modal = if (historyFromHome) AccessibilityModal.NONE else AccessibilityModal.SYSTEM_SETTINGS
                historyFromHome = false
            }
            modal == AccessibilityModal.SUPPORT ->
                modal = AccessibilityModal.SYSTEM_SETTINGS
            // "רק אנשים שאבחר" (ALLOWED_RECIPIENTS) is opened from a moment's audience picker, which
            // REPLACES the edit-page modal. Back must ALWAYS land on Home, never re-open the edit
            // page — pinned explicitly here so it stays Home even if modal-stacking ever changes.
            modal == AccessibilityModal.ALLOWED_RECIPIENTS ->
                { modal = AccessibilityModal.NONE; recipientsRefresh++ }
            modal != AccessibilityModal.NONE -> { modal = AccessibilityModal.NONE; recipientsRefresh++ }
            tab != AccessibilityTab.HOME -> tab = AccessibilityTab.HOME
        }
    }

    // Silent renew: when the token is valid-but-stale (>=24h), refresh it in the background.
    // Renewed -> back to AUTHENTICATED; Revoked (server 401) -> LOCK; transient NetworkError ->
    // leave AUTHENTICATED behavior intact (do NOT lock on a flaky connection).
    LaunchedEffect(authStatus) {
        if (authStatus == com.followupnadlan.auth.OtpGateStatus.NEEDS_RENEW) {
            val token = authTokenStore.load().token
            when (val result = com.followupnadlan.auth.OtpApiClient().renew(token)) {
                is com.followupnadlan.auth.OtpApiClient.RenewResult.Renewed -> {
                    authTokenStore.markRenewed(result.token, result.expiresAtMs, System.currentTimeMillis())
                    authStatus = com.followupnadlan.auth.OtpGateStatus.AUTHENTICATED
                }
                is com.followupnadlan.auth.OtpApiClient.RenewResult.Revoked -> {
                    authTokenStore.markLocked(true)
                    authStatus = com.followupnadlan.auth.OtpGateStatus.LOCKED
                }
                is com.followupnadlan.auth.OtpApiClient.RenewResult.NetworkError -> {
                    // Transient — treat as authenticated for this session; retry on next launch.
                    authStatus = com.followupnadlan.auth.OtpGateStatus.AUTHENTICATED
                }
            }
        }
    }

    if (authStatus == com.followupnadlan.auth.OtpGateStatus.LOCKED) {
        Surface(modifier = Modifier.fillMaxSize(), color = AccessibilityColors.ScreenBackground) {
            com.followupnadlan.auth.LoginScreen(
                authTokenStore = authTokenStore,
                myDetailsStore = myDetailsStore,
                onAuthenticated = { authStatus = com.followupnadlan.auth.OtpGateStatus.AUTHENTICATED }
            )
        }
        return
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
                    // tab is always HOME in MVP-1 (no bottom nav surfaces ACTIVITY/SETTINGS).
                    AccessibilityModal.NONE -> {
                        val missedTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.MISSED_CALL, selectedMissedId
                        )
                        val endedTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.CALL_ENDED, selectedEndedId
                        )
                        val noAnswerTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.NO_ANSWER_OUTGOING, selectedNoAnswerId
                        )
                        val homeWarning = HomeWarningLogic.warningFor(
                            HomeWarningState(
                                serviceEnabled = bridgingEnabled && callDetectionPreferences.isEnabled(),
                                phoneStatePermissionGranted = phoneStateGranted,
                                callLogPermissionGranted = callLogGranted,
                                profileEmpty = signaturePreview.isBlank()
                            )
                        )
                        // The formatted TEXT business card, built once from the profile and shown in
                        // whichever moments have their card toggle ON (and appended to their sends).
                        val cardText = ContactTextCard.build(myDetailsStore.load())
                        // Greeting first name from the real profile; falls back to "דני" when empty.
                        val greetingName = ContactCard.fromProfile(myDetailsStore.load()).fullName
                            .trim().split(" ").firstOrNull { it.isNotBlank() } ?: "דני"
                        // VALUE BADGE N — real today-count from the FollowUp action log (WIRING C).
                        val todayCount = HomeTodayCount.of(logStore.load())
                        HomeScreen(
                            greetingName = greetingName,
                            todayCount = todayCount,
                            warning = homeWarning,
                            masterEnabled = bridgingEnabled,
                            missedEnabled = missedMomentEnabled,
                            endedEnabled = endedMomentEnabled,
                            noAnswerEnabled = noAnswerMomentEnabled,
                            // When a moment's card is on it owns the website line, so the template's
                            // own link lines are suppressed (raw body) to avoid showing it twice.
                            missedBody = missedTemplate?.let { if (missedCardAttached) it.body.trim() else MessageComposition.build(it) }.orEmpty(),
                            endedBody = endedTemplate?.let { if (endedCardAttached) it.body.trim() else MessageComposition.build(it) }.orEmpty(),
                            noAnswerBody = noAnswerTemplate?.let { if (noAnswerCardAttached) it.body.trim() else MessageComposition.build(it) }.orEmpty(),
                            missedCardAttached = missedCardAttached,
                            endedCardAttached = endedCardAttached,
                            noAnswerCardAttached = noAnswerCardAttached,
                            cardText = cardText,
                            // The missed card's send-mode chip. §2: "נשלח אוטומטי" only when the moment
                            // is AUTOMATIC AND the Accessibility service is actually enabled; else "ידני".
                            missedSendMode = MissedSendModeLabel.of(
                                approvalIsAutomatic = approvalMode == MomentApprovalMode.AUTOMATIC,
                                accessibilityEnabled = accessibilityEnabled
                            ),
                            onToggleMaster = {
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
                            onToggleMissed = {
                                missedMomentEnabled = !missedMomentEnabled
                                settings.missedMomentEnabled = missedMomentEnabled
                            },
                            onToggleEnded = {
                                endedMomentEnabled = !endedMomentEnabled
                                settings.endedMomentEnabled = endedMomentEnabled
                            },
                            onToggleNoAnswer = {
                                noAnswerMomentEnabled = !noAnswerMomentEnabled
                                settings.noAnswerMomentEnabled = noAnswerMomentEnabled
                            },
                            onOpenMissedJourney = { modal = AccessibilityModal.MISSED_JOURNEY },
                            onOpenEndedJourney = { modal = AccessibilityModal.ENDED_JOURNEY },
                            onOpenNoAnswerJourney = { modal = AccessibilityModal.NO_ANSWER_JOURNEY },
                            // The ⚠️ leads to whichever fix it is about: an empty profile opens the
                            // card editor, anything else opens the enable/permissions flow.
                            onResolveWarning = {
                                if (homeWarning == HomeWarning.EMPTY_PROFILE) {
                                    cardEditorOpen = true
                                } else {
                                    modal = AccessibilityModal.SETUP
                                }
                            },
                            // "✏️ ערוך פרטים" on the My-Card asset opens the existing global profile
                            // editor via the same boolean that already gates SignatureCardEditorScreen.
                            onEditCard = { cardEditorOpen = true },
                            onOpenSystemSettings = { modal = AccessibilityModal.SYSTEM_SETTINGS },
                            onOpenHistory = { historyFromHome = true; modal = AccessibilityModal.HISTORY },
                            // "נסה על עצמך": compose the EXACT missed-call message the engine would
                            // send (§2 — same composition), then open WhatsApp to that number with the
                            // message prepared, so the agent sees precisely what a client gets and taps
                            // send themselves. Deliberately bypasses the recipient/cooldown filters —
                            // this is a user-initiated preview on a number they typed, not a real call.
                            onSimulateMissed = { rawNumber ->
                                val normalized = PhoneNumberNormalizer.normalizeForWhatsApp(rawNumber)
                                val message = MissedCallAutoResponseHandler(appContext)
                                    .composeMissedMessageFor(rawNumber)
                                val outcome = when {
                                    normalized.isNullOrBlank() -> "צריך מספר תקין"
                                    message.isBlank() -> "אין תבנית הודעה — ערוך את התרחיש קודם"
                                    else -> {
                                        val opened = WhatsAppReplySender(appContext).openPreparedReply(
                                            normalizedPhone = normalized,
                                            message = message,
                                            packageName = preferredWhatsAppPackage
                                        )
                                        if (opened == WhatsAppReplyOpenResult.OPENED) {
                                            "נפתח WhatsApp עם ההודעה — כך הלקוח יראה אותה"
                                        } else {
                                            "לא הצלחנו לפתוח WhatsApp — בדוק שהוא מותקן"
                                        }
                                    }
                                }
                                undoScope.launch {
                                    snackbarHostState.showSnackbar(outcome, withDismissAction = true)
                                }
                            }
                        )
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


                    AccessibilityModal.MISSED_JOURNEY -> {
                        val missedTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.MISSED_CALL, selectedMissedId
                        )
                        MomentEditScreen(
                            title = "אם לא עניתי",
                            kind = MomentEditKind.MISSED,
                            isEnabled = missedMomentEnabled,
                            activeBody = missedTemplate?.let { if (missedCardAttached) it.body.trim() else MessageComposition.build(it) }.orEmpty(),
                            channelLabel = channelLabel(selectedChannel),
                            availableChannels = FollowUpChannelSettings.available(whatsappAvailability.businessInstalled),
                            selectedChannel = selectedChannel,
                            approvalMode = approvalMode,
                            accessibilityEnabled = accessibilityEnabled,
                            generalSameNumberCooldown = sameNumberCooldown,
                            generalGlobalQuiet = globalQuiet,
                            sameNumberChoice = missedSameNumberChoice,
                            globalQuietChoice = missedGlobalQuietChoice,
                            scopeOverride = missedScopeOverride,
                            cardAttached = missedCardAttached,
                            cardText = ContactTextCard.build(myDetailsStore.load()),
                            selectedPeoplePreview = selectedPeoplePreviewFor(missedScopeOverride),
                            variants = momentVariantsFor(TemplateRole.MISSED_CALL),
                            onToggleEnabled = {
                                missedMomentEnabled = !missedMomentEnabled
                                settings.missedMomentEnabled = missedMomentEnabled
                            },
                            onSelectChannel = ::onMomentSelectChannel,
                            onSelectApprovalMode = ::onMomentSelectApproval,
                            onOpenAccessibilitySettings = { context.openAccessibilitySettings() },
                            onSelectSameNumberCooldown = { onMomentSelectSameNumberCooldown(MomentEditKind.MISSED, it) },
                            onSelectGlobalQuiet = { onMomentSelectGlobalQuiet(MomentEditKind.MISSED, it) },
                            onSelectScope = { onMomentSelectScope(MomentEditKind.MISSED, it) },
                            onToggleCardAttached = { onMomentToggleCardAttached(MomentEditKind.MISSED) },
                            onEditSelectedPeople = { modal = AccessibilityModal.ALLOWED_RECIPIENTS },
                            onBack = { modal = AccessibilityModal.NONE }
                        )
                    }

                    AccessibilityModal.ENDED_JOURNEY -> {
                        val endedTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.CALL_ENDED, selectedEndedId
                        )
                        MomentEditScreen(
                            title = "אחרי שדיברנו",
                            kind = MomentEditKind.ENDED,
                            isEnabled = endedMomentEnabled,
                            activeBody = endedTemplate?.let { if (endedCardAttached) it.body.trim() else MessageComposition.build(it) }.orEmpty(),
                            channelLabel = channelLabel(selectedChannel),
                            availableChannels = FollowUpChannelSettings.available(whatsappAvailability.businessInstalled),
                            selectedChannel = selectedChannel,
                            approvalMode = approvalMode,
                            accessibilityEnabled = accessibilityEnabled,
                            generalSameNumberCooldown = sameNumberCooldown,
                            generalGlobalQuiet = globalQuiet,
                            sameNumberChoice = endedSameNumberChoice,
                            globalQuietChoice = endedGlobalQuietChoice,
                            scopeOverride = endedScopeOverride,
                            cardAttached = endedCardAttached,
                            cardText = ContactTextCard.build(myDetailsStore.load()),
                            selectedPeoplePreview = selectedPeoplePreviewFor(endedScopeOverride),
                            variants = momentVariantsFor(TemplateRole.CALL_ENDED),
                            onToggleEnabled = {
                                endedMomentEnabled = !endedMomentEnabled
                                settings.endedMomentEnabled = endedMomentEnabled
                            },
                            onSelectChannel = ::onMomentSelectChannel,
                            onSelectApprovalMode = ::onMomentSelectApproval,
                            onOpenAccessibilitySettings = { context.openAccessibilitySettings() },
                            onSelectSameNumberCooldown = { onMomentSelectSameNumberCooldown(MomentEditKind.ENDED, it) },
                            onSelectGlobalQuiet = { onMomentSelectGlobalQuiet(MomentEditKind.ENDED, it) },
                            onSelectScope = { onMomentSelectScope(MomentEditKind.ENDED, it) },
                            onToggleCardAttached = { onMomentToggleCardAttached(MomentEditKind.ENDED) },
                            onEditSelectedPeople = { modal = AccessibilityModal.ALLOWED_RECIPIENTS },
                            onBack = { modal = AccessibilityModal.NONE }
                        )
                    }

                    AccessibilityModal.NO_ANSWER_JOURNEY -> {
                        val noAnswerTemplate = TemplateRoleSelector.forRole(
                            templates, TemplateRole.NO_ANSWER_OUTGOING, selectedNoAnswerId
                        )
                        MomentEditScreen(
                            title = "לא ענו לי",
                            kind = MomentEditKind.NO_ANSWER,
                            isEnabled = noAnswerMomentEnabled,
                            activeBody = noAnswerTemplate?.let { if (noAnswerCardAttached) it.body.trim() else MessageComposition.build(it) }.orEmpty(),
                            channelLabel = channelLabel(selectedChannel),
                            availableChannels = FollowUpChannelSettings.available(whatsappAvailability.businessInstalled),
                            selectedChannel = selectedChannel,
                            approvalMode = approvalMode,
                            accessibilityEnabled = accessibilityEnabled,
                            generalSameNumberCooldown = sameNumberCooldown,
                            generalGlobalQuiet = globalQuiet,
                            sameNumberChoice = noAnswerSameNumberChoice,
                            globalQuietChoice = noAnswerGlobalQuietChoice,
                            scopeOverride = noAnswerScopeOverride,
                            cardAttached = noAnswerCardAttached,
                            cardText = ContactTextCard.build(myDetailsStore.load()),
                            selectedPeoplePreview = selectedPeoplePreviewFor(noAnswerScopeOverride),
                            variants = momentVariantsFor(TemplateRole.NO_ANSWER_OUTGOING),
                            onToggleEnabled = {
                                noAnswerMomentEnabled = !noAnswerMomentEnabled
                                settings.noAnswerMomentEnabled = noAnswerMomentEnabled
                            },
                            onSelectChannel = ::onMomentSelectChannel,
                            onSelectApprovalMode = ::onMomentSelectApproval,
                            onOpenAccessibilitySettings = { context.openAccessibilitySettings() },
                            onSelectSameNumberCooldown = { onMomentSelectSameNumberCooldown(MomentEditKind.NO_ANSWER, it) },
                            onSelectGlobalQuiet = { onMomentSelectGlobalQuiet(MomentEditKind.NO_ANSWER, it) },
                            onSelectScope = { onMomentSelectScope(MomentEditKind.NO_ANSWER, it) },
                            onToggleCardAttached = { onMomentToggleCardAttached(MomentEditKind.NO_ANSWER) },
                            onEditSelectedPeople = { modal = AccessibilityModal.ALLOWED_RECIPIENTS },
                            onBack = { modal = AccessibilityModal.NONE }
                        )
                    }

                    AccessibilityModal.SYSTEM_SETTINGS -> SystemSettingsScreen(
                        permissions = PermissionSnapshot(
                            phoneStateGranted = phoneStateGranted,
                            callLogGranted = callLogGranted,
                            contactsGranted = contactsGranted,
                            accessibilityEnabled = accessibilityEnabled
                        ),
                        exclusionsPreview = remember(recipientsRefresh) {
                            RecipientPreviewLogic.summary(exclusionsStore.load().map { it.label })
                        },
                        diagnosticsSnapshot = diagnosticsSnapshot,
                        onRequestPermissions = {
                            callDetectionPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_CONTACTS
                                )
                            )
                        },
                        // Per-permission "אפשר": request exactly that one runtime permission.
                        onRequestPermission = { permission ->
                            singlePermissionLauncher.launch(androidPermissionFor(permission))
                        },
                        // The optional Accessibility row's "הפעל": open the OS accessibility list so
                        // the user can flip our service on. State refreshes on resume.
                        onOpenAccessibilitySettings = { context.openAccessibilitySettings() },
                        onOpenSmartRules = { modal = AccessibilityModal.SMART_RULES },
                        onOpenHistory = { historyFromHome = false; modal = AccessibilityModal.HISTORY },
                        onOpenSupport = { modal = AccessibilityModal.SUPPORT },
                        onDeleteHistory = { logStore.clear() },
                        onBack = { modal = AccessibilityModal.NONE }
                    )

                    AccessibilityModal.HISTORY -> HistoryScreen(
                        logStore = logStore,
                        // Return to wherever the log was opened from: Home (header clock) or settings.
                        onBack = {
                            modal = if (historyFromHome) AccessibilityModal.NONE else AccessibilityModal.SYSTEM_SETTINGS
                            historyFromHome = false
                        }
                    )

                    AccessibilityModal.SUPPORT -> SupportScreen(
                        onBack = { modal = AccessibilityModal.SYSTEM_SETTINGS }
                    )

                    AccessibilityModal.SMART_RULES -> SmartRulesScreen(
                        workingHoursSettings = workingHoursSettings,
                        exclusionsStore = exclusionsStore,
                        generalScope = generalScope,
                        generalSameNumberCooldown = sameNumberCooldown,
                        generalGlobalQuiet = globalQuiet,
                        onSelectGeneralSameNumberCooldown = ::onSelectGeneralSameNumberCooldown,
                        onSelectGeneralGlobalQuiet = ::onSelectGeneralGlobalQuiet,
                        onSelectGeneralScope = { scope ->
                            generalScope = scope
                            generalScopeSettings.scope = scope
                            // "רק אנשים שאבחר" needs a list to mean anything — go build it.
                            if (scope == RecipientScope.ONLY_SELECTED) {
                                modal = AccessibilityModal.ALLOWED_RECIPIENTS
                            }
                        },
                        exclusionsPreview = remember(recipientsRefresh) {
                            RecipientPreviewLogic.summary(exclusionsStore.load().map { it.label })
                        },
                        onOpenExclusions = { modal = AccessibilityModal.EXCLUSIONS },
                        onBack = { modal = AccessibilityModal.NONE; recipientsRefresh++ }
                    )

                    AccessibilityModal.MISSED_CALL_PROMPT -> MissedCallPromptScreen(
                        phone = missedCallLaunch.phone,
                        leadName = missedCallLaunch.leadName,
                        callTimestampMs = missedCallLaunch.callTimestampMs,
                        message = missedCallLaunch.message,
                        mode = FollowUpPromptModeLogic.fromCallType(missedCallLaunch.callType),
                        templates = templates,
                        selectedEndedId = selectedEndedId,
                        selectedMissedId = selectedMissedId,
                        selectedNoAnswerId = selectedNoAnswerId,
                        preferredWhatsAppPackage = preferredWhatsAppPackage,
                        missedCardAttached = missedCardAttached,
                        endedCardAttached = endedCardAttached,
                        noAnswerCardAttached = noAnswerCardAttached,
                        card = ContactCard.fromProfile(myDetailsStore.load()),
                        onDone = { modal = AccessibilityModal.NONE }
                    )
                }
                }

                // Part B: the per-variant message editor, opened from a moment's edit screen.
                // Edits the variant *body* only — links (card/website) stay in their own fields, so
                // MessageComposition keeps appending them once. A blank/new target creates a fresh
                // variant for the role (and becomes the active one on save).
                messageEditorTarget?.let { target ->
                    val role = target.role
                    val editingTemplate = if (target.isNew) null else templates.firstOrNull { it.id == target.variantId }
                    // An edit target whose variant vanished (deleted elsewhere) just closes.
                    if (!target.isNew && editingTemplate == null) {
                        messageEditorTarget = null
                    } else {
                        val isReminder = role == TemplateRole.CALL_ENDED
                        MessageEditorScreen(
                            isEnded = isReminder,
                            body = editingTemplate?.body.orEmpty(),
                            // The card preview inside the editor reflects the ended moment's own
                            // per-moment card flag (the add/remove control now lives on the edit page).
                            cardAttached = endedCardAttached,
                            titleOverride = if (role == TemplateRole.NO_ANSWER_OUTGOING) "לא ענו" else null,
                            // The vCard/delay toggles belong to the ended moment only.
                            showCardToggle = role == TemplateRole.CALL_ENDED,
                            cardText = ContactTextCard.build(myDetailsStore.load()),
                            onToggleCardAttached = {
                                endedCardAttached = !endedCardAttached
                                endedCardSettings.cardAttached = endedCardAttached
                            },
                            onSave = { newBody ->
                                if (editingTemplate != null) {
                                    templateStore.saveTemplate(editingTemplate.copy(body = newBody))
                                } else {
                                    // New variant: title mirrors the role default's title; becomes active.
                                    val added = templateStore.addTemplate(
                                        title = templateRoleLabel(role),
                                        body = newBody,
                                        role = role
                                    )
                                    if (added != null) {
                                        when (role) {
                                            TemplateRole.MISSED_CALL -> {
                                                selectedMissedId = added.id; settings.selectedMissedTemplateId = added.id
                                            }
                                            TemplateRole.CALL_ENDED -> {
                                                selectedEndedId = added.id; settings.selectedEndedTemplateId = added.id
                                            }
                                            TemplateRole.NO_ANSWER_OUTGOING -> {
                                                selectedNoAnswerId = added.id; settings.selectedNoAnswerTemplateId = added.id
                                            }
                                        }
                                    }
                                }
                                templates = templateStore.loadTemplates()
                                messageEditorTarget = null
                            },
                            onBack = { messageEditorTarget = null }
                        )
                    }
                }

                // Tapping the card in the ended preview edits its three fields in place, via the
                // dedicated SignatureCardEditorScreen.
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

// ===================== MOMENT EDIT SCREEN (edit-scenario-fixed.html) =====================
/**
 * The per-moment edit page reached from Home's "עריכה והגדרות". One unified screen for all three
 * moments (missed / ended / no-answer), matching edit-scenario-fixed.html: nav bar + per-moment
 * toggle, a LIVE WhatsApp preview bubble, a clean up-to-5 template list, and one unified
 * settings card (channel / approval / audience deep-link / 24h frequency).
 *
 * WYSIWYG: the hero bubble shows the ACTIVE variant's text; selecting another template below sets it
 * active and the bubble updates immediately (the active id comes from [variants], which the caller
 * re-derives from the store on each recomposition). The channel tag reflects [channelLabel].
 */
@Composable
private fun MomentEditScreen(
    title: String,
    kind: MomentEditKind,
    isEnabled: Boolean,
    activeBody: String,
    channelLabel: String,
    availableChannels: List<FollowUpChannel>,
    selectedChannel: FollowUpChannel,
    approvalMode: MomentApprovalMode,
    accessibilityEnabled: Boolean,
    generalSameNumberCooldown: Long?,
    generalGlobalQuiet: Long?,
    sameNumberChoice: CooldownChoice,
    globalQuietChoice: CooldownChoice,
    scopeOverride: RecipientScope?,
    cardAttached: Boolean,
    cardText: String,
    selectedPeoplePreview: RecipientPreview?,
    variants: MomentVariants,
    onToggleEnabled: () -> Unit,
    onSelectChannel: (FollowUpChannel) -> Unit,
    onSelectApprovalMode: (MomentApprovalMode) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSelectSameNumberCooldown: (CooldownChoice) -> Unit,
    onSelectGlobalQuiet: (CooldownChoice) -> Unit,
    onSelectScope: (RecipientScope?) -> Unit,
    onToggleCardAttached: () -> Unit,
    onEditSelectedPeople: () -> Unit,
    onBack: () -> Unit
) {
    val colors = AccessibilityExtra.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NAV BAR — back "חזרה" (primary, right), moment title, per-moment enable toggle (left).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onBack),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    AccessibilityIcons.ChevronStart,
                    contentDescription = "חזרה",
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text("חזרה", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = colors.heading)
            Switch(checked = isEnabled, onCheckedChange = { onToggleEnabled() })
        }

        // HERO — live preview card (chat-bg), tag row + channel name, then the WhatsApp bubble
        // carrying the ACTIVE template text + bold signature. No fabricated time / ✓✓ read-receipt.
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFEFEAE2),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0D000000)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("תצוגה מקדימה חיה", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textMuted)
                    Text(channelLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.primary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                    color = Color(0xFFD9FDD3),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = activeBody.ifBlank { " " },
                            color = Color(0xFF111B21),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                        // WAVE G: card OFF ⇒ body only, no signature at all anymore (the standalone
                        // one-line signature is never drawn here — matches the send path).
                        // WYSIWYG: the formatted text card appears here live when this moment's card
                        // toggle is ON, and disappears when it flips OFF — same formatter as send.
                        if (cardAttached && cardText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ContactTextCardBubble(raw = cardText)
                        }
                        // No fabricated timestamp / read-receipt (§2) — see FollowUp honesty note:
                        // the app has no WhatsApp receipt access, so a blue ✓✓ would falsely read
                        // as "delivered & read".
                    }
                }
                // TASK 2: the reusable card chip sits next to the preview bubble (replacing the old
                // settings Switch). On the edit page it PERSISTS to the moment's saved card setting
                // (unlike the sheet's per-send flag). Hidden entirely when the profile has no card.
                if (cardText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        CardAttachChip(attached = cardAttached, onToggle = onToggleCardAttached)
                    }
                }
            }
        }

        // CARD "תבניות הודעה" — clean variant list (up to 5). Each row: custom radio + text +
        // discreet ערוך / מחק text links. Selecting a row sets it active (updates the hero).
        AppCard(cornerRadius = 20, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("תבניות הודעה", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = colors.heading)
                Spacer(modifier = Modifier.height(14.dp))
                variants.variants.forEach { variant ->
                    CleanTemplateRow(
                        text = variant.body.trim().ifBlank { "(נוסח ריק)" },
                        selected = variant.id == variants.activeId,
                        canDelete = variants.variants.size > 1,
                        onSelect = { variants.onSelectActive(variant.id) },
                        onEdit = { variants.onEditVariant(variant.id) },
                        onDelete = { variants.onDeleteVariant(variant.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                // "+ הוסף נוסח חדש" dashed button — hidden at the max-5 cap (canAdd=false).
                if (variants.canAdd) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFFC1C7CB), RoundedCornerShape(12.dp))
                            .background(Color(0xFFF4F6F8))
                            .clickable(onClick = variants.onAddVariant)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ הוסף נוסח חדש", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Text(
                        "הגעת ל-5 נוסחים (המקסימום)",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        // CARD "הגדרות תרחיש וערוץ" — unified settings: channel / approval / audience / frequency.
        AppCard(cornerRadius = 20, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("הגדרות תרחיש וערוץ", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = colors.heading)

                // ערוץ שליחה — WhatsApp / WhatsApp Business (only when installed) / SMS.
                SettingRow(
                    title = "ערוץ שליחה",
                    subtitle = "באיזו אפליקציה יישלחו ההודעות",
                    showDivider = true
                ) {
                    SettingSelect(
                        selectedLabel = channelLabel(selectedChannel),
                        options = availableChannels.map { it to channelLabel(it) },
                        onSelect = onSelectChannel
                    )
                }

                // אישור לפני שליחה — automatic send is offered ONLY for the missed moment (the user
                // is away). Ended + no-answer happen while holding the phone, so they are ALWAYS
                // manual: a locked "ידני · באישור שלך" info row, no select. This only changes what the
                // page shows; the underlying automation flag still affects the missed path only.
                SettingRow(
                    title = "אישור לפני שליחה",
                    subtitle = "האם לשלוח לבד או לבקש אישור",
                    // The §2 note below needs to sit above the divider when shown.
                    showDivider = !(kind == MomentEditKind.MISSED &&
                        approvalMode == MomentApprovalMode.AUTOMATIC && !accessibilityEnabled)
                ) {
                    if (kind == MomentEditKind.MISSED) {
                        SettingSelect(
                            selectedLabel = approvalModeLabel(approvalMode),
                            options = listOf(
                                MomentApprovalMode.AUTOMATIC to approvalModeLabel(MomentApprovalMode.AUTOMATIC),
                                MomentApprovalMode.MANUAL to approvalModeLabel(MomentApprovalMode.MANUAL)
                            ),
                            onSelect = onSelectApprovalMode
                        )
                    } else {
                        // Locked, static: this moment never sends by itself.
                        Text(
                            "ידני · באישור שלך",
                            color = colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // §2 / TASK 3.3: automatic picked but Accessibility off ⇒ it won't actually auto-send.
                // Don't block the choice — tell the user + give the one-tap path to the OS screen.
                if (kind == MomentEditKind.MISSED &&
                    approvalMode == MomentApprovalMode.AUTOMATIC && !accessibilityEnabled
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "בחרת שליחה אוטומטית, אבל הרשאת 'נגישות' עדיין כבויה — עד שתפעיל/י אותה, ההודעה לא תישלח לבד.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = colors.textMuted
                        )
                        Text(
                            "הפעל נגישות ›",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable(onClick = onOpenAccessibilitySettings)
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F2F5)))
                }

                // מי יקבל את ההודעה? — real per-moment picker. "כמו הכללי" (null) follows the general
                // default set in Smart-Rules; any concrete scope is a per-moment override.
                SettingRow(
                    title = "מי יקבל את ההודעה?",
                    subtitle = "בחר/י למי לשלוח את ההודעה בתרחיש הזה",
                    showDivider = selectedPeoplePreview == null
                ) {
                    SettingSelect(
                        selectedLabel = momentScopeLabel(scopeOverride),
                        options = momentScopeOptions(),
                        onSelect = onSelectScope
                    )
                }

                // When the resolved scope is "רק אנשים שאבחר", show the chosen people inline so the
                // user sees who is on the list without opening the ALLOWED_RECIPIENTS screen.
                selectedPeoplePreview?.let { preview ->
                    SelectedPeopleInlineRow(preview = preview, onEditList = onEditSelectedPeople)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F2F5)))
                }

                // הגבלת תדירות — per-moment now (override-on-default). "לפי הכללי (X)" inherits the
                // general default set in הגדרות חכמות; a concrete duration or "בלי המתנה" overrides it
                // for THIS moment only, without touching the general or the other moments.
                SettingRow(
                    title = "הגבלת תדירות",
                    subtitle = "כל כמה זמן לשלוח שוב לאותו אדם · ברירת המחדל נקבעת ב'הגדרות חכמות'",
                    showDivider = true
                ) {
                    SettingSelect(
                        selectedLabel = momentCooldownLabel(
                            FollowUpCooldownOptions.sameNumber, generalSameNumberCooldown, sameNumberChoice
                        ),
                        options = momentCooldownOptions(FollowUpCooldownOptions.sameNumber, generalSameNumberCooldown),
                        onSelect = onSelectSameNumberCooldown
                    )
                }

                // מרווח מינימלי בין הודעות — the anti-burst brake, also per-moment override-on-default.
                SettingRow(
                    title = "מרווח מינימלי בין הודעות",
                    subtitle = "מרווח מינימלי בין שתי הודעות · ברירת המחדל נקבעת ב'הגדרות חכמות'",
                    showDivider = false
                ) {
                    SettingSelect(
                        selectedLabel = momentCooldownLabel(
                            FollowUpCooldownOptions.globalQuiet, generalGlobalQuiet, globalQuietChoice
                        ),
                        options = momentCooldownOptions(FollowUpCooldownOptions.globalQuiet, generalGlobalQuiet),
                        onSelect = onSelectGlobalQuiet
                    )
                }
            }
        }
    }
}

/**
 * Inline preview of the "רק אנשים שאבחר" list on the moment edit page: a compact summary of the
 * chosen people ("נשלח ל: יוסי, דנה, ועוד 4") + an "ערוך רשימה ›" link into ALLOWED_RECIPIENTS.
 * When the list is empty, a gentle prompt replaces the summary. Reuses [RecipientPreviewLogic]'s
 * pure summary (see [SelectedPeopleInlineLogic]); no store logic here.
 */
@Composable
private fun SelectedPeopleInlineRow(preview: RecipientPreview, onEditList: () -> Unit) {
    val colors = AccessibilityExtra.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = SelectedPeopleInlineLogic.line(preview),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = if (preview.count == 0) colors.textMuted else colors.textStrong,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "ערוך רשימה ›",
            color = colors.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onEditList)
        )
    }
}

/**
 * Pure wording for the inline "רק אנשים שאבחר" preview. Empty list ⇒ a gentle prompt; otherwise
 * "נשלח ל: <names>[, ועוד N]". Kept pure (no Compose) so it can be asserted in a test.
 */
internal object SelectedPeopleInlineLogic {
    fun line(preview: RecipientPreview): String {
        if (preview.count == 0) return "עדיין לא נבחרו אנשים"
        val names = preview.names.joinToString(", ")
        return if (preview.moreCount > 0) "נשלח ל: $names, ועוד ${preview.moreCount}" else "נשלח ל: $names"
    }
}

/** Hebrew label for the approval-mode select. */
private fun approvalModeLabel(mode: MomentApprovalMode): String = when (mode) {
    MomentApprovalMode.AUTOMATIC -> "אוטומטי (ללא אישור)"
    MomentApprovalMode.MANUAL -> "ידני (באישור שלי)"
}

/**
 * Hebrew label for the per-moment "מי יקבל" select. `null` = "כמו הכללי" (follow the general
 * default); a concrete scope reuses the existing [recipientScopeLabel] wording (no new categories).
 */
private fun momentScopeLabel(scope: RecipientScope?): String =
    if (scope == null) FOLLOW_GENERAL_LABEL else recipientScopeLabel(scope)

/** The per-moment "מי יקבל" options: "כמו הכללי" prepended to the 4 existing categories. */
private fun momentScopeOptions(): List<Pair<RecipientScope?, String>> =
    listOf<Pair<RecipientScope?, String>>(null to FOLLOW_GENERAL_LABEL) +
        RecipientScope.entries.map { it to recipientScopeLabel(it) }

private const val FOLLOW_GENERAL_LABEL = "כמו הכללי"

/**
 * The per-moment cooldown options for one brake, as a 3-state [CooldownChoice] list:
 *   1. "לפי הכללי (X)" ⇒ [CooldownChoice.Inherit] — X is the current general label so the user sees
 *      what they'd inherit.
 *   2. the concrete durations from [FollowUpCooldownOptions] ⇒ [CooldownChoice.Value].
 *   3. "בלי המתנה" ⇒ [CooldownChoice.Off] — the option's `null` entry, kept distinct from Inherit.
 * Reuses [FollowUpCooldownOptions] verbatim; the general label reuses its own labelFor.
 */
private fun momentCooldownOptions(
    choices: List<FollowUpCooldownOptions.Choice>,
    generalMillis: Long?
): List<Pair<CooldownChoice, String>> {
    val generalLabel = FollowUpCooldownOptions.labelFor(choices, generalMillis)
    return listOf<Pair<CooldownChoice, String>>(
        CooldownChoice.Inherit to "לפי הכללי ($generalLabel)"
    ) + choices.map { choice ->
        val state: CooldownChoice = choice.millis?.let { CooldownChoice.Value(it) } ?: CooldownChoice.Off
        state to choice.label
    }
}

/** The label for a moment's current [CooldownChoice] on one brake. */
private fun momentCooldownLabel(
    choices: List<FollowUpCooldownOptions.Choice>,
    generalMillis: Long?,
    choice: CooldownChoice
): String = when (choice) {
    CooldownChoice.Inherit -> "לפי הכללי (${FollowUpCooldownOptions.labelFor(choices, generalMillis)})"
    CooldownChoice.Off -> FollowUpCooldownOptions.labelFor(choices, null)
    is CooldownChoice.Value -> FollowUpCooldownOptions.labelFor(choices, choice.millis)
}

/**
 * The pill "הסר/הוסף כרטיס" chip (Wave E), extracted so the approval sheet AND the moment-edit page
 * render the SAME control. [attached] drives the label; [onToggle] flips it. The caller decides the
 * semantics — the sheet toggles a per-send flag, the edit page persists to the moment's saved card
 * setting. The caller also decides visibility (hidden when there is no card to attach).
 */
@Composable
private fun CardAttachChip(attached: Boolean, onToggle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = AccessibilityColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccessibilityColors.CardBorder),
        modifier = Modifier.clickable(
            onClickLabel = if (attached) "הסר כרטיס ביקור מההודעה הזו" else "הוסף כרטיס ביקור להודעה הזו"
        ) { onToggle() }
    ) {
        Text(
            text = if (attached) "הסר כרטיס" else "הוסף כרטיס",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccessibilityColors.TextBody,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * One unified settings row (edit-scenario-fixed.html .setting-row): title + subtitle on the right,
 * the control ([trailing]) on the left, optional bottom divider. Padding matches the HTML's 12px.
 */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    showDivider: Boolean,
    trailing: @Composable () -> Unit
) {
    val colors = AccessibilityExtra.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textStrong)
                Text(subtitle, fontSize = 12.sp, color = colors.textMuted)
            }
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F2F5)))
        }
    }
}

/**
 * The pill "select" control (edit-scenario-fixed.html .setting-select): a tappable grey pill showing
 * the current label; tapping opens a dropdown of the options. Reused for channel and approval.
 */
@Composable
private fun <T> SettingSelect(
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    val colors = AccessibilityExtra.colors
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF4F6F8))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(selectedLabel, color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Icon(AccessibilityIcons.ExpandMore, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 14.sp, color = colors.textStrong) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    }
                )
            }
        }
    }
}

/**
 * A clean template row (edit-scenario-fixed.html .template-item): custom radio + text + discreet
 * ערוך / מחק text links below. No trash/pencil icons — just links. Selecting the row sets it active.
 */
@Composable
private fun CleanTemplateRow(
    text: String,
    selected: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AccessibilityExtra.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.primary.copy(alpha = 0.03f) else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (selected) colors.primary else Color(0xFFEDF0F2)
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Custom radio dot (filled ring when selected), matching the HTML .radio-custom.
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.primary else Color.Transparent)
                    .border(2.dp, if (selected) colors.primary else Color(0xFFC1C7CB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text, fontSize = 14.sp, lineHeight = 19.sp, color = colors.textStrong, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "ערוך",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onEdit)
                    )
                    // The last remaining variant cannot be deleted — a moment always keeps one.
                    if (canDelete) {
                        Text(
                            "מחק",
                            color = colors.danger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable(onClick = onDelete)
                        )
                    }
                }
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
    greetingName: String,
    todayCount: Int,
    warning: HomeWarning?,
    masterEnabled: Boolean,
    missedEnabled: Boolean,
    endedEnabled: Boolean,
    noAnswerEnabled: Boolean,
    missedBody: String,
    endedBody: String,
    noAnswerBody: String,
    missedCardAttached: Boolean,
    endedCardAttached: Boolean,
    noAnswerCardAttached: Boolean,
    cardText: String,
    missedSendMode: String,
    onToggleMaster: () -> Unit,
    onToggleMissed: () -> Unit,
    onToggleEnded: () -> Unit,
    onToggleNoAnswer: () -> Unit,
    onOpenMissedJourney: () -> Unit,
    onOpenEndedJourney: () -> Unit,
    onOpenNoAnswerJourney: () -> Unit,
    onResolveWarning: () -> Unit,
    onEditCard: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    // Activity log ("יומן פעילות"), one tap from Home via the header clock icon. The log is
    // operational info the user checks several times a day — not a setting — so it lives here beside
    // ⚙️ rather than buried in system settings (it is still reachable from there too).
    onOpenHistory: () -> Unit,
    // "נסה על עצמך" hook: given a phone number, compose the exact follow-up (§2) and open WhatsApp to
    // it. A "try on yourself" card at the bottom of Home drives this. Nullable only so previews/tests
    // may omit it; in the app it is always supplied.
    onSimulateMissed: ((String) -> Unit)? = null
) {
    val colors = AccessibilityExtra.colors
    // Which accordion cards are currently open. All start CLOSED; the user expands what they want.
    // Multiple may be open at once — the HTML JS only flips the tapped card's own state, never
    // force-closing the others.
    val openCards = remember { mutableStateListOf<Int>() }
    fun toggleOpen(index: Int) {
        if (openCards.contains(index)) openCards.remove(index) else openCards.add(index)
    }

    // Master OFF ⇒ every card reads as off: dimmed + not sending. Master ON ⇒ each card follows its
    // own per-moment toggle. (Kill-switch semantics, plan A.)
    val missedActive = masterEnabled && missedEnabled
    val endedActive = masterEnabled && endedEnabled
    val noAnswerActive = masterEnabled && noAnswerEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header — greeting + value badge (left column) + ⚙️ (HOME.html .header).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "שלום, $greetingName 👋",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    color = colors.heading
                )
                // VALUE BADGE — teal pill, real today-count N (WIRING C). Shown even when N == 0.
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF17B3A3).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "✨ טיפלנו ב-$todayCount לקוחות היום!",
                        color = Color(0xFF17B3A3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            // Header actions: activity log (clock) + system settings (gear). The log sits first so
            // it reads as the primary "what happened" affordance, not an afterthought of settings.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    AccessibilityIcons.Schedule,
                    contentDescription = "יומן פעילות",
                    tint = colors.textMuted,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenHistory)
                        .padding(4.dp)
                        .size(24.dp)
                )
                Icon(
                    AccessibilityIcons.Settings,
                    contentDescription = "הגדרות מערכת",
                    tint = colors.textMuted,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenSystemSettings)
                        .padding(4.dp)
                        .size(24.dp)
                )
            }
        }

        // MASTER CARD — the single dark gradient card with the real master kill-switch (WIRING A).
        // Replaces the previous pass's separate teal status-banner + "שליחת הודעות המשך" row.
        HomeMasterCard(checked = masterEnabled, onToggle = onToggleMaster)

        // Shown only when something is actually broken; otherwise Home stays silent. Not in the
        // HTML, but §2 honesty: a broken bridge the user is unaware of is a false "messages sent".
        warning?.let {
            HomeWarningRow(text = HomeWarningLogic.message(it), onClick = onResolveWarning)
        }

        // MY-CARD ASSET — the global "כרטיס הביקור שלי". A calm display of the one shared text card
        // (from MyDetailsStore, the same string that is sent), plus a single "✏️ ערוך פרטים" affordance
        // that opens the existing SignatureCardEditorScreen. No editable fields on Home. Empty profile
        // shows a gentle prompt, never a blank bubble (§2).
        HomeMyCardAsset(cardText = cardText, onEditCard = onEditCard)

        Text("התרחישים שלך (3)", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = colors.heading)

        // THREE ACCORDION CARDS. Tap header = expand/collapse; quick-toggle = per-moment enable
        // (no expand); edit button = navigate; dimmed when off (master or per-moment).
        HomeAccordionCard(
            emoji = "📞",
            title = "אם לא עניתי",
            body = missedBody,
            // §2: reflects real capability — "נשלח אוטומטי" only when automatic AND Accessibility on.
            sendMode = missedSendMode,
            enabled = missedEnabled,
            active = missedActive,
            open = openCards.contains(0),
            card = if (missedCardAttached) HomeCardPreview(text = cardText) else null,
            onHeaderClick = { toggleOpen(0) },
            onToggle = onToggleMissed,
            onEdit = onOpenMissedJourney
        )

        HomeAccordionCard(
            emoji = "🤝",
            title = "אחרי שדיברנו",
            body = endedBody,
            // Ended never auto-sends.
            sendMode = MissedSendModeLabel.MANUAL,
            enabled = endedEnabled,
            active = endedActive,
            open = openCards.contains(1),
            card = if (endedCardAttached) HomeCardPreview(text = cardText) else null,
            onHeaderClick = { toggleOpen(1) },
            onToggle = onToggleEnded,
            onEdit = onOpenEndedJourney
        )

        HomeAccordionCard(
            emoji = "📵",
            title = "לא ענו לי",
            body = noAnswerBody,
            // No-answer never auto-sends.
            sendMode = MissedSendModeLabel.MANUAL,
            enabled = noAnswerEnabled,
            active = noAnswerActive,
            open = openCards.contains(2),
            card = if (noAnswerCardAttached) HomeCardPreview(text = cardText) else null,
            onHeaderClick = { toggleOpen(2) },
            onToggle = onToggleNoAnswer,
            onEdit = onOpenNoAnswerJourney
        )

        // "נסה על עצמך" — a friendly self-test any agent can run: type your own number, tap, and
        // WhatsApp opens with the exact follow-up a client would get (§2). Always shown.
        onSimulateMissed?.let { HomeTryOnYourselfCard(onTry = it) }
    }
}

/**
 * "נסה על עצמך" card. The agent types a phone number (usually their own) and taps — the app composes
 * the EXACT missed-call follow-up the engine would send (§2, same composition) and opens WhatsApp to
 * that number with it prepared, so they see precisely what a client receives and press send
 * themselves. Honest by design: the copy states this opens a real WhatsApp message, and it
 * deliberately skips the recipient/cooldown filters because it is a user-initiated preview, not a real
 * incoming call.
 */
@Composable
private fun HomeTryOnYourselfCard(onTry: (String) -> Unit) {
    val colors = AccessibilityExtra.colors
    var number by remember { mutableStateOf("") }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AccessibilityColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccessibilityColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "🧪 נסה על עצמך",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = colors.heading
            )
            Text(
                "הקלד את המספר שלך ולחץ — ייפתח WhatsApp עם ההודעה בדיוק כפי שהלקוח יקבל אותה. אתה שולח אותה לעצמך.",
                fontSize = 13.sp,
                color = colors.textMuted
            )
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                singleLine = true,
                label = { Text("המספר שלך") },
                placeholder = { Text("0521234567") },
                modifier = Modifier.fillMaxWidth()
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (number.isNotBlank()) Color(0xFF17B3A3) else Color(0xFFCED6DB),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = number.isNotBlank()) { onTry(number) }
            ) {
                Text(
                    "פתח את ההודעה ב-WhatsApp",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * MASTER CARD — dark gradient card (HOME.html .master-card) carrying the real master kill-switch.
 * Title "האפליקציה עובדת ברקע", teal subtitle, and the switch (→ global isEnabled). Master OFF dims
 * all three accordion cards and stops all sending (handled by the caller via `active` flags).
 */
@Composable
private fun HomeMasterCard(checked: Boolean, onToggle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF111B21), Color(0xFF1E2D24))))
                .clickable(onClick = onToggle)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "האפליקציה עובדת ברקע",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "מזהה שיחות ומכינה הודעות המשך",
                    color = Color(0xFF17B3A3),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

/**
 * MY-CARD ASSET — the global "כרטיס הביקור שלי" card at the top of Home. Displays the single shared
 * text business card ([cardText], the exact string that is also sent, from MyDetailsStore) using the
 * SAME styled renderer the moment bubbles use ([ContactTextCardBubble]) so preview == sent (§2). One
 * discreet "✏️ ערוך פרטים" affordance opens the existing global profile editor via [onEditCard]. When
 * the profile has no name ([cardText] blank), a gentle prompt is shown instead of a blank bubble.
 * Display + one edit action only — no editable fields live on Home.
 */
@Composable
private fun HomeMyCardAsset(cardText: String, onEditCard: () -> Unit) {
    val colors = AccessibilityExtra.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0A000000)),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🪪", fontSize = 17.sp)
                    Text(
                        text = "כרטיס הביקור שלי",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = colors.textStrong
                    )
                }
                // The single edit affordance → opens the existing SignatureCardEditorScreen.
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable(onClick = onEditCard)
                ) {
                    Text(
                        text = "✏️ ערוך פרטים",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (cardText.isBlank()) {
                // Empty profile — gentle prompt, never a blank bubble (§2).
                Text(
                    text = "עדיין לא הגדרת כרטיס ביקור. הוסיפו את הפרטים שלכם כדי לצרף אותם להודעות ההמשך.",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            } else {
                // WhatsApp chat-bubble on the chat background, matching the moment previews.
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                    color = Color(0xFFD9FDD3),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFEAE2), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        ContactTextCardBubble(raw = cardText)
                    }
                }
            }
        }
    }
}

/** The formatted text card for a Home accordion bubble — the exact string that is also sent. */
private data class HomeCardPreview(val text: String)

/** WhatsApp link blue and the grey hint colour, straight from app-text-vcard.html. */
private val WaLinkBlue = Color(0xFF027EB5)
private val WaHintGrey = Color(0xFF54656F)

/**
 * Renders the formatted TEXT business card ([raw] = the exact string that is also sent) with
 * WhatsApp-style formatting applied from that single source string (§2): `*bold*` → bold, bare
 * URLs/phones → link blue + underline, `_italic_` hint line → smaller, grey, italic. Line breaks
 * are preserved (the raw text carries them). Never maintains a second string.
 */
@Composable
internal fun ContactTextCardBubble(raw: String) {
    val hintRaw = "_${ContactTextCard.SAVE_HINT}_"
    // Split the card into its lines so the grey hint line can be drawn smaller/grey while the rest
    // keeps the body size; within each line the inline styles are applied from the same source.
    val lines = raw.split("\n")
    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            if (line.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                return@forEach
            }
            val isHint = line.trim() == hintRaw
            val annotated = buildAnnotatedString {
                WhatsAppTextStyling.spans(line).forEach { span ->
                    when (span.style) {
                        WhatsAppTextStyling.SpanStyle.BOLD ->
                            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(span.text) }
                        WhatsAppTextStyling.SpanStyle.ITALIC ->
                            withStyle(androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic, color = WaHintGrey)) { append(span.text) }
                        WhatsAppTextStyling.SpanStyle.LINK ->
                            withStyle(androidx.compose.ui.text.SpanStyle(color = WaLinkBlue, textDecoration = TextDecoration.Underline)) { append(span.text) }
                        WhatsAppTextStyling.SpanStyle.PLAIN ->
                            append(span.text)
                    }
                }
            }
            Text(
                text = annotated,
                color = if (isHint) WaHintGrey else Color(0xFF111B21),
                fontSize = if (isHint) 12.sp else 14.sp,
                lineHeight = if (isHint) 16.sp else 20.sp
            )
        }
    }
}

/**
 * The small send-mode pill on an accordion header: "נשלח אוטומטי" (teal) or "ידני" (muted). The
 * label is resolved upstream by [MissedSendModeLabel] so this stays presentation-only — it never
 * decides the mode, only shows it.
 */
@Composable
private fun HomeSendModeChip(sendMode: String) {
    val colors = AccessibilityExtra.colors
    val isAuto = sendMode == MissedSendModeLabel.AUTOMATIC
    val bg = if (isAuto) Color(0xFF17B3A3).copy(alpha = 0.12f) else Color(0xFFF0F2F5)
    val fg = if (isAuto) Color(0xFF17B3A3) else colors.textMuted
    Surface(shape = RoundedCornerShape(999.dp), color = bg) {
        Text(
            text = sendMode,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
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
 * Accordion moment card (HOME.html .accordion-card). Collapsed: emoji + {bold title + one-line
 * ellipsized snippet of the active message} + quick-toggle + chevron-in-circle. Tapping the header
 * expands/collapses this card; the chevron rotates 180° and turns primary-blue when open, and the
 * snippet hides. The quick-toggle flips per-moment enable WITHOUT expanding. When off (per-moment
 * OR master) the card dims (opacity 0.5 + grayscale-ish) but stays tappable-to-expand and its
 * toggle stays interactive.
 *
 * @param enabled the per-moment toggle position (what the switch shows / persists).
 * @param active whether the moment actually sends now = master ON && enabled. Drives the dimming so
 *   master-off dims every card even though each per-moment switch keeps its own position.
 */
@Composable
private fun HomeAccordionCard(
    emoji: String,
    title: String,
    body: String,
    sendMode: String,
    enabled: Boolean,
    active: Boolean,
    open: Boolean,
    card: HomeCardPreview?,
    onHeaderClick: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val colors = AccessibilityExtra.colors
    // Dim mirrors the HTML .disabled rule (opacity 0.5 + slight grayscale). Approximated by alpha
    // on the content (Compose has no cheap grayscale filter); the card stays tappable to expand.
    val dimAlpha = if (active) 1f else 0.5f
    val chevronRotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron-rot")

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0A000000)),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.alpha(dimAlpha)) {
            // card-header — always visible; tapping it expands/collapses this card.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onHeaderClick)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // card-info: emoji + column of {title, snippet}. Snippet hides while open.
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = emoji, fontSize = 19.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = colors.textStrong
                            )
                            // Send-mode chip: how this moment sends. Subtle pill; "נשלח אוטומטי" reads
                            // teal (it acts on its own), "ידני" reads muted.
                            HomeSendModeChip(sendMode = sendMode)
                        }
                        if (!open) {
                            Text(
                                text = body.ifBlank { " " },
                                fontSize = 13.sp,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
                // card-controls: quick per-moment toggle (no expand) + chevron in a circle.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Switch(checked = enabled, onCheckedChange = { onToggle() })
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (open) colors.primary else Color(0xFFF4F6F8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AccessibilityIcons.ExpandMore,
                            contentDescription = if (open) "כווץ" else "הרחב",
                            tint = if (open) Color.White else colors.textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }
            }

            // card-body — chat-bg + WhatsApp bubble + edit button. Shown only when open.
            if (open) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFEAE2))
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                        color = Color(0xFFD9FDD3),
                        shadowElevation = 1.dp,
                        // Tapping the preview bubble also opens this moment's edit page (same route as
                        // the "✏️ עריכה והגדרות" button). Header-tap still only expands/collapses.
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = body.ifBlank { " " },
                                color = Color(0xFF111B21),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            // WAVE G: card OFF ⇒ body only, no signature at all anymore — the
                            // standalone signature is never drawn here, matching the send path (§2).

                            // Formatted TEXT business card inside the bubble — shown only when this
                            // moment's card toggle is ON. It is the exact text that is also sent
                            // (§2), rendered with WhatsApp *bold* / link / italic styling.
                            card?.takeIf { it.text.isNotBlank() }?.let {
                                Spacer(modifier = Modifier.height(10.dp))
                                ContactTextCardBubble(raw = it.text)
                            }

                            // No fabricated timestamp / read-receipt (§2): the app has no WhatsApp
                            // receipt access, so a blue ✓✓ + made-up time would falsely read as
                            // "delivered & read".
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // card-footer-actions: the ONLY route to edit from Home → navigates.
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable(onClick = onEdit)
                        ) {
                            Text(
                                text = "✏️ עריכה והגדרות",
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
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

/** Hebrew label for a template's call scenario. */
private fun templateRoleLabel(role: TemplateRole): String = when (role) {
    TemplateRole.MISSED_CALL -> "שיחה שלא נענתה"
    TemplateRole.CALL_ENDED -> "סיום שיחה"
    TemplateRole.NO_ANSWER_OUTGOING -> "לא ענו"
}

// ===================== SCREEN 4 — PURE APPROVAL BOTTOM-SHEET =====================
// "Configure once, execute effortlessly." Reached from a follow-up notification (missed / ended /
// no-answer). One decision: send the pre-configured message, or not. No chips/toggles/cursor by
// default — the artifact is shown exactly as it will be sent, read-only, with a discreet "ערוך
// הודעה" link that reveals an inline textarea + a picker of THIS moment's saved variants. Mirrors
// quick-send-pure.html. [שלח] sends via the existing verified WhatsApp path (FollowUpPromptSender,
// which stamps the moment's source so the "היום" count and history pick it up); [לא עכשיו] just
// dismisses without sending. Replaces the old chips/tags/vCard-toggle editor.

/** The context-header avatar emoji + human sub-line template per moment (matches the HTML). */
private data class PromptMomentChrome(val emoji: String, val subLinePrefix: String)

private fun promptMomentChrome(mode: FollowUpPromptMode): PromptMomentChrome = when (mode) {
    FollowUpPromptMode.MISSED_CALL -> PromptMomentChrome("📞", "שיחה שלא נענתה")
    FollowUpPromptMode.CALL_ENDED -> PromptMomentChrome("🤝", "שיחה שהסתיימה")
    FollowUpPromptMode.NO_ANSWER_OUTGOING -> PromptMomentChrome("📵", "לא ענו לשיחה שלך")
}

// Artifact palette locked to the HTML source of truth (the sent WhatsApp bubble), independent of
// the Home-screen bubble token so the Home palette stays untouched.
private val PromptSheetBg = Color(0xFFF4F7F6)
private val PromptChatBg = Color(0xFFEFEAE2)
private val PromptBubbleGreen = Color(0xFFD9FDD3)
private val PromptCheckBlue = Color(0xFF53BDEB)

@Composable
private fun MissedCallPromptScreen(
    phone: String,
    leadName: String,
    callTimestampMs: Long,
    message: String,
    mode: FollowUpPromptMode,
    templates: List<MessageTemplate>,
    selectedEndedId: String,
    selectedMissedId: String,
    selectedNoAnswerId: String,
    preferredWhatsAppPackage: String,
    missedCardAttached: Boolean,
    endedCardAttached: Boolean,
    noAnswerCardAttached: Boolean,
    card: ContactCard,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    val role = when (mode) {
        FollowUpPromptMode.MISSED_CALL -> TemplateRole.MISSED_CALL
        FollowUpPromptMode.NO_ANSWER_OUTGOING -> TemplateRole.NO_ANSWER_OUTGOING
        FollowUpPromptMode.CALL_ENDED -> TemplateRole.CALL_ENDED
    }
    val roleTemplates = templates.filter { it.role == role }
    val selectedIdForRole = when (role) {
        TemplateRole.MISSED_CALL -> selectedMissedId
        TemplateRole.NO_ANSWER_OUTGOING -> selectedNoAnswerId
        TemplateRole.CALL_ENDED -> selectedEndedId
    }
    val defaultTemplate = TemplateRoleSelector.forRole(templates, role, selectedIdForRole)
    // The formatted TEXT business card is drawn in the bubble — and appended to the sent text —
    // only when the shown moment's own card flag is on (per-moment, all three moments).
    val cardAttached = when (mode) {
        FollowUpPromptMode.MISSED_CALL -> missedCardAttached
        FollowUpPromptMode.NO_ANSWER_OUTGOING -> noAnswerCardAttached
        FollowUpPromptMode.CALL_ENDED -> endedCardAttached
    }
    // Built once from the profile; the SAME string is both previewed (styled) and sent (plain),
    // so preview == sent (§2). Empty when the profile has no name — then no card anywhere.
    val cardText = ContactTextCard.build(card)
    // Whether a card is even possible for this profile — drives whether the per-send toggle chip
    // shows at all. A nameless profile can never have a card, toggle or not.
    val cardAvailable = cardText.isNotBlank()
    // PER-SEND toggle (Wave E): seeded from the moment's saved setting, but flipping it here never
    // writes back to missedCardSettings/endedCardSettings/noAnswerCardSettings — it only changes
    // THIS outgoing message, same one-off lifetime as `draft` below.
    var cardOn by remember { mutableStateOf(cardAttached && cardAvailable) }
    // The active variant body drives the artifact. The message passed from the notification wins
    // when present (it is the exact text that was prepared); otherwise fall back to the active
    // variant. When the card is on it OWNS the website line, so the template's own link lines are
    // suppressed (raw body) to avoid showing the website twice.
    val fallbackBody = defaultTemplate?.let {
        if (cardOn) it.body.trim() else MessageComposition.build(it)
    }.orEmpty()
    // Normalise the incoming message to the PURE body: any text card and any one-line signature
    // already baked in are stripped here, so the sheet is the single owner that re-appends them
    // (signature drawn separately, card drawn styled) and the preview equals the sent text exactly,
    // for every moment — never doubling the signature or the card (§2).
    val incomingBody = message.ifBlank { fallbackBody }
    val activeBody = SignatureLine.removeFrom(ContactTextCard.removeFrom(incomingBody, card), card)

    // A one-off edit / variant pick for THIS send only; never saved to the store.
    var editing by remember { mutableStateOf(false) }
    var draft by remember(activeBody) { mutableStateOf(activeBody) }
    val resolvedMessage = draft
    var status by remember { mutableStateOf<String?>(null) }

    val chrome = promptMomentChrome(mode)
    val recipient = leadName.trim().ifBlank {
        phone.takeIf { it.isNotBlank() }?.let { PhoneNumberNormalizer.toLocalIsraeliDisplay(it) } ?: "מספר לא ידוע"
    }
    // Real relative time for {X}. When the notification carried no timestamp, fall back to "עכשיו".
    val subLine = remember(callTimestampMs, chrome.subLinePrefix) {
        val rel = if (callTimestampMs > 0L) RelativeTimeHebrew.of(callTimestampMs, System.currentTimeMillis()) else "עכשיו"
        "${chrome.subLinePrefix} $rel"
    }
    val metaTime = remember(callTimestampMs) {
        val ms = if (callTimestampMs > 0L) callTimestampMs else System.currentTimeMillis()
        java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }

    // The exact text that goes out, built from the SAME pieces the bubble previews (§2). WAVE G:
    // card ON ⇒ the formatted text card (name/role/phone live in the card); card OFF ⇒ body only,
    // no signature at all. Preview == sent for every moment.
    val outgoingMessage = if (cardOn) {
        ContactTextCard.append(resolvedMessage, card)
    } else {
        resolvedMessage.trim()
    }

    fun send() {
        val error = FollowUpPromptSender.send(context, mode, phone, outgoingMessage, preferredWhatsAppPackage)
        if (error == null) onDone() else status = error
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99111B21))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDone() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = PromptSheetBg,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* swallow taps on the sheet so they don't dismiss */ }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFC1C7CB))
                        .align(Alignment.CenterHorizontally)
                )

                // Context header: avatar + recipient + human sub-line with real relative time.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chrome.emoji, fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = recipient,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = AccessibilityColors.Heading
                        )
                        Text(
                            text = subLine,
                            fontSize = 13.sp,
                            color = AccessibilityColors.TextMuted
                        )
                    }
                }

                // Artifact zone: the final WhatsApp bubble exactly as it will be sent.
                Surface(
                    color = PromptChatBg,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0D000000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Surface(
                            color = PromptBubbleGreen,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp)) {
                                if (editing) {
                                    // EDIT MODE: inline editable text + saved-variant picker.
                                    OutlinedTextField(
                                        value = draft,
                                        onValueChange = { draft = it },
                                        minLines = 3,
                                        maxLines = 10,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = resolvedMessage.ifBlank { " " },
                                        color = AccessibilityColors.TextStrong,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                    // WAVE G: card OFF ⇒ body only, no signature at all anymore —
                                    // the standalone signature is never drawn here, matching the
                                    // send path below (§2-critical: this sheet is closest to send).
                                    if (cardOn) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        ContactTextCardBubble(raw = cardText)
                                    }
                                }
                                // Meta: the REAL call time only. No ✓✓ read-receipt — the app has
                                // no WhatsApp receipt access, so a blue ✓✓ would falsely read as
                                // "delivered & read" (§2).
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(metaTime, fontSize = 10.sp, color = AccessibilityColors.TextMuted)
                                }
                            }
                        }

                        if (editing && roleTemplates.size > 1) {
                            // Saved-variant picker for THIS moment: pick one to swap the text.
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "בחירת נוסח שמור",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccessibilityColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                roleTemplates.forEach { variant ->
                                    // §2: mirror the main sheet path (fallbackBody above). When the
                                    // card is ON it OWNS the website line, so use the raw template
                                    // body (no link lines) — otherwise picking a variant would print
                                    // the website twice (body link + card). Card OFF ⇒ unchanged.
                                    val body = if (cardOn) variant.body.trim() else MessageComposition.build(variant)
                                    val selected = body == draft
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) AccessibilityColors.GreenSurface else AccessibilityColors.Surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.5.dp,
                                            if (selected) AccessibilityColors.GreenCheck else AccessibilityColors.CardBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { draft = body }
                                    ) {
                                        Text(
                                            text = variant.title.ifBlank { TemplateCardSummary.bodyPreview(body) },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccessibilityColors.TextStrong,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Discreet edit link — nothing editable is visible until it is tapped.
                        // The per-send card chip (Wave E) sits alongside it: it only changes THIS
                        // outgoing message (never the moment's saved "צירוף כרטיס ביקור" setting) and
                        // is hidden entirely when the profile has no card to attach.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (cardAvailable) {
                                CardAttachChip(attached = cardOn, onToggle = { cardOn = !cardOn })
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            Text(
                                text = if (editing) "ביטול עריכה" else "ערוך הודעה",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccessibilityColors.TextMuted,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                modifier = Modifier
                                    .clickable {
                                        if (editing) {
                                            // Collapse back to the static artifact, discarding the ad-hoc edit.
                                            draft = activeBody
                                            editing = false
                                        } else {
                                            editing = true
                                        }
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                // Exactly two choices: send or not now.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = AccessibilityColors.Surface,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D7DB)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDone() }
                    ) {
                        Text(
                            text = "לא עכשיו",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccessibilityColors.TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                    Surface(
                        color = AccessibilityColors.Primary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { send() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AccessibilityIcons.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("שלח", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }

                status?.let {
                    Text(it, color = AccessibilityColors.Danger, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
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
    onRequestPermissions: () -> Unit,
    onRequestPermission: (FollowUpPermission) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenSmartRules: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSupport: () -> Unit,
    onDeleteHistory: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }

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

                // One row per REQUIRED permission, each stating what the user gets from it. Ungranted
                // rows carry an inline "אפשר" that requests exactly that one permission.
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
                        if (!granted) {
                            PermissionRowAction(text = "אפשר", onClick = { onRequestPermission(permission) })
                        }
                    }
                }

                // OPTIONAL capabilities, visually separated from the required three so a manual-only
                // user is never told they're "missing" something essential (§2). Accessibility only
                // unlocks automatic sending on missed calls.
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AccessibilityColors.CardBorder))
                Text(
                    "לא חובה",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccessibilityColors.TextFaint
                )
                PermissionStatusLogic.optional.forEach { capability ->
                    val granted = permissions.isGranted(capability)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                PermissionStatusLogic.title(capability),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp,
                                color = AccessibilityColors.TextStrong
                            )
                            Text(
                                PermissionStatusLogic.outcome(capability),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = AccessibilityColors.TextMuted
                            )
                        }
                        if (!granted && capability == FollowUpOptionalCapability.ACCESSIBILITY) {
                            PermissionRowAction(text = "הפעל", onClick = onOpenAccessibilitySettings)
                        }
                    }
                    // Multi-tap OS flow: name the exact item the user will see (matches the manifest label).
                    if (!granted && capability == FollowUpOptionalCapability.ACCESSIBILITY) {
                        Text(
                            "בהגדרות שייפתחו: בחר/י 'FollowUp — שליחה אוטומטית' והפעל/י.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = AccessibilityColors.TextFaint
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

        // "תזמון ההצעות" moved to the moment edit page (next to the related frequency toggle), per the
        // user — the timing controls now live where the message and audience are decided.

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
    var cardEmoji by remember { mutableStateOf(profile.cardEmoji) }
    var contactsGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_CONTACTS)) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> contactsGranted = granted }

    fun persist() {
        val normalizedEmoji = CardEmojiInput.normalize(cardEmoji)
        val trimmed = profile.copy(
            agentName = fullName.trim(),
            officeName = occupation.trim(),
            phone = phone.trim(),
            website = website.trim(),
            cardEmoji = normalizedEmoji
        )
        store.save(trimmed)
        profile = trimmed
        // Reflect the normalized value back into the field so preview == saved == sent.
        cardEmoji = normalizedEmoji
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
                LabeledField(label = "שם מלא (יופיע בחתימה ובכרטיס הביקור)", value = fullName, onChange = { fullName = it })
                LabeledField(label = "תפקיד / מקצוע (יופיע בחתימה ובכרטיס הביקור)", value = occupation, onChange = { occupation = it })
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
                LabeledField(
                    label = "אימוג'י לכרטיס (לא חובה)",
                    value = cardEmoji,
                    onChange = { cardEmoji = it }
                )
                Text(
                    "אפשר להשאיר ריק — יופיע לפני השם בכרטיס",
                    fontSize = 11.sp,
                    color = AccessibilityColors.TextMuted
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

/** Compact inline "אפשר"/"הפעל" action on an ungranted permission row. */
@Composable
private fun PermissionRowAction(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = AccessibilityColors.Primary,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
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

/** The Android runtime permission string behind a [FollowUpPermission] row. */
private fun androidPermissionFor(permission: FollowUpPermission): String = when (permission) {
    FollowUpPermission.PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
    FollowUpPermission.CALL_LOG -> Manifest.permission.READ_CALL_LOG
    FollowUpPermission.CONTACTS -> Manifest.permission.READ_CONTACTS
}

/**
 * Opens the OS accessibility list so the user can enable our service. The service state is re-read
 * on ON_RESUME when the user returns, so the row/chip update without any extra plumbing.
 */
private fun Context.openAccessibilitySettings() {
    val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

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
    generalScope: RecipientScope,
    generalSameNumberCooldown: Long?,
    generalGlobalQuiet: Long?,
    onSelectGeneralSameNumberCooldown: (Long?) -> Unit,
    onSelectGeneralGlobalQuiet: (Long?) -> Unit,
    onSelectGeneralScope: (RecipientScope) -> Unit,
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

        // General default recipient scope — "מי מקבל הודעות המשך (ברירת מחדל)". This is the value
        // every moment falls back to while it stays on "כמו הכללי" (override-on-default).
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(AccessibilityIcons.Shield, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(20.dp))
                    Text("מי מקבל הודעות המשך (ברירת מחדל)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccessibilityColors.Heading)
                }
                Text(
                    "מי מקבל הודעות ברירת מחדל",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = AccessibilityColors.TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SettingSelect(
                        selectedLabel = recipientScopeLabel(generalScope),
                        options = RecipientScope.entries.map { it to recipientScopeLabel(it) },
                        onSelect = onSelectGeneralScope
                    )
                }
            }
        }

        // General frequency brakes (default) — the single source every moment inherits while it stays
        // on "לפי הכללי". Each moment can still override these on its own edit page (override-on-
        // default). Reuses the existing FollowUpCooldownOptions verbatim.
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(AccessibilityIcons.Schedule, contentDescription = null, tint = AccessibilityColors.Primary, modifier = Modifier.size(20.dp))
                    Text("הגבלת תדירות (כללי)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccessibilityColors.Heading)
                }
                Text(
                    "ברירת המחדל לכל התרחישים · כל תרחיש יכול לעקוף בעמוד שלו.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = AccessibilityColors.TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                SettingRow(
                    title = "כל כמה זמן לשלוח שוב לאותו אדם",
                    subtitle = "מרווח מינימלי בין שתי הודעות לאותו מספר",
                    showDivider = true
                ) {
                    SettingSelect(
                        selectedLabel = FollowUpCooldownOptions.labelFor(
                            FollowUpCooldownOptions.sameNumber, generalSameNumberCooldown
                        ),
                        options = FollowUpCooldownOptions.sameNumber.map { it.millis to it.label },
                        onSelect = onSelectGeneralSameNumberCooldown
                    )
                }
                SettingRow(
                    title = "מרווח מינימלי בין הודעות",
                    subtitle = "מרווח מינימלי בין שתי הודעות כלשהן",
                    showDivider = false
                ) {
                    SettingSelect(
                        selectedLabel = FollowUpCooldownOptions.labelFor(
                            FollowUpCooldownOptions.globalQuiet, generalGlobalQuiet
                        ),
                        options = FollowUpCooldownOptions.globalQuiet.map { it.millis to it.label },
                        onSelect = onSelectGeneralGlobalQuiet
                    )
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

                val visibleGroups = RecipientRulesUi.visibleBlockGroups(generalScope)
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
                    // blacklist-count pill (smart-rules.html .blacklist-count): bg #fee2e2, red text.
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFEE2E2)) {
                        Text(
                            "${exclusionsPreview.count} מספרים",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
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
