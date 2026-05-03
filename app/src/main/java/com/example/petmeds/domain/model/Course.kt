package com.example.petmeds.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Course(
    val id: Long = 0,
    val petId: Long,
    val name: String,
    val colorIndex: Int = 0,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val notes: String? = null,
    val status: LifecycleStatus = LifecycleStatus.ACTIVE,
    val completedAt: Instant? = null,
    val createdAt: Instant,
)
