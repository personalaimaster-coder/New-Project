package com.example.petmeds.domain.report

import com.example.petmeds.data.repo.CourseNoteRepository
import com.example.petmeds.data.repo.CourseRepository
import com.example.petmeds.data.repo.DoseLogRepository
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.data.repo.PetRepository
import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.CourseNote
import com.example.petmeds.domain.model.DoseLog
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.domain.model.LifecycleStatus
import com.example.petmeds.domain.model.LocalTimeStr
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.NoteCategory
import com.example.petmeds.domain.model.Pet
import com.example.petmeds.domain.model.ScheduleConfig
import com.example.petmeds.domain.model.Species
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Test

class CourseReportBuilderTest {

    private val now = Instant.parse("2026-01-10T12:00:00Z")
    private val clock = object : Clock { override fun now(): Instant = now }

    private val pet = Pet(
        id = 1L, name = "Buddy", species = Species.DOG, photoPath = null,
        breed = "Labrador", weightKg = 12.5f,
        birthDate = LocalDate(2020, 5, 1),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
    private val course = Course(
        id = 7L, petId = pet.id, name = "Allergy treatment", colorIndex = 0,
        startDate = LocalDate(2026, 1, 1), endDate = LocalDate(2026, 1, 14),
        notes = "Twice daily until end of week",
        status = LifecycleStatus.ACTIVE, completedAt = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
    private val med = Medication(
        id = 11L, petId = pet.id, courseId = course.id,
        name = "Cetirizine",
        dosageAmount = 0.5, dosageUnit = "tablet", form = MedForm.PILL,
        schedule = ScheduleConfig.DailyTimes(listOf(LocalTimeStr.of(8, 0), LocalTimeStr.of(20, 0))),
        startDate = LocalDate(2026, 1, 1), endDate = LocalDate(2026, 1, 14),
        notes = null, status = LifecycleStatus.ACTIVE, completedAt = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
    private val notes = listOf(
        CourseNote(
            id = 1L, courseId = course.id,
            occurredAt = Instant.parse("2026-01-08T18:00:00Z"),
            category = NoteCategory.OBSERVATION,
            body = "Sneezing reduced",
            photoPath = null,
            createdAt = Instant.parse("2026-01-08T18:00:00Z"),
            updatedAt = Instant.parse("2026-01-08T18:00:00Z"),
        ),
    )
    private val doseLogs = listOf(
        DoseLog(1L, med.id, Instant.parse("2026-01-09T08:00:00Z"), Instant.parse("2026-01-09T08:00:00Z"), DoseStatus.TAKEN, Instant.parse("2026-01-09T08:00:00Z")),
        DoseLog(2L, med.id, Instant.parse("2026-01-09T20:00:00Z"), null, DoseStatus.MISSED, Instant.parse("2026-01-09T20:00:00Z")),
        DoseLog(3L, med.id, Instant.parse("2026-01-10T08:00:00Z"), Instant.parse("2026-01-10T08:00:00Z"), DoseStatus.TAKEN, Instant.parse("2026-01-10T08:00:00Z")),
    )

    @Test
    fun `build aggregates pet course meds notes and adherence`() = runTest {
        val builder = CourseReportBuilder(
            courseRepository = StubCourseRepository(course),
            courseNoteRepository = StubCourseNoteRepository(notes),
            medicationRepository = StubMedicationRepository(listOf(med)),
            doseLogRepository = StubDoseLogRepository(doseLogs),
            petRepository = StubPetRepository(pet),
            adherenceCalculator = AdherenceCalculator(),
            clock = clock,
        )
        val report = builder.build(course.id)
        assertThat(report).isNotNull()
        assertThat(report!!.pet).isEqualTo(pet)
        assertThat(report.course).isEqualTo(course)
        assertThat(report.notes).isEqualTo(notes)
        assertThat(report.medications).hasSize(1)
        val row = report.medications.first()
        assertThat(row.medication).isEqualTo(med)
        assertThat(row.scheduleSummary).contains("08:00")
        assertThat(row.scheduleSummary).contains("20:00")
        assertThat(row.adherence.taken).isEqualTo(2)
        assertThat(row.adherence.missed).isEqualTo(1)
        assertThat(row.adherence.takenPct).isWithin(0.001).of(2.0 / 3.0 * 100.0)
        assertThat(report.generatedAt).isEqualTo(now)
    }

    @Test
    fun `build returns null when course missing`() = runTest {
        val builder = CourseReportBuilder(
            courseRepository = StubCourseRepository(null),
            courseNoteRepository = StubCourseNoteRepository(emptyList()),
            medicationRepository = StubMedicationRepository(emptyList()),
            doseLogRepository = StubDoseLogRepository(emptyList()),
            petRepository = StubPetRepository(pet),
            adherenceCalculator = AdherenceCalculator(),
            clock = clock,
        )
        assertThat(builder.build(course.id)).isNull()
    }
}

// ── Stubs (only the methods used by CourseReportBuilder need to be live) ─────

private class StubCourseRepository(private val course: Course?) : CourseRepository {
    override fun observeActive(): Flow<List<Course>> = flowOf(emptyList())
    override fun observeCompleted(): Flow<List<Course>> = flowOf(emptyList())
    override suspend fun findById(id: Long): Course? = course
    override suspend fun upsert(course: Course): Long = 0L
    override suspend fun markCompleted(id: Long, completedAt: Instant) = Unit
    override suspend fun reactivate(id: Long) = Unit
    override suspend fun softDelete(id: Long) = Unit
}

private class StubCourseNoteRepository(private val notes: List<CourseNote>) : CourseNoteRepository {
    override fun observeForCourse(courseId: Long): Flow<List<CourseNote>> = flowOf(notes)
    override suspend fun listForCourse(courseId: Long): List<CourseNote> = notes
    override suspend fun findById(id: Long): CourseNote? = notes.firstOrNull { it.id == id }
    override suspend fun upsert(note: CourseNote): Long = note.id
    override suspend fun delete(id: Long) = Unit
}

private class StubMedicationRepository(private val meds: List<Medication>) : MedicationRepository {
    override fun observeActive(): Flow<List<Medication>> = flowOf(meds)
    override fun observeCompleted(): Flow<List<Medication>> = flowOf(emptyList())
    override fun observeByCourse(courseId: Long): Flow<List<Medication>> = flowOf(meds)
    override suspend fun listActive(): List<Medication> = meds
    override suspend fun listActiveForCourse(courseId: Long): List<Medication> = meds
    override suspend fun findById(id: Long): Medication? = meds.firstOrNull { it.id == id }
    override suspend fun upsert(med: Medication): Long = med.id
    override suspend fun markCompleted(id: Long, completedAt: Instant) = Unit
    override suspend fun reactivate(id: Long) = Unit
    override suspend fun softDelete(id: Long) = Unit
}

private class StubDoseLogRepository(private val logs: List<DoseLog>) : DoseLogRepository {
    override fun observeWindow(from: Instant, to: Instant): Flow<List<DoseLog>> = flowOf(logs)
    override fun observeForMedication(medicationId: Long): Flow<List<DoseLog>> =
        flowOf(logs.filter { it.medicationId == medicationId })
    override fun observeAll(): Flow<List<DoseLog>> = flowOf(logs)
    override suspend fun ensurePending(medicationId: Long, scheduledAt: Instant): Long = 0L
    override suspend fun markTaken(id: Long, at: Instant) = Unit
    override suspend fun markSkipped(id: Long) = Unit
    override suspend fun logPrn(medicationId: Long, at: Instant): Long = 0L
    override suspend fun lastTaken(medicationId: Long): DoseLog? = null
    override suspend fun markMissedBefore(before: Instant): Int = 0
}

private class StubPetRepository(private val pet: Pet?) : PetRepository {
    override fun observeAll(): Flow<List<Pet>> = flowOf(listOfNotNull(pet))
    override fun observeCount(): Flow<Int> = flowOf(if (pet == null) 0 else 1)
    override suspend fun create(
        name: String, species: Species, photoPath: String?, breed: String?,
        weightKg: Float?, birthDate: LocalDate?,
    ): Long = pet?.id ?: 0L
    override suspend fun update(pet: Pet) = Unit
    override suspend fun delete(id: Long) = Unit
    override suspend fun findById(id: Long): Pet? = pet
}
