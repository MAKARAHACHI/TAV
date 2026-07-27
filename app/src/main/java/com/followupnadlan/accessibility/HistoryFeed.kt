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
        // The "לא ענו" moment shares the ended send path, so its rows are told apart by source,
        // not action type. A no-answer-sourced send outranks the action-type mapping below.
        if (entry.source == NoAnswerFollowUpMessage.SOURCE && entry.actionType.isClientFacingSend()) {
            return HistoryMoment.NO_ANSWER to "נשלחה הודעת \"לא ענו\""
        }
        return when (entry.actionType) {
            FollowUpActionType.AUTO_SMS_SENT,
            FollowUpActionType.WHATSAPP_AUTO_SENT,
            FollowUpActionType.FALLBACK_SMS_SENT,
            FollowUpActionType.WHATSAPP_REPLY_OPENED ->
                HistoryMoment.MISSED to "נשלחה הודעת \"לא עניתי\""

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
 * A client-facing follow-up SEND — a message the client actually received across any of the three
 * moments (missed / ended-card / no-answer), auto or manual, WhatsApp or SMS. Internal steps
 * (permission prompts, diagnostics, skips, failures, "prepared/pending" states) are excluded, so
 * both the activity log and the home today-count count only what reached a client.
 */
internal fun FollowUpActionType.isClientFacingSend(): Boolean = when (this) {
    FollowUpActionType.AUTO_SMS_SENT,
    FollowUpActionType.WHATSAPP_AUTO_SENT,
    FollowUpActionType.FALLBACK_SMS_SENT,
    FollowUpActionType.WHATSAPP_REPLY_OPENED,
    FollowUpActionType.CONTACT_CARD_OPENED -> true
    else -> false
}
