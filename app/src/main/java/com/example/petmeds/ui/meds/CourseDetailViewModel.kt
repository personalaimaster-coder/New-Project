package com.example.petmeds.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.repo.CourseNoteRepository
import com.example.petmeds.data.repo.CourseRepository
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.CourseNote
import com.example.petmeds.domain.model.Medication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseDetailUiState(
    val course: Course? = null,
    val medications: List<Medication> = emptyList(),
    val notes: List<CourseNote> = emptyList(),
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val medicationRepository: MedicationRepository,
    private val courseNoteRepository: CourseNoteRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CourseDetailUiState())
    val state: StateFlow<CourseDetailUiState> = _state.asStateFlow()

    private var medJob: Job? = null
    private var notesJob: Job? = null

    fun load(id: Long?) {
        if (id == null || id <= 0L) return
        viewModelScope.launch {
            _state.value = _state.value.copy(course = courseRepository.findById(id))
        }
        medJob?.cancel()
        medJob = medicationRepository.observeByCourse(id)
            .collectIn(viewModelScope) { meds ->
                _state.value = _state.value.copy(medications = meds)
            }
        notesJob?.cancel()
        notesJob = courseNoteRepository.observeForCourse(id)
            .collectIn(viewModelScope) { notes ->
                _state.value = _state.value.copy(notes = notes)
            }
    }
}

private fun <T> kotlinx.coroutines.flow.Flow<T>.collectIn(
    scope: kotlinx.coroutines.CoroutineScope,
    block: suspend (T) -> Unit,
): Job = scope.launch { collect { block(it) } }
