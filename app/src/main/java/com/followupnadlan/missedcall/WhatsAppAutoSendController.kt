package com.followupnadlan.missedcall

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.followuplog.FollowUpLogStorage
import com.followupnadlan.followuplog.FollowUpLogStore

class WhatsAppAutoSendController(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun enqueuePendingSend(phone: String, message: String, packageName: String, nowEpochMs: Long, source: String) {
        preferences.edit()
            .putString(KEY_PENDING_PHONE, phone)
            .putString(KEY_PENDING_MESSAGE, message)
            .putString(KEY_PENDING_PACKAGE, packageName)
            .putString(KEY_PENDING_SOURCE, source)
            .putLong(KEY_PENDING_CREATED_AT, nowEpochMs)
            .apply()
    }

    fun readPending(nowEpochMs: Long): PendingWhatsAppAutoSend? {
        val createdAt = preferences.getLong(KEY_PENDING_CREATED_AT, 0L)
        if (createdAt <= 0L || nowEpochMs - createdAt > PENDING_TTL_MILLIS) {
            clearPending()
            return null
        }

        val phone = preferences.getString(KEY_PENDING_PHONE, null).orEmpty()
        val message = preferences.getString(KEY_PENDING_MESSAGE, null).orEmpty()
        val packageName = preferences.getString(KEY_PENDING_PACKAGE, null).orEmpty()
        val source = preferences.getString(KEY_PENDING_SOURCE, null).orEmpty()
        if (phone.isBlank() || message.isBlank() || packageName.isBlank() || source.isBlank()) {
            clearPending()
            return null
        }

        return PendingWhatsAppAutoSend(
            phone = phone,
            message = message,
            packageName = packageName,
            source = source,
            createdAtEpochMs = createdAt
        )
    }

    fun markSent(pending: PendingWhatsAppAutoSend, nowEpochMs: Long) {
        if (!isTestSource(pending.source)) {
            MissedCallCooldownStore(context).markAutoReplyAttempt(pending.phone, nowEpochMs)
        }
        appendLog(
            FollowUpActionType.WHATSAPP_AUTO_SENT,
            pending.phone,
            pending.message,
            nowEpochMs,
            pending.source
        )
        clearPending()
    }

    private fun isTestSource(source: String): Boolean =
        source == DebugMissedCallSimulator.SOURCE

    fun markFailed(pending: PendingWhatsAppAutoSend, nowEpochMs: Long) {
        appendLog(
            FollowUpActionType.WHATSAPP_AUTO_FAILED,
            pending.phone,
            pending.message,
            nowEpochMs,
            pending.source
        )
        clearPending()
    }

    fun clearPending() {
        preferences.edit()
            .remove(KEY_PENDING_PHONE)
            .remove(KEY_PENDING_MESSAGE)
            .remove(KEY_PENDING_PACKAGE)
            .remove(KEY_PENDING_SOURCE)
            .remove(KEY_PENDING_CREATED_AT)
            .apply()
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(context, WhatsAppAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun appendLog(
        actionType: FollowUpActionType,
        phone: String,
        message: String,
        timestampEpochMs: Long,
        source: String
    ) {
        FollowUpLogStore(context).append(
            FollowUpLogEntry(
                actionType = actionType,
                timestampEpochMs = timestampEpochMs,
                messagePreview = FollowUpLogStorage.messagePreview(message),
                phone = phone,
                source = source
            )
        )
    }

    companion object {
        private const val PREFERENCES_NAME = "whatsapp_auto_send_controller"
        private const val KEY_PENDING_PHONE = "pending_phone"
        private const val KEY_PENDING_MESSAGE = "pending_message"
        private const val KEY_PENDING_PACKAGE = "pending_package"
        private const val KEY_PENDING_SOURCE = "pending_source"
        private const val KEY_PENDING_CREATED_AT = "pending_created_at"
        private const val PENDING_TTL_MILLIS = 2 * 60 * 1000L
    }
}

data class PendingWhatsAppAutoSend(
    val phone: String,
    val message: String,
    val packageName: String,
    val source: String,
    val createdAtEpochMs: Long
)
