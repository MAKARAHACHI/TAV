package com.followupnadlan.followuplog

data class FollowUpLogEntry(
    val actionType: FollowUpActionType,
    val timestampEpochMs: Long,
    val messagePreview: String
)

enum class FollowUpActionType {
    WHATSAPP_OPENED,
    SHARE_OPENED,
    COPY_USED
}
