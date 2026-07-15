package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactSelectionDiffTest {

    @Test
    fun addsNewlyCheckedThatWereNotSaved() {
        val result = ContactSelectionDiff.compute(
            savedKeys = setOf("111111111"),
            checkedKeys = setOf("111111111", "222222222"),
            contactKeys = setOf("111111111", "222222222")
        )
        assertEquals(setOf("222222222"), result.added)
        assertEquals(emptySet<String>(), result.removed)
    }

    @Test
    fun unCheckingListedSavedContactRemovesIt() {
        val result = ContactSelectionDiff.compute(
            savedKeys = setOf("111111111", "222222222"),
            checkedKeys = setOf("111111111"),
            contactKeys = setOf("111111111", "222222222")
        )
        assertEquals(emptySet<String>(), result.added)
        assertEquals(setOf("222222222"), result.removed)
    }

    @Test
    fun savedManualEntryNotInCandidateListIsNeverRemoved() {
        // 333333333 is saved (manual/name-only) but not in the current candidate list, and not checked.
        val result = ContactSelectionDiff.compute(
            savedKeys = setOf("111111111", "333333333"),
            checkedKeys = setOf("111111111"),
            contactKeys = setOf("111111111", "222222222")
        )
        assertEquals(emptySet<String>(), result.added)
        assertEquals(emptySet<String>(), result.removed)
    }

    @Test
    fun noChangesYieldsEmptyDiff() {
        val result = ContactSelectionDiff.compute(
            savedKeys = setOf("111111111"),
            checkedKeys = setOf("111111111"),
            contactKeys = setOf("111111111", "222222222")
        )
        assertEquals(emptySet<String>(), result.added)
        assertEquals(emptySet<String>(), result.removed)
    }

    @Test
    fun addAndRemoveInSameConfirm() {
        val result = ContactSelectionDiff.compute(
            savedKeys = setOf("111111111"),
            checkedKeys = setOf("222222222"),
            contactKeys = setOf("111111111", "222222222")
        )
        assertEquals(setOf("222222222"), result.added)
        assertEquals(setOf("111111111"), result.removed)
    }
}
