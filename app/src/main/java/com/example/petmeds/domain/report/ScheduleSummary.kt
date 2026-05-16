package com.example.petmeds.domain.report

import com.example.petmeds.domain.model.ScheduleConfig

/** Human-readable single-line description of a [ScheduleConfig], for the PDF. */
fun ScheduleConfig.toReportSummary(): String = when (this) {
    is ScheduleConfig.DailyTimes -> {
        if (times.isEmpty()) "Daily"
        else "Daily at " + times.joinToString(", ") { it.value }
    }
    is ScheduleConfig.IntervalHours ->
        "Every $intervalHours hours (anchor ${anchor.value})"
    is ScheduleConfig.SpecificDays -> {
        val dayNames = daysOfWeek.sorted().joinToString(", ") { dayName(it) }
        val timeNames = times.joinToString(", ") { it.value }
        "$dayNames at $timeNames"
    }
    ScheduleConfig.Prn -> "As needed (PRN)"
}

private fun dayName(iso: Int): String = when (iso) {
    1 -> "Mon"
    2 -> "Tue"
    3 -> "Wed"
    4 -> "Thu"
    5 -> "Fri"
    6 -> "Sat"
    7 -> "Sun"
    else -> "?"
}
