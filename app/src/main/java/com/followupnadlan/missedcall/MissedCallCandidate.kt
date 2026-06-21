package com.followupnadlan.missedcall

data class MissedCallCandidate(
    val phoneNumber: String?,
    val direction: MissedCallDirection,
    val wasAnswered: Boolean,
    val source: String = MissedCallAutoResponseSettings.SOURCE
)
