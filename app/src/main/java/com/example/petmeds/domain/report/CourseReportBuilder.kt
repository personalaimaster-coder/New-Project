package com.example.petmeds.domain.report

import com.example.petmeds.data.repo.CourseNoteRepository
import com.example.petmeds.data.repo.CourseRepository
import com.example.petmeds.data.repo.DoseLogRepository
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.data.repo.PetRepository
import com.example.petmeds.domain.model.DoseLog
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates everything needed for a single course PDF report.
 *
 * Pulled from repositories (not flows) so the result is a snapshot at the
 * moment of "Generate report" rather than a live-updating stream.
 */
@Singleton
class CourseReportBuilder @Inject constructor(
    private val courseRepository: CourseRepository,
    private val courseNoteRepository: CourseNoteRepository,
    private val medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository,
    private val petRepository: PetRepository,
    private val adherenceCalculator: AdherenceCalculator,
    private val clock: Clock,
) {

    suspend fun build(courseId: Long): CourseReport? {
        val course = courseRepository.findById(courseId) ?: return null
        val pet = petRepository.findById(course.petId) ?: return null

        // observeByCourse includes ACTIVE and COMPLETED (excludes DELETED) — exactly what
        // we want on a vet-facing report.
        val medications = medicationRepository.observeByCourse(courseId).first()
        val notes = courseNoteRepository.listForCourse(courseId)
        val now = clock.now()

        val rows = medications.map { med ->
            val logs: List<DoseLog> = doseLogRepository
                .observeForMedication(med.id)
                .first()
            MedicationReportRow(
                medication = med,
                scheduleSummary = med.schedule.toReportSummary(),
                adherence = adherenceCalculator.compute(med, logs, now),
                recentDoses = logs.take(MAX_RECENT_DOSES),
            )
        }

        return CourseReport(
            pet = pet,
            course = course,
            medications = rows,
            notes = notes,
            generatedAt = now,
        )
    }

    companion object {
        private const val MAX_RECENT_DOSES = 30
    }
}
