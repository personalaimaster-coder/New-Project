package com.example.petmeds.domain.model

import kotlinx.datetime.Instant

enum class NoteCategory { OBSERVATION, SYMPTOM, SIDE_EFFECT, VET_VISIT }

data class CourseNote(
    val id: Long = 0,
    val courseId: Long,
    val occurredAt: Instant,
    val category: NoteCategory = NoteCategory.OBSERVATION,
    val body: String,
    val photoPath: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
