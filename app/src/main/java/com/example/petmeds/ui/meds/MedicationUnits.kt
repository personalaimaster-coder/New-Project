package com.example.petmeds.ui.meds

import com.example.petmeds.domain.model.MedForm

const val CUSTOM_UNIT_LABEL = "Custom…"

fun unitOptionsFor(form: MedForm): List<String> = when (form) {
    MedForm.PILL -> listOf("tablet", "capsule", "half tablet", "quarter tablet")
    MedForm.LIQUID -> listOf("ml", "drop", "teaspoon (5 ml)", "tablespoon (15 ml)")
    MedForm.DROP_EYE -> listOf("drop")
    MedForm.DROP_EAR -> listOf("drop")
    MedForm.TOPICAL -> listOf("application", "pump", "gram", "strip (cm)")
} + CUSTOM_UNIT_LABEL
