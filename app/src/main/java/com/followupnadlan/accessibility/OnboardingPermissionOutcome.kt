package com.followupnadlan.accessibility

/**
 * Slide-4 ("all set") state of onboarding, derived from the ACTUAL permission result — never
 * assumed. The permission slide fires a real request; the final slide must tell the truth about
 * what the user granted (§2 — honesty over a celebratory screen). The two call-detection
 * permissions are the same signals Home's warning uses (phoneStateGranted / callLogGranted).
 */
enum class OnboardingPermissionOutcome {
    /** Both call-detection permissions granted: the service can actually run. */
    READY,

    /** At least one permission missing: the service will NOT run until the user grants it. */
    MISSING
}

object OnboardingPermissionLogic {
    /**
     * READY only when BOTH permissions are granted — mirrors the real service gate
     * (AccessibilityApp: the service starts only when phoneStateGranted && callLogGranted).
     * Any missing permission ⇒ MISSING, so slide 4 shows the honest "grant to work" state.
     */
    fun outcomeFor(phoneStateGranted: Boolean, callLogGranted: Boolean): OnboardingPermissionOutcome =
        if (phoneStateGranted && callLogGranted) {
            OnboardingPermissionOutcome.READY
        } else {
            OnboardingPermissionOutcome.MISSING
        }
}
