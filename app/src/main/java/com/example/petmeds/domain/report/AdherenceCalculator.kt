package com.example.petmeds.domain.report

import com.example.petmeds.domain.model.DoseLog
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.domain.model.Medication
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adherence summary for a single medication, derived from its [DoseLog]s.
 *
 * `pending` represents future, not-yet-due doses. They are excluded from the
 * adherence percentage so a course that has not yet finished is not penalised.
 * Past `PENDING` entries (the alarm fired but the user did not interact) are
 * counted as `missed` for the purposes of the report so the doctor sees the
 * worst-case picture.
 */
data class AdherenceStats(
    val scheduled: Int,
    val taken: Int,
    val skipped: Int,
    val missed: Int,
    val pending: Int,
    val takenPct: Double,
) {
    val taken100: Int get() = takenPct.toInt().coerceIn(0, 100)
    val isEmpty: Boolean get() = scheduled == 0
}

@Singleton
class AdherenceCalculator @Inject constructor() {

    fun compute(
        medication: Medication,
        doseLogs: List<DoseLog>,
        now: Instant,
    ): AdherenceStats {
        val forMed = doseLogs.filter { it.medicationId == medication.id }
        if (forMed.isEmpty()) {
            return AdherenceStats(0, 0, 0, 0, 0, 0.0)
        }

        var taken = 0
        var skipped = 0
        var missed = 0
        var pending = 0

        for (log in forMed) {
            when (log.status) {
                DoseStatus.TAKEN -> taken++
                DoseStatus.SKIPPED -> skipped++
                DoseStatus.MISSED -> missed++
                DoseStatus.PENDING -> {
                    val due = log.scheduledAt ?: log.createdAt
                    if (due <= now) missed++ else pending++
                }
            }
        }

        val total = forMed.size
        val countedDenominator = total - pending
        val takenPct = if (countedDenominator > 0) {
            taken.toDouble() / countedDenominator.toDouble() * 100.0
        } else 0.0

        return AdherenceStats(
            scheduled = total,
            taken = taken,
            skipped = skipped,
            missed = missed,
            pending = pending,
            takenPct = takenPct,
        )
    }
}
