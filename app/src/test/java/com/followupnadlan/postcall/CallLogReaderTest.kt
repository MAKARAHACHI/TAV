package com.followupnadlan.postcall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CallLogReaderTest {
    @Test
    fun recentIncomingCallIsAccepted() {
        val latest = CallLogReaderLogic.toLatestCall(
            row = RawCallLogRow(
                phoneNumber = "050-1234567",
                timestampMillis = 1_000L,
                durationSeconds = 45L,
                platformType = CallLogReaderLogic.PLATFORM_TYPE_INCOMING
            ),
            nowMillis = 2_000L
        )

        assertNotNull(latest)
        assertEquals("050-1234567", latest?.phoneNumber)
        assertEquals(45L, latest?.durationSeconds)
        assertEquals(FollowUpCallType.Incoming, latest?.type)
    }

    @Test
    fun recentOutgoingZeroDurationCallIsAccepted() {
        val latest = CallLogReaderLogic.toLatestCall(
            row = RawCallLogRow(
                phoneNumber = "050-7654321",
                timestampMillis = 10_000L,
                durationSeconds = 0L,
                platformType = CallLogReaderLogic.PLATFORM_TYPE_OUTGOING
            ),
            nowMillis = 11_000L
        )

        assertNotNull(latest)
        assertEquals(0L, latest?.durationSeconds)
        assertEquals(FollowUpCallType.Outgoing, latest?.type)
    }

    @Test
    fun recentMissedCallIsClassified() {
        val latest = CallLogReaderLogic.toLatestCall(
            row = RawCallLogRow(
                phoneNumber = "050-0000000",
                timestampMillis = 10_000L,
                durationSeconds = 0L,
                platformType = CallLogReaderLogic.PLATFORM_TYPE_MISSED
            ),
            nowMillis = 11_000L
        )

        assertNotNull(latest)
        assertEquals(FollowUpCallType.Missed, latest?.type)
    }

    @Test
    fun staleCallIsRejected() {
        val latest = CallLogReaderLogic.toLatestCall(
            row = RawCallLogRow(
                phoneNumber = "050-1234567",
                timestampMillis = 1_000L,
                durationSeconds = 45L,
                platformType = CallLogReaderLogic.PLATFORM_TYPE_INCOMING
            ),
            nowMillis = 1_000L + CallLogReaderLogic.DEFAULT_RECENCY_WINDOW_MILLIS + 1L
        )

        assertNull(latest)
    }

    @Test
    fun futureCallIsRejected() {
        val latest = CallLogReaderLogic.toLatestCall(
            row = RawCallLogRow(
                phoneNumber = "050-1234567",
                timestampMillis = 3_000L,
                durationSeconds = 45L,
                platformType = CallLogReaderLogic.PLATFORM_TYPE_INCOMING
            ),
            nowMillis = 2_000L
        )

        assertNull(latest)
    }

    @Test
    fun unknownTypeIsRejected() {
        val latest = CallLogReaderLogic.toLatestCall(
            row = RawCallLogRow(
                phoneNumber = "050-1234567",
                timestampMillis = 1_000L,
                durationSeconds = 45L,
                platformType = 999
            ),
            nowMillis = 2_000L
        )

        assertNull(latest)
    }

    @Test
    fun blankPhoneIsRejected() {
        val latest = CallLogReaderLogic.toLatestCall(
            row = RawCallLogRow(
                phoneNumber = "   ",
                timestampMillis = 1_000L,
                durationSeconds = 45L,
                platformType = CallLogReaderLogic.PLATFORM_TYPE_INCOMING
            ),
            nowMillis = 2_000L
        )

        assertNull(latest)
    }
}
