package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.followuplog.FollowUpLogEntry
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Which of the two trust moments produced this history row. */
internal enum class HistoryMoment { MISSED, ENDED }

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

    private fun classify(entry: FollowUpLogEntry): Pair<HistoryMoment, String>? = when (entry.actionType) {
        FollowUpActionType.AUTO_SMS_SENT,
        FollowUpActionType.WHATSAPP_AUTO_SENT,
        FollowUpActionType.FALLBACK_SMS_SENT,
        FollowUpActionType.WHATSAPP_REPLY_OPENED ->
            HistoryMoment.MISSED to "נשלחה הודעת \"לא עניתי\""

        FollowUpActionType.CONTACT_CARD_OPENED ->
            HistoryMoment.ENDED to "נשלחו פרטי קשר"

        else -> null
    }

    private fun dateGroupLabel(date: LocalDate, today: LocalDate): String = when {
        date.isEqual(today) -> "היום"
        date.isEqual(today.minusDays(1)) -> "אתמול"
        else -> DateTimeFormatter.ofPattern("d.M.yyyy").format(date)
    }
}
