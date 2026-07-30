package com.followupnadlan.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen message editor matching edit.html ("אם לא עניתי") / edit-after-call.html
 * ("אחרי שדיברנו"): nav-bar with a real save action, a live chat-bubble preview that
 * updates as the user types, the textarea, and the moment's own toggles.
 *
 * The signature line stays locked (rendered by SignatureLine, not part of the editable body) —
 * see the ended/missed journey docs for why: it is identity, changed in one place (the profile
 * card), not wording. The preview shows the body exactly as it will be sent (§2) — there is no
 * tag substitution, so no bracket-tag insert buttons.
 */
@Composable
internal fun MessageEditorScreen(
    isEnded: Boolean,
    body: String,
    cardAttached: Boolean,
    cardText: String = "",
    titleOverride: String? = null,
    showCardToggle: Boolean = true,
    onToggleCardAttached: (() -> Unit)?,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var draft by remember(body) { mutableStateOf(body) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AccessibilityColors.Surface)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                AccessibilityIcons.ChevronStart,
                contentDescription = "חזור",
                tint = AccessibilityColors.Primary,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack)
            )
            Text(
                titleOverride ?: if (isEnded) "🤝 אחרי שדיברנו" else "אם לא עניתי",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = AccessibilityColors.Heading
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccessibilityColors.Primary)
                    .clickable { onSave(draft) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text("שמירה", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AccessibilityColors.ScreenBackground)
                .verticalScroll(rememberScrollState())
        ) {
            // Live preview zone (chat-preview-zone).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color(0xFFEFEAE2))
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    if (isEnded) "כך תיראה ההודעה בסיום השיחה:" else "ככה הלקוח יראה את ההודעה:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccessibilityColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                    color = AccessibilityExtra.colors.bubbleGreen,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            draft.ifBlank { " " },
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = AccessibilityColors.TextStrong
                        )
                        // WAVE G: card OFF ⇒ body only, no signature at all anymore — the standalone
                        // signature is never drawn here, matching the send path (§2).
                        val cardShown = isEnded && cardAttached && cardText.isNotBlank()
                        // Formatted TEXT business card inside the bubble — shown/hidden live by the
                        // card toggle. It is the exact text that is also sent (§2), rendered with
                        // WhatsApp *bold* / link / italic styling.
                        if (cardShown) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ContactTextCardBubble(raw = cardText)
                        }
                        // No fabricated timestamp / read-receipt: the app has no WhatsApp receipt
                        // access, so a blue ✓✓ + made-up clock time would falsely read as
                        // "delivered & read" (§2 — honesty over chrome).
                    }
                }
            }

            // Editor form.
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                AppCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
                    Column(modifier = Modifier.padding(20.dp).imePadding()) {
                        Text("תוכן ההודעה", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AccessibilityColors.Heading)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp)
                        )
                    }
                }

                if (showCardToggle) {
                    AppCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            // Honest wording, matching the moment-edit "צירוף כרטיס ביקור" toggle
                            // (AccessibilityApp.kt): a formatted TEXT business card added to the
                            // message — NOT a .vcf file (WhatsApp blocks file-share to unsaved
                            // numbers), so no "(vCard)" and no "saves a contact" promise (§2).
                            EditorToggleRow(
                                emoji = "📇",
                                title = "צירוף כרטיס ביקור",
                                description = "הוסף כרטיס איש קשר לשמירה מהירה אצל הלקוח",
                                checked = cardAttached,
                                onToggle = onToggleCardAttached
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorToggleRow(
    emoji: String,
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccessibilityColors.FieldGrey),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 15.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
            Text(description, fontSize = 12.sp, lineHeight = 17.sp, color = AccessibilityColors.TextMuted)
        }
        Switch(checked = checked, onCheckedChange = { onToggle?.invoke() })
    }
}
