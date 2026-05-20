package com.followupnadlan.followuplog

data class FollowUpLogEntry(
    val leadName: String,
    val phoneNumber: String,
    val templateId: String,
    val templateTitle: String,
    val propertyName: String,
    val propertyLink: String,
    val messagePreview: String,
    val actionType: FollowUpActionType,
    val timestampEpochMs: Long
)

enum class FollowUpActionType {
    WHATSAPP_OPENED,
    SHARE_OPENED,
    COPY_USED
}
