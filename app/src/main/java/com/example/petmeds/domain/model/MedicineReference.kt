package com.example.petmeds.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicineReference(
    val brandName: String,
    val genericName: String,
    val composition: String,
    val manufacturer: String,
    val dosageForm: String,
    val indication: String,
    val category: String,
    val searchTerms: List<String> = emptyList(),
)
