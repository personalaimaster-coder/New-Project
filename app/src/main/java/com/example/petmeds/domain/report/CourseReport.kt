package com.example.petmeds.domain.report

import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.CourseNote
import com.example.petmeds.domain.model.DoseLog
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.Pet
import kotlinx.datetime.Instant

/**
 * Pure-data structure rendered into the PDF. Constructed by [CourseReportBuilder]
 * and serialized to HTML by `CourseReportHtml`. Keeping it free of Android types
 * makes the rendering layer trivially unit-testable.
 */
data class CourseReport(
    val pet: Pet,
    val course: Course,
    val medications: List<MedicationReportRow>,
    val notes: List<CourseNote>,
    val generatedAt: Instant,
)

data class MedicationReportRow(
    val medication: Medication,
    val scheduleSummary: String,
    val adherence: AdherenceStats,
    val recentDoses: List<DoseLog>,
)
