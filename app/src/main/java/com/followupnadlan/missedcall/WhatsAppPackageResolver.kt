package com.followupnadlan.missedcall

import android.content.Context
import android.content.pm.PackageManager

data class WhatsAppPackageAvailability(
    val messengerInstalled: Boolean,
    val businessInstalled: Boolean,
    val selectedPackage: String?
) {
    val anyInstalled: Boolean = messengerInstalled || businessInstalled
}

class WhatsAppPackageResolver(private val context: Context) {
    fun resolve(preferredPackage: String): WhatsAppPackageAvailability {
        val messengerInstalled = isInstalled(WHATSAPP_MESSENGER_PACKAGE)
        val businessInstalled = isInstalled(WHATSAPP_BUSINESS_PACKAGE)
        val selectedPackage = when {
            preferredPackage == WHATSAPP_BUSINESS_PACKAGE && businessInstalled -> WHATSAPP_BUSINESS_PACKAGE
            preferredPackage == WHATSAPP_MESSENGER_PACKAGE && messengerInstalled -> WHATSAPP_MESSENGER_PACKAGE
            businessInstalled -> WHATSAPP_BUSINESS_PACKAGE
            messengerInstalled -> WHATSAPP_MESSENGER_PACKAGE
            else -> null
        }

        return WhatsAppPackageAvailability(
            messengerInstalled = messengerInstalled,
            businessInstalled = businessInstalled,
            selectedPackage = selectedPackage
        )
    }

    private fun isInstalled(packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    companion object {
        const val WHATSAPP_MESSENGER_PACKAGE = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    }
}
