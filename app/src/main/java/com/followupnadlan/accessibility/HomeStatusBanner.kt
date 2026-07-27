package com.followupnadlan.accessibility

/**
 * Part C1 — the Home status banner. STATUS ONLY: it reflects real service/permission/battery state
 * and is never a switch. Teal when everything the system needs is in place; warning when something
 * is missing, in which case tapping it navigates to the fix.
 *
 * Computed from the same real signals the rest of Home already uses — the background detection
 * service being enabled, the two call permissions, and the battery-optimization exemption. Pure
 * logic, no Android — see HomeStatusBannerTest.
 */
internal enum class HomeStatusBannerState { ACTIVE, WARNING }

internal data class HomeStatusBannerInput(
    /** The background detection service is enabled AND the master toggle is on. */
    val serviceRunning: Boolean,
    val phoneStatePermissionGranted: Boolean,
    val callLogPermissionGranted: Boolean,
    /** Battery-optimization exemption granted (so the OEM won't kill the service). */
    val batteryOptimizationExempt: Boolean
)

internal data class HomeStatusBannerDisplay(
    val state: HomeStatusBannerState,
    val text: String
)

internal object HomeStatusBannerLogic {
    const val ACTIVE_TEXT = "המערכת פעילה ומזהה שיחות ✓"
    const val WARNING_TEXT = "המערכת מושבתת · הקש להפעלה"

    fun compute(input: HomeStatusBannerInput): HomeStatusBannerDisplay {
        val healthy = input.serviceRunning &&
            input.phoneStatePermissionGranted &&
            input.callLogPermissionGranted &&
            input.batteryOptimizationExempt
        return if (healthy) {
            HomeStatusBannerDisplay(HomeStatusBannerState.ACTIVE, ACTIVE_TEXT)
        } else {
            HomeStatusBannerDisplay(HomeStatusBannerState.WARNING, WARNING_TEXT)
        }
    }
}
