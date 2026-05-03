package com.example.petmeds.domain.model

import kotlinx.datetime.Instant

enum class DoseStatus { PENDING, TAKEN, SKIPPED, MISSED }

data class DoseLog(
    val id: Long = 0,
    val medicationId: Long,
    val scheduledAt: Instant?,
    val takenAt: Instant?,
    val status: DoseStatus,
    val createdAt: Instant,
)
