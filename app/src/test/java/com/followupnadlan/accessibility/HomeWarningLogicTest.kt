package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HomeWarningLogicTest {
    private val healthy = HomeWarningState(
        serviceEnabled = true,
        phoneStatePermissionGranted = true,
        callLogPermissionGranted = true,
        profileEmpty = false
    )

    // "שקט = עובד" — when nothing is broken, Home shows no status line at all.
    @Test
    fun showsNothingWhenEverythingWorks() {
        assertNull(HomeWarningLogic.warningFor(healthy))
    }

    @Test
    fun warnsWhenTheServiceIsOff() {
        assertEquals(
            HomeWarning.NOT_ANSWERING,
            HomeWarningLogic.warningFor(healthy.copy(serviceEnabled = false))
        )
    }

    @Test
    fun warnsWhenAPermissionIsMissing() {
        assertEquals(
            HomeWarning.NOT_ANSWERING,
            HomeWarningLogic.warningFor(healthy.copy(phoneStatePermissionGranted = false))
        )
        assertEquals(
            HomeWarning.NOT_ANSWERING,
            HomeWarningLogic.warningFor(healthy.copy(callLogPermissionGranted = false))
        )
    }

    @Test
    fun warnsAboutAnEmptyProfileWhenTheServiceItselfIsFine() {
        assertEquals(
            HomeWarning.EMPTY_PROFILE,
            HomeWarningLogic.warningFor(healthy.copy(profileEmpty = true))
        )
    }

    // Silence beats two warnings: "clients get nothing" outranks "clients get it unsigned".
    @Test
    fun reportsOnlyTheMostSevereProblem() {
        val allBroken = healthy.copy(serviceEnabled = false, profileEmpty = true)
        assertEquals(HomeWarning.NOT_ANSWERING, HomeWarningLogic.warningFor(allBroken))
    }

    @Test
    fun messagesSpeakOfTheClientNotTheMechanism() {
        HomeWarning.entries.forEach { warning ->
            val message = HomeWarningLogic.message(warning)
            listOf("Accessibility", "permission", "service", "scope", "fallback").forEach { jargon ->
                assertFalse(
                    "warning text must not leak the mechanism: $message",
                    message.contains(jargon, ignoreCase = true)
                )
            }
        }
    }
}
