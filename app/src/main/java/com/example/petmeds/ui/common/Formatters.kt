package com.example.petmeds.ui.common

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Formats an Instant in the system timezone as "HH:mm" (24h). */
fun formatTime(instant: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val t: LocalTime = instant.toLocalDateTime(zone).time
    return "%02d:%02d".format(t.hour, t.minute)
}

fun formatDayHeader(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today, ${date.dayOfMonth} ${date.month.name.lowercase().replaceFirstChar { it.titlecase() }}"
    date == today.plusDays(1) -> "Tomorrow"
    date.year == today.year ->
        "${date.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }}, " +
                "${date.dayOfMonth} ${date.month.name.lowercase().replaceFirstChar { it.titlecase() }}"
    else -> "${date.dayOfMonth} ${date.month.name.lowercase().replaceFirstChar { it.titlecase() }} ${date.year}"
}

private fun LocalDate.plusDays(days: Int): LocalDate =
    this.plus(days, DateTimeUnit.DAY)

/** "1.5 ml" or "1 tablet" with no trailing zero noise. */
fun formatDosage(amount: Double, unit: String): String {
    val a = if (amount == amount.toLong().toDouble()) amount.toLong().toString()
    else amount.toString()
    return "$a $unit"
}
