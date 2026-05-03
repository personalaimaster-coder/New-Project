package com.example.petmeds.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.petmeds.domain.model.DoseLog
import com.example.petmeds.domain.model.DoseStatus
import kotlinx.datetime.Instant

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("medicationId"),
        Index("scheduledAt"),
        Index("medicationId", "scheduledAt", unique = true),
    ]
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val scheduledAt: Instant?,
    val takenAt: Instant?,
    val status: DoseStatus,
    val createdAt: Instant,
) {
    fun toDomain(): DoseLog = DoseLog(id, medicationId, scheduledAt, takenAt, status, createdAt)

    companion object {
        fun fromDomain(d: DoseLog): DoseLogEntity =
            DoseLogEntity(d.id, d.medicationId, d.scheduledAt, d.takenAt, d.status, d.createdAt)
    }
}
