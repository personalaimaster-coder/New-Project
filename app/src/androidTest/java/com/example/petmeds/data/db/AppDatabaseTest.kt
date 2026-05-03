package com.example.petmeds.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.Species
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
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
                name = "Carprofen 25mg",
                dosageAmount = 1.0,
                dosageUnit = "tablet",
                form = MedForm.PILL,
                scheduleJson = """{"type":"DailyTimes","times":["08:00"]}""",
                startDate = LocalDate(2026, 1, 1),
                endDate = null,
                notes = "Give with food",
                isActive = true,
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
}
