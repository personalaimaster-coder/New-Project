package com.example.petmeds.domain.report

import com.example.petmeds.domain.model.DoseLog
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.domain.model.LifecycleStatus
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.ScheduleConfig
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Test

class AdherenceCalculatorTest {

    private val calc = AdherenceCalculator()
    private val now: Instant = Instant.parse("2026-01-10T12:00:00Z")

    private val medication = Medication(
        id = 7L,
        petId = 1L,
        name = "Carprofen",
        dosageAmount = 1.0,
        dosageUnit = "tablet",
        form = MedForm.PILL,
        schedule = ScheduleConfig.Prn,
        startDate = LocalDate(2026, 1, 1),
        endDate = null,
        notes = null,
        status = LifecycleStatus.ACTIVE,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun log(
        status: DoseStatus,
        scheduledAt: Instant?,
        id: Long = 0L,
        medicationId: Long = medication.id,
    ) = DoseLog(
        id = id,
        medicationId = medicationId,
        scheduledAt = scheduledAt,
        takenAt = if (status == DoseStatus.TAKEN) scheduledAt else null,
        status = status,
        createdAt = scheduledAt ?: Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun `empty log returns zeros`() {
        val stats = calc.compute(medication, emptyList(), now)
        assertThat(stats.scheduled).isEqualTo(0)
        assertThat(stats.takenPct).isEqualTo(0.0)
        assertThat(stats.isEmpty).isTrue()
    }

    @Test
    fun `all taken yields 100 percent`() {
        val logs = listOf(
            log(DoseStatus.TAKEN, Instant.parse("2026-01-09T08:00:00Z")),
            log(DoseStatus.TAKEN, Instant.parse("2026-01-09T20:00:00Z")),
            log(DoseStatus.TAKEN, Instant.parse("2026-01-10T08:00:00Z")),
        )
        val stats = calc.compute(medication, logs, now)
        assertThat(stats.scheduled).isEqualTo(3)
        assertThat(stats.taken).isEqualTo(3)
        assertThat(stats.takenPct).isEqualTo(100.0)
        assertThat(stats.taken100).isEqualTo(100)
    }

    @Test
    fun `mixed log mixes counts and ignores other medications`() {
        val logs = listOf(
            log(DoseStatus.TAKEN, Instant.parse("2026-01-09T08:00:00Z")),
            log(DoseStatus.MISSED, Instant.parse("2026-01-09T20:00:00Z")),
            log(DoseStatus.SKIPPED, Instant.parse("2026-01-10T08:00:00Z")),
            log(DoseStatus.TAKEN, Instant.parse("2026-01-10T08:00:00Z"), medicationId = 999L),
        )
        val stats = calc.compute(medication, logs, now)
        assertThat(stats.scheduled).isEqualTo(3)
        assertThat(stats.taken).isEqualTo(1)
        assertThat(stats.missed).isEqualTo(1)
        assertThat(stats.skipped).isEqualTo(1)
        assertThat(stats.pending).isEqualTo(0)
        assertThat(stats.takenPct).isWithin(0.001).of(100.0 / 3.0)
    }

    @Test
    fun `pending due in the past counts as missed`() {
        val logs = listOf(
            log(DoseStatus.TAKEN, Instant.parse("2026-01-09T08:00:00Z")),
            log(DoseStatus.PENDING, Instant.parse("2026-01-10T08:00:00Z")), // due before now
        )
        val stats = calc.compute(medication, logs, now)
        assertThat(stats.taken).isEqualTo(1)
        assertThat(stats.missed).isEqualTo(1)
        assertThat(stats.pending).isEqualTo(0)
        assertThat(stats.takenPct).isWithin(0.001).of(50.0)
    }

    @Test
    fun `pending due in the future is excluded from the percentage`() {
        val logs = listOf(
            log(DoseStatus.TAKEN, Instant.parse("2026-01-09T08:00:00Z")),
            log(DoseStatus.PENDING, Instant.parse("2026-01-11T08:00:00Z")), // future
        )
        val stats = calc.compute(medication, logs, now)
        assertThat(stats.scheduled).isEqualTo(2)
        assertThat(stats.taken).isEqualTo(1)
        assertThat(stats.pending).isEqualTo(1)
        // denominator excludes the pending future dose
        assertThat(stats.takenPct).isEqualTo(100.0)
    }

    @Test
    fun `all-future-pending course returns zero percent`() {
        val logs = listOf(
            log(DoseStatus.PENDING, Instant.parse("2026-01-11T08:00:00Z")),
            log(DoseStatus.PENDING, Instant.parse("2026-01-12T08:00:00Z")),
        )
        val stats = calc.compute(medication, logs, now)
        assertThat(stats.scheduled).isEqualTo(2)
        assertThat(stats.pending).isEqualTo(2)
        assertThat(stats.takenPct).isEqualTo(0.0)
    }
}
