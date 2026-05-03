package com.example.petmeds.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.repo.CourseRepository
import com.example.petmeds.data.repo.PetRepository
import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.LifecycleStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class CourseEditorUiState(
    val name: String = "",
    val startDate: LocalDate = todayLocalDate(),
    val endDate: LocalDate? = null,
    val notes: String = "",
    val colorIndex: Int = 0,
    val nameError: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class CourseEditorViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val petRepository: PetRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(CourseEditorUiState())
    val state: StateFlow<CourseEditorUiState> = _state.asStateFlow()

    private var editingId: Long = 0L
    private var createdAt = clock.now()

    fun load(id: Long?) {
        if (id == null || id <= 0L) return
        editingId = id
        viewModelScope.launch {
            val course = courseRepository.findById(id) ?: return@launch
            createdAt = course.createdAt
            _state.value = _state.value.copy(
                name = course.name,
                startDate = course.startDate,
                endDate = course.endDate,
                notes = course.notes.orEmpty(),
                colorIndex = course.colorIndex,
            )
        }
    }

    fun update(transform: (CourseEditorUiState) -> CourseEditorUiState) {
        _state.value = transform(_state.value)
    }

    fun submit() {
        val s = _state.value
        if (s.name.isBlank()) {
            update { it.copy(nameError = true) }
            return
        }
        if (s.saving) return
        update { it.copy(saving = true) }
        viewModelScope.launch {
            val petId = petRepository.observeAll().firstOrNull().orEmpty().firstOrNull()?.id ?: 0L
            courseRepository.upsert(
                Course(
                    id = editingId,
                    petId = petId,
                    name = s.name.trim(),
                    colorIndex = s.colorIndex,
                    startDate = s.startDate,
                    endDate = s.endDate,
                    notes = s.notes.takeIf { it.isNotBlank() },
                    status = LifecycleStatus.ACTIVE,
                    completedAt = null,
                    createdAt = createdAt,
                )
            )
            update { it.copy(saving = false, saved = true) }
        }
    }
}

private fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
