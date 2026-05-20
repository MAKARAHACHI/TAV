package com.followupnadlan.postcall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog

class CallLogReader(
    private val context: Context,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val recencyWindowMillis: Long = CallLogReaderLogic.DEFAULT_RECENCY_WINDOW_MILLIS
) {
    fun readLatestCall(): LatestCallLogEntry? {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                CALL_LOG_PROJECTION,
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null

                val row = RawCallLogRow(
                    phoneNumber = cursor.getString(COLUMN_NUMBER).orEmpty(),
                    timestampMillis = cursor.getLong(COLUMN_DATE),
                    durationSeconds = cursor.getLong(COLUMN_DURATION),
                    platformType = cursor.getInt(COLUMN_TYPE)
                )
                CallLogReaderLogic.toLatestCall(row, nowMillis(), recencyWindowMillis)
            }
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private companion object {
        val CALL_LOG_PROJECTION = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )
        const val COLUMN_NUMBER = 0
        const val COLUMN_DATE = 1
        const val COLUMN_DURATION = 2
        const val COLUMN_TYPE = 3
    }
}

enum class FollowUpCallType {
    Incoming,
    Outgoing,
    Missed
}

data class LatestCallLogEntry(
    val phoneNumber: String,
    val durationSeconds: Long,
    val timestampMillis: Long,
    val type: FollowUpCallType
)

data class RawCallLogRow(
    val phoneNumber: String,
    val timestampMillis: Long,
    val durationSeconds: Long,
    val platformType: Int
)

object CallLogReaderLogic {
    const val DEFAULT_RECENCY_WINDOW_MILLIS = 2 * 60 * 1000L
    const val PLATFORM_TYPE_INCOMING = 1
    const val PLATFORM_TYPE_OUTGOING = 2
    const val PLATFORM_TYPE_MISSED = 3

    fun classify(platformType: Int): FollowUpCallType? = when (platformType) {
        PLATFORM_TYPE_INCOMING -> FollowUpCallType.Incoming
        PLATFORM_TYPE_OUTGOING -> FollowUpCallType.Outgoing
        PLATFORM_TYPE_MISSED -> FollowUpCallType.Missed
        else -> null
    }

    fun toLatestCall(
        row: RawCallLogRow,
        nowMillis: Long,
        recencyWindowMillis: Long = DEFAULT_RECENCY_WINDOW_MILLIS
    ): LatestCallLogEntry? {
        val phone = row.phoneNumber.trim()
        val type = classify(row.platformType) ?: return null
        if (phone.isBlank()) return null
        if (row.timestampMillis <= 0L) return null

        val ageMillis = nowMillis - row.timestampMillis
        if (ageMillis < 0L || ageMillis > recencyWindowMillis) return null

        return LatestCallLogEntry(
            phoneNumber = phone,
            durationSeconds = row.durationSeconds.coerceAtLeast(0L),
            timestampMillis = row.timestampMillis,
            type = type
        )
    }
}
