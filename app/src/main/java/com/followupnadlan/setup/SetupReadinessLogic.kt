package com.followupnadlan.setup

data class SetupReadinessInput(
    val notificationsGranted: Boolean,
    val phoneStateGranted: Boolean,
    val callLogGranted: Boolean,
    val contactsGranted: Boolean,
    val detectionEnabled: Boolean,
    val notificationChannelEnabled: Boolean,
    val batteryOptimizationKnown: Boolean,
    val batteryOptimizationAllowsBackground: Boolean
)

object SetupReadinessLogic {
    fun evaluate(input: SetupReadinessInput): SetupReadinessResult {
        val checks = listOf(
            requiredCheck(
                id = CheckId.NOTIFICATIONS,
                passed = input.notificationsGranted,
                actionKey = "notifications"
            ),
            requiredCheck(
                id = CheckId.PHONE_STATE,
                passed = input.phoneStateGranted,
                actionKey = "phone_state"
            ),
            optionalCheck(
                id = CheckId.CALL_LOG,
                passed = input.callLogGranted,
                actionKey = "call_log"
            ),
            optionalCheck(
                id = CheckId.CONTACTS,
                passed = input.contactsGranted,
                actionKey = "contacts"
            ),
            requiredCheck(
                id = CheckId.DETECTION_ENABLED,
                passed = input.detectionEnabled,
                actionKey = "detection_enabled"
            ),
            requiredCheck(
                id = CheckId.CHANNEL_ENABLED,
                passed = input.notificationChannelEnabled,
                actionKey = "channel_enabled"
            ),
            batteryCheck(input)
        )

        return SetupReadinessResult(
            verdict = verdictFor(checks),
            checks = checks
        )
    }

    private fun requiredCheck(
        id: CheckId,
        passed: Boolean,
        actionKey: String
    ): SetupCheckResult = SetupCheckResult(
        id = id,
        state = if (passed) CheckState.PASS else CheckState.FAIL,
        required = true,
        actionKey = actionKey
    )

    private fun optionalCheck(
        id: CheckId,
        passed: Boolean,
        actionKey: String
    ): SetupCheckResult = SetupCheckResult(
        id = id,
        state = if (passed) CheckState.PASS else CheckState.OPTIONAL_MISSING,
        required = false,
        actionKey = actionKey
    )

    private fun batteryCheck(input: SetupReadinessInput): SetupCheckResult {
        val state = when {
            !input.batteryOptimizationKnown -> CheckState.UNKNOWN
            input.batteryOptimizationAllowsBackground -> CheckState.PASS
            else -> CheckState.FAIL
        }
        return SetupCheckResult(
            id = CheckId.BATTERY_OPTIMIZATION,
            state = state,
            required = false,
            actionKey = "battery_optimization"
        )
    }

    private fun verdictFor(checks: List<SetupCheckResult>): ReadinessVerdict {
        val phoneState = checks.first { it.id == CheckId.PHONE_STATE }
        if (phoneState.state == CheckState.FAIL) {
            return ReadinessVerdict.MANUAL_ONLY
        }

        val requiredFailed = checks.any { it.required && it.state == CheckState.FAIL }
        return if (requiredFailed) {
            ReadinessVerdict.PARTIAL
        } else {
            ReadinessVerdict.READY
        }
    }
}
