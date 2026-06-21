package com.followupnadlan

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.followupnadlan.data.AppDatabase
import com.followupnadlan.data.followup.FollowUpTaskDao
import com.followupnadlan.data.followup.FollowUpTaskEntity
import com.followupnadlan.data.lead.LeadDao
import com.followupnadlan.data.lead.LeadEntity
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.followuplog.FollowUpLogStorage
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.missedcall.DebugMissedCallSimulationResult
import com.followupnadlan.missedcall.DebugMissedCallSimulator
import com.followupnadlan.missedcall.DebugMissedCallStatusFormatter
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.missedcall.MissedCallCandidate
import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel
import com.followupnadlan.missedcall.MissedCallDirection
import com.followupnadlan.missedcall.MissedCallWhatsAppMode
import com.followupnadlan.missedcall.MissedCallAutoResponseHandler
import com.followupnadlan.missedcall.WhatsAppAutoSendController
import com.followupnadlan.missedcall.WhatsAppPackageResolver
import com.followupnadlan.missedcall.WhatsAppReplySender
import com.followupnadlan.notifications.FollowUpNotificationHelper
import com.followupnadlan.notifications.ReminderNotificationHelper
import com.followupnadlan.pipeline.FollowUpSource
import com.followupnadlan.pipeline.FollowUpTaskStatus
import com.followupnadlan.pipeline.LeadPipeline
import com.followupnadlan.pipeline.LeadStatus
import com.followupnadlan.pipeline.LeadType
import com.followupnadlan.postcall.CallDetectionPreferences
import com.followupnadlan.postcall.CallDetectionService
import com.followupnadlan.postcall.PostCallCard
import com.followupnadlan.postcall.PostCallCards
import com.followupnadlan.profile.MyDetailsProfile
import com.followupnadlan.profile.MyDetailsStore
import com.followupnadlan.setup.BatteryOptimizationIntents
import com.followupnadlan.setup.CheckId
import com.followupnadlan.setup.CheckState
import com.followupnadlan.setup.OemGuidance
import com.followupnadlan.setup.ReadinessVerdict
import com.followupnadlan.setup.SelfTestChecker
import com.followupnadlan.setup.SelfTestSnapshot
import com.followupnadlan.setup.SetupPreferences
import com.followupnadlan.templates.MessageTemplate
import com.followupnadlan.templates.SprintOneTemplates
import com.followupnadlan.templates.TemplateStore
import com.followupnadlan.templates.TemplateTagInsertionLogic
import com.followupnadlan.templates.TemplateTagRenderer
import com.followupnadlan.templates.TemplateTags
import com.followupnadlan.templates.TemplateTagValues
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import com.followupnadlan.whatsapp.WhatsAppLinkBuilder
import com.followupnadlan.snooze.ReminderScheduler
import com.followupnadlan.snooze.SnoozeOption
import com.followupnadlan.snooze.SnoozeTimeCalculator
import java.time.ZoneId
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialLaunchState = FollowUpLaunchState.fromIntent(intent)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        FollowUpApp(initialLaunchState = initialLaunchState)
                    }
                }
            }
        }
    }
}

private enum class AppScreen {
    ManualComposer,
    PostCallDecision,
    MyDetails,
    MessageTemplates,
    SetupWizard,
    SelfTest
}

private enum class SetupWizardStep {
    Welcome,
    Permissions,
    AgentProfile,
    BatteryGuidance,
    SelfTestHandoff
}

private data class WizardPermission(
    val permission: String,
    val title: String,
    val explanation: String,
    val optional: Boolean = false,
    val minSdk: Int? = null
)

private data class FollowUpLaunchState(
    val phone: String = "",
    val leadName: String = "",
    val templateId: String = "",
    val callDurationSeconds: Long? = null,
    val callTimestampMillis: Long? = null,
    val callType: String? = null,
    val snoozedTaskId: Long? = null,
    val openedFromNotification: Boolean = false
) {
    companion object {
        fun fromIntent(intent: Intent?): FollowUpLaunchState {
            if (intent?.action == ReminderNotificationHelper.ACTION_OPEN_SNOOZED_TASK) {
                return FollowUpLaunchState(
                    snoozedTaskId = intent.optionalLongExtra(ReminderNotificationHelper.EXTRA_TASK_ID),
                    openedFromNotification = false
                )
            }

            if (intent?.action != FollowUpNotificationHelper.ACTION_OPEN_FOLLOW_UP) {
                return FollowUpLaunchState()
            }

            return FollowUpLaunchState(
                phone = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_PHONE).orEmpty(),
                leadName = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_LEAD_NAME).orEmpty(),
                templateId = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_TEMPLATE_ID).orEmpty(),
                callDurationSeconds = intent.optionalLongExtra(FollowUpNotificationHelper.EXTRA_CALL_DURATION_SECONDS),
                callTimestampMillis = intent.optionalLongExtra(FollowUpNotificationHelper.EXTRA_CALL_TIMESTAMP_MILLIS),
                callType = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_CALL_TYPE),
                openedFromNotification = true
            )
        }
    }
}

@Composable
private fun FollowUpApp(initialLaunchState: FollowUpLaunchState) {
    val context = LocalContext.current
    val myDetailsStore = remember(context) { MyDetailsStore(context.applicationContext) }
    val templateStore = remember(context) { TemplateStore(context.applicationContext) }
    val followUpLogStore = remember(context) { FollowUpLogStore(context.applicationContext) }
    val notificationHelper = remember(context) { FollowUpNotificationHelper(context.applicationContext) }
    val database = remember(context) { AppDatabase.getInstance(context.applicationContext) }
    val reminderScheduler = remember(context) { ReminderScheduler(context.applicationContext) }
    val setupPreferences = remember(context) { SetupPreferences(context.applicationContext) }
    val selfTestChecker = remember(context) { SelfTestChecker(context.applicationContext) }
    val missedCallAutoResponseSettings = remember(context) {
        MissedCallAutoResponseSettings(context.applicationContext)
    }
    val whatsAppAutoSendController = remember(context) {
        WhatsAppAutoSendController(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val callDetectionPreferences = remember(context) { CallDetectionPreferences(context.applicationContext) }
    var currentScreen by remember {
        mutableStateOf(
            when {
                initialLaunchState.openedFromNotification -> AppScreen.PostCallDecision
                !setupPreferences.isSetupCompleted() -> AppScreen.SetupWizard
                else -> AppScreen.ManualComposer
            }
        )
    }
    var templateRevision by remember { mutableStateOf(0) }
    var manualPhone by remember { mutableStateOf(initialLaunchState.phone) }
    var manualLeadName by remember { mutableStateOf(initialLaunchState.leadName) }
    var manualTemplateId by remember { mutableStateOf(initialLaunchState.templateId) }
    var postCallDurationSeconds by remember { mutableStateOf(initialLaunchState.callDurationSeconds) }
    var postCallTimestampMillis by remember { mutableStateOf(initialLaunchState.callTimestampMillis) }
    var postCallType by remember { mutableStateOf(initialLaunchState.callType) }
    var manualMessageOverride by remember { mutableStateOf<String?>(null) }
    var manualMessageRevision by remember { mutableStateOf(0) }
    var restoredTaskId by remember { mutableStateOf<Long?>(null) }
    var postCallSelectionStatus by remember { mutableStateOf<String?>(null) }
    var notificationStatus by remember { mutableStateOf<String?>(null) }
    var callDetectionEnabled by remember { mutableStateOf(callDetectionPreferences.isEnabled()) }
    var callDetectionStatus by remember {
        mutableStateOf(if (callDetectionEnabled) "זיהוי שיחות מסומן כפעיל במכשיר." else null)
    }
    var missedCallAutoResponseEnabled by remember {
        mutableStateOf(missedCallAutoResponseSettings.isEnabled)
    }
    var missedCallPrimaryChannel by remember {
        mutableStateOf(missedCallAutoResponseSettings.primaryChannel)
    }
    var missedCallWhatsAppMode by remember {
        mutableStateOf(missedCallAutoResponseSettings.whatsappMode)
    }
    var missedCallSmsFallbackEnabled by remember {
        mutableStateOf(missedCallAutoResponseSettings.smsFallbackEnabled)
    }
    var missedCallManualSmsFallbackEnabled by remember {
        mutableStateOf(missedCallAutoResponseSettings.manualSmsFallbackEnabled)
    }
    var missedCallAutoResponseStatus by remember {
        mutableStateOf<String?>(
            missedCallAutoResponseStatus(
                context = context,
                enabled = missedCallAutoResponseEnabled,
                primaryChannel = missedCallPrimaryChannel,
                whatsappMode = missedCallWhatsAppMode,
                smsFallbackEnabled = missedCallSmsFallbackEnabled,
                accessibilityEnabled = whatsAppAutoSendController.isAccessibilityServiceEnabled()
            )
        )
    }
    var debugMissedCallStatus by remember { mutableStateOf<String?>(null) }
    var pendingWizardPermission by remember { mutableStateOf<WizardPermission?>(null) }
    var wizardPermissionStatus by remember { mutableStateOf<String?>(null) }
    var batteryGuidanceStatus by remember { mutableStateOf<String?>(null) }
    var pendingNotificationLaunch by remember { mutableStateOf<FollowUpLaunchState?>(null) }
    val wizardPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permission = pendingWizardPermission
        pendingWizardPermission = null
        wizardPermissionStatus = when {
            permission == null -> null
            granted -> "${permission.title}: ההרשאה אושרה."
            permission.optional -> "${permission.title}: ההרשאה לא אושרה. אפשר להמשיך, והשלמה אוטומטית תהיה מוגבלת."
            else -> "${permission.title}: ההרשאה לא אושרה. מצב ידני ממשיך לעבוד."
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pendingLaunch = pendingNotificationLaunch
        pendingNotificationLaunch = null
        if (granted && pendingLaunch != null) {
            notificationHelper.showFollowUpNotification(
                phone = pendingLaunch.phone,
                leadName = pendingLaunch.leadName,
                templateId = pendingLaunch.templateId
            )
            notificationStatus = "ההתראה נוצרה. הקש עליה כדי לפתוח כרטיס שליחה מהיר."
        } else {
            notificationStatus = "הרשאת התראות נדחתה. אפשר עדיין להשתמש במסך השליחה הידני."
        }
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            missedCallAutoResponseSettings.smsFallbackEnabled = true
            missedCallSmsFallbackEnabled = true
            missedCallAutoResponseStatus = "אם WhatsApp לא זמין, תישלח הודעת SMS לפי ההרשאות שאישרת."
        } else {
            missedCallAutoResponseSettings.smsFallbackEnabled = false
            missedCallSmsFallbackEnabled = false
            missedCallAutoResponseStatus = "לא ניתן לשלוח SMS אוטומטי ללא הרשאה. אפשר עדיין לפתוח הודעה מוכנה לשליחה ידנית."
        }
    }
    val triggerTestNotification: (String, String, String) -> Unit = { phone, leadName, templateId ->
        val launchState = FollowUpLaunchState(phone = phone, leadName = leadName, templateId = templateId)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotificationLaunch = launchState
            notificationStatus = "נדרשת הרשאת התראות כדי להציג כרטיס פולואפ."
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationHelper.showFollowUpNotification(
                phone = launchState.phone,
                leadName = launchState.leadName,
                templateId = launchState.templateId
            )
            notificationStatus = "ההתראה נוצרה. הקש עליה כדי לפתוח כרטיס שליחה מהיר."
        }
    }
    val debugMissedCallSimulator = remember(context) {
        DebugMissedCallSimulator { candidate ->
            MissedCallAutoResponseHandler(context.applicationContext).handleMissedIncomingCandidate(candidate)
        }
    }
    val triggerDebugMissedCall: ((String) -> Unit)? = if (BuildConfig.DEBUG) {
        { phone ->
            val trimmedPhone = phone.trim()
            val handler = MissedCallAutoResponseHandler(context.applicationContext)
            debugMissedCallStatus = when (debugMissedCallSimulator.simulate(phone)) {
                DebugMissedCallSimulationResult.TRIGGERED ->
                    DebugMissedCallStatusFormatter.format(
                        handler.previewMissedIncomingCandidate(
                            MissedCallCandidate(
                                phoneNumber = trimmedPhone,
                                direction = MissedCallDirection.INCOMING,
                                wasAnswered = false,
                                source = DebugMissedCallSimulator.SOURCE
                            )
                        )
                    )
                DebugMissedCallSimulationResult.EMPTY_NUMBER ->
                    "יש להזין מספר טלפון לבדיקה."
                DebugMissedCallSimulationResult.RELEASE_BUILD_BLOCKED ->
                    "הסימולטור זמין רק בגרסאות debug."
            }
        }
    } else {
        null
    }
    val startCallDetection: () -> Unit = {
        val resultMessage = startCallDetectionService(context)
        if (resultMessage == null) {
            callDetectionPreferences.setEnabled(true)
            callDetectionEnabled = true
            callDetectionStatus = "זיהוי שיחות הופעל. האפליקציה לא קוראת יומן שיחות ולא שולחת הודעות."
        } else {
            callDetectionPreferences.setEnabled(false)
            callDetectionEnabled = false
            callDetectionStatus = resultMessage
        }
    }
    val callDetectionPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val phoneGranted = grants[Manifest.permission.READ_PHONE_STATE] == true ||
            context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val callLogGranted = grants[Manifest.permission.READ_CALL_LOG] == true ||
            context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val contactsGranted = grants[Manifest.permission.READ_CONTACTS] == true ||
            context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            grants[Manifest.permission.POST_NOTIFICATIONS] == true ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        if (!phoneGranted) {
            callDetectionPreferences.setEnabled(false)
            callDetectionEnabled = false
            callDetectionStatus = "הרשאת מצב טלפון נדחתה. מצב ידני ממשיך לעבוד."
        } else {
            startCallDetection()
            callDetectionStatus = callDetectionStatusAfterPermissions(
                notificationsGranted = notificationsGranted,
                callLogGranted = callLogGranted,
                contactsGranted = contactsGranted
            )
        }
    }
    val toggleCallDetection: () -> Unit = {
        if (callDetectionEnabled) {
            stopCallDetectionService(context)
            callDetectionPreferences.setEnabled(false)
            callDetectionEnabled = false
            callDetectionStatus = "זיהוי שיחות כובה. מצב ידני ממשיך לעבוד."
        } else {
            val permissions = buildList {
                add(Manifest.permission.READ_PHONE_STATE)
                add(Manifest.permission.READ_CALL_LOG)
                add(Manifest.permission.READ_CONTACTS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.filter {
                context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }

            if (permissions.isEmpty()) {
                startCallDetection()
            } else {
                callDetectionStatus = "כדי לזהות סיום שיחה ולמלא מספר, האפליקציה צריכה הרשאת מצב טלפון וקריאת השיחה האחרונה. אנשי קשר משמשים רק למילוי שם פרטי אם מאשרים. המידע נשאר מקומי ולא נשלחות הודעות אוטומטית."
                callDetectionPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }
    val toggleMissedCallAutoResponse: () -> Unit = {
        if (missedCallAutoResponseEnabled) {
            missedCallAutoResponseSettings.isEnabled = false
            missedCallAutoResponseEnabled = false
            missedCallAutoResponseStatus = "כבוי — לא יישלחו הודעות אוטומטיות."
        } else {
            missedCallAutoResponseSettings.isEnabled = true
            missedCallAutoResponseEnabled = true
            missedCallAutoResponseStatus = missedCallAutoResponseStatus(
                context = context,
                enabled = true,
                primaryChannel = missedCallPrimaryChannel,
                whatsappMode = missedCallWhatsAppMode,
                smsFallbackEnabled = missedCallSmsFallbackEnabled,
                accessibilityEnabled = whatsAppAutoSendController.isAccessibilityServiceEnabled()
            )
        }
    }
    val keepMissedCallAutoResponseDisabled: () -> Unit = {
        missedCallAutoResponseSettings.isEnabled = false
        missedCallAutoResponseEnabled = false
        missedCallAutoResponseStatus = "כבוי — לא יישלחו הודעות אוטומטיות."
    }
    val selectMissedCallPrimaryChannel: (MissedCallResponsePrimaryChannel) -> Unit = { channel ->
        missedCallAutoResponseSettings.primaryChannel = channel
        missedCallPrimaryChannel = channel
        missedCallAutoResponseStatus = missedCallAutoResponseStatus(
            context = context,
            enabled = missedCallAutoResponseEnabled,
            primaryChannel = channel,
            whatsappMode = missedCallWhatsAppMode,
            smsFallbackEnabled = missedCallSmsFallbackEnabled,
            accessibilityEnabled = whatsAppAutoSendController.isAccessibilityServiceEnabled()
        )
    }
    val selectMissedCallWhatsAppMode: (MissedCallWhatsAppMode) -> Unit = { mode ->
        missedCallAutoResponseSettings.whatsappMode = mode
        missedCallWhatsAppMode = mode
        if (mode == MissedCallWhatsAppMode.ACCESSIBILITY_AUTO) {
            missedCallAutoResponseSettings.whatsappAutomationEnabled = true
            followUpLogStore.append(
                FollowUpLogEntry(
                    actionType = FollowUpActionType.WHATSAPP_AUTO_SEND_ENABLED,
                    timestampEpochMs = System.currentTimeMillis(),
                    messagePreview = "",
                    source = MissedCallAutoResponseSettings.SOURCE
                )
            )
        } else {
            missedCallAutoResponseSettings.whatsappAutomationEnabled = false
            followUpLogStore.append(
                FollowUpLogEntry(
                    actionType = FollowUpActionType.WHATSAPP_AUTO_SEND_DISABLED,
                    timestampEpochMs = System.currentTimeMillis(),
                    messagePreview = "",
                    source = MissedCallAutoResponseSettings.SOURCE
                )
            )
        }
        missedCallAutoResponseStatus = missedCallAutoResponseStatus(
            context = context,
            enabled = missedCallAutoResponseEnabled,
            primaryChannel = missedCallPrimaryChannel,
            whatsappMode = mode,
            smsFallbackEnabled = missedCallSmsFallbackEnabled,
            accessibilityEnabled = whatsAppAutoSendController.isAccessibilityServiceEnabled()
        )
    }
    val toggleMissedCallSmsFallback: () -> Unit = {
        val next = !missedCallSmsFallbackEnabled
        if (next && context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            missedCallAutoResponseStatus = "האפליקציה צריכה הרשאת SMS כדי לשלוח SMS רק כאשר WhatsApp לא זמין."
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        } else {
            missedCallAutoResponseSettings.smsFallbackEnabled = next
            missedCallSmsFallbackEnabled = next
            missedCallAutoResponseStatus = missedCallAutoResponseStatus(
                context = context,
                enabled = missedCallAutoResponseEnabled,
                primaryChannel = missedCallPrimaryChannel,
                whatsappMode = missedCallWhatsAppMode,
                smsFallbackEnabled = next,
                accessibilityEnabled = whatsAppAutoSendController.isAccessibilityServiceEnabled()
            )
        }
    }
    val toggleMissedCallManualSmsFallback: () -> Unit = {
        val next = !missedCallManualSmsFallbackEnabled
        missedCallAutoResponseSettings.manualSmsFallbackEnabled = next
        missedCallManualSmsFallbackEnabled = next
    }
    val openAccessibilitySettings: () -> Unit = {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    LaunchedEffect(initialLaunchState.snoozedTaskId) {
        val taskId = initialLaunchState.snoozedTaskId ?: return@LaunchedEffect
        val task = database.followUpTaskDao().getById(taskId)
        if (task == null) {
            notificationStatus = "התזכורת נפתחה, אבל הכרטיס כבר לא נמצא. אפשר להמשיך ידנית."
            currentScreen = AppScreen.ManualComposer
            return@LaunchedEffect
        }

        manualPhone = task.phone.orEmpty()
        manualLeadName = task.contactName.orEmpty()
        manualTemplateId = task.selectedTemplateId.orEmpty()
        manualMessageOverride = task.draftText
        manualMessageRevision += 1
        postCallDurationSeconds = task.callDurationSeconds
        postCallTimestampMillis = task.callEndedAtEpochMs
        postCallType = null
        restoredTaskId = task.id
        currentScreen = AppScreen.PostCallDecision
        notificationStatus = "תזכורת נפתחה. הכרטיס שוחזר לעריכה."
        database.followUpTaskDao().update(
            task.copy(
                status = FollowUpTaskStatus.OPENED,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { currentScreen = AppScreen.ManualComposer },
                    modifier = Modifier.weight(1f),
                    enabled = currentScreen != AppScreen.ManualComposer
                ) {
                    Text("שליחת WhatsApp")
                }
                Button(
                    onClick = { currentScreen = AppScreen.PostCallDecision },
                    modifier = Modifier.weight(1f),
                    enabled = currentScreen != AppScreen.PostCallDecision
                ) {
                    Text("מה קרה?")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { currentScreen = AppScreen.MyDetails },
                    modifier = Modifier.weight(1f),
                    enabled = currentScreen != AppScreen.MyDetails
                ) {
                    Text("הפרטים שלי")
                }
                Button(
                    onClick = { currentScreen = AppScreen.MessageTemplates },
                    modifier = Modifier.weight(1f),
                    enabled = currentScreen != AppScreen.MessageTemplates
                ) {
                    Text("תבניות")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { currentScreen = AppScreen.SetupWizard },
                modifier = Modifier.weight(1f),
                enabled = currentScreen != AppScreen.SetupWizard
            ) {
                Text("הגדרה ובדיקה")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                AppScreen.ManualComposer -> ManualWhatsAppScreen(
                    myDetailsStore = myDetailsStore,
                    templateStore = templateStore,
                    followUpLogStore = followUpLogStore,
                    templateRevision = templateRevision,
                    phone = manualPhone,
                    leadName = manualLeadName,
                    onPhoneChange = { manualPhone = it },
                    onLeadNameChange = { manualLeadName = it },
                    initialTemplateId = manualTemplateId,
                    initialMessageOverride = manualMessageOverride,
                    initialMessageRevision = manualMessageRevision,
                    restoredTaskId = restoredTaskId,
                    followUpTaskDao = database.followUpTaskDao(),
                    leadDao = database.leadDao(),
                    reminderScheduler = reminderScheduler,
                    onRestoredTaskStatusChanged = { restoredTaskId = it },
                    notificationStatus = notificationStatus,
                    onTriggerTestNotification = triggerTestNotification,
                    callDetectionEnabled = callDetectionEnabled,
                    callDetectionStatus = callDetectionStatus,
                    onToggleCallDetection = toggleCallDetection,
                    missedCallAutoResponseEnabled = missedCallAutoResponseEnabled,
                    missedCallPrimaryChannel = missedCallPrimaryChannel,
                    missedCallWhatsAppMode = missedCallWhatsAppMode,
                    missedCallSmsFallbackEnabled = missedCallSmsFallbackEnabled,
                    missedCallManualSmsFallbackEnabled = missedCallManualSmsFallbackEnabled,
                    missedCallAutoResponseStatus = missedCallAutoResponseStatus,
                    onToggleMissedCallAutoResponse = toggleMissedCallAutoResponse,
                    onKeepMissedCallAutoResponseDisabled = keepMissedCallAutoResponseDisabled,
                    onSelectMissedCallPrimaryChannel = selectMissedCallPrimaryChannel,
                    onSelectMissedCallWhatsAppMode = selectMissedCallWhatsAppMode,
                    onToggleMissedCallSmsFallback = toggleMissedCallSmsFallback,
                    onToggleMissedCallManualSmsFallback = toggleMissedCallManualSmsFallback,
                    onOpenAccessibilitySettings = openAccessibilitySettings,
                    debugSimulatorStatus = debugMissedCallStatus,
                    onTriggerDebugMissedCall = triggerDebugMissedCall
                )
                AppScreen.PostCallDecision -> PostCallScreen(
                    phone = manualPhone,
                    leadName = manualLeadName,
                    initialTemplateId = manualTemplateId,
                    initialMessageOverride = manualMessageOverride,
                    callDurationSeconds = postCallDurationSeconds,
                    callTimestampMillis = postCallTimestampMillis,
                    callType = postCallType,
                    openedFromNotification = initialLaunchState.openedFromNotification,
                    myDetailsStore = myDetailsStore,
                    followUpLogStore = followUpLogStore,
                    followUpTaskDao = database.followUpTaskDao(),
                    leadDao = database.leadDao(),
                    reminderScheduler = reminderScheduler,
                    restoredTaskId = restoredTaskId,
                    onRestoredTaskStatusChanged = { restoredTaskId = it },
                    selectionStatus = postCallSelectionStatus,
                    onSelectionStatusChanged = { postCallSelectionStatus = it },
                    onEditMessage = { card, draft ->
                        manualTemplateId = card.composerHint.templateId
                        manualMessageOverride = draft
                        manualMessageRevision += 1
                        postCallSelectionStatus = null
                        notificationStatus = "נבחר כרטיס: ${card.title}. ההודעה הוכנה לעריכה ידנית."
                        currentScreen = AppScreen.ManualComposer
                    },
                    onCloseCard = {
                        postCallSelectionStatus = "כרטיס הפולואפ נסגר. לא תופיע תזכורת נוספת לפעולה זו."
                        currentScreen = AppScreen.ManualComposer
                    }
                )
                AppScreen.MyDetails -> MyDetailsScreen(myDetailsStore)
                AppScreen.MessageTemplates -> TemplateManagementScreen(
                    templateStore = templateStore,
                    myDetailsStore = myDetailsStore,
                    onTemplatesChanged = { templateRevision += 1 }
                )
                AppScreen.SetupWizard -> SetupWizardScreen(
                    myDetailsStore = myDetailsStore,
                    permissionStatus = wizardPermissionStatus,
                    batteryGuidanceStatus = batteryGuidanceStatus,
                    onRequestPermission = { permission ->
                        if (permission.minSdk != null && Build.VERSION.SDK_INT < permission.minSdk) {
                            wizardPermissionStatus = "${permission.title}: אין צורך בהרשאה זו בגרסת Android הנוכחית."
                        } else if (context.checkSelfPermission(permission.permission) == PackageManager.PERMISSION_GRANTED) {
                            wizardPermissionStatus = "${permission.title}: ההרשאה כבר מאושרת."
                        } else {
                            pendingWizardPermission = permission
                            wizardPermissionStatus = "${permission.title}: בקשת הרשאה נפתחה."
                            wizardPermissionLauncher.launch(permission.permission)
                        }
                    },
                    onOpenBatterySettings = {
                        val intent = BatteryOptimizationIntents.appSettingsIntent(context)
                        if (intent == null) {
                            batteryGuidanceStatus = "לא נמצאו הגדרות לפתיחה אוטומטית. פתח ידנית את הגדרות האפליקציה ובטל חיסכון סוללה אם קיים."
                        } else {
                            try {
                                context.startActivity(intent)
                                batteryGuidanceStatus = "הגדרות האפליקציה נפתחו. חפש סוללה או פעילות ברקע ואפשר פעולה ברקע."
                            } catch (_: RuntimeException) {
                                batteryGuidanceStatus = "לא ניתן לפתוח את ההגדרות במכשיר הזה. פתח ידנית את הגדרות האפליקציה ואפשר פעולה ברקע."
                            }
                        }
                    },
                    onStartSelfTest = { currentScreen = AppScreen.SelfTest },
                    onFinish = {
                        setupPreferences.setSetupCompleted(true)
                        currentScreen = AppScreen.ManualComposer
                    }
                )
                AppScreen.SelfTest -> SelfTestScreen(
                    checker = selfTestChecker,
                    setupPreferences = setupPreferences,
                    myDetailsStore = myDetailsStore,
                    onBackToWizard = { currentScreen = AppScreen.SetupWizard },
                    onOpenNotificationSettings = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: RuntimeException) {
                            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            try {
                                context.startActivity(fallback)
                            } catch (_: RuntimeException) {
                                // Some OEM builds hide both notification and app-detail settings.
                            }
                        }
                    },
                    onEnableDetection = { currentScreen = AppScreen.ManualComposer },
                    onRequestPhoneStatePermission = {
                        val permission = WizardPermission(
                            permission = Manifest.permission.READ_PHONE_STATE,
                            title = "מצב טלפון",
                            explanation = "זיהוי סוף שיחה צריך הרשאת מצב טלפון. בלי ההרשאה הזו מצב ידני ממשיך לעבוד."
                        )
                        pendingWizardPermission = permission
                        wizardPermissionStatus = "${permission.title}: בקשת הרשאה נפתחה."
                        wizardPermissionLauncher.launch(permission.permission)
                    },
                    onFinishSetup = {
                        setupPreferences.setSetupCompleted(true)
                        currentScreen = AppScreen.ManualComposer
                    }
                )
            }
        }
    }
}

@Composable
private fun SetupWizardScreen(
    myDetailsStore: MyDetailsStore,
    permissionStatus: String?,
    batteryGuidanceStatus: String?,
    onRequestPermission: (WizardPermission) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onStartSelfTest: () -> Unit,
    onFinish: () -> Unit
) {
    var step by remember { mutableStateOf(SetupWizardStep.Welcome) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (step) {
            SetupWizardStep.Welcome -> SetupWelcomeStep(
                onContinue = { step = SetupWizardStep.Permissions }
            )
            SetupWizardStep.Permissions -> SetupPermissionsStep(
                permissionStatus = permissionStatus,
                onRequestPermission = onRequestPermission,
                onContinue = { step = SetupWizardStep.AgentProfile }
            )
            SetupWizardStep.AgentProfile -> SetupAgentProfileStep(
                store = myDetailsStore,
                onContinue = { step = SetupWizardStep.BatteryGuidance }
            )
            SetupWizardStep.BatteryGuidance -> SetupBatteryGuidanceStep(
                status = batteryGuidanceStatus,
                onOpenBatterySettings = onOpenBatterySettings,
                onContinue = { step = SetupWizardStep.SelfTestHandoff }
            )
            SetupWizardStep.SelfTestHandoff -> SetupSelfTestHandoffStep(
                onStartSelfTest = onStartSelfTest,
                onFinish = onFinish
            )
        }
    }
}

@Composable
private fun SetupWelcomeStep(onContinue: () -> Unit) {
    Text(
        text = "ברוך הבא ל-FollowUp נדלן",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "האפליקציה עוזרת לך לא לשכוח המשך טיפול אחרי שיחות נדלן. היא מכינה כרטיס המשך טיפול, מאפשרת הודעת WhatsApp ידנית, ושומרת מצב ידני זמין גם בלי הרשאות.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text("המשך")
    }
}

@Composable
private fun SetupPermissionsStep(
    permissionStatus: String?,
    onRequestPermission: (WizardPermission) -> Unit,
    onContinue: () -> Unit
) {
    val permissions = listOf(
        WizardPermission(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            title = "התראות",
            explanation = "נדרשות כדי להציג כרטיס המשך טיפול ותזכורות אחרי שיחה.",
            minSdk = Build.VERSION_CODES.TIRAMISU
        ),
        WizardPermission(
            permission = Manifest.permission.READ_PHONE_STATE,
            title = "מצב טלפון",
            explanation = "נדרש כדי לזהות ששיחה הסתיימה. אם לא תאשר, מצב ידני ימשיך לעבוד."
        ),
        WizardPermission(
            permission = Manifest.permission.READ_CALL_LOG,
            title = "יומן שיחות",
            explanation = "אופציונלי: משמש רק למילוי מספר הטלפון האחרון אחרי שיחה. אפשר להמשיך בלי זה.",
            optional = true
        ),
        WizardPermission(
            permission = Manifest.permission.READ_CONTACTS,
            title = "אנשי קשר",
            explanation = "אופציונלי: משמש רק למילוי שם איש הקשר כשקיים. אפשר להמשיך בלי זה.",
            optional = true
        )
    )

    Text(
        text = "הרשאות",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "כל הרשאה מוסברת לפני הבקשה. דחייה לא חוסמת את האפליקציה או את מצב השליחה הידני.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
    permissions.forEach { permission ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(permission.title, style = MaterialTheme.typography.titleMedium)
                Text(permission.explanation, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { onRequestPermission(permission) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("בקש הרשאה")
                }
            }
        }
    }
    permissionStatus?.let {
        Text(text = it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
    }
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text("המשך")
    }
}

@Composable
private fun SetupAgentProfileStep(
    store: MyDetailsStore,
    onContinue: () -> Unit
) {
    val savedProfile = remember(store) { store.load() }
    var agentName by remember { mutableStateOf(savedProfile.agentName) }
    var officeName by remember { mutableStateOf(savedProfile.officeName) }
    var phone by remember { mutableStateOf(savedProfile.phone) }
    var website by remember { mutableStateOf(savedProfile.website) }
    var businessCard by remember { mutableStateOf(savedProfile.businessCard) }
    var signature by remember { mutableStateOf(savedProfile.signature) }
    var property1Name by remember { mutableStateOf(savedProfile.property1Name) }
    var property1Link by remember { mutableStateOf(savedProfile.property1Link) }
    var property2Name by remember { mutableStateOf(savedProfile.property2Name) }
    var property2Link by remember { mutableStateOf(savedProfile.property2Link) }
    var property3Name by remember { mutableStateOf(savedProfile.property3Name) }
    var property3Link by remember { mutableStateOf(savedProfile.property3Link) }
    var activePropertyIndex by remember { mutableStateOf(savedProfile.activePropertyIndex.coerceIn(1, 3)) }

    Text(
        text = "פרטי הסוכן",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "הפרטים נשמרים באותו מאגר מקומי של מסך הפרטים שלי. ערכים קיימים לא מתאפסים.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = agentName,
        onValueChange = { agentName = it },
        label = { Text("שם הסוכן") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = officeName,
        onValueChange = { officeName = it },
        label = { Text("שם המשרד") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("טלפון") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = website,
        onValueChange = { website = it },
        label = { Text("אתר") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = businessCard,
        onValueChange = { businessCard = it },
        label = { Text("כרטיס ביקור") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = signature,
        onValueChange = { signature = it },
        label = { Text("חתימה") },
        minLines = 4,
        modifier = Modifier.fillMaxWidth()
    )
    PropertyFields(
        index = 1,
        name = property1Name,
        link = property1Link,
        activePropertyIndex = activePropertyIndex,
        onNameChange = { property1Name = it },
        onLinkChange = { property1Link = it },
        onSelectActive = { activePropertyIndex = 1 }
    )
    PropertyFields(
        index = 2,
        name = property2Name,
        link = property2Link,
        activePropertyIndex = activePropertyIndex,
        onNameChange = { property2Name = it },
        onLinkChange = { property2Link = it },
        onSelectActive = { activePropertyIndex = 2 }
    )
    PropertyFields(
        index = 3,
        name = property3Name,
        link = property3Link,
        activePropertyIndex = activePropertyIndex,
        onNameChange = { property3Name = it },
        onLinkChange = { property3Link = it },
        onSelectActive = { activePropertyIndex = 3 }
    )
    Button(
        onClick = {
            store.save(
                MyDetailsProfile(
                    agentName = agentName,
                    officeName = officeName,
                    phone = phone,
                    website = website,
                    businessCard = businessCard,
                    signature = signature,
                    property1Name = property1Name,
                    property1Link = property1Link,
                    property2Name = property2Name,
                    property2Link = property2Link,
                    property3Name = property3Name,
                    property3Link = property3Link,
                    activePropertyIndex = activePropertyIndex
                )
            )
            onContinue()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("שמור והמשך")
    }
}

@Composable
private fun SetupBatteryGuidanceStep(
    status: String?,
    onOpenBatterySettings: () -> Unit,
    onContinue: () -> Unit
) {
    val guidance = remember {
        OemGuidance.forManufacturer("${Build.MANUFACTURER} ${Build.BRAND}".lowercase())
    }

    Text(
        text = "הגדרות סוללה",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = guidance.title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = guidance.body,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
        Text("פתח הגדרות סוללה")
    }
    status?.let {
        Text(text = it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
    }
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text("המשך")
    }
}

@Composable
private fun SetupSelfTestHandoffStep(
    onStartSelfTest: () -> Unit,
    onFinish: () -> Unit
) {
    Text(
        text = "כעת נבדוק שהכל עובד",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "במסך הבא תופיע בדיקה עצמית של הרשאות, התראות, זיהוי שיחות וחיסכון סוללה. הבדיקה לא מחייגת ולא משנה הגדרות לבד.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onStartSelfTest, modifier = Modifier.fillMaxWidth()) {
        Text("התחל בדיקה")
    }
    OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
        Text("סיים הגדרה")
    }
}

@Composable
private fun SelfTestScreen(
    checker: SelfTestChecker,
    setupPreferences: SetupPreferences,
    myDetailsStore: MyDetailsStore,
    onBackToWizard: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onEnableDetection: () -> Unit,
    onRequestPhoneStatePermission: () -> Unit,
    onFinishSetup: () -> Unit
) {
    var snapshot by remember(checker) { mutableStateOf(checker.run()) }
    var testStarted by remember { mutableStateOf(false) }
    var observationMessage by remember {
        mutableStateOf(if (setupPreferences.isSelfTestPassed()) "הבדיקה האחרונה סומנה על ידי המשתמש כהצלחה." else null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "בדיקה עצמית",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = readinessLabel(snapshot.verdict),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "הבדיקה קוראת מצב מכשיר והרשאות בלבד. היא לא מפעילה שירות, לא מבצעת שיחה ולא שולחת הודעה.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        snapshot.checks.forEach { (id, state) ->
            SelfTestCheckRow(
                id = id,
                state = state,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onEnableDetection = onEnableDetection,
                onRequestPhoneStatePermission = onRequestPhoneStatePermission
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "בדיקת עבודה",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (testStarted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "חייג למספר כלשהו, נתק אחרי כמה שניות, ובדוק אם כרטיס המשך השיחה הופיע. האפליקציה לא מבצעת את השיחה בשבילך.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (testStarted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = {
                        testStarted = true
                        observationMessage = "התחל בדיקה ידנית: בצע שיחה קצרה מחוץ לאפליקציה וחזור לסמן מה ראית."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("התחל בדיקה")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            setupPreferences.setSelfTestPassed(true)
                            observationMessage = "נרשם שהמשתמש ראה את כרטיס המשך השיחה. זו תצפית ידנית, לא זיהוי אוטומטי שנרשם ביומן."
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("זה עבד ✓")
                    }
                    OutlinedButton(
                        onClick = {
                            setupPreferences.setSelfTestPassed(false)
                            observationMessage = troubleshootingSuggestion(snapshot)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("זה לא עבד ✗")
                    }
                }
                observationMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Button(
            onClick = {
                snapshot = checker.run()
                observationMessage = "הבדיקה רועננה לפי מצב המכשיר הנוכחי."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("בדוק שוב")
        }

        Text(
            text = "אם חסרה הרשאה, מצב ידני עדיין זמין: כתיבת מספר, בחירת תבנית, פתיחת WhatsApp ידנית, שיתוף והעתקה.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
        val phoneStatePassed = snapshot.checks
            .firstOrNull { it.first == CheckId.PHONE_STATE }
            ?.second == CheckState.PASS

        val agentProfileSet = remember(checker) {
            myDetailsStore.load().let { it.agentName.isNotBlank() && it.phone.isNotBlank() }
        }

        val canFinish = phoneStatePassed && agentProfileSet

        if (!canFinish) {
            Text(
                text = when {
                    !agentProfileSet -> "חסר פרופיל סוכן. חזור לאשף והשלם שם וטלפון כדי לסיים."
                    else -> "הרשאת מצב טלפון חסרה. בלעדיה אין זיהוי שיחות — אשר אותה כדי לסיים."
                },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBackToWizard, modifier = Modifier.weight(1f)) {
                Text("חזור לאשף")
            }
            Button(
                onClick = onFinishSetup,
                enabled = canFinish,
                modifier = Modifier.weight(1f)
            ) {
                Text("סיים הגדרה")
            }
        }
    }
}

@Composable
private fun SelfTestCheckRow(
    id: CheckId,
    state: CheckState,
    onOpenNotificationSettings: () -> Unit,
    onEnableDetection: () -> Unit,
    onRequestPhoneStatePermission: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = checkName(id),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stateLabel(state),
                    color = stateColor(state),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            actionLine(id, state)?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
            when {
                id == CheckId.NOTIFICATIONS && state == CheckState.FAIL -> {
                    OutlinedButton(onClick = onOpenNotificationSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("פתח הגדרות התראות")
                    }
                }
                id == CheckId.CHANNEL_ENABLED && state == CheckState.FAIL -> {
                    OutlinedButton(onClick = onOpenNotificationSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("פתח הגדרות התראות")
                    }
                }
                id == CheckId.DETECTION_ENABLED && state == CheckState.FAIL -> {
                    OutlinedButton(onClick = onEnableDetection, modifier = Modifier.fillMaxWidth()) {
                        Text("הפעל זיהוי שיחות")
                    }
                }
                id == CheckId.PHONE_STATE && state == CheckState.FAIL -> {
                    OutlinedButton(onClick = onRequestPhoneStatePermission, modifier = Modifier.fillMaxWidth()) {
                        Text("בקש הרשאה")
                    }
                }
            }
        }
    }
}

@Composable
private fun stateColor(state: CheckState) = when (state) {
    CheckState.PASS -> MaterialTheme.colorScheme.primary
    CheckState.FAIL -> MaterialTheme.colorScheme.error
    CheckState.OPTIONAL_MISSING -> MaterialTheme.colorScheme.secondary
    CheckState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun readinessLabel(verdict: ReadinessVerdict): String = when (verdict) {
    ReadinessVerdict.READY -> "מוכן לזיהוי שיחות"
    ReadinessVerdict.PARTIAL -> "מצב ידני — חלק מההרשאות חסרות"
    ReadinessVerdict.MANUAL_ONLY -> "מצב ידני בלבד"
}

private fun checkName(id: CheckId): String = when (id) {
    CheckId.NOTIFICATIONS -> "התראות"
    CheckId.PHONE_STATE -> "מצב טלפון"
    CheckId.CALL_LOG -> "יומן שיחות"
    CheckId.CONTACTS -> "אנשי קשר"
    CheckId.DETECTION_ENABLED -> "זיהוי שיחות פעיל"
    CheckId.CHANNEL_ENABLED -> "ערוץ התראות"
    CheckId.BATTERY_OPTIMIZATION -> "חיסכון סוללה"
    CheckId.MISSED_CALL_AUTO_RESPONSE_ENABLED -> "תגובה אוטומטית לשיחה שלא נענתה"
    CheckId.WHATSAPP_INSTALLED -> "WhatsApp מותקן"
    CheckId.WHATSAPP_ACCESSIBILITY_SERVICE -> "שירות נגישות ל-WhatsApp"
    CheckId.SMS_PERMISSION -> "הרשאת SMS"
    CheckId.MISSED_CALL_TEMPLATE -> "תבנית תגובה אוטומטית"
    CheckId.MISSED_CALL_COOLDOWN_STORE -> "שמירת מניעת כפילויות"
}

private fun stateLabel(state: CheckState): String = when (state) {
    CheckState.PASS -> "עבר ✓"
    CheckState.FAIL -> "נכשל ✗"
    CheckState.OPTIONAL_MISSING -> "אופציונלי"
    CheckState.UNKNOWN -> "לא ידוע"
}

private fun actionLine(id: CheckId, state: CheckState): String? = when {
    state == CheckState.PASS -> null
    id == CheckId.NOTIFICATIONS -> "כדי לראות כרטיס פולואפ אחרי שיחה צריך לאפשר התראות לאפליקציה."
    id == CheckId.CHANNEL_ENABLED -> "ערוץ ההתראות כבוי. פתח את הגדרות ההתראות והפעל את ערוץ כרטיסי הפולואפ."
    id == CheckId.PHONE_STATE -> "זיהוי סוף שיחה צריך הרשאת מצב טלפון. בלי זה אפשר להמשיך במצב ידני."
    id == CheckId.CALL_LOG -> "יומן שיחות הוא אופציונלי. בלי ההרשאה הזו המספר לא יתמלא אוטומטית."
    id == CheckId.CONTACTS -> "אנשי קשר הם אופציונליים. בלי ההרשאה הזו השם לא יתמלא אוטומטית."
    id == CheckId.DETECTION_ENABLED -> "זיהוי שיחות כבוי. הפעל אותו במסך השליחה הראשי."
    id == CheckId.BATTERY_OPTIMIZATION && state == CheckState.UNKNOWN -> "לא ניתן היה לקרוא את מצב חיסכון הסוללה במכשיר הזה."
    id == CheckId.BATTERY_OPTIMIZATION -> "ייתכן שחיסכון סוללה מונע פעולה ברקע. פתח את שלב הסוללה באשף ובדוק החרגה לאפליקציה."
    id == CheckId.MISSED_CALL_AUTO_RESPONSE_ENABLED -> "התגובה האוטומטית לשיחות שלא נענו כבויה. זה מצב תקין אם לא בחרת להפעיל אותה."
    id == CheckId.WHATSAPP_INSTALLED -> "כדי להשתמש ב-WhatsApp first צריך להתקין WhatsApp או WhatsApp Business. אם אין WhatsApp, אפשר להשתמש בגיבוי SMS."
    id == CheckId.WHATSAPP_ACCESSIBILITY_SERVICE -> "נדרש להפעיל שירות נגישות כדי לשלוח WhatsApp אוטומטית. בלי זה האפליקציה תפתח WhatsApp מוכן לשליחה ידנית."
    id == CheckId.SMS_PERMISSION -> "האפליקציה צריכה הרשאת SMS רק עבור גיבוי SMS אוטומטי. בלי הרשאה אפשר לפתוח הודעה מוכנה לשליחה ידנית."
    id == CheckId.MISSED_CALL_TEMPLATE -> "חסרה תבנית תגובה לשיחה שלא נענתה. בדוק את מסך התבניות."
    id == CheckId.MISSED_CALL_COOLDOWN_STORE -> "לא ניתן לקרוא את שמירת מניעת הכפילויות המקומית."
    else -> null
}

private fun troubleshootingSuggestion(snapshot: SelfTestSnapshot): String = when {
    snapshot.state(CheckId.PHONE_STATE) == CheckState.FAIL ->
        "לא נרשם שהבדיקה עבדה. קודם אשר הרשאת מצב טלפון, ואז הפעל זיהוי שיחות ובדוק שוב."
    snapshot.state(CheckId.NOTIFICATIONS) == CheckState.FAIL ||
        snapshot.state(CheckId.CHANNEL_ENABLED) == CheckState.FAIL ->
        "לא נרשם שהבדיקה עבדה. בדוק שההתראות וערוץ ההתראות פעילים, ואז נסה שוב."
    snapshot.state(CheckId.DETECTION_ENABLED) == CheckState.FAIL ->
        "לא נרשם שהבדיקה עבדה. הפעל זיהוי שיחות במסך השליחה הראשי, ואז חזור לבדיקה."
    snapshot.state(CheckId.BATTERY_OPTIMIZATION) == CheckState.FAIL ||
        snapshot.verdict == ReadinessVerdict.PARTIAL ->
        "לא נרשם שהבדיקה עבדה. אם ההרשאות תקינות, חזור לשלב הסוללה ובדוק שהאפליקציה מוחרגת מחיסכון סוללה."
    else ->
        "לא נרשם שהבדיקה עבדה. נסה שיחה קצרה נוספת; אם עדיין אין כרטיס, בדוק את הגדרות הסוללה וההתראות במכשיר."
}

@Composable
private fun PostCallScreen(
    phone: String,
    leadName: String,
    initialTemplateId: String,
    initialMessageOverride: String?,
    callDurationSeconds: Long?,
    callTimestampMillis: Long?,
    callType: String?,
    openedFromNotification: Boolean,
    myDetailsStore: MyDetailsStore,
    followUpLogStore: FollowUpLogStore,
    followUpTaskDao: FollowUpTaskDao,
    leadDao: LeadDao,
    reminderScheduler: ReminderScheduler,
    restoredTaskId: Long?,
    onRestoredTaskStatusChanged: (Long?) -> Unit,
    selectionStatus: String?,
    onSelectionStatusChanged: (String?) -> Unit,
    onEditMessage: (PostCallCard, String) -> Unit,
    onCloseCard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myDetailsProfile = remember(myDetailsStore) { myDetailsStore.load() }
    val initialCard = remember(initialTemplateId, initialMessageOverride) {
        initialPostCallCard(initialTemplateId, initialMessageOverride)
    }
    var selectedCard by remember(initialCard) { mutableStateOf(initialCard) }
    var selectedMessage by remember(initialCard, initialMessageOverride) {
        mutableStateOf(initialMessageOverride ?: initialCard.composerHint.initialMessage)
    }
    var snoozeOptionsOpen by remember { mutableStateOf(false) }
    val baseContextLine = when {
        leadName.isNotBlank() && phone.isNotBlank() -> "שיחה עם $leadName · $phone"
        leadName.isNotBlank() -> "שיחה עם $leadName"
        phone.isNotBlank() -> "שיחה עם $phone"
        else -> "לא זוהה מספר מההתראה. אפשר לבחור כרטיס ולהמשיך ידנית."
    }
    val metadataLine = callMetadataLabel(callType, callDurationSeconds)
    val contextLine = if (metadataLine == null || phone.isBlank()) {
        baseContextLine
    } else {
        "$baseContextLine · $metadataLine"
    }

    val renderedMessage = TemplateTagRenderer.render(
        selectedMessage,
        TemplateTagValues(
            leadName = leadName,
            agentName = myDetailsProfile.agentName,
            officeName = myDetailsProfile.officeName,
            phone = myDetailsProfile.phone,
            website = myDetailsProfile.website,
            businessCard = myDetailsProfile.businessCard,
            signature = myDetailsProfile.signature,
            propertyName = activePropertyName(myDetailsProfile),
            propertyLink = activePropertyLink(myDetailsProfile)
        )
    )
    val normalizedPhone = PhoneNumberNormalizer.normalizeForWhatsApp(phone)
    val pipelineSource = if (openedFromNotification) {
        FollowUpSource.POST_CALL_AUTO
    } else {
        FollowUpSource.MANUAL_COMPOSER
    }

    suspend fun ensureTask(card: PostCallCard): FollowUpTaskEntity {
        fun FollowUpTaskEntity.withCurrentCardState(now: Long): FollowUpTaskEntity =
            LeadPipeline.mergeCurrentCardState(
                task = this,
                phone = phone.takeIf { it.isNotBlank() },
                contactName = leadName.takeIf { it.isNotBlank() },
                selectedTemplateId = card.composerHint.templateId,
                draftText = selectedMessage,
                callEndedAtEpochMs = callTimestampMillis,
                callDurationSeconds = callDurationSeconds,
                leadType = null,
                propertyLink = activePropertyLink(myDetailsProfile),
                source = pipelineSource,
                nowEpochMs = now
            )

        restoredTaskId?.let { taskId ->
            followUpTaskDao.getById(taskId)?.let { task ->
                val merged = task.withCurrentCardState(System.currentTimeMillis())
                followUpTaskDao.update(merged)
                return merged
            }
        }

        val lookupPhone = phone.takeIf { it.isNotBlank() }
        if (lookupPhone != null) {
            followUpTaskDao.getLatestByPhoneAndStatuses(
                phone = lookupPhone,
                statuses = FollowUpTaskStatus.active.toList()
            )?.let { task ->
                val merged = task.withCurrentCardState(System.currentTimeMillis())
                followUpTaskDao.update(merged)
                onRestoredTaskStatusChanged(merged.id)
                return merged
            }
        }

        val now = System.currentTimeMillis()
        val task = LeadPipeline.createPendingPostCallTask(
            phone = phone.takeIf { it.isNotBlank() },
            contactName = leadName.takeIf { it.isNotBlank() },
            selectedTemplateId = card.composerHint.templateId,
            draftText = selectedMessage,
            callEndedAtEpochMs = callTimestampMillis,
            callDurationSeconds = callDurationSeconds,
            leadType = null,
            propertyLink = activePropertyLink(myDetailsProfile),
            nowEpochMs = now,
            source = pipelineSource
        )
        val taskId = followUpTaskDao.insert(task)
        onRestoredTaskStatusChanged(taskId)
        return task.copy(id = taskId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "מה קרה בשיחה?",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = contextLine,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        PostCallCards.all.forEach { card ->
            Card(
                onClick = {
                    selectedCard = card
                    selectedMessage = card.composerHint.initialMessage
                    snoozeOptionsOpen = false
                    onSelectionStatusChanged(null)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = card.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "הפעולה הבאה: ${selectedCard.title}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = renderedMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (normalizedPhone == null || renderedMessage.isBlank()) {
                            onSelectionStatusChanged("יש להשלים מספר תקין והודעה לפני פתיחת WhatsApp.")
                            return@Button
                        }
                        scope.launch {
                            val task = ensureTask(selectedCard)
                            val whatsappPackages = WhatsAppPackageResolver(context).resolve("")
                            val targetPackage = whatsappPackages.selectedPackage
                            val controller = WhatsAppAutoSendController(context)
                            val accessibilityEnabled = controller.isAccessibilityServiceEnabled()

                            if (accessibilityEnabled && targetPackage != null) {
                                controller.enqueuePendingSend(
                                    phone = normalizedPhone,
                                    message = renderedMessage,
                                    packageName = targetPackage,
                                    nowEpochMs = System.currentTimeMillis(),
                                    source = "PostCallScreen"
                                )
                                val openResult = openWhatsApp(
                                    context = context,
                                    link = WhatsAppLinkBuilder.build(normalizedPhone, renderedMessage)
                                )
                                if (openResult == null) {
                                    followUpTaskDao.update(
                                        LeadPipeline.markWhatsAppOpened(
                                            task = task,
                                            nowEpochMs = System.currentTimeMillis()
                                        )
                                    )
                                    followUpLogStore.append(
                                        followUpLogEntry(
                                            renderedMessage = renderedMessage,
                                            actionType = FollowUpActionType.WHATSAPP_OPENED
                                        )
                                    )
                                    onSelectionStatusChanged("WhatsApp נפתח — השירות לוחץ שליחה באופן אוטומטי.")
                                } else {
                                    onSelectionStatusChanged(openResult)
                                }
                            } else {
                                val resultMessage = openWhatsApp(
                                    context = context,
                                    link = WhatsAppLinkBuilder.build(normalizedPhone, renderedMessage)
                                )
                                if (resultMessage == null) {
                                    followUpTaskDao.update(
                                        LeadPipeline.markWhatsAppOpened(
                                            task = task,
                                            nowEpochMs = System.currentTimeMillis()
                                        )
                                    )
                                    followUpLogStore.append(
                                        followUpLogEntry(
                                            renderedMessage = renderedMessage,
                                            actionType = FollowUpActionType.WHATSAPP_OPENED
                                        )
                                    )
                                    onSelectionStatusChanged("WhatsApp נפתח. השליחה נשארת ידנית בתוך WhatsApp.")
                                } else {
                                    onSelectionStatusChanged(resultMessage)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("פתח WhatsApp")
                }
                OutlinedButton(
                    onClick = {
                        snoozeOptionsOpen = !snoozeOptionsOpen
                        onSelectionStatusChanged(null)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("הזכר לי אחר כך")
                }
                if (snoozeOptionsOpen) {
                    SnoozeOption.entries.forEach { option ->
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val task = ensureTask(selectedCard)
                                    val now = System.currentTimeMillis()
                                    val reminderAt = SnoozeTimeCalculator.computeTriggerAt(
                                        option = option,
                                        nowMillis = now,
                                        zoneId = ZoneId.systemDefault()
                                    )
                                    followUpTaskDao.update(
                                        LeadPipeline.snoozeTask(
                                            task = task,
                                            reminderAtEpochMs = reminderAt,
                                            nowEpochMs = now
                                        )
                                    )
                                    reminderScheduler.schedule(task.id, reminderAt)
                                    snoozeOptionsOpen = false
                                    onSelectionStatusChanged("התזכורת נקבעה.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.label)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (phone.isBlank() && leadName.isBlank()) {
                            onSelectionStatusChanged("יש להזין שם או מספר לפני שמירת ליד.")
                            return@OutlinedButton
                        }
                        scope.launch {
                            val task = ensureTask(selectedCard)
                            val now = System.currentTimeMillis()
                            val existingLead = task.phone
                                ?.takeIf { it.isNotBlank() }
                                ?.let { leadDao.getByPhone(it) }
                            val lead = LeadPipeline.leadFromTask(
                                task = task,
                                nowEpochMs = now,
                                existingLead = existingLead
                            )
                            if (existingLead == null) {
                                leadDao.insert(lead)
                            } else {
                                leadDao.update(lead)
                            }
                            followUpTaskDao.update(
                                LeadPipeline.markSavedAsLead(
                                    task = task,
                                    nowEpochMs = now
                                )
                            )
                            onSelectionStatusChanged("הליד נשמר למעקב מקומי.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("שמור למעקב")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val task = ensureTask(selectedCard)
                            followUpTaskDao.update(
                                LeadPipeline.closeTask(
                                    task = task,
                                    nowEpochMs = System.currentTimeMillis()
                                )
                            )
                            reminderScheduler.cancel(task.id)
                            onRestoredTaskStatusChanged(null)
                            onCloseCard()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("סגור ללא פולואפ")
                }
                OutlinedButton(
                    onClick = { onEditMessage(selectedCard, selectedMessage) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ערוך הודעה")
                }
            }
        }

        selectionStatus?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MissedCallAutoResponseSettingsCard(
    enabled: Boolean,
    primaryChannel: MissedCallResponsePrimaryChannel,
    whatsappMode: MissedCallWhatsAppMode,
    smsFallbackEnabled: Boolean,
    manualSmsFallbackEnabled: Boolean,
    status: String?,
    onToggle: () -> Unit,
    onCancel: () -> Unit,
    onSelectPrimaryChannel: (MissedCallResponsePrimaryChannel) -> Unit,
    onSelectWhatsAppMode: (MissedCallWhatsAppMode) -> Unit,
    onToggleSmsFallback: () -> Unit,
    onToggleManualSmsFallback: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val context = LocalContext.current
    val whatsappPackages = remember(context) {
        WhatsAppPackageResolver(context).resolve(preferredPackage = "")
    }
    val accessibilityEnabled = remember(context, whatsappMode) {
        WhatsAppAutoSendController(context).isAccessibilityServiceEnabled()
    }
    val smsPermissionGranted =
        context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    val whatsappPreparedReplyAvailable =
        primaryChannel == MissedCallResponsePrimaryChannel.WHATSAPP_FIRST && whatsappPackages.anyInstalled
    val whatsappAutoSendUserEnabled = whatsappMode == MissedCallWhatsAppMode.ACCESSIBILITY_AUTO

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("תגובה לשיחה שלא נענתה", style = MaterialTheme.typography.titleMedium)
            Text(
                "כאשר שיחה נכנסת לא נענית, האפליקציה מכינה תגובה עסקית מיידית. WhatsApp הוא הערוץ הראשי, ו-SMS משמש רק כגיבוי.",
                style = MaterialTheme.typography.bodyMedium
            )
            status?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
            AutoResponseReadinessStatusSection(
                responseActive = enabled,
                primaryChannel = primaryChannel,
                whatsappInstalled = whatsappPackages.messengerInstalled,
                whatsappBusinessInstalled = whatsappPackages.businessInstalled,
                whatsappPreparedReplyAvailable = whatsappPreparedReplyAvailable,
                whatsappAutoSendActive = whatsappAutoSendUserEnabled && accessibilityEnabled,
                accessibilityEnabled = accessibilityEnabled,
                whatsappAutoSendUserEnabled = whatsappAutoSendUserEnabled,
                smsFallbackEnabled = smsFallbackEnabled,
                smsPermissionGranted = smsPermissionGranted
            )
            Text(
                text = if (enabled) "פעיל" else "כבוי",
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Button(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (enabled) "כבה תגובה אוטומטית" else "הפעל תגובה אוטומטית")
            }
            if (!enabled) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("לא עכשיו")
                }
            }

            Text("ערוץ ראשי", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onSelectPrimaryChannel(MissedCallResponsePrimaryChannel.WHATSAPP_FIRST) },
                    enabled = primaryChannel != MissedCallResponsePrimaryChannel.WHATSAPP_FIRST,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("WhatsApp first")
                }
                Button(
                    onClick = { onSelectPrimaryChannel(MissedCallResponsePrimaryChannel.SMS_ONLY) },
                    enabled = primaryChannel != MissedCallResponsePrimaryChannel.SMS_ONLY,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("SMS only")
                }
            }

            Text("מצב WhatsApp", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = { onSelectWhatsAppMode(MissedCallWhatsAppMode.PREPARED_MANUAL) },
                enabled = whatsappMode != MissedCallWhatsAppMode.PREPARED_MANUAL,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("פתיחת WhatsApp מוכן לשליחה")
            }
            OutlinedButton(
                onClick = { onSelectWhatsAppMode(MissedCallWhatsAppMode.ACCESSIBILITY_AUTO) },
                enabled = whatsappMode != MissedCallWhatsAppMode.ACCESSIBILITY_AUTO,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("שליחה אוטומטית ב-WhatsApp באמצעות נגישות")
            }

            if (whatsappMode == MissedCallWhatsAppMode.ACCESSIBILITY_AUTO) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("שליחה אוטומטית ב-WhatsApp", style = MaterialTheme.typography.titleSmall)
                    Text("האפליקציה יכולה לפתוח את WhatsApp ולשלוח הודעה אוטומטית לשיחה שלא נענתה.")
                    Text("הפעולה תתבצע רק לאחר שתפעיל את האפשרות ותאשר שירות נגישות.")
                    Text("השימוש מיועד רק לתגובה לשיחה שלא נענתה, לפי תבנית שאתה קובע מראש.")
                    Text("אפשר לכבות את האפשרות בכל רגע.")
                    Button(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                        Text("הפעל שליחה אוטומטית ב-WhatsApp")
                    }
                    OutlinedButton(
                        onClick = { onSelectWhatsAppMode(MissedCallWhatsAppMode.PREPARED_MANUAL) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("פתח WhatsApp מוכן לשליחה ידנית")
                    }
                }
            }

            Text("גיבוי", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = onToggleSmsFallback, modifier = Modifier.fillMaxWidth()) {
                Text(if (smsFallbackEnabled) "כבה SMS אם WhatsApp לא זמין" else "שלח SMS אם WhatsApp לא זמין")
            }
            OutlinedButton(onClick = onToggleManualSmsFallback, modifier = Modifier.fillMaxWidth()) {
                Text(if (manualSmsFallbackEnabled) "כבה SMS מוכן ללא הרשאה" else "פתח SMS מוכן אם אין הרשאת שליחה אוטומטית")
            }
            Text(
                "ברירת המחדל היא 6 שעות קירור לכל מספר. לא נשלחת גם הודעת WhatsApp וגם SMS אלא אם הערוץ הקודם נכשל.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AutoResponseReadinessStatusSection(
    responseActive: Boolean,
    primaryChannel: MissedCallResponsePrimaryChannel,
    whatsappInstalled: Boolean,
    whatsappBusinessInstalled: Boolean,
    whatsappPreparedReplyAvailable: Boolean,
    whatsappAutoSendActive: Boolean,
    accessibilityEnabled: Boolean,
    whatsappAutoSendUserEnabled: Boolean,
    smsFallbackEnabled: Boolean,
    smsPermissionGranted: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("בדיקת מוכנות לתגובה אוטומטית", style = MaterialTheme.typography.titleSmall)
        ReadinessStatusLine("תגובה לשיחה שלא נענתה", if (responseActive) "פעילה" else "כבויה")
        ReadinessStatusLine(
            "ערוץ ראשי",
            if (primaryChannel == MissedCallResponsePrimaryChannel.WHATSAPP_FIRST) "WhatsApp תחילה" else "SMS בלבד"
        )
        ReadinessStatusLine("WhatsApp מותקן", yesNo(whatsappInstalled))
        ReadinessStatusLine("WhatsApp Business מותקן", yesNo(whatsappBusinessInstalled))
        ReadinessStatusLine("הודעת WhatsApp מוכנה לשליחה ידנית", yesNo(whatsappPreparedReplyAvailable))
        ReadinessStatusLine("שליחה אוטומטית ב-WhatsApp", if (whatsappAutoSendActive) "פעילה" else "לא פעילה")
        ReadinessStatusLine("שירות נגישות", if (accessibilityEnabled) "פעיל" else "נדרש להפעיל")
        ReadinessStatusLine("שליחה אוטומטית הופעלה על ידי המשתמש", yesNo(whatsappAutoSendUserEnabled))
        ReadinessStatusLine("גיבוי SMS", if (smsFallbackEnabled) "פעיל" else "כבוי")
        ReadinessStatusLine("הרשאת SMS", if (smsPermissionGranted) "קיימת" else "חסרה")
        ReadinessStatusLine("הגנת כפילויות", "פעילה")
        if (whatsappAutoSendUserEnabled && !accessibilityEnabled) {
            Text(
                "כדי לשלוח WhatsApp אוטומטית, יש להפעיל את שירות הנגישות של האפליקציה.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        if (smsFallbackEnabled && !smsPermissionGranted) {
            Text(
                "אם WhatsApp לא זמין, לא ניתן לשלוח SMS אוטומטי ללא הרשאה. אפשר עדיין לפתוח הודעה מוכנה לשליחה ידנית.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ReadinessStatusLine(label: String, value: String) {
    Text("$label: $value", style = MaterialTheme.typography.bodySmall)
}

private fun yesNo(value: Boolean): String = if (value) "כן" else "לא"

@Composable
private fun ManualWhatsAppScreen(
    myDetailsStore: MyDetailsStore,
    templateStore: TemplateStore,
    followUpLogStore: FollowUpLogStore,
    templateRevision: Int,
    phone: String,
    leadName: String,
    onPhoneChange: (String) -> Unit,
    onLeadNameChange: (String) -> Unit,
    initialTemplateId: String,
    initialMessageOverride: String?,
    initialMessageRevision: Int,
    restoredTaskId: Long?,
    followUpTaskDao: FollowUpTaskDao,
    leadDao: LeadDao,
    reminderScheduler: ReminderScheduler,
    onRestoredTaskStatusChanged: (Long?) -> Unit,
    notificationStatus: String?,
    onTriggerTestNotification: (phone: String, leadName: String, templateId: String) -> Unit,
    callDetectionEnabled: Boolean,
    callDetectionStatus: String?,
    onToggleCallDetection: () -> Unit,
    missedCallAutoResponseEnabled: Boolean,
    missedCallPrimaryChannel: MissedCallResponsePrimaryChannel,
    missedCallWhatsAppMode: MissedCallWhatsAppMode,
    missedCallSmsFallbackEnabled: Boolean,
    missedCallManualSmsFallbackEnabled: Boolean,
    missedCallAutoResponseStatus: String?,
    onToggleMissedCallAutoResponse: () -> Unit,
    onKeepMissedCallAutoResponseDisabled: () -> Unit,
    onSelectMissedCallPrimaryChannel: (MissedCallResponsePrimaryChannel) -> Unit,
    onSelectMissedCallWhatsAppMode: (MissedCallWhatsAppMode) -> Unit,
    onToggleMissedCallSmsFallback: () -> Unit,
    onToggleMissedCallManualSmsFallback: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    debugSimulatorStatus: String?,
    onTriggerDebugMissedCall: ((String) -> Unit)?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val templates = remember(templateStore, templateRevision) { templateStore.loadTemplates() }
    val myDetailsProfile = remember(myDetailsStore) { myDetailsStore.load() }
    val initialSelectedTemplate = templates.firstOrNull { it.id == initialTemplateId } ?: templates.first()
    var selectedTemplate by remember(templates, initialTemplateId, initialMessageRevision) {
        mutableStateOf(initialSelectedTemplate)
    }
    var message by remember(templates, initialTemplateId, initialMessageRevision) {
        mutableStateOf(initialMessageOverride ?: defaultMessageFor(initialSelectedTemplate))
    }
    var templateMenuOpen by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var snoozeOptionsOpen by remember { mutableStateOf(false) }
    var phoneValidationRequested by remember { mutableStateOf(false) }
    var messageValidationRequested by remember { mutableStateOf(false) }
    var debugMissedCallPhone by remember { mutableStateOf("0501234567") }

    val renderedMessage = TemplateTagRenderer.render(
        message,
        TemplateTagValues(
            leadName = leadName,
            agentName = myDetailsProfile.agentName,
            officeName = myDetailsProfile.officeName,
            phone = myDetailsProfile.phone,
            website = myDetailsProfile.website,
            businessCard = myDetailsProfile.businessCard,
            signature = myDetailsProfile.signature,
            propertyName = activePropertyName(myDetailsProfile),
            propertyLink = activePropertyLink(myDetailsProfile)
        )
    )
    val normalizedPhone = PhoneNumberNormalizer.normalizeForWhatsApp(phone)
    val whatsappLink = normalizedPhone?.let { WhatsAppLinkBuilder.build(it, renderedMessage) }.orEmpty()
    val phoneValidationMessage = when {
        !phoneValidationRequested -> null
        phone.isBlank() -> "יש להזין מספר טלפון או איש קשר."
        normalizedPhone == null -> "מספר הטלפון לא תקין."
        else -> null
    }
    val messageValidationMessage = when {
        !messageValidationRequested -> null
        renderedMessage.isBlank() -> "יש לכתוב הודעה לפני פתיחת WhatsApp, שיתוף או העתקה."
        else -> null
    }

    val scheduleSnooze: (SnoozeOption) -> Unit = { option ->
        val now = System.currentTimeMillis()
        val reminderAt = SnoozeTimeCalculator.computeTriggerAt(
            option = option,
            nowMillis = now,
            zoneId = ZoneId.systemDefault()
        )
        val task = FollowUpTaskEntity(
            id = restoredTaskId ?: 0L,
            phone = phone,
            contactName = leadName,
            callEndedAtEpochMs = null,
            callDurationSeconds = null,
            source = FollowUpSource.MANUAL_COMPOSER,
            selectedTemplateId = selectedTemplate.id,
            draftText = message,
            leadType = null,
            propertyLink = activePropertyLink(myDetailsProfile),
            reminderAtEpochMs = reminderAt,
            status = FollowUpTaskStatus.SNOOZED,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        scope.launch {
            val savedTaskId = followUpTaskDao.insert(task)
            reminderScheduler.schedule(savedTaskId, reminderAt)
            onRestoredTaskStatusChanged(savedTaskId)
            snoozeOptionsOpen = false
            statusMessage = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                "התזכורת נקבעה, אבל הרשאת התראות חסרה ולכן ייתכן שלא תוצג התראה."
            } else {
                "תזכורת נקבעה."
            }
        }
    }
    val saveLead: () -> Unit = saveLead@{
        val now = System.currentTimeMillis()
        if (phone.isBlank() && leadName.isBlank()) {
            statusMessage = "יש להזין שם או מספר לפני שמירת ליד."
            return@saveLead
        }

        scope.launch {
            leadDao.insert(
                LeadEntity(
                    fullName = leadName.takeIf { it.isNotBlank() },
                    phone = phone,
                    type = LeadType.UNKNOWN,
                    status = LeadStatus.NEW,
                    notes = null,
                    lastCallAtEpochMs = null,
                    lastFollowUpAtEpochMs = now,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            restoredTaskId?.let { taskId ->
                followUpTaskDao.getById(taskId)?.let { task ->
                    followUpTaskDao.update(
                        task.copy(
                            status = FollowUpTaskStatus.SAVED_AS_LEAD,
                            updatedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                }
            }
            statusMessage = "הליד נשמר במכשיר."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "שליחת המשך טיפול ב-WhatsApp",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start
        )
        Text(
            text = "הזן מספר, בחר תבנית, ערוך את ההודעה ופתח את WhatsApp. השליחה מתבצעת ידנית בתוך WhatsApp.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedButton(
            onClick = {
                onTriggerTestNotification(phone, leadName, selectedTemplate.id)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("בדיקת התראת פולואפ")
        }
        notificationStatus?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
        }
        OutlinedButton(
            onClick = onToggleCallDetection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (callDetectionEnabled) "כבה זיהוי שיחות אוטומטי" else "הפעל זיהוי שיחות אוטומטי")
        }
        callDetectionStatus?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
        }

        MissedCallAutoResponseSettingsCard(
            enabled = missedCallAutoResponseEnabled,
            primaryChannel = missedCallPrimaryChannel,
            whatsappMode = missedCallWhatsAppMode,
            smsFallbackEnabled = missedCallSmsFallbackEnabled,
            manualSmsFallbackEnabled = missedCallManualSmsFallbackEnabled,
            status = missedCallAutoResponseStatus,
            onToggle = onToggleMissedCallAutoResponse,
            onCancel = onKeepMissedCallAutoResponseDisabled,
            onSelectPrimaryChannel = onSelectMissedCallPrimaryChannel,
            onSelectWhatsAppMode = onSelectMissedCallWhatsAppMode,
            onToggleSmsFallback = onToggleMissedCallSmsFallback,
            onToggleManualSmsFallback = onToggleMissedCallManualSmsFallback,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings
        )
        if (BuildConfig.DEBUG && onTriggerDebugMissedCall != null) {
            DebugMissedCallSimulatorCard(
                phone = debugMissedCallPhone,
                status = debugSimulatorStatus,
                onPhoneChange = {
                    debugMissedCallPhone = it
                },
                onTrigger = onTriggerDebugMissedCall
            )
        }

        OutlinedTextField(
            value = phone,
            onValueChange = {
                onPhoneChange(it)
                statusMessage = null
            },
            label = { Text("מספר טלפון / איש קשר") },
            placeholder = { Text("050-1234567") },
            singleLine = true,
            isError = phoneValidationMessage != null,
            supportingText = {
                phoneValidationMessage?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = leadName,
            onValueChange = {
                onLeadNameChange(it)
                statusMessage = null
            },
            label = { Text("שם לקוח (lead_name)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Box {
            OutlinedButton(
                onClick = { templateMenuOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedTemplate.title)
            }
            DropdownMenu(
                expanded = templateMenuOpen,
                onDismissRequest = { templateMenuOpen = false }
            ) {
                templates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.title) },
                        onClick = {
                            selectedTemplate = template
                            message = defaultMessageFor(template)
                            statusMessage = null
                            templateMenuOpen = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = message,
            onValueChange = {
                message = it
                statusMessage = null
            },
            label = { Text("הודעה לעריכה") },
            minLines = 7,
            isError = messageValidationMessage != null,
            supportingText = {
                messageValidationMessage?.let { Text(it) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("הודעה אחרי תגיות", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = renderedMessage.ifBlank { "ההודעה הריקה לא תישלח" },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("קישור שיווצר", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = whatsappLink.ifBlank { "יוצג לאחר הזנת מספר תקין" },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    phoneValidationRequested = true
                    messageValidationRequested = true
                    val currentPhone = normalizedPhone
                    if (currentPhone == null || renderedMessage.isBlank()) {
                        statusMessage = "יש להשלים את השדות המסומנים לפני פתיחת WhatsApp."
                        return@Button
                    }
                    val whatsappPackages = WhatsAppPackageResolver(context).resolve("")
                    val targetPackage = whatsappPackages.selectedPackage
                    val controller = WhatsAppAutoSendController(context)
                    val accessibilityEnabled = controller.isAccessibilityServiceEnabled()

                    if (accessibilityEnabled && targetPackage != null) {
                        controller.enqueuePendingSend(
                            phone = currentPhone,
                            message = renderedMessage,
                            packageName = targetPackage,
                            nowEpochMs = System.currentTimeMillis(),
                            source = "ManualComposer"
                        )
                    }

                    val resultMessage = openWhatsApp(context, WhatsAppLinkBuilder.build(currentPhone, renderedMessage))
                    statusMessage = resultMessage
                    if (resultMessage == null) {
                        followUpLogStore.append(
                            followUpLogEntry(
                                renderedMessage = renderedMessage,
                                actionType = FollowUpActionType.WHATSAPP_OPENED
                            )
                        )
                        if (accessibilityEnabled && targetPackage != null) {
                            statusMessage = "WhatsApp נפתח — השירות לוחץ שליחה באופן אוטומטי."
                        } else {
                            statusMessage = "WhatsApp נפתח. השליחה נשארת ידנית בתוך WhatsApp."
                        }
                        restoredTaskId?.let { taskId ->
                            scope.launch {
                                followUpTaskDao.getById(taskId)?.let { task ->
                                    followUpTaskDao.update(
                                        task.copy(
                                            status = FollowUpTaskStatus.WHATSAPP_OPENED,
                                            updatedAtEpochMs = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("פתח WhatsApp")
            }
            OutlinedButton(
                onClick = {
                    messageValidationRequested = true
                    if (renderedMessage.isBlank()) {
                        statusMessage = "יש לכתוב הודעה לפני שיתוף ידני."
                        return@OutlinedButton
                    }
                    val result = openShareSheet(context, renderedMessage)
                    statusMessage = result.statusMessage
                    if (result.opened) {
                        followUpLogStore.append(
                            followUpLogEntry(
                                renderedMessage = renderedMessage,
                                actionType = FollowUpActionType.SHARE_OPENED
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("שיתוף ידני")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    messageValidationRequested = true
                    if (renderedMessage.isBlank()) {
                        statusMessage = "יש לכתוב הודעה לפני העתקה."
                        return@OutlinedButton
                    }
                    statusMessage = copyMessageToClipboard(context, renderedMessage)
                    followUpLogStore.append(
                        followUpLogEntry(
                            renderedMessage = renderedMessage,
                            actionType = FollowUpActionType.COPY_USED
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("העתק הודעה")
            }
            OutlinedButton(
                onClick = {
                    message = defaultMessageFor(selectedTemplate)
                    statusMessage = null
                    messageValidationRequested = false
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("איפוס הודעה")
            }
        }

        OutlinedButton(
            onClick = {
                snoozeOptionsOpen = !snoozeOptionsOpen
                statusMessage = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("הזכר לי אחר כך")
        }

        if (snoozeOptionsOpen) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("בחר מועד לתזכורת", style = MaterialTheme.typography.titleSmall)
                    SnoozeOption.entries.forEach { option ->
                        OutlinedButton(
                            onClick = { scheduleSnooze(option) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.label)
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = saveLead,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("שמור כליד")
        }

        statusMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DebugMissedCallSimulatorCard(
    phone: String,
    status: String?,
    onPhoneChange: (String) -> Unit,
    onTrigger: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("בדיקת שיחה שלא נענתה", style = MaterialTheme.typography.titleMedium)
            Text(
                "מצב בדיקה בלבד. האירוע נשלח דרך אותו מטפל של שיחה שלא נענתה, בלי לעקוף את החלטות ה-WhatsApp, ה-SMS או ה-cooldown.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("מספר טלפון לבדיקה") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onTrigger(phone) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("הפעל בדיקת שיחה שלא נענתה")
            }
            status?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TemplateManagementScreen(
    templateStore: TemplateStore,
    myDetailsStore: MyDetailsStore,
    onTemplatesChanged: () -> Unit
) {
    val builtInTemplates = remember { SprintOneTemplates.all }
    var savedTemplates by remember(templateStore) { mutableStateOf(templateStore.loadTemplates()) }
    val myDetailsProfile = remember(myDetailsStore) { myDetailsStore.load() }
    var selectedTemplate by remember { mutableStateOf(savedTemplates.first()) }
    var draftBodyField by remember {
        mutableStateOf(TextFieldValue(selectedTemplate.body, TextRange(selectedTemplate.body.length)))
    }
    var leadName by remember { mutableStateOf("") }
    var templateMenuOpen by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val renderedPreview = TemplateTagRenderer.render(
        draftBodyField.text,
        TemplateTagValues(
            leadName = leadName,
            agentName = myDetailsProfile.agentName,
            officeName = myDetailsProfile.officeName,
            phone = myDetailsProfile.phone,
            website = myDetailsProfile.website,
            businessCard = myDetailsProfile.businessCard,
            signature = myDetailsProfile.signature,
            propertyName = activePropertyName(myDetailsProfile),
            propertyLink = activePropertyLink(myDetailsProfile)
        )
    )
    val builtInBody = builtInTemplates.firstOrNull { it.id == selectedTemplate.id }?.body.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ניהול תבניות הודעה",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "ערוך תבניות מקומיות, הוסף תגים, ושמור לשימוש מהיר במסך השליחה.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Box {
            OutlinedButton(
                onClick = { templateMenuOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedTemplate.title)
            }
            DropdownMenu(
                expanded = templateMenuOpen,
                onDismissRequest = { templateMenuOpen = false }
            ) {
                savedTemplates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.title) },
                        onClick = {
                            selectedTemplate = template
                            draftBodyField = TextFieldValue(template.body, TextRange(template.body.length))
                            statusMessage = null
                            templateMenuOpen = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = leadName,
            onValueChange = {
                leadName = it
                statusMessage = null
            },
            label = { Text("שם לקוח לתצוגה מקדימה (lead_name)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("הוסף תג לתבנית", style = MaterialTheme.typography.titleMedium)
                SupportedTagButtons(onTagSelected = { tag ->
                    val insertion = TemplateTagInsertionLogic.insertTag(
                        text = draftBodyField.text,
                        selectionStart = draftBodyField.selection.start,
                        selectionEnd = draftBodyField.selection.end,
                        tag = tag
                    )
                    draftBodyField = TextFieldValue(
                        text = insertion.text,
                        selection = TextRange(insertion.cursorPosition)
                    )
                    statusMessage = null
                })
            }
        }

        OutlinedTextField(
            value = draftBodyField,
            onValueChange = {
                draftBodyField = it
                statusMessage = null
            },
            label = { Text("טקסט תבנית") },
            minLines = 8,
            modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("תצוגה מקדימה אחרי תגים", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = renderedPreview.ifBlank { "התבנית הריקה תיחסם במסך השליחה" },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val updatedTemplate = selectedTemplate.copy(body = draftBodyField.text)
                    templateStore.saveTemplate(updatedTemplate)
                    savedTemplates = templateStore.loadTemplates()
                    selectedTemplate = updatedTemplate
                    onTemplatesChanged()
                    statusMessage = "התבנית נשמרה במכשיר."
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("שמור תבנית")
            }
            OutlinedButton(
                onClick = {
                    templateStore.resetTemplate(selectedTemplate.id)
                    val resetTemplate = selectedTemplate.copy(body = builtInBody)
                    savedTemplates = templateStore.loadTemplates()
                    selectedTemplate = resetTemplate
                    draftBodyField = TextFieldValue(builtInBody, TextRange(builtInBody.length))
                    onTemplatesChanged()
                    statusMessage = "התבנית חזרה לנוסח המקורי."
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("שחזר מקור")
            }
        }

        statusMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SupportedTagButtons(onTagSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TemplateTags.supported.chunked(3).forEach { rowTags ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTags.forEach { tag ->
                    OutlinedButton(
                        onClick = { onTagSelected(tag.key) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tag.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun MyDetailsScreen(store: MyDetailsStore) {
    val savedProfile = remember(store) { store.load() }
    var agentName by remember { mutableStateOf(savedProfile.agentName) }
    var officeName by remember { mutableStateOf(savedProfile.officeName) }
    var phone by remember { mutableStateOf(savedProfile.phone) }
    var website by remember { mutableStateOf(savedProfile.website) }
    var businessCard by remember { mutableStateOf(savedProfile.businessCard) }
    var signature by remember { mutableStateOf(savedProfile.signature) }
    var property1Name by remember { mutableStateOf(savedProfile.property1Name) }
    var property1Link by remember { mutableStateOf(savedProfile.property1Link) }
    var property2Name by remember { mutableStateOf(savedProfile.property2Name) }
    var property2Link by remember { mutableStateOf(savedProfile.property2Link) }
    var property3Name by remember { mutableStateOf(savedProfile.property3Name) }
    var property3Link by remember { mutableStateOf(savedProfile.property3Link) }
    var activePropertyIndex by remember { mutableStateOf(savedProfile.activePropertyIndex.coerceIn(1, 3)) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "הפרטים שלי",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "שמירת פרטי הסוכן נעשית מקומית במכשיר בלבד.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = agentName,
            onValueChange = {
                agentName = it
                statusMessage = null
            },
            label = { Text("שם הסוכן (agent_name)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = officeName,
            onValueChange = {
                officeName = it
                statusMessage = null
            },
            label = { Text("שם המשרד (office_name)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it
                statusMessage = null
            },
            label = { Text("טלפון (phone)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = website,
            onValueChange = {
                website = it
                statusMessage = null
            },
            label = { Text("אתר (website)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = businessCard,
            onValueChange = {
                businessCard = it
                statusMessage = null
            },
            label = { Text("כרטיס ביקור (business_card)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = signature,
            onValueChange = {
                signature = it
                statusMessage = null
            },
            label = { Text("חתימה (signature)") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("נכסים פעילים", style = MaterialTheme.typography.titleMedium)
                Text(
                    "ערוך עד 3 נכסים, בחר נכס פעיל, ושמור. התגים {property_name} ו-{property_link} ישתמשו בנכס הפעיל.",
                    style = MaterialTheme.typography.bodySmall
                )
                PropertyFields(
                    index = 1,
                    name = property1Name,
                    link = property1Link,
                    activePropertyIndex = activePropertyIndex,
                    onNameChange = {
                        property1Name = it
                        statusMessage = null
                    },
                    onLinkChange = {
                        property1Link = it
                        statusMessage = null
                    },
                    onSelectActive = {
                        activePropertyIndex = 1
                        statusMessage = null
                    }
                )
                PropertyFields(
                    index = 2,
                    name = property2Name,
                    link = property2Link,
                    activePropertyIndex = activePropertyIndex,
                    onNameChange = {
                        property2Name = it
                        statusMessage = null
                    },
                    onLinkChange = {
                        property2Link = it
                        statusMessage = null
                    },
                    onSelectActive = {
                        activePropertyIndex = 2
                        statusMessage = null
                    }
                )
                PropertyFields(
                    index = 3,
                    name = property3Name,
                    link = property3Link,
                    activePropertyIndex = activePropertyIndex,
                    onNameChange = {
                        property3Name = it
                        statusMessage = null
                    },
                    onLinkChange = {
                        property3Link = it
                        statusMessage = null
                    },
                    onSelectActive = {
                        activePropertyIndex = 3
                        statusMessage = null
                    }
                )
            }
        }

        Button(
            onClick = {
                store.save(
                    MyDetailsProfile(
                        agentName = agentName,
                        officeName = officeName,
                        phone = phone,
                        website = website,
                        businessCard = businessCard,
                        signature = signature,
                        property1Name = property1Name,
                        property1Link = property1Link,
                        property2Name = property2Name,
                        property2Link = property2Link,
                        property3Name = property3Name,
                        property3Link = property3Link,
                        activePropertyIndex = activePropertyIndex
                    )
                )
                statusMessage = "הפרטים נשמרו במכשיר."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("שמור פרטים")
        }

        statusMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PropertyFields(
    index: Int,
    name: String,
    link: String,
    activePropertyIndex: Int,
    onNameChange: (String) -> Unit,
    onLinkChange: (String) -> Unit,
    onSelectActive: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "נכס $index",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            if (activePropertyIndex == index) {
                Button(onClick = onSelectActive, enabled = false) {
                    Text("נכס פעיל")
                }
            } else {
                OutlinedButton(onClick = onSelectActive) {
                    Text("בחר כפעיל")
                }
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("שם נכס $index") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = link,
            onValueChange = onLinkChange,
            label = { Text("קישור נכס $index") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun activePropertyName(profile: MyDetailsProfile): String = when (profile.activePropertyIndex.coerceIn(1, 3)) {
    1 -> profile.property1Name
    2 -> profile.property2Name
    else -> profile.property3Name
}

private fun activePropertyLink(profile: MyDetailsProfile): String = when (profile.activePropertyIndex.coerceIn(1, 3)) {
    1 -> profile.property1Link
    2 -> profile.property2Link
    else -> profile.property3Link
}

private fun defaultMessageFor(template: MessageTemplate): String = template.body

private fun initialPostCallCard(
    templateId: String,
    draftText: String?
): PostCallCard =
    PostCallCards.all.firstOrNull { card ->
        card.composerHint.templateId == templateId && draftText == card.composerHint.initialMessage
    } ?: PostCallCards.all.firstOrNull { card ->
        card.composerHint.templateId == templateId
    } ?: PostCallCards.all.first()

private fun callDetectionStatusAfterPermissions(
    notificationsGranted: Boolean,
    callLogGranted: Boolean,
    contactsGranted: Boolean
): String {
    val notes = mutableListOf("זיהוי שיחות הופעל.")
    if (!callLogGranted) {
        notes += "ללא הרשאת יומן שיחות, המספר יישאר להזנה ידנית."
    }
    if (!contactsGranted) {
        notes += "ללא הרשאת אנשי קשר, השם יישאר להזנה ידנית."
    }
    if (!notificationsGranted) {
        notes += "ללא הרשאת התראות, ייתכן שלא תוצג התראת פולואפ."
    }
    notes += "אין שליחה אוטומטית ל-WhatsApp."
    return notes.joinToString(" ")
}

private fun missedCallAutoResponseStatus(
    context: Context,
    enabled: Boolean,
    primaryChannel: MissedCallResponsePrimaryChannel,
    whatsappMode: MissedCallWhatsAppMode,
    smsFallbackEnabled: Boolean,
    accessibilityEnabled: Boolean
): String =
    when {
        !enabled ->
            "כבוי — לא יישלחו הודעות אוטומטיות."
        primaryChannel == MissedCallResponsePrimaryChannel.SMS_ONLY &&
            context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED ->
            "פעיל — שיחה שלא נענתה תישלח ב-SMS בלבד."
        primaryChannel == MissedCallResponsePrimaryChannel.SMS_ONLY ->
            "נדרשת הרשאת SMS כדי לשלוח SMS אוטומטי."
        whatsappMode == MissedCallWhatsAppMode.ACCESSIBILITY_AUTO && !accessibilityEnabled ->
            "נדרש להפעיל שירות נגישות כדי לשלוח WhatsApp אוטומטית."
        whatsappMode == MissedCallWhatsAppMode.ACCESSIBILITY_AUTO ->
            "פעיל — שיחה שלא נענתה תישלח אוטומטית ב-WhatsApp באמצעות שירות נגישות."
        smsFallbackEnabled ->
            "פעיל — שיחה שלא נענתה תפתח WhatsApp עם הודעה מוכנה. אם WhatsApp לא זמין, תישלח הודעת SMS לפי ההרשאות שאישרת."
        else ->
            "פעיל — שיחה שלא נענתה תפתח WhatsApp עם הודעה מוכנה."
    }

private fun callMetadataLabel(callType: String?, durationSeconds: Long?): String? {
    val typeLabel = when (callType) {
        "incoming" -> "נכנסת"
        "outgoing" -> "יוצאת"
        "missed" -> "לא נענתה"
        else -> null
    }
    val durationLabel = durationSeconds?.let { "${it.coerceAtLeast(0L)} שניות" }
    return listOfNotNull(typeLabel, durationLabel)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
}

private fun Intent.optionalLongExtra(name: String): Long? =
    if (hasExtra(name)) getLongExtra(name, 0L) else null

private fun startCallDetectionService(context: Context): String? {
    val intent = Intent(context, CallDetectionService::class.java)
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        null
    } catch (_: SecurityException) {
        "לא ניתן להפעיל זיהוי שיחות. בדוק הרשאות מצב טלפון והתראות."
    } catch (_: IllegalStateException) {
        "לא ניתן להפעיל שירות זיהוי שיחות כרגע. מצב ידני עדיין זמין."
    }
}

private fun stopCallDetectionService(context: Context) {
    context.stopService(Intent(context, CallDetectionService::class.java))
}

private fun followUpLogEntry(
    renderedMessage: String,
    actionType: FollowUpActionType
): FollowUpLogEntry = FollowUpLogEntry(
    actionType = actionType,
    timestampEpochMs = System.currentTimeMillis(),
    messagePreview = FollowUpLogStorage.messagePreview(renderedMessage)
)

private fun openWhatsApp(context: Context, link: String): String? {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
    return try {
        context.startActivity(intent)
        null
    } catch (_: ActivityNotFoundException) {
        "לא הצלחנו לפתוח את WhatsApp או דפדפן מתאים. אפשר להשתמש בשיתוף ידני או בהעתקת ההודעה."
    }
}

private data class ShareSheetResult(
    val opened: Boolean,
    val statusMessage: String
)

private fun openShareSheet(context: Context, message: String): ShareSheetResult {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    val chooser = Intent.createChooser(shareIntent, "שתף הודעה")
    try {
        context.startActivity(chooser)
        return ShareSheetResult(
            opened = true,
            statusMessage = "נפתח שיתוף ידני. בחר אפליקציה ושלח ידנית."
        )
    } catch (_: ActivityNotFoundException) {
        return ShareSheetResult(
            opened = false,
            statusMessage = "לא נמצאה אפליקציה שיכולה לשתף את ההודעה."
        )
    }
}

private fun copyMessageToClipboard(context: Context, message: String): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("FollowUp message", message))
    return "ההודעה הועתקה."
}
