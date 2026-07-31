package com.followupnadlan.auth

import android.content.Context

/**
 * Persists the WhatsApp-OTP session in plain SharedPreferences (mirrors MyDetailsStore). The
 * server token is opaque; we store it alongside the verified phone (972 digits), the server-given
 * expiry, the timestamp of the last successful request/renew (drives the 24h silent-renew), and a
 * "revoked" lock flag set only when the server returns 401 on renew.
 */
class AuthTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AuthState {
        val token = preferences.getString(KEY_TOKEN, "").orEmpty()
        return AuthState(
            hasToken = token.isNotBlank(),
            token = token,
            phone = preferences.getString(KEY_PHONE, "").orEmpty(),
            expiresAtMs = preferences.getLong(KEY_EXPIRES_AT_MS, 0L),
            lastRenewOkMs = preferences.getLong(KEY_LAST_RENEW_OK_MS, 0L),
            revokedLocked = preferences.getBoolean(KEY_REVOKED_LOCKED, false)
        )
    }

    /** Store a freshly verified token; clears any previous revoked lock. nowMs = last-success time. */
    fun saveToken(token: String, phone: String, expiresAtMs: Long, nowMs: Long) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_PHONE, phone)
            .putLong(KEY_EXPIRES_AT_MS, expiresAtMs)
            .putLong(KEY_LAST_RENEW_OK_MS, nowMs)
            .putBoolean(KEY_REVOKED_LOCKED, false)
            .apply()
    }

    /** Store a renewed token + expiry and bump the last-success time (keeps the same phone). */
    fun markRenewed(token: String, expiresAtMs: Long, nowMs: Long) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRES_AT_MS, expiresAtMs)
            .putLong(KEY_LAST_RENEW_OK_MS, nowMs)
            .putBoolean(KEY_REVOKED_LOCKED, false)
            .apply()
    }

    /** Set/clear the revoked lock. When revoked, the token is dropped so the gate stays LOCKED. */
    fun markLocked(revoked: Boolean) {
        val editor = preferences.edit().putBoolean(KEY_REVOKED_LOCKED, revoked)
        if (revoked) {
            editor.remove(KEY_TOKEN).remove(KEY_EXPIRES_AT_MS).remove(KEY_LAST_RENEW_OK_MS)
        }
        editor.apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "auth"
        const val KEY_TOKEN = "token"
        const val KEY_PHONE = "phone"
        const val KEY_EXPIRES_AT_MS = "expires_at_ms"
        const val KEY_LAST_RENEW_OK_MS = "last_renew_ok_ms"
        const val KEY_REVOKED_LOCKED = "revoked_locked"
    }
}
