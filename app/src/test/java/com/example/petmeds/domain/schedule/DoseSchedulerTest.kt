package com.example.petmeds.domain.schedule

import com.example.petmeds.domain.model.LocalTimeStr
import com.example.petmeds.domain.model.LifecycleStatus
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.ScheduleConfig
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test

class DoseSchedulerTest {

    private val zone = TimeZone.of("America/New_York")
    private val scheduler = DoseScheduler()

    private fun med(
        schedule: ScheduleConfig,
        start: LocalDate = LocalDate(2026, 1, 1),
        end: LocalDate? = null,
    ) = Medication(
        id = 1, petId = 1, name = "Carprofen 25mg",
        dosageAmount = 1.0, dosageUnit = "tablet", form = MedForm.PILL,
        schedule = schedule, startDate = start, endDate = end,
        notes = null, status = LifecycleStatus.ACTIVE, createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun `daily times produces two slots per day in horizon`() {
        val now = Instant.parse("2026-01-01T12:00:00Z") // 7am ET
        val m = med(
            ScheduleConfig.DailyTimes(listOf(LocalTimeStr.of(8, 0), LocalTimeStr.of(20, 0))),
        )

        val slots = scheduler.computeUpcoming(m, now, horizonDays = 2, zone = zone)

        assertThat(slots).hasSize(6) // today (8/20) + +1 (8/20) + +2 (8/20)
        assertThat(slots.first()).isEqualTo(Instant.parse("2026-01-01T13:00:00Z")) // 8am ET
    }

    @Test
    fun `past slots today are filtered out`() {
        val now = Instant.parse("2026-01-01T15:00:00Z") // 10am ET
        val m = med(ScheduleConfig.DailyTimes(listOf(LocalTimeStr.of(8, 0), LocalTimeStr.of(20, 0))))

        val slots = scheduler.computeUpcoming(m, now, horizonDays = 0, zone = zone)

        // only the 8pm slot remains today
        assertThat(slots).containsExactly(Instant.parse("2026-01-02T01:00:00Z"))
    }

    @Test
    fun `prn returns empty`() {
        val now = Instant.parse("2026-01-01T12:00:00Z")
        val slots = scheduler.computeUpcoming(med(ScheduleConfig.Prn), now, zone = zone)
        assertThat(slots).isEmpty()
    }

    @Test
    fun `inactive medication returns empty`() {
        val now = Instant.parse("2026-01-01T12:00:00Z")
        val m = med(ScheduleConfig.DailyTimes(listOf(LocalTimeStr.of(8, 0)))).copy(status = LifecycleStatus.COMPLETED)
        assertThat(scheduler.computeUpcoming(m, now, zone = zone)).isEmpty()
    }

    @Test
    fun `end date is inclusive`() {
        val now = Instant.parse("2026-01-01T05:00:00Z") // midnight ET
        val m = med(
            ScheduleConfig.DailyTimes(listOf(LocalTimeStr.of(8, 0))),
            start = LocalDate(2026, 1, 1),
            end = LocalDate(2026, 1, 2),
        )
        val slots = scheduler.computeUpcoming(m, now, horizonDays = 7, zone = zone)
        assertThat(slots).hasSize(2)
        assertThat(slots.last()).isEqualTo(Instant.parse("2026-01-02T13:00:00Z"))
    }

    @Test
    fun `interval hours produces fixed-clock slots anchored at start`() {
        val now = Instant.parse("2026-01-01T12:00:00Z") // 7am ET
        val m = med(
            ScheduleConfig.IntervalHours(intervalHours = 8, anchor = LocalTimeStr.of(8, 0)),
            start = LocalDate(2026, 1, 1),
        )

        val slots = scheduler.computeUpcoming(m, now, horizonDays = 0, zone = zone)

        // Anchor 8am ET = 13:00 UTC; +8h = 21:00 UTC; +16h = 05:00 UTC next day. Today range only.
        assertThat(slots).containsExactly(
            Instant.parse("2026-01-01T13:00:00Z"),
            Instant.parse("2026-01-01T21:00:00Z"),
        ).inOrder()
    }

    @Test
    fun `specific days only fires on selected weekdays`() {
        // Thu Jan 1 2026
        val now = Instant.parse("2026-01-01T05:00:00Z")
        val m = med(
            ScheduleConfig.SpecificDays(
                times = listOf(LocalTimeStr.of(9, 0)),
                daysOfWeek = setOf(1, 3, 5), // Mon/Wed/Fri
            ),
            start = LocalDate(2026, 1, 1),
        )

        val slots = scheduler.computeUpcoming(m, now, horizonDays = 4, zone = zone)

        // Window Thu(1)..Mon(5): only Fri (Jan 2) and Mon (Jan 5) qualify.
        assertThat(slots).containsExactly(
            Instant.parse("2026-01-02T14:00:00Z"),
            Instant.parse("2026-01-05T14:00:00Z"),
        ).inOrder()
    }

    @Test
    fun `month rollover works`() {
        val now = Instant.parse("2026-01-31T05:00:00Z") // midnight ET on Jan 31
        val m = med(
            ScheduleConfig.DailyTimes(listOf(LocalTimeStr.of(8, 0))),
            start = LocalDate(2026, 1, 31),
        )

        val slots = scheduler.computeUpcoming(m, now, horizonDays = 1, zone = zone)

        assertThat(slots).containsExactly(
            Instant.parse("2026-01-31T13:00:00Z"),
            Instant.parse("2026-02-01T13:00:00Z"),
        ).inOrder()
    }

    @Test
    fun `dst spring-forward keeps wall-clock time stable for daily times`() {
        // US DST 2026: Sun Mar 8 02:00 -> 03:00. 8am ET should still be 8am ET.
        val now = Instant.parse("2026-03-07T05:00:00Z") // midnight ET Mar 7
        val m = med(
            ScheduleConfig.DailyTimes(listOf(LocalTimeStr.of(8, 0))),
            start = LocalDate(2026, 3, 7),
        )

        val slots = scheduler.computeUpcoming(m, now, horizonDays = 2, zone = zone)

        // Mar 7 (EST/UTC-5): 8am ET = 13:00 UTC. Mar 8 & 9 (EDT/UTC-4): 8am ET = 12:00 UTC.
        assertThat(slots).containsExactly(
            Instant.parse("2026-03-07T13:00:00Z"),
            Instant.parse("2026-03-08T12:00:00Z"),
            Instant.parse("2026-03-09T12:00:00Z"),
        ).inOrder()
    }
}
