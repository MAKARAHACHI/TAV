package com.followupnadlan.setup

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import com.followupnadlan.notifications.FollowUpNotificationHelper
import com.followupnadlan.postcall.CallDetectionPreferences

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

        return SelfTestSnapshot(
            verdict = result.verdict,
            checks = result.checks.map { it.id to it.state }
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
