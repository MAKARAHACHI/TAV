package com.followupnadlan.postcall

import org.junit.Assert.assertEquals
import org.junit.Test

class CallDetectionServiceLifecycleTest {
    @Test
    fun bridgeEnabledWithRequiredPermissionsStartsForegroundService() {
        assertEquals(
            CallDetectionServiceAction.START,
            CallDetectionServiceLifecycle.actionFor(
                bridgeEnabled = true,
                readPhoneStateGranted = true,
                readCallLogGranted = true
            )
        )
    }

    @Test
    fun bridgeDisabledStopsForegroundService() {
        assertEquals(
            CallDetectionServiceAction.STOP,
            CallDetectionServiceLifecycle.actionFor(
                bridgeEnabled = false,
                readPhoneStateGranted = true,
                readCallLogGranted = true
            )
        )
    }

    @Test
    fun missingReadPhoneStateStopsForegroundService() {
        assertEquals(
            CallDetectionServiceAction.STOP,
            CallDetectionServiceLifecycle.actionFor(
                bridgeEnabled = true,
                readPhoneStateGranted = false,
                readCallLogGranted = true
            )
        )
    }

    @Test
    fun missingReadCallLogStopsForegroundService() {
        assertEquals(
            CallDetectionServiceAction.STOP,
            CallDetectionServiceLifecycle.actionFor(
                bridgeEnabled = true,
                readPhoneStateGranted = true,
                readCallLogGranted = false
            )
        )
    }
}
