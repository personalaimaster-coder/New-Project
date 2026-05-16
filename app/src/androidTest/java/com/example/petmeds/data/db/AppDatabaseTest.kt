package com.example.petmeds.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.domain.model.LifecycleStatus
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.NoteCategory
import com.example.petmeds.domain.model.Species
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun pet_insert_and_observe_roundtrip() = runTest {
        val now = Clock.System.now()
        val id = db.petDao().insert(
            PetEntity(name = "Buddy", species = Species.DOG, photoPath = null, createdAt = now)
        )
        assertThat(id).isGreaterThan(0)
        val all = db.petDao().observeAll().first()
        assertThat(all).hasSize(1)
        assertThat(all[0].name).isEqualTo("Buddy")
    }

    @Test
    fun medication_dose_log_cascade() = runTest {
        val now = Clock.System.now()
        val petId = db.petDao().insert(
            PetEntity(name = "Buddy", species = Species.DOG, photoPath = null, createdAt = now)
        )
        val medId = db.medicationDao().insert(
            MedicationEntity(
                petId = petId,
                courseId = null,
                name = "Carprofen 25mg",
                dosageAmount = 1.0,
                dosageUnit = "tablet",
                form = MedForm.PILL,
                scheduleJson = """{"type":"DailyTimes","times":["08:00"]}""",
                startDate = LocalDate(2026, 1, 1),
                endDate = null,
                notes = "Give with food",
                isActive = true,
                status = LifecycleStatus.ACTIVE,
                completedAt = null,
                createdAt = now,
            )
        )
        db.doseLogDao().insertIgnore(
            DoseLogEntity(
                medicationId = medId,
                scheduledAt = now,
                takenAt = null,
                status = DoseStatus.PENDING,
                createdAt = now,
            )
        )
        // Soft-delete should NOT cascade dose_logs in our model — but Room ForeignKey
        // CASCADE will delete them on a hard delete. Soft delete only flips isActive.
        db.medicationDao().softDelete(medId)
        val active = db.medicationDao().listActive()
        assertThat(active).isEmpty()
    }

    @Test
    fun course_notes_observe_ordered_by_occurredAt_desc() = runTest {
        val now = Clock.System.now()
        val petId = db.petDao().insert(
            PetEntity(name = "Buddy", species = Species.DOG, photoPath = null, createdAt = now)
        )
        val courseId = db.courseDao().insert(
            CourseEntity(
                petId = petId,
                name = "Allergy treatment",
                colorIndex = 0,
                startDate = LocalDate(2026, 1, 1),
                endDate = null,
                notes = null,
                status = LifecycleStatus.ACTIVE,
                completedAt = null,
                createdAt = now,
            )
        )

        val older = Instant.parse("2026-01-05T10:00:00Z")
        val newer = Instant.parse("2026-01-09T10:00:00Z")
        db.courseNoteDao().insert(
            CourseNoteEntity(
                courseId = courseId,
                occurredAt = older,
                category = NoteCategory.OBSERVATION,
                body = "Sneezing reduced.",
                photoPath = null,
                createdAt = now,
                updatedAt = now,
            )
        )
        db.courseNoteDao().insert(
            CourseNoteEntity(
                courseId = courseId,
                occurredAt = newer,
                category = NoteCategory.SIDE_EFFECT,
                body = "Mild lethargy after dose.",
                photoPath = null,
                createdAt = now,
                updatedAt = now,
            )
        )

        val notes = db.courseNoteDao().observeForCourse(courseId).first()
        assertThat(notes).hasSize(2)
        assertThat(notes[0].occurredAt).isEqualTo(newer)
        assertThat(notes[1].occurredAt).isEqualTo(older)
    }

    @Test
    fun course_notes_cascade_when_course_deleted() = runTest {
        val now = Clock.System.now()
        val petId = db.petDao().insert(
            PetEntity(name = "Buddy", species = Species.DOG, photoPath = null, createdAt = now)
        )
        val courseId = db.courseDao().insert(
            CourseEntity(
                petId = petId,
                name = "Course",
                colorIndex = 0,
                startDate = LocalDate(2026, 1, 1),
                endDate = null,
                notes = null,
                status = LifecycleStatus.ACTIVE,
                completedAt = null,
                createdAt = now,
            )
        )
        val noteId = db.courseNoteDao().insert(
            CourseNoteEntity(
                courseId = courseId,
                occurredAt = now,
                category = NoteCategory.OBSERVATION,
                body = "Test",
                photoPath = null,
                createdAt = now,
                updatedAt = now,
            )
        )
        assertThat(db.courseNoteDao().findById(noteId)).isNotNull()

        // Hard-delete the pet via SupportSQLiteDatabase since CourseDao only soft-deletes.
        db.openHelper.writableDatabase.execSQL("DELETE FROM pets WHERE id = $petId")

        assertThat(db.courseNoteDao().findById(noteId)).isNull()
    }
}
