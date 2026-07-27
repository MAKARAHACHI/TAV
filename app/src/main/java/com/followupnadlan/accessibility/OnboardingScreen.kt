package com.followupnadlan.accessibility

import android.Manifest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.followupnadlan.profile.MyDetailsStore
import com.followupnadlan.setup.SetupPreferences

/**
 * 4-slide first-run onboarding (onboarding.html): welcome, details, permissions, all-set.
 * Slide 2 writes straight to MyDetailsStore (same source the rest of the app reads); slide 3
 * requests the real call-detection permissions. Finishing sets SetupPreferences' completed
 * flag, so this only ever shows once.
 */
@Composable
internal fun OnboardingScreen(
    myDetailsStore: MyDetailsStore,
    setupPreferences: SetupPreferences,
    onRequestCallPermissions: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var slide by remember { mutableIntStateOf(1) }
    val profile = remember { myDetailsStore.load() }
    var name by remember { mutableStateOf(profile.agentName) }
    var role by remember { mutableStateOf(profile.officeName) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRequestCallPermissions()
    }

    fun finish() {
        myDetailsStore.save(profile.copy(agentName = name.trim(), officeName = role.trim()))
        setupPreferences.setSetupCompleted(true)
        onFinish()
    }

    val backgroundColor = if (slide == 1) AccessibilityColors.Surface else AccessibilityColors.ScreenBackground

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp)) {
            when (slide) {
                1 -> OnboardingWelcomeSlide()
                2 -> OnboardingDetailsSlide(name = name, onNameChange = { name = it }, role = role, onRoleChange = { role = it })
                3 -> OnboardingPermissionsSlide()
                else -> OnboardingAllSetSlide()
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(AccessibilityColors.Surface).padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { i -> OnboardingDot(active = i == slide) }
            }

            val (label, background) = when (slide) {
                2 -> "המשך לשלב הבא" to AccessibilityColors.Primary
                3 -> "אשר גישה (חובה)" to AccessibilityColors.Heading
                4 -> "התחל לעבוד" to AccessibilityColors.Primary
                else -> "המשך" to AccessibilityColors.Primary
            }
            PillButton(
                text = label,
                background = background,
                onClick = {
                    when (slide) {
                        3 -> {
                            callPermissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)
                            )
                            slide = 4
                        }
                        4 -> finish()
                        else -> slide += 1
                    }
                }
            )

            if (slide == 3) {
                Text(
                    "אשר מאוחר יותר",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccessibilityColors.TextMuted,
                    modifier = Modifier.clickable { slide = 4 }
                )
            }
        }
    }
}

@Composable
private fun OnboardingWelcomeSlide() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("✨", fontSize = 72.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 40.dp)) {
            Text(
                "ברוכים הבאים\nל-FollowUp",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                color = AccessibilityColors.Heading
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "העוזר האישי שלך לניהול לקוחות.\nמעכשיו, המערכת תדאג שאף לקוח לא ירגיש ששכחו אותו.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = AccessibilityColors.TextMuted
            )
        }
    }
}

@Composable
private fun OnboardingDetailsSlide(name: String, onNameChange: (String) -> Unit, role: String, onRoleChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("📝", fontSize = 48.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("בואו נכיר", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AccessibilityColors.Heading)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "הפרטים האלו ישמשו ליצירת כרטיס הביקור הדיגיטלי שיישלח ללקוחות שלך.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = AccessibilityColors.TextMuted
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        OnboardingLabeledField(label = "איך קוראים לך?", value = name, placeholder = "לדוגמה: דני לוי", onChange = onNameChange)
        Spacer(modifier = Modifier.height(16.dp))
        OnboardingLabeledField(label = "מה התפקיד או העסק שלך?", value = role, placeholder = "לדוגמה: עו״ד מקרקעין", onChange = onRoleChange)
    }
}

@Composable
private fun OnboardingLabeledField(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccessibilityColors.TextMuted)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = AccessibilityColors.TextFaint) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun OnboardingPermissionsSlide() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("🛡️", fontSize = 48.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("כדי שזה יעבוד קסם...", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = AccessibilityColors.Heading)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "אנחנו צריכים את אישורך לזהות שיחות. הכל פועל אופליין על המכשיר שלך, בלי שרתים ובלי מעקב.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = AccessibilityColors.TextMuted
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        OnboardingPermBox(emoji = "📞", title = "זיהוי שיחות נכנסות", description = "כדי לדעת מתי שיחה לא נענתה או הסתיימה.")
        Spacer(modifier = Modifier.height(12.dp))
        OnboardingPermBox(emoji = "💬", title = "שליחת הודעות", description = "כדי לשלוח את הודעת ההמשך בשמך.")
    }
}

@Composable
private fun OnboardingPermBox(emoji: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AccessibilityColors.SubtleSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(AccessibilityColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 19.sp)
        }
        Column {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
            Text(description, fontSize = 12.sp, color = AccessibilityColors.TextMuted)
        }
    }
}

@Composable
private fun OnboardingAllSetSlide() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("🎉", fontSize = 72.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 40.dp)) {
            Text("הכל מוכן!", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AccessibilityColors.Heading)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "המערכת מוגדרת ועובדת ברקע.\nאתה יכול להמשיך בשגרת היום שלך, אנחנו נדאג ללקוחות שמתקשרים.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = AccessibilityColors.TextMuted
            )
        }
    }
}

@Composable
private fun OnboardingDot(active: Boolean) {
    Box(
        modifier = Modifier
            .height(8.dp)
            .width(if (active) 24.dp else 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) AccessibilityColors.Primary else Color(0xFFE1E5E8))
    )
}
