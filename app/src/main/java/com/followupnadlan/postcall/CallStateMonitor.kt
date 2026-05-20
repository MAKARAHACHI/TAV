package com.followupnadlan.postcall

class CallStateMonitor(
    private val minCallDurationSeconds: Long = DEFAULT_MIN_CALL_DURATION_SECONDS,
    private val onCallEnded: (durationSeconds: Long) -> Unit
) {
    enum class CallState {
        IDLE,
        RINGING,
        OFFHOOK
    }

    private var lastState: CallState = CallState.IDLE
    private var offhookStartedAtMillis: Long? = null

    fun onStateChanged(newState: CallState, nowMillis: Long) {
        if (newState == lastState) return

        val previousState = lastState
        lastState = newState

        when (newState) {
            CallState.OFFHOOK -> {
                offhookStartedAtMillis = nowMillis
            }
            CallState.IDLE -> {
                val startedAt = offhookStartedAtMillis
                offhookStartedAtMillis = null
                if (previousState == CallState.OFFHOOK && startedAt != null) {
                    val durationSeconds = ((nowMillis - startedAt) / MILLIS_PER_SECOND).coerceAtLeast(0)
                    if (durationSeconds >= minCallDurationSeconds) {
                        onCallEnded(durationSeconds)
                    }
                }
            }
            CallState.RINGING -> Unit
        }
    }

    private companion object {
        const val DEFAULT_MIN_CALL_DURATION_SECONDS = 5L
        const val MILLIS_PER_SECOND = 1000L
    }
}
