package com.followupnadlan.missedcall

import com.followupnadlan.BuildConfig

enum class DebugMissedCallSimulationResult {
    TRIGGERED,
    EMPTY_NUMBER,
    RELEASE_BUILD_BLOCKED
}

class DebugMissedCallSimulator(
    private val isDebugBuild: Boolean = BuildConfig.DEBUG,
    private val onSimulate: (MissedCallCandidate) -> Unit
) {
    fun simulate(phoneNumber: String): DebugMissedCallSimulationResult {
        if (!isDebugBuild) {
            return DebugMissedCallSimulationResult.RELEASE_BUILD_BLOCKED
        }

        val trimmedPhone = phoneNumber.trim()
        if (trimmedPhone.isBlank()) {
            return DebugMissedCallSimulationResult.EMPTY_NUMBER
        }

        onSimulate(
            MissedCallCandidate(
                phoneNumber = trimmedPhone,
                direction = MissedCallDirection.INCOMING,
                wasAnswered = false,
                source = SOURCE
            )
        )
        return DebugMissedCallSimulationResult.TRIGGERED
    }

    companion object {
        const val SOURCE = "debug_missed_call_simulator"
    }
}
