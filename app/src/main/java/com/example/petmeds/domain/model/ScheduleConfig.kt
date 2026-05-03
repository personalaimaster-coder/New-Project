package com.example.petmeds.domain.model

import kotlinx.serialization.Serializable

/**
 * A medication's recurrence rule. We persist this as JSON in the Room database
 * so adding new variants does not require a schema migration.
 *
 * MVP supports DAILY_TIMES; INTERVAL_HOURS / SPECIFIC_DAYS / PRN are modelled
 * here so the UI and domain layer don't need to change later.
 */
@Serializable
sealed class ScheduleConfig {

    /** Times of day in 24-hour format, e.g. listOf(LocalTimeStr("08:00"), LocalTimeStr("20:00")). */
    @Serializable
    data class DailyTimes(val times: List<LocalTimeStr>) : ScheduleConfig()

    /** Fire every [intervalHours] starting from [anchor] (a fixed clock slot). */
    @Serializable
    data class IntervalHours(val intervalHours: Int, val anchor: LocalTimeStr) : ScheduleConfig()

    /** Specific days of the week (1=Mon..7=Sun) at [times]. */
    @Serializable
    data class SpecificDays(
        val times: List<LocalTimeStr>,
        val daysOfWeek: Set<Int>,
    ) : ScheduleConfig()

    /** As needed — no scheduled alarms; logged ad hoc. */
    @Serializable
    data object Prn : ScheduleConfig()
}

/**
 * Lightweight wrapper around an "HH:mm" string. We avoid serializing
 * kotlinx.datetime.LocalTime directly so the persisted JSON is human-readable
 * and stable across kotlinx-datetime versions.
 */
@Serializable
@JvmInline
value class LocalTimeStr(val value: String) {
    init { require(REGEX.matches(value)) { "Expected HH:mm, got: $value" } }
    val hour: Int get() = value.substring(0, 2).toInt()
    val minute: Int get() = value.substring(3, 5).toInt()
    companion object {
        private val REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        fun of(hour: Int, minute: Int): LocalTimeStr =
            LocalTimeStr("%02d:%02d".format(hour, minute))
    }
}
