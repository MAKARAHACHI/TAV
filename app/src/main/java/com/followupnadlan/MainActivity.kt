package com.followupnadlan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.followuplog.FollowUpLogStorage
import com.followupnadlan.followuplog.FollowUpLogStore
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.notifications.FollowUpNotificationHelper
import com.followupnadlan.postcall.CallDetectionPreferences
import com.followupnadlan.postcall.CallDetectionService
import com.followupnadlan.postcall.CallDetectionServiceAction
import com.followupnadlan.postcall.CallDetectionServiceLifecycle

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

    /**
     * Every foreground visit re-raises the detection service, not just a cold start.
     *
     * The service is what turns an ended conversation into a follow-up suggestion, and it is the
     * only path that does — [com.followupnadlan.postcall.PhoneStateReceiver] handles missed calls
     * on its own but has nothing for ended ones. That receiver also cannot revive the service:
     * starting a foreground service from a background broadcast is forbidden on Android 12+, so
     * its start() call fails silently, and a call ending is exactly when nothing else is running.
     * Doing this from a resumed Activity is permitted, so the app coming to the foreground is the
     * one moment we can reliably repair a service the OEM battery manager killed.
     *
     * onCreate alone was not enough: returning to a still-created Activity skips it entirely.
     */
    override fun onResume() {
        super.onResume()
        syncCallDetectionService()
    }

    private fun syncCallDetectionService() {
        val settings = MissedCallAutoResponseSettings(applicationContext)
        val preferences = CallDetectionPreferences(applicationContext)
        val bridgeEnabled = settings.isEnabled
        preferences.setEnabled(bridgeEnabled)
        val action = CallDetectionServiceLifecycle.actionFor(
            bridgeEnabled = bridgeEnabled,
            readPhoneStateGranted = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED,
            readCallLogGranted = checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
        runCatching {
            when (action) {
                CallDetectionServiceAction.START -> CallDetectionService.start(applicationContext)
                CallDetectionServiceAction.STOP -> CallDetectionService.stop(applicationContext)
            }
        }
    }

    private fun missedCallLaunchFromIntent(intent: Intent?): MissedCallLaunch {
        if (intent?.action != FollowUpNotificationHelper.ACTION_OPEN_FOLLOW_UP) {
            return MissedCallLaunch()
        }
        if (intent.getStringExtra(FollowUpNotificationHelper.EXTRA_MANUAL_ACTION) == FollowUpNotificationHelper.MANUAL_ACTION_CANCEL) {
            val message = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_MESSAGE).orEmpty()
            FollowUpLogStore(applicationContext).append(
                FollowUpLogEntry(
                    actionType = FollowUpActionType.MANUAL_REPLY_CANCELLED,
                    timestampEpochMs = System.currentTimeMillis(),
                    messagePreview = FollowUpLogStorage.messagePreview(message),
                    phone = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_PHONE).orEmpty(),
                    source = MissedCallAutoResponseSettings.SOURCE
                )
            )
            return MissedCallLaunch()
        }
        return MissedCallLaunch(
            phone = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_PHONE).orEmpty(),
            message = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_MESSAGE).orEmpty(),
            fromNotification = true,
            callType = intent.getStringExtra(FollowUpNotificationHelper.EXTRA_CALL_TYPE)
        )
    }
}
