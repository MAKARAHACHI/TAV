package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingPermissionLogicTest {
    @Test
    fun bothGrantedIsReady() {
        assertEquals(
            OnboardingPermissionOutcome.READY,
            OnboardingPermissionLogic.outcomeFor(phoneStateGranted = true, callLogGranted = true)
        )
    }

    @Test
    fun missingPhoneStateIsMissing() {
        assertEquals(
            OnboardingPermissionOutcome.MISSING,
            OnboardingPermissionLogic.outcomeFor(phoneStateGranted = false, callLogGranted = true)
        )
    }

    @Test
    fun missingCallLogIsMissing() {
        assertEquals(
            OnboardingPermissionOutcome.MISSING,
            OnboardingPermissionLogic.outcomeFor(phoneStateGranted = true, callLogGranted = false)
        )
    }

    @Test
    fun bothMissingIsMissing() {
        assertEquals(
            OnboardingPermissionOutcome.MISSING,
            OnboardingPermissionLogic.outcomeFor(phoneStateGranted = false, callLogGranted = false)
        )
    }
}
