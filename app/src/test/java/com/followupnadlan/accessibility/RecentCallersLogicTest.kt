package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentCallersLogicTest {

    @Test
    fun dropsRowsWithoutDigits() {
        val rows = listOf(
            RecentCallerRow(name = "Unknown", phone = "", timestampEpochMs = 10),
            RecentCallerRow(name = null, phone = "050-1234567", timestampEpochMs = 5)
        )
        val result = RecentCallersLogic.dedupe(rows)
        assertEquals(1, result.size)
        assertEquals("050-1234567", result[0].phone)
    }

    @Test
    fun keepsNewestPerNumberDistinctByLastNineDigits() {
        val rows = listOf(
            RecentCallerRow(name = "ישן", phone = "+972501234567", timestampEpochMs = 1),
            RecentCallerRow(name = "חדש", phone = "0501234567", timestampEpochMs = 9)
        )
        val result = RecentCallersLogic.dedupe(rows)
        assertEquals(1, result.size)
        assertEquals("חדש", result[0].name)
        assertEquals("0501234567", result[0].phone)
    }

    @Test
    fun sortsNewestFirst() {
        val rows = listOf(
            RecentCallerRow(name = "A", phone = "0501111111", timestampEpochMs = 1),
            RecentCallerRow(name = "B", phone = "0502222222", timestampEpochMs = 3),
            RecentCallerRow(name = "C", phone = "0503333333", timestampEpochMs = 2)
        )
        val result = RecentCallersLogic.dedupe(rows)
        assertEquals(listOf("B", "C", "A"), result.map { it.name })
    }

    @Test
    fun unknownNameFallsBackToLocalIsraeliDisplay() {
        val rows = listOf(
            RecentCallerRow(name = null, phone = "+972521234567", timestampEpochMs = 1),
            RecentCallerRow(name = "   ", phone = "0539876543", timestampEpochMs = 2)
        )
        val result = RecentCallersLogic.dedupe(rows)
        assertEquals("0539876543", result[0].name)
        assertEquals("0521234567", result[1].name)
    }

    @Test
    fun capsToMax() {
        val rows = (1..30).map {
            RecentCallerRow(name = "n$it", phone = "05000000%02d".format(it), timestampEpochMs = it.toLong())
        }
        val result = RecentCallersLogic.dedupe(rows, max = 20)
        assertEquals(20, result.size)
    }
}
