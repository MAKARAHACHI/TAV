package com.followupnadlan.postcall

import android.content.Context
import com.followupnadlan.postcall.CallStateMonitor.CallState

data class PendingIncomingCall(
    val phoneNumber: String?,
    val timestampMillis: Long,
    val sawOffhook: Boolean
)

enum class PendingIncomingCallAction {
    NONE,
    STORED,
    MARKED_ANSWERED,
    CONFIRMED_MISSED,
    IGNORED_ANSWERED,
    IGNORED_STALE
}

data class PendingIncomingCallTransition(
    val nextPending: PendingIncomingCall?,
    val action: PendingIncomingCallAction,
    val confirmedMissedPhoneNumber: String? = null
)

object PendingIncomingCallLogic {
    const val DEFAULT_PENDING_WINDOW_MILLIS: Long = 5 * 60 * 1000L

    fun transition(
        pending: PendingIncomingCall?,
        newState: CallState,
        nowMillis: Long,
        incomingNumber: String?,
        pendingWindowMillis: Long = DEFAULT_PENDING_WINDOW_MILLIS
    ): PendingIncomingCallTransition =
        when (newState) {
            CallState.RINGING -> PendingIncomingCallTransition(
                nextPending = PendingIncomingCall(
                    phoneNumber = incomingNumber?.trim()?.takeIf { it.isNotBlank() },
                    timestampMillis = nowMillis,
                    sawOffhook = false
                ),
                action = PendingIncomingCallAction.STORED
            )
            CallState.OFFHOOK -> if (pending != null) {
                PendingIncomingCallTransition(
                    nextPending = pending.copy(sawOffhook = true),
                    action = PendingIncomingCallAction.MARKED_ANSWERED
                )
            } else {
                PendingIncomingCallTransition(nextPending = null, action = PendingIncomingCallAction.NONE)
            }
            CallState.IDLE -> when {
                pending == null -> PendingIncomingCallTransition(null, PendingIncomingCallAction.NONE)
                pending.sawOffhook -> PendingIncomingCallTransition(null, PendingIncomingCallAction.IGNORED_ANSWERED)
                nowMillis - pending.timestampMillis !in 0..pendingWindowMillis ->
                    PendingIncomingCallTransition(null, PendingIncomingCallAction.IGNORED_STALE)
                else -> PendingIncomingCallTransition(
                    nextPending = null,
                    action = PendingIncomingCallAction.CONFIRMED_MISSED,
                    confirmedMissedPhoneNumber = pending.phoneNumber
                )
            }
        }
}

class PendingIncomingCallStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): PendingIncomingCall? {
        val timestamp = preferences.getLong(KEY_TIMESTAMP, 0L)
        if (timestamp <= 0L) return null
        return PendingIncomingCall(
            phoneNumber = preferences.getString(KEY_PHONE, null)?.takeIf { it.isNotBlank() },
            timestampMillis = timestamp,
            sawOffhook = preferences.getBoolean(KEY_SAW_OFFHOOK, false)
        )
    }

    fun save(pending: PendingIncomingCall?) {
        if (pending == null) {
            clear()
            return
        }
        preferences.edit()
            .putString(KEY_PHONE, pending.phoneNumber.orEmpty())
            .putLong(KEY_TIMESTAMP, pending.timestampMillis)
            .putBoolean(KEY_SAW_OFFHOOK, pending.sawOffhook)
            .apply()
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_PHONE)
            .remove(KEY_TIMESTAMP)
            .remove(KEY_SAW_OFFHOOK)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "pending_incoming_call"
        const val KEY_PHONE = "phone"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_SAW_OFFHOOK = "saw_offhook"
    }
}
