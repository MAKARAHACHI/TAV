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
import com.followupnadlan.notifications.FollowUpNotificationHelper
import com.followupnadlan.notifications.ReminderNotificationHelper
import com.followupnadlan.postcall.CallDetectionPreferences
import com.followupnadlan.postcall.CallDetectionService
import com.followupnadlan.postcall.PostCallCard
import com.followupnadlan.postcall.PostCallCards
import com.followupnadlan.profile.MyDetailsProfile
import com.followupnadlan.profile.MyDetailsStore
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
    MessageTemplates
}

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
    val scope = rememberCoroutineScope()
    val callDetectionPreferences = remember(context) { CallDetectionPreferences(context.applicationContext) }
    var currentScreen by remember {
        mutableStateOf(if (initialLaunchState.openedFromNotification) AppScreen.PostCallDecision else AppScreen.ManualComposer)
    }
    var templateRevision by remember { mutableStateOf(0) }
    var manualPhone by remember { mutableStateOf(initialLaunchState.phone) }
    var manualLeadName by remember { mutableStateOf(initialLaunchState.leadName) }
    var manualTemplateId by remember { mutableStateOf(initialLaunchState.templateId) }
    var postCallDurationSeconds by remember { mutableStateOf(initialLaunchState.callDurationSeconds) }
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
    var pendingNotificationLaunch by remember { mutableStateOf<FollowUpLaunchState?>(null) }
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
        restoredTaskId = task.id
        currentScreen = AppScreen.ManualComposer
        notificationStatus = "תזכורת נפתחה. הכרטיס שוחזר לעריכה."
        database.followUpTaskDao().update(
            task.copy(
                status = FOLLOW_UP_STATUS_OPENED,
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
                    onToggleCallDetection = toggleCallDetection
                )
                AppScreen.PostCallDecision -> PostCallScreen(
                    phone = manualPhone,
                    leadName = manualLeadName,
                    callDurationSeconds = postCallDurationSeconds,
                    callType = postCallType,
                    selectionStatus = postCallSelectionStatus,
                    onCardSelected = { card ->
                        manualTemplateId = card.composerHint.templateId
                        manualMessageOverride = card.composerHint.initialMessage
                        manualMessageRevision += 1
                        postCallSelectionStatus = null
                        notificationStatus = "נבחר כרטיס: ${card.title}. ההודעה הוכנה לעריכה ידנית."
                        currentScreen = AppScreen.ManualComposer
                    }
                )
                AppScreen.MyDetails -> MyDetailsScreen(myDetailsStore)
                AppScreen.MessageTemplates -> TemplateManagementScreen(
                    templateStore = templateStore,
                    myDetailsStore = myDetailsStore,
                    onTemplatesChanged = { templateRevision += 1 }
                )
            }
        }
    }
}

@Composable
private fun PostCallScreen(
    phone: String,
    leadName: String,
    callDurationSeconds: Long?,
    callType: String?,
    selectionStatus: String?,
    onCardSelected: (PostCallCard) -> Unit
) {
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
                onClick = { onCardSelected(card) },
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
    onToggleCallDetection: () -> Unit
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
            source = FOLLOW_UP_SOURCE_MANUAL_COMPOSER,
            selectedTemplateId = selectedTemplate.id,
            draftText = message,
            leadType = null,
            propertyLink = activePropertyLink(myDetailsProfile),
            reminderAtEpochMs = reminderAt,
            status = FOLLOW_UP_STATUS_SNOOZED,
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
                    type = LEAD_TYPE_UNKNOWN,
                    status = LEAD_STATUS_NEW,
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
                            status = FOLLOW_UP_STATUS_SAVED_AS_LEAD,
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
                    val resultMessage = openWhatsApp(context, WhatsAppLinkBuilder.build(currentPhone, renderedMessage))
                    statusMessage = resultMessage
                    if (resultMessage == null) {
                        followUpLogStore.append(
                            followUpLogEntry(
                                renderedMessage = renderedMessage,
                                actionType = FollowUpActionType.WHATSAPP_OPENED
                            )
                        )
                        restoredTaskId?.let { taskId ->
                            scope.launch {
                                followUpTaskDao.getById(taskId)?.let { task ->
                                    followUpTaskDao.update(
                                        task.copy(
                                            status = FOLLOW_UP_STATUS_WHATSAPP_OPENED,
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

private const val FOLLOW_UP_SOURCE_MANUAL_COMPOSER = "MANUAL_COMPOSER"
private const val FOLLOW_UP_STATUS_SNOOZED = "SNOOZED"
private const val FOLLOW_UP_STATUS_OPENED = "OPENED"
private const val FOLLOW_UP_STATUS_WHATSAPP_OPENED = "WHATSAPP_OPENED"
private const val FOLLOW_UP_STATUS_SAVED_AS_LEAD = "SAVED_AS_LEAD"
private const val LEAD_TYPE_UNKNOWN = "UNKNOWN"
private const val LEAD_STATUS_NEW = "NEW"

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
