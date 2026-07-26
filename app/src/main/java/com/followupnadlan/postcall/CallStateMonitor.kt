package com.followupnadlan.postcall

/**
 * Reports call-state transitions. Every call that reaches IDLE is reported through [onCallEnded] —
 * answered or not, incoming or outgoing — and the monitor holds no duration threshold of its own.
 *
 * It used to hold one, and it used to fire only after OFFHOOK. Both were filters in the wrong
 * place: this class times OFFHOOK wall-clock while [EndedSuggestionDecider] reads the duration the
 * call log actually recorded, so a gate here could silently drop calls the real rule would have
 * allowed. All eligibility now lives in the decider; the monitor only observes.
 *
 * [onMissedIncomingCall] is unchanged and still fires only for a genuinely missed incoming call —
 * it drives the missed-call auto-reply, which is a different feature with different rules.
 */
class CallStateMonitor(
    private val onCallEnded: (durationSeconds: Long) -> Unit,
    private val onMissedIncomingCall: () -> Unit = {}
) {
    enum class CallState {
        IDLE,
        RINGING,
        OFFHOOK
    }

    private var lastState: CallState = CallState.IDLE
    private var offhookStartedAtMillis: Long? = null
    private var incomingRang = false
    private var answeredDuringCurrentCall = false

    fun onStateChanged(newState: CallState, nowMillis: Long) {
        if (newState == lastState) return

        val previousState = lastState
        lastState = newState

        when (newState) {
            CallState.OFFHOOK -> {
                offhookStartedAtMillis = nowMillis
                answeredDuringCurrentCall = true
            }
            CallState.IDLE -> {
                val startedAt = offhookStartedAtMillis
                offhookStartedAtMillis = null
                // A ring nobody picked up ends a call too, and it is the strongest follow-up case
                // there is — so RINGING -> IDLE reports as well, with a duration of zero.
                val callHappened = previousState == CallState.OFFHOOK || previousState == CallState.RINGING
                if (callHappened) {
                    val durationSeconds = startedAt
                        ?.let { ((nowMillis - it) / MILLIS_PER_SECOND).coerceAtLeast(0) }
                        ?: 0L
                    onCallEnded(durationSeconds)
                }
                if (incomingRang && !answeredDuringCurrentCall) {
                    onMissedIncomingCall()
                }
                incomingRang = false
                answeredDuringCurrentCall = false
            }
            CallState.RINGING -> {
                incomingRang = true
                answeredDuringCurrentCall = false
            }
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
