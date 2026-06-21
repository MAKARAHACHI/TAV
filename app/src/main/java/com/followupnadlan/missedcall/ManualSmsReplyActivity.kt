package com.followupnadlan.missedcall

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.followuplog.FollowUpLogStorage
import com.followupnadlan.followuplog.FollowUpLogStore

class ManualSmsReplyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val phone = intent.getStringExtra(EXTRA_PHONE).orEmpty()
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        if (phone.isBlank() || message.isBlank()) {
            finish()
            return
        }

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", message)
        }

        try {
            startActivity(smsIntent)
            FollowUpLogStore(applicationContext).append(
                FollowUpLogEntry(
                    actionType = FollowUpActionType.FALLBACK_SMS_OPENED,
                    timestampEpochMs = System.currentTimeMillis(),
                    messagePreview = FollowUpLogStorage.messagePreview(message),
                    phone = phone,
                    source = MissedCallAutoResponseSettings.SOURCE
                )
            )
        } catch (_: ActivityNotFoundException) {
            // No truthful fallback-opened log: no composer opened.
        } finally {
            finish()
        }
    }

    companion object {
        const val EXTRA_PHONE = "manual_sms_reply_phone"
        const val EXTRA_MESSAGE = "manual_sms_reply_message"
    }
}
