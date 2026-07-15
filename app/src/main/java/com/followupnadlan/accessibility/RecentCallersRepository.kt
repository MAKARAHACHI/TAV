package com.followupnadlan.accessibility

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import com.followupnadlan.followuplog.FollowUpLogStore

/**
 * "My recent callers" — what the user expects, not just calls the app handled.
 * Source chain:
 *   1. Call Log (CallLog.Calls) if READ_CALL_LOG is granted (already requested for bridging).
 *      Includes calls from before the app was installed and unsaved numbers.
 *   2. Otherwise FollowUpLogStore entries with a phone (no permission needed).
 *   3. Otherwise empty.
 * Mapping/dedup/sort is the pure [RecentCallersLogic.dedupe].
 */
class RecentCallersRepository(
    private val context: Context,
    private val followUpLogStore: FollowUpLogStore = FollowUpLogStore(context)
) {
    fun load(): List<ContactCandidate> {
        val fromCallLog = readCallLog()
        val rows = if (fromCallLog.isNotEmpty()) fromCallLog else readFollowUpLog()
        return RecentCallersLogic.dedupe(rows)
    }

    private fun readCallLog(): List<RecentCallerRow> {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                PROJECTION,
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT $QUERY_LIMIT"
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                buildList {
                    while (cursor.moveToNext()) {
                        val phone = cursor.getString(numberIndex).orEmpty().trim()
                        if (phone.isBlank()) continue
                        add(
                            RecentCallerRow(
                                name = cursor.getString(nameIndex)?.trim()?.takeIf { it.isNotBlank() },
                                phone = phone,
                                timestampEpochMs = cursor.getLong(dateIndex)
                            )
                        )
                    }
                }
            }.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    private fun readFollowUpLog(): List<RecentCallerRow> =
        followUpLogStore.load()
            .filter { it.phone.isNotBlank() }
            .map { RecentCallerRow(name = null, phone = it.phone, timestampEpochMs = it.timestampEpochMs) }

    private companion object {
        const val QUERY_LIMIT = 100
        val PROJECTION = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE
        )
    }
}
