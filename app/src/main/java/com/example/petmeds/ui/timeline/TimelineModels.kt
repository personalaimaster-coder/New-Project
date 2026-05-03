package com.example.petmeds.ui.timeline

import com.example.petmeds.domain.model.DoseStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class TimelineItem(
    val key: String,
    val medicationId: Long,
    val medName: String,
    val dosage: String,
    val notes: String?,
    val petId: Long,
    val petName: String,
    val petPhotoPath: String?,
    val scheduledAt: Instant,
    val doseLogId: Long?,
    val status: DoseStatus,
    /** colour slot index (0-5) for per-med accent */
    val colorIndex: Int = 0,
)

data class TimelineDay(
    val date: LocalDate,
    val items: List<TimelineItem>,
)

data class TimelineUiState(
    val loading: Boolean = true,
    val hasPet: Boolean = false,
    val parentName: String? = null,
    val petName: String = "",
    val petPhotoPath: String? = null,
    val days: List<TimelineDay> = emptyList(),
    val empty: Boolean = false,
    val todayDone: Int = 0,
    val todayTotal: Int = 0,
    // Sectioned today data
    val overdue: List<TimelineItem> = emptyList(),
    val dueNow: List<TimelineItem> = emptyList(),
    val upNext: List<TimelineItem> = emptyList(),
    val tomorrow: List<TimelineItem> = emptyList(),
    val doneToday: List<TimelineItem> = emptyList(),
    val hasTodaySections: Boolean = false,
)
