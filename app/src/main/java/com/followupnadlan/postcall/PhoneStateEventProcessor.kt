package com.followupnadlan.postcall

import android.content.Context
import com.followupnadlan.missedcall.MissedCallAutoResponseAction
import com.followupnadlan.postcall.CallStateMonitor.CallState

class PhoneStateEventProcessor(
    context: Context,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val onConfirmedMissedCall: (String?) -> MissedCallAutoResponseAction
) {
    private val pendingStore = PendingIncomingCallStore(context)
    private val diagnostics = CallDetectionDiagnostics(context)

    fun handleState(newState: CallState, incomingNumber: String? = null) {
        diagnostics.recordPhoneState(eventFor(newState))
        if (newState == CallState.RINGING) {
            diagnostics.recordIncomingNumber(incomingNumber)
        }

        val now = nowMillis()
        val transition = PendingIncomingCallLogic.transition(
            pending = pendingStore.load(),
            newState = newState,
            nowMillis = now,
            incomingNumber = incomingNumber
        )
        pendingStore.save(transition.nextPending)

        when (transition.action) {
            PendingIncomingCallAction.STORED ->
                diagnostics.recordEvent(CallDetectionDiagnostics.MISSED_CALL_CANDIDATE_STORED)
            PendingIncomingCallAction.CONFIRMED_MISSED -> {
                diagnostics.recordMissedCallDetected(now)
                diagnostics.recordEvent(CallDetectionDiagnostics.MISSED_CALL_HANDLER_STARTED)
                val action = onConfirmedMissedCall(transition.confirmedMissedPhoneNumber)
                diagnostics.recordEvent(CallDetectionDiagnostics.MISSED_CALL_DECISION_RESULT, action.name)
            }
            PendingIncomingCallAction.IGNORED_ANSWERED ->
                diagnostics.recordEvent(CallDetectionDiagnostics.MISSED_CALL_IGNORED_ANSWERED)
            PendingIncomingCallAction.IGNORED_STALE ->
                diagnostics.recordEvent(PendingIncomingCallAction.IGNORED_STALE.name)
            PendingIncomingCallAction.MARKED_ANSWERED,
            PendingIncomingCallAction.NONE -> Unit
        }
    }

    private fun eventFor(state: CallState): String = when (state) {
        CallState.RINGING -> CallDetectionDiagnostics.PHONE_STATE_RECEIVED_RINGING
        CallState.OFFHOOK -> CallDetectionDiagnostics.PHONE_STATE_RECEIVED_OFFHOOK
        CallState.IDLE -> CallDetectionDiagnostics.PHONE_STATE_RECEIVED_IDLE
    }
}
