package com.example.petmeds.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

enum class Species { DOG, CAT, RABBIT, BIRD, OTHER }

data class Pet(
    val id: Long = 0,
    val name: String,
    val species: Species,
    val photoPath: String? = null,
    val breed: String? = null,
    val weightKg: Float? = null,
    val birthDate: LocalDate? = null,
    val createdAt: Instant,
)
