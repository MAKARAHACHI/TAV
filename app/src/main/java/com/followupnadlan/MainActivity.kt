package com.followupnadlan

import android.content.ActivityNotFoundException
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
import com.followupnadlan.templates.MessageTemplate
import com.followupnadlan.templates.SprintOneTemplates
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import com.followupnadlan.whatsapp.WhatsAppLinkBuilder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ManualWhatsAppScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualWhatsAppScreen() {
    val context = LocalContext.current
    val templates = remember { SprintOneTemplates.all }
    var selectedTemplate by remember { mutableStateOf(templates.first()) }
    var phone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf(defaultMessageFor(selectedTemplate)) }
    var templateMenuOpen by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val normalizedPhone = PhoneNumberNormalizer.normalizeForWhatsApp(phone)
    val whatsappLink = normalizedPhone?.let { WhatsAppLinkBuilder.build(it, message) }.orEmpty()

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
            label = { Text("מספר טלפון") },
            placeholder = { Text("050-1234567") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
            modifier = Modifier.fillMaxWidth()
        )

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
                    if (normalizedPhone == null) {
                        statusMessage = "יש להזין מספר טלפון תקין."
                        return@Button
                    }
                    if (message.isBlank()) {
                        statusMessage = "יש לכתוב הודעה לפני פתיחת WhatsApp."
                        return@Button
                    }
                    statusMessage = openWhatsApp(context, WhatsAppLinkBuilder.build(normalizedPhone, message))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("פתח WhatsApp")
            }
            OutlinedButton(
                onClick = {
                    message = defaultMessageFor(selectedTemplate)
                    statusMessage = null
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

private fun defaultMessageFor(template: MessageTemplate): String = template.body

private fun openWhatsApp(context: Context, link: String): String? {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
    return try {
        context.startActivity(intent)
        null
    } catch (_: ActivityNotFoundException) {
        "לא הצלחנו לפתוח את WhatsApp או דפדפן מתאים."
    }
}
