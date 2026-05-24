package com.followupnadlan.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupReadinessLogicTest {
    @Test
    fun allGrantedReturnsReady() {
        val result = SetupReadinessLogic.evaluate(readyInput())

        assertEquals(ReadinessVerdict.READY, result.verdict)
        assertEquals(CheckState.PASS, result.check(CheckId.NOTIFICATIONS).state)
        assertEquals(CheckState.PASS, result.check(CheckId.PHONE_STATE).state)
        assertEquals(CheckState.PASS, result.check(CheckId.CALL_LOG).state)
        assertEquals(CheckState.PASS, result.check(CheckId.CONTACTS).state)
        assertEquals(CheckState.PASS, result.check(CheckId.DETECTION_ENABLED).state)
        assertEquals(CheckState.PASS, result.check(CheckId.CHANNEL_ENABLED).state)
        assertEquals(CheckState.PASS, result.check(CheckId.BATTERY_OPTIMIZATION).state)
    }

    @Test
    fun notificationsDeniedDoesNotReturnReady() {
        val result = SetupReadinessLogic.evaluate(
            readyInput(notificationsGranted = false)
        )

        assertEquals(ReadinessVerdict.PARTIAL, result.verdict)
        assertEquals(CheckState.FAIL, result.check(CheckId.NOTIFICATIONS).state)
    }

    @Test
    fun phoneStateDeniedReturnsManualOnly() {
        val result = SetupReadinessLogic.evaluate(
            readyInput(phoneStateGranted = false)
        )

        assertEquals(ReadinessVerdict.MANUAL_ONLY, result.verdict)
        assertEquals(CheckState.FAIL, result.check(CheckId.PHONE_STATE).state)
    }

    @Test
    fun callLogDeniedIsOptionalAndDoesNotBlockReady() {
        val result = SetupReadinessLogic.evaluate(
            readyInput(callLogGranted = false)
        )

        assertEquals(ReadinessVerdict.READY, result.verdict)
        assertEquals(CheckState.OPTIONAL_MISSING, result.check(CheckId.CALL_LOG).state)
    }

    @Test
    fun contactsDeniedIsOptionalAndDoesNotBlockReady() {
        val result = SetupReadinessLogic.evaluate(
            readyInput(contactsGranted = false)
        )

        assertEquals(ReadinessVerdict.READY, result.verdict)
        assertEquals(CheckState.OPTIONAL_MISSING, result.check(CheckId.CONTACTS).state)
    }

    @Test
    fun detectionToggleOffReturnsPartialWithActionableCheck() {
        val result = SetupReadinessLogic.evaluate(
            readyInput(detectionEnabled = false)
        )

        assertEquals(ReadinessVerdict.PARTIAL, result.verdict)
        assertEquals(CheckState.FAIL, result.check(CheckId.DETECTION_ENABLED).state)
        assertEquals("detection_enabled", result.check(CheckId.DETECTION_ENABLED).actionKey)
    }

    @Test
    fun notificationChannelDisabledReturnsPartial() {
        val result = SetupReadinessLogic.evaluate(
            readyInput(notificationChannelEnabled = false)
        )

        assertEquals(ReadinessVerdict.PARTIAL, result.verdict)
        assertEquals(CheckState.FAIL, result.check(CheckId.CHANNEL_ENABLED).state)
    }

    @Test
    fun batteryUnknownDoesNotBlockReady() {
        val result = SetupReadinessLogic.evaluate(
            readyInput(
                batteryOptimizationKnown = false,
                batteryOptimizationAllowsBackground = false
            )
        )

        assertEquals(ReadinessVerdict.READY, result.verdict)
        assertEquals(CheckState.UNKNOWN, result.check(CheckId.BATTERY_OPTIMIZATION).state)
    }

    @Test
    fun batteryBlockingIsAdvisoryAndDoesNotBlockReady() {
        val result = SetupReadinessLogic.evaluate(
            readyInput(batteryOptimizationAllowsBackground = false)
        )

        assertEquals(ReadinessVerdict.READY, result.verdict)
        assertEquals(CheckState.FAIL, result.check(CheckId.BATTERY_OPTIMIZATION).state)
    }

    private fun readyInput(
        notificationsGranted: Boolean = true,
        phoneStateGranted: Boolean = true,
        callLogGranted: Boolean = true,
        contactsGranted: Boolean = true,
        detectionEnabled: Boolean = true,
        notificationChannelEnabled: Boolean = true,
        batteryOptimizationKnown: Boolean = true,
        batteryOptimizationAllowsBackground: Boolean = true
    ): SetupReadinessInput = SetupReadinessInput(
        notificationsGranted = notificationsGranted,
        phoneStateGranted = phoneStateGranted,
        callLogGranted = callLogGranted,
        contactsGranted = contactsGranted,
        detectionEnabled = detectionEnabled,
        notificationChannelEnabled = notificationChannelEnabled,
        batteryOptimizationKnown = batteryOptimizationKnown,
        batteryOptimizationAllowsBackground = batteryOptimizationAllowsBackground
    )
}
