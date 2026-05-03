package com.example.petmeds.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.LifecycleStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("petId"), Index("petId", "status")]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val name: String,
    val colorIndex: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val notes: String?,
    val status: LifecycleStatus,
    val completedAt: Instant?,
    val createdAt: Instant,
) {
    fun toDomain(): Course = Course(
        id = id,
        petId = petId,
        name = name,
        colorIndex = colorIndex,
        startDate = startDate,
        endDate = endDate,
        notes = notes,
        status = status,
        completedAt = completedAt,
        createdAt = createdAt,
    )

    companion object {
        fun fromDomain(course: Course): CourseEntity = CourseEntity(
            id = course.id,
            petId = course.petId,
            name = course.name,
            colorIndex = course.colorIndex,
            startDate = course.startDate,
            endDate = course.endDate,
            notes = course.notes,
            status = course.status,
            completedAt = course.completedAt,
            createdAt = course.createdAt,
        )
    }
}
