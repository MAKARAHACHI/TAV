package com.followupnadlan.auth

/**
 * Snapshot of the persisted auth session, returned by [AuthTokenStore.load]. Pure data — no
 * Android types — so [OtpGateLogic] can be tested without a device.
 *
 * @param hasToken true when a non-blank token is stored.
 * @param token the opaque server token (empty when none).
 * @param phone the verified number in 972XXXXXXXXX digits (empty when none).
 * @param expiresAtMs server-supplied expiry (epoch ms); 0 when none.
 * @param lastRenewOkMs epoch ms of the last successful request/renew that produced this token.
 * @param revokedLocked true when the server told us the token was revoked (401 on renew).
 */
data class AuthState(
    val hasToken: Boolean,
    val token: String,
    val phone: String,
    val expiresAtMs: Long,
    val lastRenewOkMs: Long,
    val revokedLocked: Boolean
)
