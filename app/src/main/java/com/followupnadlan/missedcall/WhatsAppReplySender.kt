package com.followupnadlan.missedcall

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.followupnadlan.whatsapp.WhatsAppLinkBuilder

enum class WhatsAppReplyOpenResult {
    OPENED,
    FAILED
}

class WhatsAppReplySender(private val context: Context) {
    fun openPreparedReply(
        normalizedPhone: String,
        message: String,
        packageName: String?
    ): WhatsAppReplyOpenResult {
        if (normalizedPhone.isBlank() || message.isBlank()) {
            return WhatsAppReplyOpenResult.FAILED
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WhatsAppLinkBuilder.build(normalizedPhone, message))).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            packageName?.let(::setPackage)
        }

        return try {
            context.startActivity(intent)
            WhatsAppReplyOpenResult.OPENED
        } catch (_: ActivityNotFoundException) {
            WhatsAppReplyOpenResult.FAILED
        } catch (_: SecurityException) {
            WhatsAppReplyOpenResult.FAILED
        } catch (_: RuntimeException) {
            WhatsAppReplyOpenResult.FAILED
        }
    }
}
