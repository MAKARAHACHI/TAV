package com.followupnadlan.accessibility

import com.followupnadlan.followuplog.FollowUpLogEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The home value-badge number: how many DISTINCT clients got a follow-up today.
 *
 * Pure and tested (see HomeTodayCountTest). Mirrors [HistoryFeed]'s filtering via
 * [isClientFacingSend] so internal/diagnostic entries (permission prompts, skips, failures,
 * "prepared/pending" states) never inflate the count — only real client-facing sends count, across
 * all three moments (missed / ended-card / no-answer), auto or manual, WhatsApp or SMS.
 *
 * Distinct = by client phone: several sends to the same client today count as one client handled.
 * Entries with a blank phone still count as one send each (they cannot be de-duplicated by client).
 * An empty log ⇒ 0.
 */
internal object HomeTodayCount {
    fun of(
        entries: List<FollowUpLogEntry>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId)
    ): Int {
        val distinctPhones = mutableSetOf<String>()
        var blankPhoneSends = 0
        entries.forEach { entry ->
            if (!entry.actionType.isClientFacingSend()) return@forEach
            val onToday = Instant.ofEpochMilli(entry.timestampEpochMs)
                .atZone(zoneId)
                .toLocalDate()
                .isEqual(today)
            if (!onToday) return@forEach
            if (entry.phone.isBlank()) blankPhoneSends++ else distinctPhones.add(entry.phone)
        }
        return distinctPhones.size + blankPhoneSends
    }
}
