package com.followupnadlan.accessibility

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel
import com.followupnadlan.missedcall.MissedCallWhatsAppMode
import com.followupnadlan.missedcall.WhatsAppPackageResolver
import com.followupnadlan.profile.MyDetailsStore
import com.followupnadlan.templates.MessageTemplate
import com.followupnadlan.templates.TemplateStore
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import com.followupnadlan.whatsapp.WhatsAppLinkBuilder

/** Top-level tabs in the accessibility app: בית / היום / הגדרות. */
private enum class AccessibilityTab { HOME, ACTIVITY, SETTINGS }

/** Modal screens layered above the tabs (consent, templates, exclusions, missed-call prompt). */
private enum class AccessibilityModal { NONE, SETUP, TEMPLATES, EXCLUSIONS, MISSED_CALL_PROMPT }

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
    val settings = remember(context) { MissedCallAutoResponseSettings(context.applicationContext) }
    val templateStore = remember(context) { TemplateStore(context.applicationContext) }
    val logStore = remember(context) { FollowUpLogStore(context.applicationContext) }
    val recipientScopeSettings = remember(context) { RecipientScopeSettings(context.applicationContext) }
    val exclusionsStore = remember(context) { ExclusionsStore(context.applicationContext) }
    val myDetailsStore = remember(context) { MyDetailsStore(context.applicationContext) }

    var tab by remember { mutableStateOf(AccessibilityTab.HOME) }
    var modal by remember {
        mutableStateOf(if (missedCallLaunch.fromNotification) AccessibilityModal.MISSED_CALL_PROMPT else AccessibilityModal.NONE)
    }

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

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        settings.smsFallbackEnabled = granted
        smsFallback = granted
    }

    Surface(modifier = Modifier.fillMaxSize(), color = AccessibilityColors.ScreenBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (modal) {
                    AccessibilityModal.NONE -> when (tab) {
                        AccessibilityTab.HOME -> HomeScreen(
                            bridgingEnabled = bridgingEnabled,
                            smsFallback = smsFallback,
                            onToggleBridging = {
                                if (bridgingEnabled) {
                                    settings.isEnabled = false
                                    bridgingEnabled = false
                                } else {
                                    modal = AccessibilityModal.SETUP
                                }
                            }
                        )
                        AccessibilityTab.ACTIVITY -> ActivityScreen(logStore = logStore)
                        AccessibilityTab.SETTINGS -> SettingsScreen(
                            askBeforeSend = askBeforeSend,
                            recipientScope = recipientScope,
                            preferredWhatsApp = preferredWhatsApp,
                            smsFallback = smsFallback,
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
                            onOpenTemplates = { modal = AccessibilityModal.TEMPLATES },
                            onDeleteHistory = { logStore.clear() }
                        )
                    }

                    AccessibilityModal.SETUP -> SetupConsentScreen(
                        onEnable = {
                            settings.isEnabled = true
                            bridgingEnabled = true
                            modal = AccessibilityModal.NONE
                        },
                        onDismiss = { modal = AccessibilityModal.NONE }
                    )

                    AccessibilityModal.TEMPLATES -> TemplatesScreen(
                        templateStore = templateStore,
                        selectedTemplateId = selectedTemplateId,
                        onSelectTemplate = { id ->
                            selectedTemplateId = id
                            settings.selectedTemplateId = id
                        },
                        onBack = { modal = AccessibilityModal.NONE }
                    )

                    AccessibilityModal.EXCLUSIONS -> ExclusionsScreen(
                        store = exclusionsStore,
                        onBack = { modal = AccessibilityModal.NONE }
                    )

                    AccessibilityModal.MISSED_CALL_PROMPT -> MissedCallPromptScreen(
                        phone = missedCallLaunch.phone,
                        message = missedCallLaunch.message,
                        templateStore = templateStore,
                        myDetailsStore = myDetailsStore,
                        selectedTemplateId = selectedTemplateId,
                        onDone = { modal = AccessibilityModal.NONE }
                    )
                }
            }

            // Bottom navigation is hidden while a full-screen modal is open.
            if (modal == AccessibilityModal.NONE) {
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
    smsFallback: Boolean,
    onToggleBridging: () -> Unit
) {
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
                text = "אם קשה לך לענות בקול — השיחה ממשיכה בכתב.",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AccessibilityColors.Primary
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
                    text = "אם לא אענה לשיחה, האפליקציה תבקש מהמתקשר לכתוב לי.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AccessibilityColors.TextStrong,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    StatusChip(AccessibilityIcons.Chat, AccessibilityColors.GreenBright, "WhatsApp פעיל")
                    StatusChip(
                        AccessibilityIcons.Sms,
                        AccessibilityColors.Primary,
                        "SMS כגיבוי",
                        trailingTint = if (smsFallback) AccessibilityColors.GreenBright else AccessibilityColors.UnselectedIcon
                    )
                    StatusChip(AccessibilityIcons.Shield, AccessibilityColors.TextMuted, "הגנת כפילויות פעילה")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (bridgingEnabled) {
            PillButton(
                text = "גישור לשיחות פעיל",
                onClick = onToggleBridging,
                background = AccessibilityColors.Green,
                leadingIcon = AccessibilityIcons.PhoneInTalk
            )
            CaptionWithIcon(
                text = "מוכן לשיחות שלא נענו",
                icon = AccessibilityIcons.CheckCircle,
                color = AccessibilityColors.Green
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
private data class TemplateChoice(val id: String, val title: String, val subtitle: String)

private val TEMPLATE_CHOICES = listOf(
    TemplateChoice("missed_call_auto_response", "אני חירש/ת או כבד/ת שמיעה...", "אנא כתבו לי כאן בכתב, תודה."),
    TemplateChoice("missed_call", "קשה לי לענות לשיחות קוליות...", "אשמח אם תכתבו לי הודעה."),
    TemplateChoice("buyer_property_details", "אני מעדיף/ה תקשורת בכתב...", "תודה על ההבנה.")
)

@Composable
private fun TemplatesScreen(
    templateStore: TemplateStore,
    selectedTemplateId: String,
    onSelectTemplate: (String) -> Unit,
    onBack: () -> Unit
) {
    val templates = remember(templateStore) { templateStore.loadTemplates() }
    var editing by remember { mutableStateOf(false) }
    val activeTemplate = templates.firstOrNull { it.id == selectedTemplateId } ?: templates.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModalHeader(title = "הודעת ברירת מחדל", onBack = onBack)
        Text(
            text = "בחר/י את הנוסח שיישלח כברירת מחדל.",
            fontSize = 14.sp,
            color = AccessibilityColors.TextMuted,
            modifier = Modifier.fillMaxWidth()
        )

        TEMPLATE_CHOICES.forEach { choice ->
            val selected = choice.id == selectedTemplateId
            TemplateCard(choice = choice, selected = selected, onClick = { onSelectTemplate(choice.id) })
        }

        if (editing && activeTemplate != null) {
            TemplateEditor(
                template = activeTemplate,
                onSave = { body ->
                    templateStore.saveTemplate(activeTemplate.copy(body = body))
                    editing = false
                },
                onCancel = { editing = false }
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinePillButton(
                text = "ערוך נוסח",
                onClick = { editing = true },
                borderColor = AccessibilityColors.Primary,
                contentColor = AccessibilityColors.Primary,
                leadingIcon = AccessibilityIcons.Edit
            )
        }
    }
}

@Composable
private fun TemplateCard(choice: TemplateChoice, selected: Boolean, onClick: () -> Unit) {
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
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(choice.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccessibilityColors.TextStrong)
                Text(choice.subtitle, fontSize = 13.sp, color = AccessibilityColors.TextMuted)
            }
            Icon(
                if (selected) AccessibilityIcons.RadioChecked else AccessibilityIcons.RadioUnchecked,
                contentDescription = null,
                tint = if (selected) AccessibilityColors.GreenCheck else AccessibilityColors.UnselectedIcon,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun TemplateEditor(template: MessageTemplate, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var body by remember(template.id) { mutableStateOf(template.body) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ערוך נוסח", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccessibilityColors.Heading)
            androidx.compose.material3.OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(text = "שמור נוסח", onClick = { onSave(body) }, modifier = Modifier.weight(1f))
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
    templateStore: TemplateStore,
    myDetailsStore: MyDetailsStore,
    selectedTemplateId: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val resolvedMessage = remember(message, selectedTemplateId) {
        message.ifBlank {
            templateStore.loadTemplates().firstOrNull { it.id == selectedTemplateId }?.body.orEmpty()
        }
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

// ===================== SCREEN 5 — ACTIVITY =====================
@Composable
private fun ActivityScreen(logStore: FollowUpLogStore) {
    val rows = remember(logStore) { ActivityFeed.rows(logStore.load()) }
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
    smsFallback: Boolean,
    onSelectAskBeforeSend: (Boolean) -> Unit,
    onSelectRecipientScope: (RecipientScope) -> Unit,
    onSelectWhatsApp: (WhatsAppChoice) -> Unit,
    onToggleSmsFallback: () -> Unit,
    onOpenExclusions: () -> Unit,
    onOpenTemplates: () -> Unit,
    onDeleteHistory: () -> Unit
) {
    var deleted by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(title = "הגדרות", trailingIcon = AccessibilityIcons.Settings, modifier = Modifier.padding(horizontal = 4.dp))

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
                RadioRow("אנשי קשר בלבד", selected = recipientScope == RecipientScope.CONTACTS_ONLY, onClick = { onSelectRecipientScope(RecipientScope.CONTACTS_ONLY) })
                RadioRow("כל מספר", selected = recipientScope == RecipientScope.ANY_NUMBER, onClick = { onSelectRecipientScope(RecipientScope.ANY_NUMBER) })
                NavigationRow(label = "למי לא לשלוח?", onClick = onOpenExclusions, topDivider = true)
            }
        }

        // WhatsApp regular / business
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
                modifier = Modifier.fillMaxWidth().clickable {
                    onDeleteHistory()
                    deleted = true
                }.padding(14.dp),
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

// ===================== SCREEN 7 — EXCLUSIONS =====================
@Composable
private fun ExclusionsScreen(store: ExclusionsStore, onBack: () -> Unit) {
    var entries by remember { mutableStateOf(store.load()) }
    var addingNumber by remember { mutableStateOf(false) }
    var numberDraft by remember { mutableStateOf("") }
    var nameDraft by remember { mutableStateOf("") }
    var addingName by remember { mutableStateOf(false) }

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

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(
                label = "הוסף איש קשר",
                icon = AccessibilityIcons.PersonAdd,
                onClick = { addingName = true; addingNumber = false },
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                label = "הוסף מספר ידנית",
                icon = AccessibilityIcons.Dialpad,
                onClick = { addingNumber = true; addingName = false },
                modifier = Modifier.weight(1f)
            )
        }

        if (addingName) {
            InlineAddField(
                value = nameDraft,
                onValueChange = { nameDraft = it },
                label = "שם איש קשר",
                onAdd = {
                    if (nameDraft.isNotBlank()) {
                        entries = entries + ExcludedRecipient(label = nameDraft.trim())
                        nameDraft = ""
                        addingName = false
                    }
                }
            )
        }
        if (addingNumber) {
            InlineAddField(
                value = numberDraft,
                onValueChange = { numberDraft = it },
                label = "מספר טלפון",
                onAdd = {
                    if (numberDraft.isNotBlank()) {
                        entries = entries + ExcludedRecipient(label = numberDraft.trim(), number = numberDraft.trim())
                        numberDraft = ""
                        addingNumber = false
                    }
                }
            )
        }

        entries.forEach { entry ->
            ExclusionRow(entry = entry, onRemove = { entries = entries - entry })
        }

        Spacer(modifier = Modifier.height(8.dp))
        PillButton(text = "שמור", onClick = { store.save(entries); onBack() })
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
private fun InlineAddField(value: String, onValueChange: (String) -> Unit, label: String, onAdd: () -> Unit) {
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
            modifier = Modifier.weight(1f)
        )
        PillButton(text = "הוסף", onClick = onAdd, modifier = Modifier.width(90.dp))
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
