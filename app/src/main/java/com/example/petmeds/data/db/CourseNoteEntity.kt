package com.example.petmeds.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.petmeds.domain.model.CourseNote
import com.example.petmeds.domain.model.NoteCategory
import kotlinx.datetime.Instant

@Entity(
    tableName = "course_notes",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("courseId"), Index("courseId", "occurredAt")]
)
data class CourseNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val occurredAt: Instant,
    val category: NoteCategory,
    val body: String,
    val photoPath: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun toDomain(): CourseNote = CourseNote(
        id = id,
        courseId = courseId,
        occurredAt = occurredAt,
        category = category,
        body = body,
        photoPath = photoPath,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(note: CourseNote): CourseNoteEntity = CourseNoteEntity(
            id = note.id,
            courseId = note.courseId,
            occurredAt = note.occurredAt,
            category = note.category,
            body = note.body,
            photoPath = note.photoPath,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
        )
    }
}
