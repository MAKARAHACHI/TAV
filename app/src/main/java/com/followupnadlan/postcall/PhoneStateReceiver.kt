package com.followupnadlan.postcall

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import com.followupnadlan.missedcall.MissedCallAutoResponseHandler
import com.followupnadlan.postcall.CallStateMonitor.CallState

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val diagnostics = CallDetectionDiagnostics(appContext)
        diagnostics.setReceiverActive(true)

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (CallDetectionPreferences(appContext).isEnabled()) {
                    CallDetectionService.start(appContext)
                }
            }
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> handlePhoneState(appContext, intent, diagnostics)
        }
    }

    private fun handlePhoneState(
        context: Context,
        intent: Intent,
        diagnostics: CallDetectionDiagnostics
    ) {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            diagnostics.recordEvent(CallDetectionDiagnostics.PERMISSION_MISSING_READ_PHONE_STATE)
            return
        }
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            diagnostics.recordEvent(CallDetectionDiagnostics.PERMISSION_MISSING_READ_CALL_LOG)
            return
        }
        if (!CallDetectionPreferences(context).isEnabled()) return

        runCatching { CallDetectionService.start(context) }
        val state = stateFrom(intent.getStringExtra(TelephonyManager.EXTRA_STATE)) ?: return
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val pendingResult = goAsync()
        val delayMillis = if (state == CallState.IDLE) CALL_LOG_READ_DELAY_MILLIS else 0L
        Handler(Looper.getMainLooper()).postDelayed(
            {
                try {
                    PhoneStateEventProcessor(context) { phone ->
                        MissedCallAutoResponseHandler(context).handleConfirmedMissedIncomingCandidate(phone)
                    }.handleState(state, phoneNumber)
                } finally {
                    pendingResult.finish()
                }
            },
            delayMillis
        )
    }

    private fun stateFrom(value: String?): CallState? = when (value) {
        TelephonyManager.EXTRA_STATE_RINGING -> CallState.RINGING
        TelephonyManager.EXTRA_STATE_OFFHOOK -> CallState.OFFHOOK
        TelephonyManager.EXTRA_STATE_IDLE -> CallState.IDLE
        else -> null
    }

    private companion object {
        const val CALL_LOG_READ_DELAY_MILLIS = 1_000L
    }
}
