package com.followupnadlan.accessibility

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.followupnadlan.setup.BatteryOptimizationIntents

/**
 * "תמיכה ומערכת" (support.html): live health banner, real battery-optimization exemption
 * request, FAQ accordion, WhatsApp support link. The support number is a placeholder until
 * the user gives a real one — see plan v2.
 */
@Composable
internal fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val powerManager = remember(context) { context.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager }
    var batteryExempt by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }

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
                "תמיכה ומערכת",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = AccessibilityColors.Heading,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HealthBanner()

            if (!batteryExempt) {
                BatteryCard(
                    onFix = {
                        val intent = BatteryOptimizationIntents.requestIgnoreOptimizationsIntent(context)
                        if (intent != null) {
                            runCatching { context.startActivity(intent) }
                        }
                        batteryExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                    }
                )
            }

            FaqSection()

            SupportCard()

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HealthBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(AccessibilityColors.Green, Color(0xFF128C7E))))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Column {
            Text("המערכת פועלת כראוי", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text("FollowUp מחוברת לרקע וממתינה", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun BatteryCard(onFix: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFFFFBEB))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(AccessibilityIcons.Battery, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
            Text("חשוב: הגדרת סוללה", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFFB45309))
        }
        Text(
            "כדי שנוכל לשלוח הודעות גם כשהטלפון בכיס, עליך להגדיר לאנדרואיד \"ללא הגבלת סוללה\" עבור האפליקציה.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = Color(0xFF92400E)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF59E0B))
                .clickable(onClick = onFix)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("תקן כעת בהגדרות", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

private data class FaqEntry(val question: String, val answer: String)

private val faqEntries = listOf(
    FaqEntry(
        "למה האפליקציה לא שלחה הודעה?",
        "זה יכול לקרות מ-3 סיבות: המספר חסוי, הלקוח נמצא ב\"רשימה השחורה\", או שהגדרת \"שעות פעילות\" והשיחה התקבלה מחוץ לשעות אלו."
    ),
    FaqEntry(
        "האם הלקוח רואה שזה בוט?",
        "לא. ההודעות נשלחות כהודעת וואטסאפ רגילה לחלוטין מהמספר האישי שלך, ונראות כאילו אתה הקלדת אותן."
    ),
    FaqEntry(
        "האם צריך חיבור לאינטרנט?",
        "לא צריך שרת משלנו — FollowUp פועלת מקומית על המכשיר שלך. שליחה בפועל דרך וואטסאפ עדיין דורשת שהטלפון מחובר לרשת."
    )
)

@Composable
private fun FaqSection() {
    var openIndex by remember { mutableIntStateOf(-1) }
    Column {
        Text("שאלות נפוצות", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AccessibilityColors.Heading)
        Spacer(modifier = Modifier.height(16.dp))
        AppCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                faqEntries.forEachIndexed { index, entry ->
                    FaqItem(
                        entry = entry,
                        open = openIndex == index,
                        showDivider = index != faqEntries.lastIndex,
                        onToggle = { openIndex = if (openIndex == index) -1 else index }
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqItem(entry: FaqEntry, open: Boolean, showDivider: Boolean, onToggle: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                entry.question,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AccessibilityColors.TextStrong,
                modifier = Modifier.weight(1f)
            )
            Icon(
                AccessibilityIcons.ExpandMore,
                contentDescription = null,
                tint = AccessibilityColors.Primary,
                modifier = Modifier.size(20.dp).rotate(if (open) 180f else 0f)
            )
        }
        AnimatedVisibility(visible = open, enter = expandVertically(), exit = shrinkVertically()) {
            Text(
                entry.answer,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = AccessibilityColors.TextMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F0F0)))
        }
    }
}

@Composable
private fun SupportCard() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("צריכים עזרה נוספת?", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AccessibilityColors.Heading)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "הצוות שלנו זמין לעזור לכם להגדיר הכל כמו שצריך.",
                    fontSize = 13.sp,
                    color = AccessibilityColors.TextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1A25D366))
                        .clickable {
                            // Placeholder support number until the user supplies a real one (plan v2).
                            val url = "https://wa.me/972500000000"
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AccessibilityIcons.WhatsApp, contentDescription = null, tint = Color(0xFF1DA851), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("דברו איתנו בוואטסאפ", color = Color(0xFF1DA851), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
