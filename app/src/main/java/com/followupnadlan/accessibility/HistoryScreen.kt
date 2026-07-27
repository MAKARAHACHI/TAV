package com.followupnadlan.accessibility

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import com.followupnadlan.whatsapp.WhatsAppLinkBuilder

private enum class HistoryTab { ALL, MISSED }

/**
 * "יומן פעילות" (history.html): segmented tabs, date-grouped activity cards. Reuses the
 * existing FollowUpLogStore as its data source — no new storage, the log entries already
 * written on every send/skip are the minimal DB the design calls for.
 * ✓✓ delivery ticks are drawn static (WhatsApp gives the app no read-receipt access).
 */
@Composable
internal fun HistoryScreen(logStore: FollowUpLogStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val nameByDigits = remember(context) {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ContactsPickerRepository(context.applicationContext).load()
                .associate { it.phone.filter(Char::isDigit).takeLast(9) to it.name }
        } else {
            emptyMap()
        }
    }
    val allRows = remember(context) {
        HistoryFeed.rows(logStore.load()) { phone ->
            nameByDigits[phone.filter(Char::isDigit).takeLast(9)]
        }
    }
    var tab by remember { mutableStateOf(HistoryTab.ALL) }
    val missedCount = allRows.count { it.moment == HistoryMoment.MISSED }
    val visibleRows = if (tab == HistoryTab.MISSED) allRows.filter { it.moment == HistoryMoment.MISSED } else allRows
    val grouped = visibleRows.groupBy { it.dateGroup }.toList()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                AccessibilityIcons.ChevronStart,
                contentDescription = "חזור",
                tint = AccessibilityColors.IconDark,
                modifier = Modifier.size(26.dp).clickable(onClick = onBack)
            )
            Text(
                "יומן פעילות",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = AccessibilityColors.Heading,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AccessibilityColors.FieldGrey)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HistorySegment(
                label = "הכל (${allRows.size})",
                selected = tab == HistoryTab.ALL,
                modifier = Modifier.weight(1f),
                onClick = { tab = HistoryTab.ALL }
            )
            HistorySegment(
                label = "לא עניתי ($missedCount)",
                selected = tab == HistoryTab.MISSED,
                modifier = Modifier.weight(1f),
                onClick = { tab = HistoryTab.MISSED }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (visibleRows.isEmpty()) {
            Text(
                "אין עוד פעילויות להציג",
                fontSize = 13.sp,
                color = AccessibilityColors.TextFaint,
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (dateLabel, rows) ->
                    HistoryDateHeader(dateLabel)
                    rows.forEach { row -> HistoryActivityCard(row) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "אין עוד פעילויות להציג",
                    fontSize = 13.sp,
                    color = AccessibilityColors.TextFaint,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HistorySegment(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) AccessibilityColors.Surface else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (selected) AccessibilityColors.TextStrong else AccessibilityColors.TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun HistoryDateHeader(label: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = AccessibilityColors.TextFaint,
            modifier = Modifier
                .background(AccessibilityColors.ScreenBackground)
                .padding(horizontal = 10.dp)
        )
    }
}

@Composable
private fun HistoryActivityCard(row: HistoryRow) {
    val context = LocalContext.current
    val tagColor = if (row.moment == HistoryMoment.MISSED) AccessibilityColors.Primary else AccessibilityColors.Green

    AppCard(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), cornerRadius = 20) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).fillMaxSize().background(tagColor))
            Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircleAvatar(
                            background = if (row.contactName != null) AccessibilityColors.PrimaryContainer else AccessibilityColors.FieldGrey,
                            initial = row.contactName?.take(2),
                            icon = if (row.contactName == null) AccessibilityIcons.Person else null
                        )
                        Column {
                            Text(
                                row.contactName ?: row.displayPhone,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = AccessibilityColors.TextStrong
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    if (row.moment == HistoryMoment.MISSED) AccessibilityIcons.PhoneMissed else AccessibilityIcons.PhoneInTalk,
                                    contentDescription = null,
                                    tint = AccessibilityColors.TextFaint,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    if (row.moment == HistoryMoment.MISSED) "שיחה שלא נענתה" else "סיום שיחה",
                                    fontSize = 12.sp,
                                    color = AccessibilityColors.TextFaint
                                )
                            }
                        }
                    }
                    Text(row.time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AccessibilityColors.TextFaint)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccessibilityColors.SubtleSurface)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(row.summary, fontSize = 13.sp, color = AccessibilityColors.TextStrong)
                    // ✓✓ static — WhatsApp gives no read-receipt access; see plan v2 §"3 not-wired".
                    Text("✓✓", fontSize = 13.sp, color = AccessibilityColors.WaCheck, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HistoryActionButton(
                        label = "וואטסאפ",
                        icon = AccessibilityIcons.WhatsApp,
                        background = Color(0x1A25D366),
                        contentColor = Color(0xFF1DA851),
                        modifier = Modifier.weight(1f)
                    ) {
                        val normalized = PhoneNumberNormalizer.normalizeForWhatsApp(row.phone) ?: return@HistoryActionButton
                        val url = WhatsAppLinkBuilder.build(normalized, "")
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                    HistoryActionButton(
                        label = if (row.moment == HistoryMoment.MISSED) "חזור אליו" else "חייג",
                        icon = AccessibilityIcons.Call,
                        background = AccessibilityColors.FieldGrey,
                        contentColor = AccessibilityColors.TextStrong,
                        modifier = Modifier.weight(1f)
                    ) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${row.phone}")))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = contentColor)
    }
}
