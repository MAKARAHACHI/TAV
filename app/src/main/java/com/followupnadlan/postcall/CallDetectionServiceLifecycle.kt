package com.followupnadlan.postcall

enum class CallDetectionServiceAction {
    START,
    STOP
}

object CallDetectionServiceLifecycle {
    fun actionFor(
        bridgeEnabled: Boolean,
        readPhoneStateGranted: Boolean,
        readCallLogGranted: Boolean
    ): CallDetectionServiceAction =
        if (bridgeEnabled && readPhoneStateGranted && readCallLogGranted) {
            CallDetectionServiceAction.START
        } else {
            CallDetectionServiceAction.STOP
        }
}
