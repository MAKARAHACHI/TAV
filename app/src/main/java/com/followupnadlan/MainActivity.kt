package com.followupnadlan

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.followupnadlan.profile.MyDetailsProfile
import com.followupnadlan.profile.MyDetailsStore
import com.followupnadlan.templates.MessageTemplate
import com.followupnadlan.templates.SprintOneTemplates
import com.followupnadlan.templates.TemplateTagRenderer
import com.followupnadlan.templates.TemplateTagValues
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import com.followupnadlan.whatsapp.WhatsAppLinkBuilder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        FollowUpApp()
                    }
                }
            }
        }
    }
}

private enum class AppScreen {
    ManualComposer,
    MyDetails
}

@Composable
private fun FollowUpApp() {
    val context = LocalContext.current
    val myDetailsStore = remember(context) { MyDetailsStore(context.applicationContext) }
    var currentScreen by remember { mutableStateOf(AppScreen.ManualComposer) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp),
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
                onClick = { currentScreen = AppScreen.MyDetails },
                modifier = Modifier.weight(1f),
                enabled = currentScreen != AppScreen.MyDetails
            ) {
                Text("הפרטים שלי")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                AppScreen.ManualComposer -> ManualWhatsAppScreen(myDetailsStore)
                AppScreen.MyDetails -> MyDetailsScreen(myDetailsStore)
            }
        }
    }
}

@Composable
private fun ManualWhatsAppScreen(myDetailsStore: MyDetailsStore) {
    val context = LocalContext.current
    val templates = remember { SprintOneTemplates.all }
    val myDetailsProfile = remember(myDetailsStore) { myDetailsStore.load() }
    var selectedTemplate by remember { mutableStateOf(templates.first()) }
    var phone by remember { mutableStateOf("") }
    var leadName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf(defaultMessageFor(selectedTemplate)) }
    var templateMenuOpen by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
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

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it
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
                leadName = it
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
                    statusMessage = openWhatsApp(context, WhatsAppLinkBuilder.build(currentPhone, renderedMessage))
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
                    statusMessage = openShareSheet(context, renderedMessage)
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

        statusMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(12.dp))
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

private fun openWhatsApp(context: Context, link: String): String? {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
    return try {
        context.startActivity(intent)
        null
    } catch (_: ActivityNotFoundException) {
        "לא הצלחנו לפתוח את WhatsApp או דפדפן מתאים. אפשר להשתמש בשיתוף ידני או בהעתקת ההודעה."
    }
}

private fun openShareSheet(context: Context, message: String): String? {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    val chooser = Intent.createChooser(shareIntent, "שתף הודעה")
    return try {
        context.startActivity(chooser)
        "נפתח שיתוף ידני. בחר אפליקציה ושלח ידנית."
    } catch (_: ActivityNotFoundException) {
        "לא נמצאה אפליקציה שיכולה לשתף את ההודעה."
    }
}

private fun copyMessageToClipboard(context: Context, message: String): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("FollowUp message", message))
    return "ההודעה הועתקה."
}
