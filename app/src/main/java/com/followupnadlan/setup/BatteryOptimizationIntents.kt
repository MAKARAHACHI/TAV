package com.followupnadlan.setup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object BatteryOptimizationIntents {
    fun appSettingsIntent(context: Context): Intent? = try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) == null) {
            null
        } else {
            intent
        }
    } catch (_: RuntimeException) {
        null
    }

    /**
     * Direct "exempt this app" prompt (skips the settings list). Falls back to
     * [appSettingsIntent] on OEMs that don't resolve it (some MIUI/EMUI builds).
     */
    @Suppress("BatteryLife")
    fun requestIgnoreOptimizationsIntent(context: Context): Intent? = try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            appSettingsIntent(context)
        } else {
            intent
        }
    } catch (_: RuntimeException) {
        appSettingsIntent(context)
    }
}
