package com.followupnadlan.setup

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.missedcall.MissedCallCooldownStore
import com.followupnadlan.missedcall.MissedCallResponsePrimaryChannel
import com.followupnadlan.missedcall.MissedCallWhatsAppMode
import com.followupnadlan.missedcall.WhatsAppAutoSendController
import com.followupnadlan.missedcall.WhatsAppPackageResolver
import com.followupnadlan.notifications.FollowUpNotificationHelper
import com.followupnadlan.postcall.CallDetectionPreferences
import com.followupnadlan.templates.TemplateStore

data class SelfTestSnapshot(
    val verdict: ReadinessVerdict,
    val checks: List<Pair<CheckId, CheckState>>
) {
    fun state(id: CheckId): CheckState =
        checks.first { it.first == id }.second
}

class SelfTestChecker(private val context: Context) {
    fun run(): SelfTestSnapshot {
        val batteryOptimizationState = batteryOptimizationState()
        val result = SetupReadinessLogic.evaluate(
            SetupReadinessInput(
                notificationsGranted = notificationsGranted(),
                phoneStateGranted = permissionGranted(Manifest.permission.READ_PHONE_STATE),
                callLogGranted = permissionGranted(Manifest.permission.READ_CALL_LOG),
                contactsGranted = permissionGranted(Manifest.permission.READ_CONTACTS),
                detectionEnabled = CallDetectionPreferences(context).isEnabled(),
                notificationChannelEnabled = notificationChannelEnabled(),
                batteryOptimizationKnown = batteryOptimizationState != null,
                batteryOptimizationAllowsBackground = batteryOptimizationState == true
            )
        )

        val missedCallSettings = MissedCallAutoResponseSettings(context)
        val whatsappPackages = WhatsAppPackageResolver(context).resolve(missedCallSettings.preferredWhatsAppPackage)
        val accessibilityEnabled = WhatsAppAutoSendController(context).isAccessibilityServiceEnabled()
        val missedCallChecks = listOf(
            CheckId.MISSED_CALL_AUTO_RESPONSE_ENABLED to if (missedCallSettings.isEnabled) {
                CheckState.PASS
            } else {
                CheckState.OPTIONAL_MISSING
            },
            CheckId.WHATSAPP_INSTALLED to if (
                missedCallSettings.primaryChannel == MissedCallResponsePrimaryChannel.WHATSAPP_FIRST &&
                !whatsappPackages.anyInstalled
            ) {
                CheckState.FAIL
            } else {
                CheckState.PASS
            },
            CheckId.WHATSAPP_ACCESSIBILITY_SERVICE to if (
                missedCallSettings.primaryChannel == MissedCallResponsePrimaryChannel.WHATSAPP_FIRST &&
                missedCallSettings.whatsappMode == MissedCallWhatsAppMode.ACCESSIBILITY_AUTO &&
                !accessibilityEnabled
            ) {
                CheckState.FAIL
            } else {
                CheckState.PASS
            },
            CheckId.SMS_PERMISSION to if (permissionGranted(Manifest.permission.SEND_SMS)) {
                CheckState.PASS
            } else if (missedCallSettings.isEnabled && missedCallSettings.primaryChannel == MissedCallResponsePrimaryChannel.SMS_ONLY) {
                CheckState.FAIL
            } else {
                CheckState.OPTIONAL_MISSING
            },
            CheckId.MISSED_CALL_TEMPLATE to if (TemplateStore(context).loadTemplates().any { it.id == missedCallSettings.selectedTemplateId }) {
                CheckState.PASS
            } else {
                CheckState.FAIL
            },
            CheckId.MISSED_CALL_COOLDOWN_STORE to if (MissedCallCooldownStore(context).isAccessible()) {
                CheckState.PASS
            } else {
                CheckState.FAIL
            }
        )

        return SelfTestSnapshot(
            verdict = result.verdict,
            checks = result.checks.map { it.id to it.state } + missedCallChecks
        )
    }

    private fun notificationsGranted(): Boolean {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val appNotificationsEnabled = notificationManager.areNotificationsEnabled()
        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            permissionGranted(Manifest.permission.POST_NOTIFICATIONS)
        return appNotificationsEnabled && runtimePermissionGranted
    }

    private fun notificationChannelEnabled(): Boolean {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = notificationManager.getNotificationChannel(FollowUpNotificationHelper.CHANNEL_ID)
        return channel?.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun permissionGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun batteryOptimizationState(): Boolean? =
        try {
            val powerManager = context.getSystemService(PowerManager::class.java)
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: RuntimeException) {
            null
        } catch (_: SecurityException) {
            null
        }
}
