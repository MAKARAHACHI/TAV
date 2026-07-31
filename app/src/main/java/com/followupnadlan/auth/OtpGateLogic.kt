package com.followupnadlan.auth

/**
 * Which login state the app is in, derived purely from the stored token + the clock. Kept free of
 * Android imports so it is unit-testable (mirrors OnboardingPermissionLogic).
 */
enum class OtpGateStatus {
    /** Token present, not expired, and renewed recently enough — the app runs normally. */
    AUTHENTICATED,

    /** Token still valid but stale (>= RENEW_AFTER_MS since last renew) — try a silent renew. */
    NEEDS_RENEW,

    /** No token, or token expired — the login screen must be shown. */
    LOCKED
}

object OtpGateLogic {
    /** Silent-renew trigger: once a token is this old (since last renew) we attempt a renew. 24h. */
    const val RENEW_AFTER_MS = 86_400_000L

    /** Server token time-to-live (7 days). Documentation only — the server is the authority. */
    const val TOKEN_TTL_MS = 604_800_000L

    /**
     * Rules (checked in order):
     *  - no token            -> LOCKED
     *  - now >= expiresAtMs   -> LOCKED (expired; boundary equal is expired)
     *  - now - lastRenewOkMs >= RENEW_AFTER_MS -> NEEDS_RENEW (boundary equal renews)
     *  - otherwise            -> AUTHENTICATED
     */
    fun evaluate(
        hasToken: Boolean,
        expiresAtMs: Long,
        lastRenewOkMs: Long,
        nowMs: Long
    ): OtpGateStatus {
        if (!hasToken) return OtpGateStatus.LOCKED
        if (nowMs >= expiresAtMs) return OtpGateStatus.LOCKED
        if (nowMs - lastRenewOkMs >= RENEW_AFTER_MS) return OtpGateStatus.NEEDS_RENEW
        return OtpGateStatus.AUTHENTICATED
    }
}
