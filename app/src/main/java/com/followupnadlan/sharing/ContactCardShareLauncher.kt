package com.followupnadlan.sharing

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.followupnadlan.missedcall.WhatsAppPackageResolver

/**
 * Builds and fires an `ACTION_SEND` for the vCard. Targets the user's chosen WhatsApp
 * (regular / Business) when installed via the existing [WhatsAppPackageResolver]; otherwise falls
 * back to the system chooser. It never targets a specific chat (no `jid` / `wa.me`) — the user
 * picks the conversation and taps send themselves. Grants read-only access to the file only.
 */
class ContactCardShareLauncher(private val context: Context) {

    sealed interface LaunchResult {
        data class Opened(val targetOpening: TargetOpening) : LaunchResult
        /** No installed app can receive a `text/x-vcard` share. */
        data object NoReceiver : LaunchResult
    }

    fun launch(uri: Uri, preferredPackage: String): LaunchResult {
        val target = WhatsAppPackageResolver(context.applicationContext)
            .resolve(preferredPackage)
            .selectedPackage

        // Direct to the chosen WhatsApp when it is installed and can take the vCard.
        if (target != null && start(sendIntent(uri).setPackage(target))) {
            return LaunchResult.Opened(TargetOpening.DIRECT)
        }
        // Fallback: let the user pick any app that accepts a contact card.
        val chooser = Intent.createChooser(sendIntent(uri), CHOOSER_TITLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (start(chooser)) {
            return LaunchResult.Opened(TargetOpening.FALLBACK_CHOOSER)
        }
        return LaunchResult.NoReceiver
    }

    private fun sendIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        // clipData carries the read grant through some chooser / target routes reliably.
        clipData = ClipData.newRawUri("contact.vcf", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun start(intent: Intent): Boolean {
        // A chooser / target may run outside an Activity task, so start a fresh one when needed.
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private companion object {
        const val MIME_TYPE = "text/x-vcard"
        const val CHOOSER_TITLE = "שיתוף כרטיס איש קשר"
    }
}
