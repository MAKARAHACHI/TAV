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

    // §2: Accessibility is OPTIONAL — it must never gate answering, in either direction.
    @Test
    fun answeringDoesNotDependOnAccessibility() {
        assertTrue(PermissionStatusLogic.canAnswerCalls(allGranted.copy(accessibilityEnabled = false)))
        assertTrue(PermissionStatusLogic.canAnswerCalls(allGranted.copy(accessibilityEnabled = true)))
    }

    // The summary counts the required three only — the optional capability never inflates it.
    @Test
    fun summaryCountsRequiredOnlyNotOptional() {
        assertEquals("3 מ-3 פעילות", PermissionStatusLogic.summary(allGranted.copy(accessibilityEnabled = true)))
        assertEquals("3 מ-3 פעילות", PermissionStatusLogic.summary(allGranted.copy(accessibilityEnabled = false)))
        assertEquals(3, PermissionStatusLogic.grantedCount(allGranted.copy(accessibilityEnabled = false)))
    }

    // Required list stays exactly three; the optional capability lives in its own list.
    @Test
    fun requiredAndOptionalAreSeparate() {
        assertEquals(3, PermissionStatusLogic.all.size)
        assertTrue(PermissionStatusLogic.optional.contains(FollowUpOptionalCapability.ACCESSIBILITY))
        assertTrue(allGranted.copy(accessibilityEnabled = true).isGranted(FollowUpOptionalCapability.ACCESSIBILITY))
        assertFalse(allGranted.copy(accessibilityEnabled = false).isGranted(FollowUpOptionalCapability.ACCESSIBILITY))
    }

    // Optional capability, like the required ones, explains the consequence — never the identifier.
    @Test
    fun optionalCapabilityExplainsWhatTheUserGets() {
        PermissionStatusLogic.optional.forEach { capability ->
            val title = PermissionStatusLogic.title(capability)
            val outcome = PermissionStatusLogic.outcome(capability)
            assertTrue("missing title for $capability", title.isNotBlank())
            assertTrue("missing outcome for $capability", outcome.isNotBlank())
            listOf("ACCESSIBILITY", "Settings.", "Secure").forEach { jargon ->
                assertFalse(
                    "outcome must not leak the identifier: $outcome",
                    outcome.contains(jargon, ignoreCase = true)
                )
            }
        }
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
