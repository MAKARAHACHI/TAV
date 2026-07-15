package com.followupnadlan.postcall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedCallBackfillLogicTest {
    @Test
    fun recentMissedCallIsBackfillCandidate() {
        val candidates = MissedCallBackfillLogic.candidates(
            rows = listOf(
                row(phoneNumber = "0501234567", timestampMillis = 10_000L)
            ),
            nowMillis = 11_000L,
            alreadyHandledKeys = emptySet()
        )

        assertEquals(listOf(MissedCallBackfillCandidate("0501234567", 10_000L)), candidates)
    }

    @Test
    fun oldMissedCallIsIgnored() {
        val candidates = MissedCallBackfillLogic.candidates(
            rows = listOf(
                row(
                    phoneNumber = "0501234567",
                    timestampMillis = 1_000L
                )
            ),
            nowMillis = 1_000L + MissedCallBackfillLogic.DEFAULT_BACKFILL_WINDOW_MILLIS + 1L,
            alreadyHandledKeys = emptySet()
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun alreadyHandledMissedCallIsNotProcessedTwice() {
        val candidate = MissedCallBackfillCandidate("0501234567", 10_000L)

        val candidates = MissedCallBackfillLogic.candidates(
            rows = listOf(row(phoneNumber = "0501234567", timestampMillis = 10_000L)),
            nowMillis = 11_000L,
            alreadyHandledKeys = setOf(candidate.key)
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun incomingAnsweredRowsAreNotBackfillCandidates() {
        val candidates = MissedCallBackfillLogic.candidates(
            rows = listOf(
                row(
                    phoneNumber = "0501234567",
                    timestampMillis = 10_000L,
                    platformType = CallLogReaderLogic.PLATFORM_TYPE_INCOMING
                )
            ),
            nowMillis = 11_000L,
            alreadyHandledKeys = emptySet()
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun missingNumberIsNotBackfillCandidate() {
        val candidates = MissedCallBackfillLogic.candidates(
            rows = listOf(row(phoneNumber = " ", timestampMillis = 10_000L)),
            nowMillis = 11_000L,
            alreadyHandledKeys = emptySet()
        )

        assertTrue(candidates.isEmpty())
    }

    private fun row(
        phoneNumber: String,
        timestampMillis: Long,
        platformType: Int = CallLogReaderLogic.PLATFORM_TYPE_MISSED
    ): RawCallLogRow =
        RawCallLogRow(
            phoneNumber = phoneNumber,
            timestampMillis = timestampMillis,
            durationSeconds = 0L,
            platformType = platformType
        )
}
