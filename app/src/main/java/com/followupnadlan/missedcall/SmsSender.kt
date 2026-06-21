package com.followupnadlan.missedcall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager

enum class SmsSendResult {
    ATTEMPTED,
    NO_PERMISSION,
    FAILED
}

class SmsSender(private val context: Context) {
    fun send(phoneNumber: String, message: String): SmsSendResult {
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return SmsSendResult.NO_PERMISSION
        }
        if (phoneNumber.isBlank() || message.isBlank()) {
            return SmsSendResult.FAILED
        }

        return try {
            val smsManager = smsManager()
            val parts = smsManager.divideMessage(message)
            if (parts.size <= 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            SmsSendResult.ATTEMPTED
        } catch (_: SecurityException) {
            SmsSendResult.NO_PERMISSION
        } catch (_: IllegalArgumentException) {
            SmsSendResult.FAILED
        } catch (_: RuntimeException) {
            SmsSendResult.FAILED
        }
    }

    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
}
