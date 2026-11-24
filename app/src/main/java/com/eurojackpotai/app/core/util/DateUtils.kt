package com.eurojackpotai.app.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** EuroJackpot draws happen every Tuesday and Friday evening (CET/CEST). */
object DrawSchedule {
    private val drawDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)

    fun nextDrawDateTime(from: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val drawTime = from.toLocalDate().atTime(21, 0)
        var candidate = if (from.isBefore(drawTime) && from.dayOfWeek in drawDays) drawTime else drawTime.plusDays(1)
        while (candidate.dayOfWeek !in drawDays || candidate.isBefore(from)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }
}

object DateFormatters {
    val displayDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Used for the compact draw-date chips in "My Numbers" (e.g. "9/22 (Tue)") — "E"
    // localizes the weekday abbreviation to the device locale on its own, so this needs
    // no separate KO/EN string resource.
    private val shortDateWithWeekday: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d (E)")

    // For the "last updated" timestamp on Home/Results — includes time-of-day (unlike
    // shortDate) since a sync can happen more than once on the same calendar day.
    private val dateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun formatDisplay(date: LocalDate): String = date.format(displayDate)
    fun formatShort(date: LocalDate): String = date.format(shortDate)
    fun formatShortWithWeekday(date: LocalDate): String = date.format(shortDateWithWeekday)
    fun formatDateTime(dateTime: LocalDateTime): String = dateTime.format(this.dateTime)
}
