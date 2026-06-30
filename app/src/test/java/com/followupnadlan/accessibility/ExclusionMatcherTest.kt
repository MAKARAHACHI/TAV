package com.followupnadlan.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExclusionMatcherTest {
    private val entries = listOf(
        ExcludedRecipient(label = "אמא"), // name-only, no number
        ExcludedRecipient(label = "050-1234567", number = "050-1234567")
    )

    @Test
    fun matchesSameLocalNumber() {
        assertTrue(ExclusionMatcher.isExcluded(entries, "0501234567"))
    }

    @Test
    fun matchesInternationalFormOfSameNumber() {
        assertTrue(ExclusionMatcher.isExcluded(entries, "+972501234567"))
    }

    @Test
    fun doesNotMatchDifferentNumber() {
        assertFalse(ExclusionMatcher.isExcluded(entries, "0529998888"))
    }

    @Test
    fun nameOnlyEntryNeverMatchesByNumber() {
        assertFalse(ExclusionMatcher.isExcluded(listOf(ExcludedRecipient(label = "אמא")), "0501234567"))
    }

    @Test
    fun blankOrShortTargetIsNotExcluded() {
        assertFalse(ExclusionMatcher.isExcluded(entries, ""))
        assertFalse(ExclusionMatcher.isExcluded(entries, "123"))
        assertFalse(ExclusionMatcher.isExcluded(entries, null))
    }
}
