package com.followupnadlan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.followupnadlan.accessibility.AccessibilityApp
import com.followupnadlan.accessibility.AccessibilityTheme
import com.followupnadlan.accessibility.MissedCallLaunch
import com.followupnadlan.notifications.FollowUpNotificationHelper

/**
 * Host for the native Compose accessibility UI (missed-call text bridge).
 * All business CRM screens were removed; the underlying stores, missed-call detection,
 * WhatsApp-first / SMS-fallback decision, cooldown, templates and logs are unchanged.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launch = missedCallLaunchFromIntent(intent)
        setContent {
            AccessibilityTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AccessibilityApp(missedCallLaunch = launch)
                    }
                }
            }
        }
    }

    private fun missedCallLaunchFromIntent(intent: Intent?): MissedCallLaunch {
        if (intent?.action != FollowUpNotificationHelper.ACTION_OPEN_FOLLOW_UP) {
            return MissedCallLaunch()
        }
        return MissedCallLaunch(
            phone = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_PHONE).orEmpty(),
            message = "",
            fromNotification = true
        )
    }
}
