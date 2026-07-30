package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.postcall.NoAnswerFollowUpMessage
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Which trust moment produced this history row (missed incoming / call ended / no-answer outgoing). */
internal enum class HistoryMoment { MISSED, ENDED, NO_ANSWER }

internal data class HistoryRow(
    val moment: HistoryMoment,
    val phone: String,
    val displayPhone: String,
    val contactName: String?,
    val summary: String,
    val time: String,
    val dateGroup: String,
    val timestampEpochMs: Long
)

/**
 * Maps the existing FollowUp action log into the "יומן פעילות" cards (history.html):
 * grouped by day, one row per completed send/skip for the two moments. Internal steps
 * (permission prompts, diagnostics) are filtered out — only what the client actually saw.
 */
internal object HistoryFeed {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun rows(
        entries: List<FollowUpLogEntry>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId),
        nameLookup: (String) -> String? = { null }
    ): List<HistoryRow> =
        entries
            .sortedByDescending { it.timestampEpochMs }
            .mapNotNull { entry -> toRow(entry, zoneId, today, nameLookup) }

    private fun toRow(
        entry: FollowUpLogEntry,
        zoneId: ZoneId,
        today: LocalDate,
        nameLookup: (String) -> String?
    ): HistoryRow? {
        val (moment, summary) = classify(entry) ?: return null
        val instant = Instant.ofEpochMilli(entry.timestampEpochMs)
        val zoned = instant.atZone(zoneId)
        return HistoryRow(
            moment = moment,
            phone = entry.phone,
            displayPhone = if (entry.phone.isBlank()) "" else PhoneNumberNormalizer.toLocalIsraeliDisplay(entry.phone),
            contactName = entry.phone.takeIf { it.isNotBlank() }?.let(nameLookup),
            summary = summary,
            time = zoned.toLocalTime().format(timeFormatter),
            dateGroup = dateGroupLabel(zoned.toLocalDate(), today),
            timestampEpochMs = entry.timestampEpochMs
        )
    }

    private fun classify(entry: FollowUpLogEntry): Pair<HistoryMoment, String>? {
        // The "לא ענו" moment shares the missed send path, so its rows are told apart by source,
        // not action type. A no-answer-sourced row outranks the action-type mapping below for the
        // MOMENT only — the honest sent-vs-opened wording (below) still depends on the actual action
        // type: opened is never worded as sent, even under the no-answer source.
        if (entry.source == NoAnswerFollowUpMessage.SOURCE) {
            return when (entry.actionType) {
                FollowUpActionType.AUTO_SMS_SENT,
                FollowUpActionType.WHATSAPP_AUTO_SENT,
                FollowUpActionType.FALLBACK_SMS_SENT ->
                    HistoryMoment.NO_ANSWER to "נשלחה הודעת \"לא ענו\""

                FollowUpActionType.WHATSAPP_REPLY_OPENED ->
                    HistoryMoment.NO_ANSWER to "WhatsApp נפתח (לחץ שלח)"

                else -> null
            }
        }
        return when (entry.actionType) {
            FollowUpActionType.AUTO_SMS_SENT,
            FollowUpActionType.WHATSAPP_AUTO_SENT,
            FollowUpActionType.FALLBACK_SMS_SENT ->
                HistoryMoment.MISSED to "נשלחה הודעת \"לא עניתי\""

            // Opened WhatsApp with the chat prepared — NOT proof of a send (§2). The user still has
            // to press send, and for a number without WhatsApp the chat may not even resolve.
            FollowUpActionType.WHATSAPP_REPLY_OPENED ->
                HistoryMoment.MISSED to "WhatsApp נפתח (לחץ שלח)"

            FollowUpActionType.CONTACT_CARD_OPENED ->
                HistoryMoment.ENDED to "נשלחו פרטי קשר"

            else -> null
        }
    }

    private fun dateGroupLabel(date: LocalDate, today: LocalDate): String = when {
        date.isEqual(today) -> "היום"
        date.isEqual(today.minusDays(1)) -> "אתמול"
        else -> DateTimeFormatter.ofPattern("d.M.yyyy").format(date)
    }
}

/**
 * A client-facing follow-up SEND — a message best-available evidence says the client actually
 * received, across any of the three moments (missed / ended-card / no-answer), auto or manual,
 * WhatsApp or SMS. Internal steps (permission prompts, diagnostics, skips, failures) are excluded,
 * AND so is merely OPENING WhatsApp (§2: opened != sent — WhatsApp exposes no delivery receipt, and
 * the user still has to press send themselves) so both the activity log and the home today-count
 * count only what reached a client, never what was merely prepared/opened.
 */
internal fun FollowUpActionType.isClientFacingSend(): Boolean = when (this) {
    FollowUpActionType.AUTO_SMS_SENT,
    FollowUpActionType.WHATSAPP_AUTO_SENT,
    FollowUpActionType.FALLBACK_SMS_SENT,
    FollowUpActionType.CONTACT_CARD_OPENED -> true
    else -> false
}
