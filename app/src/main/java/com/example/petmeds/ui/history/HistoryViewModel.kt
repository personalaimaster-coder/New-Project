package com.example.petmeds.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.repo.CourseRepository
import com.example.petmeds.data.repo.DoseLogRepository
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.domain.model.Medication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class MedicationHistoryRow(
    val medication: Medication,
    val courseName: String?,
    val doseStats: String,
    val monthLabel: String,
)

data class CourseHistoryRow(
    val course: Course,
    val medCount: Int,
    val doseStats: String,
    val monthLabel: String,
)

data class HistoryUiState(
    val medications: List<MedicationHistoryRow> = emptyList(),
    val courses: List<CourseHistoryRow> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    medicationRepository: MedicationRepository,
    courseRepository: CourseRepository,
    doseLogRepository: DoseLogRepository,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = combine(
        medicationRepository.observeCompleted(),
        courseRepository.observeActive(),
        courseRepository.observeCompleted(),
        doseLogRepository.observeAll(),
    ) { completedMeds, activeCourses, completedCourses, logs ->
        val allCourses = (activeCourses + completedCourses).associateBy { it.id }
        val logsByMed = logs.groupBy { it.medicationId }
        val medsByCourse = completedMeds.groupBy { it.courseId }
        val medRows = completedMeds.map { med ->
            val medLogs = logsByMed[med.id].orEmpty()
            MedicationHistoryRow(
                medication = med,
                courseName = med.courseId?.let { allCourses[it]?.name },
                doseStats = "${medLogs.count { it.status == DoseStatus.TAKEN }} / ${medLogs.size} doses taken",
                monthLabel = med.completedAt.monthLabel(),
            )
        }
        val courseRows = completedCourses.map { course ->
            val courseMeds = medsByCourse[course.id].orEmpty()
            val courseLogs = courseMeds.flatMap { logsByMed[it.id].orEmpty() }
            CourseHistoryRow(
                course = course,
                medCount = courseMeds.size,
                doseStats = "${courseLogs.count { it.status == DoseStatus.TAKEN }} / ${courseLogs.size} doses taken",
                monthLabel = course.completedAt.monthLabel(),
            )
        }
        HistoryUiState(medications = medRows, courses = courseRows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}

private fun Instant?.monthLabel(): String {
    val dt = (this ?: kotlinx.datetime.Clock.System.now()).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = dt.month.name.lowercase().replaceFirstChar { it.titlecase() }
    return "Completed in $month ${dt.year}"
}
