package com.example.petmeds.domain.schedule

import com.example.petmeds.domain.model.LocalTimeStr
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.ScheduleConfig
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure-Kotlin scheduler. Computes the next scheduled-dose [Instant]s for a
 * medication. No Android types, no IO — fully unit testable.
 *
 * Semantics chosen (and intentionally documented; PRD was ambiguous):
 *  - "Every N hours" uses fixed clock slots anchored to [IntervalHours.anchor]
 *    on the medication's [Medication.startDate]. This matches how vets prescribe
 *    ("8am / 4pm / midnight") and keeps reminders predictable across days.
 *  - PRN (as needed) returns no scheduled instants.
 *  - End date is inclusive (the last day still has its scheduled doses).
 */
@Singleton
class DoseScheduler @Inject constructor() {

    /**
     * @param med medication to schedule
     * @param now current instant
     * @param horizonDays how far forward to plan
     * @param zone time zone to interpret times-of-day in (default = system)
     * @return list of distinct, future-or-equal instants (sorted ascending)
     */
    fun computeUpcoming(
        med: Medication,
        now: Instant,
        horizonDays: Int = 2,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<Instant> {
        if (!med.isActive) return emptyList()
        if (med.schedule is ScheduleConfig.Prn) return emptyList()

        val today = now.toLocalDateTime(zone).date
        val rangeStart = maxOf(med.startDate, today)
        val horizonEnd = today.plus(horizonDays, DateTimeUnit.DAY)
        val rangeEnd = med.endDate?.let { minOf(it, horizonEnd) } ?: horizonEnd
        if (rangeStart > rangeEnd) return emptyList()

        return when (val s = med.schedule) {
            is ScheduleConfig.DailyTimes ->
                dailySlots(rangeStart, rangeEnd, s.times.map { it.asLocalTime() }, zone)
            is ScheduleConfig.SpecificDays ->
                dailySlots(
                    rangeStart, rangeEnd, s.times.map { it.asLocalTime() }, zone,
                    dayFilter = { it.dayOfWeek.isoDayNumber in s.daysOfWeek }
                )
            is ScheduleConfig.IntervalHours ->
                intervalSlots(med.startDate, rangeStart, rangeEnd, s, zone)
            ScheduleConfig.Prn -> emptyList()
        }
            .filter { it >= now }
            .distinct()
            .sorted()
    }

    private fun dailySlots(
        from: LocalDate,
        to: LocalDate,
        times: List<LocalTime>,
        zone: TimeZone,
        dayFilter: (LocalDate) -> Boolean = { true },
    ): List<Instant> {
        val out = mutableListOf<Instant>()
        var day = from
        while (day <= to) {
            if (dayFilter(day)) {
                for (t in times) {
                    out += LocalDateTime(day, t).toInstant(zone)
                }
            }
            day = day.plus(1, DateTimeUnit.DAY)
        }
        return out
    }

    private fun intervalSlots(
        anchorDate: LocalDate,
        from: LocalDate,
        to: LocalDate,
        s: ScheduleConfig.IntervalHours,
        zone: TimeZone,
    ): List<Instant> {
        require(s.intervalHours in 1..24) { "intervalHours must be 1..24" }
        val anchor = LocalDateTime(anchorDate, s.anchor.asLocalTime()).toInstant(zone)
        val windowStart = from.atStartOfDayIn(zone)
        val windowEnd = to.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val intervalSeconds = s.intervalHours.toLong() * 3600L
        val deltaSeconds = (windowStart - anchor).inWholeSeconds
        val firstK = if (deltaSeconds <= 0) 0L
        else ((deltaSeconds + intervalSeconds - 1) / intervalSeconds)
        val out = mutableListOf<Instant>()
        var k = firstK
        while (true) {
            val cand = anchor.plus((k * intervalSeconds).seconds)
            if (cand >= windowEnd) break
            if (cand >= windowStart) out += cand
            k++
        }
        return out
    }
}

private fun LocalTimeStr.asLocalTime(): LocalTime = LocalTime(hour, minute)

private val DayOfWeek.isoDayNumber: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }
