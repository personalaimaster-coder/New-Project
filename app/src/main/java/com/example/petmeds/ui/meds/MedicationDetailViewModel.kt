package com.example.petmeds.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.repo.CourseRepository
import com.example.petmeds.data.repo.DoseLogRepository
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.DoseLog
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.notifications.DoseAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

data class MedicationDetailUiState(
    val loading: Boolean = true,
    val medication: Medication? = null,
    val course: Course? = null,
    val logs: List<DoseLog> = emptyList(),
)

@HiltViewModel
class MedicationDetailViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val courseRepository: CourseRepository,
    private val doseLogRepository: DoseLogRepository,
    private val alarmScheduler: DoseAlarmScheduler,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(MedicationDetailUiState())
    val state: StateFlow<MedicationDetailUiState> = _state.asStateFlow()

    private var medicationId: Long = 0L
    private var logsJob: Job? = null

    fun load(id: Long?) {
        if (id == null || id <= 0L || id == medicationId) return
        medicationId = id
        viewModelScope.launch {
            refreshMedication()
            logsJob?.cancel()
            logsJob = launch {
                doseLogRepository.observeForMedication(id).collect { logs ->
                    _state.value = _state.value.copy(logs = logs)
                }
            }
        }
    }

    fun logTakenNow() {
        val id = medicationId.takeIf { it > 0L } ?: return
        viewModelScope.launch { doseLogRepository.logPrn(id, clock.now()) }
    }

    fun markComplete() {
        val id = medicationId.takeIf { it > 0L } ?: return
        viewModelScope.launch {
            medicationRepository.markCompleted(id, clock.now())
            alarmScheduler.cancelFor(id)
            refreshMedication()
        }
    }

    fun reactivate() {
        val id = medicationId.takeIf { it > 0L } ?: return
        viewModelScope.launch {
            medicationRepository.reactivate(id)
            alarmScheduler.rescheduleFor(id)
            refreshMedication()
        }
    }

    fun delete() {
        val id = medicationId.takeIf { it > 0L } ?: return
        viewModelScope.launch {
            medicationRepository.softDelete(id)
            alarmScheduler.cancelFor(id)
            refreshMedication()
        }
    }

    private suspend fun refreshMedication() {
        val med = medicationRepository.findById(medicationId)
        val course = med?.courseId?.let { courseRepository.findById(it) }
        _state.value = _state.value.copy(
            loading = false,
            medication = med,
            course = course,
        )
    }
}
