package com.followupnadlan.accessibility

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Maps raw FollowUp log entries into the friendly "מה קרה היום" rows from the design.
 * Only outcomes meaningful to a deaf/hard-of-hearing user are shown; internal/debug
 * entries are filtered out. No new logging is produced here — this is a read view.
 */
data class ActivityRow(
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val phone: String,
    val time: String
)

object ActivityFeed {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun rows(entries: List<FollowUpLogEntry>, zoneId: ZoneId = ZoneId.systemDefault()): List<ActivityRow> =
        entries
            .sortedByDescending { it.timestampEpochMs }
            .mapNotNull { entry -> friendly(entry)?.let { (icon, tint, title) ->
                ActivityRow(
                    icon = icon,
                    iconTint = tint,
                    title = title,
                    phone = entry.phone,
                    time = formatTime(entry.timestampEpochMs, zoneId)
                )
            } }

    private fun friendly(entry: FollowUpLogEntry): Triple<ImageVector, Color, String>? = when (entry.actionType) {
        FollowUpActionType.MISSED_CALL_DETECTED ->
            Triple(AccessibilityIcons.PhoneMissed, AccessibilityColors.Danger, "זוהתה שיחה שלא נענתה")

        FollowUpActionType.WHATSAPP_AUTO_SENT,
        FollowUpActionType.WHATSAPP_REPLY_OPENED,
        FollowUpActionType.WHATSAPP_OPENED ->
            Triple(AccessibilityIcons.Chat, AccessibilityColors.GreenBright, "נשלחה הודעת WhatsApp")

        FollowUpActionType.AUTO_SMS_SENT,
        FollowUpActionType.FALLBACK_SMS_SENT,
        FollowUpActionType.FALLBACK_SMS_OPENED ->
            Triple(AccessibilityIcons.Sms, AccessibilityColors.Primary, "נשלח SMS כגיבוי")

        FollowUpActionType.AUTO_SMS_SKIPPED_DUPLICATE ->
            Triple(AccessibilityIcons.Schedule, AccessibilityColors.Warning, "לא נשלח בגלל הגנת כפילויות")

        FollowUpActionType.MANUAL_REPLY_OPENED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "המשתמש ביטל")

        FollowUpActionType.AUTO_SMS_SKIPPED_EXCLUDED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — בחרת לא לשלוח למספר הזה")

        FollowUpActionType.AUTO_SMS_SKIPPED_CONTACTS_ONLY ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — אין הרשאת אנשי קשר כדי לוודא שהמספר שמור.")

        FollowUpActionType.AUTO_SMS_SKIPPED_NO_NUMBER,
        FollowUpActionType.AUTO_SMS_SKIPPED_NO_PERMISSION,
        FollowUpActionType.AUTO_SMS_SKIPPED_DISABLED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — המספר לא מתאים להגדרות השליחה")

        else -> null
    }

    private fun formatTime(epochMs: Long, zoneId: ZoneId): String =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalTime().format(timeFormatter)
}
