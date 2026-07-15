package com.followupnadlan.accessibility

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Maps raw FollowUp log entries into the friendly "מה קרה היום" rows.
 * Internal/debug steps are filtered out so the Activity screen stays user-facing.
 */
data class ActivityRow(
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val phone: String,
    val time: String,
    val contactName: String? = null
)

object ActivityFeed {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Builds the friendly rows. [nameLookup] resolves a saved contact name for a raw phone
     * (returns null when unknown or without contacts permission) so the row can show name + number.
     */
    fun rows(
        entries: List<FollowUpLogEntry>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nameLookup: (String) -> String? = { null }
    ): List<ActivityRow> =
        entries
            .sortedByDescending { it.timestampEpochMs }
            .mapNotNull { entry ->
                friendly(entry)?.let { (icon, tint, title) ->
                    ActivityRow(
                        icon = icon,
                        iconTint = tint,
                        title = title,
                        phone = if (entry.phone.isBlank()) "" else PhoneNumberNormalizer.toLocalIsraeliDisplay(entry.phone),
                        time = formatTime(entry.timestampEpochMs, zoneId),
                        contactName = entry.phone.takeIf { it.isNotBlank() }?.let(nameLookup)
                    )
                }
            }

    private fun friendly(entry: FollowUpLogEntry): Triple<ImageVector, Color, String>? = when (entry.actionType) {
        FollowUpActionType.MISSED_CALL_DETECTED ->
            Triple(AccessibilityIcons.PhoneMissed, AccessibilityColors.Danger, "זוהתה שיחה שלא נענתה")

        FollowUpActionType.WHATSAPP_AUTO_SENT ->
            Triple(AccessibilityIcons.Chat, AccessibilityColors.GreenBright, "נשלחה הודעת WhatsApp")

        FollowUpActionType.WHATSAPP_REPLY_OPENED,
        FollowUpActionType.WHATSAPP_OPENED,
        FollowUpActionType.MANUAL_WHATSAPP_COMPOSER_OPENED ->
            Triple(AccessibilityIcons.Chat, AccessibilityColors.GreenBright, "נפתחה שיחת WhatsApp")

        FollowUpActionType.AUTO_SMS_SENT,
        FollowUpActionType.FALLBACK_SMS_SENT ->
            Triple(AccessibilityIcons.Sms, AccessibilityColors.Primary, "נשלח SMS כגיבוי")

        FollowUpActionType.FALLBACK_SMS_OPENED,
        FollowUpActionType.MANUAL_SMS_COMPOSER_OPENED ->
            Triple(AccessibilityIcons.Sms, AccessibilityColors.Primary, "נפתחה הודעת SMS לאישור")

        FollowUpActionType.MANUAL_REPLY_PENDING ->
            Triple(AccessibilityIcons.Schedule, AccessibilityColors.Warning, "ממתין לאישור שלך")

        FollowUpActionType.MANUAL_REPLY_CANCELLED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "המשתמש ביטל")

        FollowUpActionType.AUTO_SMS_SKIPPED_EXCLUDED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — בחרת לא לשלוח למספר הזה")

        FollowUpActionType.AUTO_SMS_SKIPPED_BLOCKED_CONTACT ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — אנשי קשר חסומים בהגדרה שלך")

        FollowUpActionType.AUTO_SMS_SKIPPED_BLOCKED_NON_CONTACT ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — מספרים לא שמורים חסומים בהגדרה שלך")

        FollowUpActionType.AUTO_SMS_SKIPPED_FIRST_TIME ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — מספר חדש בפעם הראשונה")

        FollowUpActionType.AUTO_SMS_SKIPPED_CONTACT_TYPE_UNVERIFIED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — אין הרשאת אנשי קשר כדי לוודא את סוג המספר.")

        FollowUpActionType.WHATSAPP_REPLY_FAILED,
        FollowUpActionType.WHATSAPP_AUTO_FAILED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא הצלחנו לפתוח WhatsApp")

        FollowUpActionType.AUTO_SMS_FAILED,
        FollowUpActionType.FALLBACK_SMS_FAILED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא הצלחנו לשלוח SMS")

        FollowUpActionType.AUTO_SMS_SKIPPED_DUPLICATE ->
            Triple(AccessibilityIcons.Schedule, AccessibilityColors.Warning, "לא נשלח בגלל הגנת כפילויות")

        FollowUpActionType.AUTO_SMS_SKIPPED_CONTACTS_ONLY ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — אין הרשאת אנשי קשר כדי לוודא שהמספר שמור")

        FollowUpActionType.AUTO_SMS_SKIPPED_NOT_ALLOWED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — המספר לא נמצא ברשימת המותרים")

        FollowUpActionType.AUTO_SMS_SKIPPED_NO_NUMBER,
        FollowUpActionType.AUTO_SMS_SKIPPED_NO_PERMISSION,
        FollowUpActionType.AUTO_SMS_SKIPPED_DISABLED ->
            Triple(AccessibilityIcons.Block, AccessibilityColors.TextFaint, "לא נשלח — המספר לא מתאים להגדרות השליחה")

        else -> null
    }

    private fun formatTime(epochMs: Long, zoneId: ZoneId): String =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalTime().format(timeFormatter)
}
