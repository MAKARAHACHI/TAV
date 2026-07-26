package com.followupnadlan.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusTest {
    private val allGranted = PermissionSnapshot(
        phoneStateGranted = true,
        callLogGranted = true,
        contactsGranted = true
    )

    @Test
    fun countsGrantedPermissions() {
        assertEquals(3, PermissionStatusLogic.grantedCount(allGranted))
        assertEquals(0, PermissionStatusLogic.grantedCount(PermissionSnapshot(false, false, false)))
        assertEquals(2, PermissionStatusLogic.grantedCount(allGranted.copy(contactsGranted = false)))
    }

    @Test
    fun summaryReadsAsALiveCount() {
        assertEquals("3 מ-3 פעילות", PermissionStatusLogic.summary(allGranted))
        assertEquals("2 מ-3 פעילות", PermissionStatusLogic.summary(allGranted.copy(callLogGranted = false)))
    }

    @Test
    fun answeringNeedsPhoneStateAndCallLog() {
        assertTrue(PermissionStatusLogic.canAnswerCalls(allGranted))
        assertFalse(PermissionStatusLogic.canAnswerCalls(allGranted.copy(phoneStateGranted = false)))
        assertFalse(PermissionStatusLogic.canAnswerCalls(allGranted.copy(callLogGranted = false)))
    }

    // Without contacts the app still answers — it just can't tell a client from an acquaintance.
    @Test
    fun answeringDoesNotDependOnContacts() {
        assertTrue(PermissionStatusLogic.canAnswerCalls(allGranted.copy(contactsGranted = false)))
    }

    @Test
    fun everyPermissionExplainsWhatTheUserGets() {
        PermissionStatusLogic.all.forEach { permission ->
            val outcome = PermissionStatusLogic.outcome(permission)
            assertTrue("missing outcome text for $permission", outcome.isNotBlank())
            // Never the Android identifier — the user reads the consequence, not the constant.
            listOf("READ_", "permission", "MANIFEST").forEach { jargon ->
                assertFalse(
                    "outcome must not leak the identifier: $outcome",
                    outcome.contains(jargon, ignoreCase = true)
                )
            }
        }
    }
}
