package com.followupnadlan.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin client for the LIVE WhatsApp-OTP server. Public endpoints — NO api-key ever sent from the
 * app. HTTP status codes are mapped to typed results; any exception/timeout becomes NetworkError.
 * §2 honesty: requestCode returns Sent ONLY on 200 {"sent":true}; every other outcome is surfaced
 * truthfully so the UI never claims a code was sent when it wasn't.
 */
class OtpApiClient(
    private val client: OkHttpClient = defaultClient
) {
    sealed interface RequestResult {
        object Sent : RequestResult
        object RateLimited : RequestResult
        object SendFailed : RequestResult
        object NetworkError : RequestResult
    }

    sealed interface VerifyResult {
        data class Success(val token: String, val expiresAtMs: Long) : VerifyResult
        object Invalid : VerifyResult
        object NetworkError : VerifyResult
    }

    sealed interface RenewResult {
        data class Renewed(val token: String, val expiresAtMs: Long) : RenewResult
        object Revoked : RenewResult
        object NetworkError : RenewResult
    }

    /** POST /otp/request. 200 sent:true -> Sent; 429 -> RateLimited; 502 -> SendFailed; else NetworkError. */
    suspend fun requestCode(phone972: String): RequestResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("phone", phone972).toString()
        try {
            client.newCall(post("$BASE_URL/otp/request", body)).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val sent = runCatching { JSONObject(response.body?.string().orEmpty()).optBoolean("sent", false) }
                            .getOrDefault(false)
                        if (sent) RequestResult.Sent else RequestResult.SendFailed
                    }
                    429 -> RequestResult.RateLimited
                    502 -> RequestResult.SendFailed
                    else -> RequestResult.NetworkError
                }
            }
        } catch (t: Throwable) {
            RequestResult.NetworkError
        }
    }

    /** POST /otp/verify. 200 -> Success(token,expiresAt); 401 -> Invalid; else NetworkError. */
    suspend fun verify(phone972: String, code: String): VerifyResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("phone", phone972).put("code", code).toString()
        try {
            client.newCall(post("$BASE_URL/otp/verify", body)).execute().use { response ->
                when (response.code) {
                    200 -> parseToken(response.body?.string())
                        ?.let { VerifyResult.Success(it.first, it.second) }
                        ?: VerifyResult.NetworkError
                    401 -> VerifyResult.Invalid
                    else -> VerifyResult.NetworkError
                }
            }
        } catch (t: Throwable) {
            VerifyResult.NetworkError
        }
    }

    /** POST /otp/renew. 200 -> Renewed(token,expiresAt); 401 -> Revoked; else NetworkError. */
    suspend fun renew(token: String): RenewResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("token", token).toString()
        try {
            client.newCall(post("$BASE_URL/otp/renew", body)).execute().use { response ->
                when (response.code) {
                    200 -> parseToken(response.body?.string())
                        ?.let { RenewResult.Renewed(it.first, it.second) }
                        ?: RenewResult.NetworkError
                    401 -> RenewResult.Revoked
                    else -> RenewResult.NetworkError
                }
            }
        } catch (t: Throwable) {
            RenewResult.NetworkError
        }
    }

    /** Parses {"token":...,"expiresAt":...}; null when either field is missing/blank. */
    private fun parseToken(raw: String?): Pair<String, Long>? {
        val json = runCatching { JSONObject(raw.orEmpty()) }.getOrNull() ?: return null
        val token = json.optString("token", "")
        if (token.isBlank()) return null
        val expiresAt = json.optLong("expiresAt", 0L)
        if (expiresAt <= 0L) return null
        return token to expiresAt
    }

    private fun post(url: String, jsonBody: String): Request =
        Request.Builder()
            .url(url)
            .header("content-type", JSON_MEDIA_TYPE)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

    companion object {
        const val BASE_URL = "https://otpf.ta-v.com"
        private const val JSON_MEDIA_TYPE = "application/json"

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
