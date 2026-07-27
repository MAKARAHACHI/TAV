package com.followupnadlan.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStatusBannerLogicTest {
    private val healthy = HomeStatusBannerInput(
        serviceRunning = true,
        phoneStatePermissionGranted = true,
        callLogPermissionGranted = true,
        batteryOptimizationExempt = true
    )

    @Test
    fun allSignalsGoodIsActiveTeal() {
        val display = HomeStatusBannerLogic.compute(healthy)
        assertEquals(HomeStatusBannerState.ACTIVE, display.state)
        assertEquals(HomeStatusBannerLogic.ACTIVE_TEXT, display.text)
    }

    @Test
    fun serviceOffIsWarning() {
        val display = HomeStatusBannerLogic.compute(healthy.copy(serviceRunning = false))
        assertEquals(HomeStatusBannerState.WARNING, display.state)
        assertEquals(HomeStatusBannerLogic.WARNING_TEXT, display.text)
    }

    @Test
    fun missingPermissionIsWarning() {
        assertEquals(
            HomeStatusBannerState.WARNING,
            HomeStatusBannerLogic.compute(healthy.copy(phoneStatePermissionGranted = false)).state
        )
        assertEquals(
            HomeStatusBannerState.WARNING,
            HomeStatusBannerLogic.compute(healthy.copy(callLogPermissionGranted = false)).state
        )
    }

    @Test
    fun batteryNotExemptIsWarning() {
        assertEquals(
            HomeStatusBannerState.WARNING,
            HomeStatusBannerLogic.compute(healthy.copy(batteryOptimizationExempt = false)).state
        )
    }
}
