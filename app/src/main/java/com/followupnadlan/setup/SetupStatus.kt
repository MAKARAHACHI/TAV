package com.followupnadlan.setup

enum class CheckId {
    NOTIFICATIONS,
    PHONE_STATE,
    CALL_LOG,
    CONTACTS,
    DETECTION_ENABLED,
    CHANNEL_ENABLED,
    BATTERY_OPTIMIZATION
}

enum class CheckState {
    PASS,
    FAIL,
    OPTIONAL_MISSING,
    UNKNOWN
}

enum class ReadinessVerdict {
    READY,
    PARTIAL,
    MANUAL_ONLY
}

data class SetupCheckResult(
    val id: CheckId,
    val state: CheckState,
    val required: Boolean,
    val actionKey: String
)

data class SetupReadinessResult(
    val verdict: ReadinessVerdict,
    val checks: List<SetupCheckResult>
) {
    fun check(id: CheckId): SetupCheckResult =
        checks.first { it.id == id }
}
