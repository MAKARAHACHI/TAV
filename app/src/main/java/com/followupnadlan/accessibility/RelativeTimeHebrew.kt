package com.followupnadlan.accessibility

/**
 * Human Hebrew relative time for the approval sheet's context sub-line ("לפני דקה" / "לפני 3 דקות"
 * / "לפני שעה"). Pure and tested — it takes the two epoch-millis it needs and returns the phrase,
 * so it never reads a clock itself. Used by the pure approval bottom-sheet to fill {X} in
 * "שיחה שלא נענתה לפני {X}" and friends.
 *
 * Buckets, coarse on purpose (the sheet wants a glanceable "how long ago", not a stopwatch):
 * under a minute → "עכשיו"; a single minute → "לפני דקה"; 2–59 minutes → "לפני N דקות";
 * a single hour → "לפני שעה"; 2–23 hours → "לפני N שעות"; a single day → "אתמול";
 * more → "לפני N ימים". A future or equal timestamp is clamped to "עכשיו".
 */
object RelativeTimeHebrew {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val DAY_MS = 24 * HOUR_MS

    fun of(thenMs: Long, nowMs: Long): String {
        val elapsed = nowMs - thenMs
        if (elapsed < MINUTE_MS) return "עכשיו"

        val minutes = elapsed / MINUTE_MS
        if (minutes < 60) return if (minutes == 1L) "לפני דקה" else "לפני $minutes דקות"

        val hours = elapsed / HOUR_MS
        if (hours < 24) return if (hours == 1L) "לפני שעה" else "לפני $hours שעות"

        val days = elapsed / DAY_MS
        return if (days == 1L) "אתמול" else "לפני $days ימים"
    }
}
