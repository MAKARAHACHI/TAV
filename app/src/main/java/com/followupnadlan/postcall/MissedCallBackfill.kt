package com.followupnadlan.postcall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.followupnadlan.missedcall.MissedCallAutoResponseAction

data class MissedCallBackfillCandidate(
    val phoneNumber: String,
    val timestampMillis: Long
) {
    val key: String = "${timestampMillis}:${phoneNumber.trim()}"
}

object MissedCallBackfillLogic {
    const val DEFAULT_BACKFILL_WINDOW_MILLIS: Long = 5 * 60 * 1000L

    fun candidates(
        rows: List<RawCallLogRow>,
        nowMillis: Long,
        alreadyHandledKeys: Set<String>,
        backfillWindowMillis: Long = DEFAULT_BACKFILL_WINDOW_MILLIS
    ): List<MissedCallBackfillCandidate> =
        rows.mapNotNull { row ->
            if (row.platformType != CallLogReaderLogic.PLATFORM_TYPE_MISSED) return@mapNotNull null
            val phone = row.phoneNumber.trim()
            if (phone.isBlank()) return@mapNotNull null
            val age = nowMillis - row.timestampMillis
            if (age !in 0..backfillWindowMillis) return@mapNotNull null
            MissedCallBackfillCandidate(phone, row.timestampMillis)
        }.filterNot { it.key in alreadyHandledKeys }
}

class MissedCallBackfill(
    private val context: Context,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val diagnostics = CallDetectionDiagnostics(context)
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun processRecentMissedCalls(
        onCandidate: (String) -> MissedCallAutoResponseAction
    ) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            diagnostics.recordEvent(CallDetectionDiagnostics.PERMISSION_MISSING_READ_CALL_LOG)
            return
        }

        val handled = handledKeys()
        val candidates = MissedCallBackfillLogic.candidates(
            rows = CallLogReader(context, nowMillis, DEFAULT_QUERY_WINDOW_MILLIS).readRecentMissedCalls(),
            nowMillis = nowMillis(),
            alreadyHandledKeys = handled
        )
        if (candidates.isEmpty()) return

        val nextHandled = handled.toMutableSet()
        candidates.forEach { candidate ->
            diagnostics.recordEvent(CallDetectionDiagnostics.MISSED_CALL_HANDLER_STARTED)
            val action = onCandidate(candidate.phoneNumber)
            diagnostics.recordEvent(CallDetectionDiagnostics.MISSED_CALL_DECISION_RESULT, action.name)
            nextHandled += candidate.key
        }
        saveHandledKeys(nextHandled)
    }

    private fun handledKeys(): Set<String> =
        preferences.getStringSet(KEY_HANDLED_KEYS, emptySet()).orEmpty()

    private fun saveHandledKeys(keys: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_HANDLED_KEYS, keys.toList().takeLast(MAX_HANDLED_KEYS).toSet())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "missed_call_backfill"
        const val KEY_HANDLED_KEYS = "handled_keys"
        const val MAX_HANDLED_KEYS = 50
        const val DEFAULT_QUERY_WINDOW_MILLIS = MissedCallBackfillLogic.DEFAULT_BACKFILL_WINDOW_MILLIS
    }
}
