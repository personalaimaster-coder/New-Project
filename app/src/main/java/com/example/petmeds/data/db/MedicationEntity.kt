package com.example.petmeds.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.petmeds.domain.model.LifecycleStatus
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.ScheduleConfig
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index("petId"), Index("courseId"), Index("petId", "status")]
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val courseId: Long?,
    val name: String,
    val dosageAmount: Double,
    val dosageUnit: String,
    val form: MedForm,
    val scheduleJson: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val notes: String?,
    val isActive: Boolean,
    val status: LifecycleStatus,
    val completedAt: Instant?,
    val createdAt: Instant,
) {
    fun toDomain(json: Json): Medication = Medication(
        id = id,
        petId = petId,
        courseId = courseId,
        name = name,
        dosageAmount = dosageAmount,
        dosageUnit = dosageUnit,
        form = form,
        schedule = json.decodeFromString(ScheduleConfig.serializer(), scheduleJson),
        startDate = startDate,
        endDate = endDate,
        notes = notes,
        status = status,
        completedAt = completedAt,
        createdAt = createdAt,
    )

    companion object {
        fun fromDomain(m: Medication, json: Json): MedicationEntity = MedicationEntity(
            id = m.id,
            petId = m.petId,
            courseId = m.courseId,
            name = m.name,
            dosageAmount = m.dosageAmount,
            dosageUnit = m.dosageUnit,
            form = m.form,
            scheduleJson = json.encodeToString(ScheduleConfig.serializer(), m.schedule),
            startDate = m.startDate,
            endDate = m.endDate,
            notes = m.notes,
            isActive = m.isActive,
            status = m.status,
            completedAt = m.completedAt,
            createdAt = m.createdAt,
        )
    }
}
