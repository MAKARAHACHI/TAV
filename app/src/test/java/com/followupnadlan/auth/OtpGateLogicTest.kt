package com.followupnadlan.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class OtpGateLogicTest {

    private val now = 1_000_000_000_000L

    @Test
    fun noTokenIsLocked() {
        assertEquals(
            OtpGateStatus.LOCKED,
            OtpGateLogic.evaluate(
                hasToken = false,
                expiresAtMs = now + OtpGateLogic.TOKEN_TTL_MS,
                lastRenewOkMs = now,
                nowMs = now
            )
        )
    }

    @Test
    fun expiredIsLocked() {
        assertEquals(
            OtpGateStatus.LOCKED,
            OtpGateLogic.evaluate(
                hasToken = true,
                expiresAtMs = now - 1L,
                lastRenewOkMs = now - 1L,
                nowMs = now
            )
        )
    }

    @Test
    fun nowEqualsExpiryIsLocked() {
        assertEquals(
            OtpGateStatus.LOCKED,
            OtpGateLogic.evaluate(
                hasToken = true,
                expiresAtMs = now,
                lastRenewOkMs = now,
                nowMs = now
            )
        )
    }

    @Test
    fun freshTokenIsAuthenticated() {
        // Renewed 1h ago, not expired -> AUTHENTICATED.
        assertEquals(
            OtpGateStatus.AUTHENTICATED,
            OtpGateLogic.evaluate(
                hasToken = true,
                expiresAtMs = now + OtpGateLogic.TOKEN_TTL_MS,
                lastRenewOkMs = now - 3_600_000L,
                nowMs = now
            )
        )
    }

    @Test
    fun staleButNotExpiredNeedsRenew() {
        // Renewed 2 days ago, still within TTL -> NEEDS_RENEW.
        assertEquals(
            OtpGateStatus.NEEDS_RENEW,
            OtpGateLogic.evaluate(
                hasToken = true,
                expiresAtMs = now + OtpGateLogic.TOKEN_TTL_MS,
                lastRenewOkMs = now - (2 * OtpGateLogic.RENEW_AFTER_MS),
                nowMs = now
            )
        )
    }

    @Test
    fun renewBoundaryExactlyRenewAfterNeedsRenew() {
        // delta == RENEW_AFTER_MS -> NEEDS_RENEW (boundary is inclusive).
        assertEquals(
            OtpGateStatus.NEEDS_RENEW,
            OtpGateLogic.evaluate(
                hasToken = true,
                expiresAtMs = now + OtpGateLogic.TOKEN_TTL_MS,
                lastRenewOkMs = now - OtpGateLogic.RENEW_AFTER_MS,
                nowMs = now
            )
        )
    }

    @Test
    fun justUnderRenewBoundaryIsAuthenticated() {
        // delta == RENEW_AFTER_MS - 1 -> still AUTHENTICATED.
        assertEquals(
            OtpGateStatus.AUTHENTICATED,
            OtpGateLogic.evaluate(
                hasToken = true,
                expiresAtMs = now + OtpGateLogic.TOKEN_TTL_MS,
                lastRenewOkMs = now - (OtpGateLogic.RENEW_AFTER_MS - 1L),
                nowMs = now
            )
        )
    }
}
