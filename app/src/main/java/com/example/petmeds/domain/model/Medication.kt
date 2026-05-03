package com.example.petmeds.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

enum class MedForm { PILL, LIQUID, DROP_EYE, DROP_EAR, TOPICAL }

data class Medication(
    val id: Long = 0,
    val petId: Long,
    val courseId: Long? = null,
    val name: String,
    val dosageAmount: Double,
    val dosageUnit: String,
    val form: MedForm,
    val schedule: ScheduleConfig,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val notes: String? = null,
    val status: LifecycleStatus = LifecycleStatus.ACTIVE,
    val completedAt: Instant? = null,
    val createdAt: Instant,
) {
    val isActive: Boolean get() = status == LifecycleStatus.ACTIVE
}
