package com.example.petmeds.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.repo.CourseRepository
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.notifications.DoseAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseSection(
    val course: Course?,
    val medications: List<Medication>,
)

@HiltViewModel
class MedsListViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: DoseAlarmScheduler,
) : ViewModel() {

    val sections: StateFlow<List<CourseSection>> = combine(
        courseRepository.observeActive(),
        medicationRepository.observeActive(),
    ) { courses, meds ->
        val byCourse = meds.groupBy { it.courseId }
        val courseSections = courses.map { course ->
            CourseSection(course = course, medications = byCourse[course.id].orEmpty())
        }
        val uncategorized = byCourse[null].orEmpty()
        if (uncategorized.isEmpty()) courseSections
        else courseSections + CourseSection(course = null, medications = uncategorized)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            medicationRepository.softDelete(id)
            alarmScheduler.cancelFor(id)
        }
    }

    fun markCourseComplete(courseId: Long) {
        viewModelScope.launch {
            medicationRepository.listActiveForCourse(courseId).forEach { med ->
                medicationRepository.markCompleted(med.id, kotlinx.datetime.Clock.System.now())
                alarmScheduler.cancelFor(med.id)
            }
            courseRepository.markCompleted(courseId, kotlinx.datetime.Clock.System.now())
        }
    }
}
