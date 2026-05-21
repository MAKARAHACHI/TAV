package com.followupnadlan.snooze

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object SnoozeTimeCalculator {
    private val tonightTime: LocalTime = LocalTime.of(20, 0)
    private val tomorrowMorningTime: LocalTime = LocalTime.of(9, 0)

    fun computeTriggerAt(option: SnoozeOption, nowMillis: Long, zoneId: ZoneId): Long {
        val nowInstant = Instant.ofEpochMilli(nowMillis)
        val now = nowInstant.atZone(zoneId)

        return when (option) {
            SnoozeOption.IN_15_MIN -> nowInstant.plus(15, ChronoUnit.MINUTES).toEpochMilli()
            SnoozeOption.IN_30_MIN -> nowInstant.plus(30, ChronoUnit.MINUTES).toEpochMilli()
            SnoozeOption.IN_1_HOUR -> nowInstant.plus(1, ChronoUnit.HOURS).toEpochMilli()
            SnoozeOption.TONIGHT -> {
                val todayTonight = now.toLocalDate().atTime(tonightTime).atZone(zoneId)
                val trigger = if (todayTonight.toInstant().isAfter(nowInstant)) {
                    todayTonight
                } else {
                    todayTonight.plusDays(1)
                }
                trigger.toInstant().toEpochMilli()
            }
            SnoozeOption.TOMORROW_MORNING -> now
                .toLocalDate()
                .plusDays(1)
                .atTime(tomorrowMorningTime)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }
    }
}
