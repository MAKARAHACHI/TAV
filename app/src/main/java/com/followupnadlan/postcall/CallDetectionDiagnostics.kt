package com.followupnadlan.postcall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

data class CallDetectionDiagnosticsSnapshot(
    val lastEvent: String,
    val lastEventAtMillis: Long,
    val lastPhoneStateEvent: String,
    val lastIncomingNumber: String,
    val lastMissedCallDetectedAtMillis: Long,
    val receiverActive: Boolean,
    val serviceActive: Boolean,
    val readPhoneStateGranted: Boolean,
    val readCallLogGranted: Boolean
)

class CallDetectionDiagnostics(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun recordEvent(event: String, detail: String = "") {
        Log.d(LOG_TAG, if (detail.isBlank()) event else "$event:$detail")
        preferences.edit()
            .putString(KEY_LAST_EVENT, if (detail.isBlank()) event else "$event:$detail")
            .putLong(KEY_LAST_EVENT_AT, System.currentTimeMillis())
            .apply()
    }

    fun recordPhoneState(event: String) {
        Log.d(LOG_TAG, event)
        preferences.edit()
            .putString(KEY_LAST_PHONE_STATE_EVENT, event)
            .putString(KEY_LAST_EVENT, event)
            .putLong(KEY_LAST_EVENT_AT, System.currentTimeMillis())
            .apply()
    }

    fun recordIncomingNumber(number: String?) {
        val trimmed = number.orEmpty().trim()
        preferences.edit()
            .putString(KEY_LAST_INCOMING_NUMBER, trimmed)
            .apply()
        recordEvent(if (trimmed.isBlank()) INCOMING_NUMBER_MISSING else INCOMING_NUMBER_PRESENT)
    }

    fun recordMissedCallDetected(atMillis: Long) {
        preferences.edit()
            .putLong(KEY_LAST_MISSED_CALL_DETECTED_AT, atMillis)
            .apply()
        recordEvent(MISSED_CALL_CONFIRMED)
    }

    fun setReceiverActive(active: Boolean) {
        preferences.edit().putBoolean(KEY_RECEIVER_ACTIVE, active).apply()
        if (active) recordEvent(CALL_RECEIVER_REGISTERED)
    }

    fun setServiceActive(active: Boolean) {
        preferences.edit().putBoolean(KEY_SERVICE_ACTIVE, active).apply()
        recordEvent(if (active) BACKGROUND_SERVICE_STARTED else BACKGROUND_SERVICE_STOPPED)
    }

        fun snapshot(): CallDetectionDiagnosticsSnapshot =
        CallDetectionDiagnosticsSnapshot(
            lastEvent = preferences.getString(KEY_LAST_EVENT, "").orEmpty(),
            lastEventAtMillis = preferences.getLong(KEY_LAST_EVENT_AT, 0L),
            lastPhoneStateEvent = preferences.getString(KEY_LAST_PHONE_STATE_EVENT, "").orEmpty(),
            lastIncomingNumber = preferences.getString(KEY_LAST_INCOMING_NUMBER, "").orEmpty(),
            lastMissedCallDetectedAtMillis = preferences.getLong(KEY_LAST_MISSED_CALL_DETECTED_AT, 0L),
            receiverActive = preferences.getBoolean(KEY_RECEIVER_ACTIVE, false),
            serviceActive = preferences.getBoolean(KEY_SERVICE_ACTIVE, false),
            readPhoneStateGranted = hasPermission(Manifest.permission.READ_PHONE_STATE),
            readCallLogGranted = hasPermission(Manifest.permission.READ_CALL_LOG)
        )

    private fun hasPermission(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val CALL_RECEIVER_REGISTERED = "CALL_RECEIVER_REGISTERED"
        const val PHONE_STATE_RECEIVED_RINGING = "PHONE_STATE_RECEIVED_RINGING"
        const val PHONE_STATE_RECEIVED_OFFHOOK = "PHONE_STATE_RECEIVED_OFFHOOK"
        const val PHONE_STATE_RECEIVED_IDLE = "PHONE_STATE_RECEIVED_IDLE"
        const val INCOMING_NUMBER_PRESENT = "INCOMING_NUMBER_PRESENT"
        const val INCOMING_NUMBER_MISSING = "INCOMING_NUMBER_MISSING"
        const val MISSED_CALL_CANDIDATE_STORED = "MISSED_CALL_CANDIDATE_STORED"
        const val MISSED_CALL_CONFIRMED = "MISSED_CALL_CONFIRMED"
        const val MISSED_CALL_IGNORED_ANSWERED = "MISSED_CALL_IGNORED_ANSWERED"
        const val MISSED_CALL_HANDLER_STARTED = "MISSED_CALL_HANDLER_STARTED"
        const val MISSED_CALL_DECISION_RESULT = "MISSED_CALL_DECISION_RESULT"
        const val BACKGROUND_SERVICE_STARTED = "BACKGROUND_SERVICE_STARTED"
        const val BACKGROUND_SERVICE_STOPPED = "BACKGROUND_SERVICE_STOPPED"
        const val PERMISSION_MISSING_READ_PHONE_STATE = "PERMISSION_MISSING_READ_PHONE_STATE"
        const val PERMISSION_MISSING_READ_CALL_LOG = "PERMISSION_MISSING_READ_CALL_LOG"

        private const val PREFERENCES_NAME = "call_detection_diagnostics"
        private const val LOG_TAG = "CallDetection"
        private const val KEY_LAST_EVENT = "last_event"
        private const val KEY_LAST_EVENT_AT = "last_event_at"
        private const val KEY_LAST_PHONE_STATE_EVENT = "last_phone_state_event"
        private const val KEY_LAST_INCOMING_NUMBER = "last_incoming_number"
        private const val KEY_LAST_MISSED_CALL_DETECTED_AT = "last_missed_call_detected_at"
        private const val KEY_RECEIVER_ACTIVE = "receiver_active"
        private const val KEY_SERVICE_ACTIVE = "service_active"
    }
}
