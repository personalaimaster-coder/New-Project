package com.example.petmeds.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.ocr.OcrMedResult
import com.example.petmeds.data.repo.CourseRepository
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.data.repo.MedicineReferenceRepository
import com.example.petmeds.data.repo.PetRepository
import com.example.petmeds.domain.model.MedicineReference
import com.example.petmeds.domain.model.Course
import com.example.petmeds.domain.model.LifecycleStatus
import com.example.petmeds.domain.model.LocalTimeStr
import com.example.petmeds.domain.model.MedForm
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.ScheduleConfig
import com.example.petmeds.notifications.DoseAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

enum class FrequencyOption { DAILY_TIMES, INTERVAL_HOURS, PRN }

data class MedDraft(
    val name: String = "",
    val dosageAmount: String = "1",
    val dosageUnit: String = "tablet",
    val dosageUnitIsCustom: Boolean = false,
    val form: MedForm = MedForm.PILL,
    val frequencyOption: FrequencyOption = FrequencyOption.DAILY_TIMES,
    val dailyTimes: List<LocalTimeStr> = listOf(LocalTimeStr.of(8, 0), LocalTimeStr.of(20, 0)),
    val intervalHours: Int = 8,
    val intervalAnchor: LocalTimeStr = LocalTimeStr.of(8, 0),
    val startDate: LocalDate = todayLocal(),
    val endDate: LocalDate? = null,
    val notes: String = "",
    val useScheduleFromFirst: Boolean = false,
    val nameError: Boolean = false,
    val dosageError: Boolean = false,
)

data class MedEditorUiState(
    val loading: Boolean = false,
    val editingId: Long? = null,
    val courses: List<Course> = emptyList(),
    val selectedCourseId: Long? = null,
    val drafts: List<MedDraft> = listOf(MedDraft()),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val courseError: Boolean = false,
    val medicineSuggestions: List<MedicineReference> = emptyList(),
    val medicineInfoTarget: MedicineReference? = null,
) {
    val isEditMode: Boolean get() = editingId != null
}

@HiltViewModel
class MedEditorViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val medicationRepository: MedicationRepository,
    private val petRepository: PetRepository,
    private val medicineReferenceRepository: MedicineReferenceRepository,
    private val alarmScheduler: DoseAlarmScheduler,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(MedEditorUiState())
    val state: StateFlow<MedEditorUiState> = _state.asStateFlow()

    private var editingId: Long = 0L
    private var createdAt = clock.now()

    init {
        courseRepository.observeActive()
            .onEach { courses -> update { it.copy(courses = courses) } }
            .launchIn(viewModelScope)
    }

    fun load(id: Long?) {
        if (id == null || id <= 0L) return
        editingId = id
        viewModelScope.launch {
            val m = medicationRepository.findById(id) ?: return@launch
            createdAt = m.createdAt
            _state.value = _state.value.copy(
                editingId = m.id,
                selectedCourseId = m.courseId,
                drafts = listOf(m.toDraft()),
            )
        }
    }

    fun update(transform: (MedEditorUiState) -> MedEditorUiState) {
        _state.value = transform(_state.value)
    }

    fun updateDraft(index: Int, transform: (MedDraft) -> MedDraft) {
        update { state ->
            state.copy(
                drafts = state.drafts.mapIndexed { i, draft ->
                    if (i == index) transform(draft) else draft
                },
            )
        }
    }

    fun addDailyTime(index: Int, time: LocalTimeStr) {
        val current = _state.value.drafts.getOrNull(index)?.dailyTimes ?: return
        if (current.any { it.value == time.value }) return
        updateDraft(index) { it.copy(dailyTimes = (current + time).sortedBy { lt -> lt.value }) }
    }

    fun removeDailyTime(index: Int, time: LocalTimeStr) {
        updateDraft(index) { draft ->
            draft.copy(dailyTimes = draft.dailyTimes.filterNot { lt -> lt.value == time.value })
        }
    }

    fun addDraft() {
        val first = _state.value.drafts.firstOrNull() ?: MedDraft()
        update { state ->
            state.copy(
                drafts = state.drafts + MedDraft(
                    dailyTimes = first.dailyTimes,
                    frequencyOption = first.frequencyOption,
                    intervalHours = first.intervalHours,
                    intervalAnchor = first.intervalAnchor,
                    startDate = first.startDate,
                    endDate = first.endDate,
                    useScheduleFromFirst = true,
                ),
            )
        }
    }

    fun removeDraft(index: Int) {
        update { state ->
            if (state.drafts.size <= 1) state
            else state.copy(drafts = state.drafts.filterIndexed { i, _ -> i != index })
        }
    }

    fun setInitialCourse(courseId: Long) {
        if (courseId > 0L) update { it.copy(selectedCourseId = courseId, courseError = false) }
    }

    fun searchMedicines(query: String) {
        if (query.isBlank()) {
            update { it.copy(medicineSuggestions = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = medicineReferenceRepository.search(query)
            update { it.copy(medicineSuggestions = results) }
        }
    }

    fun clearSuggestions() {
        update { it.copy(medicineSuggestions = emptyList()) }
    }

    fun showMedicineInfo(medicine: MedicineReference) {
        update { it.copy(medicineInfoTarget = medicine) }
    }

    fun dismissMedicineInfo() {
        update { it.copy(medicineInfoTarget = null) }
    }

    fun lookupMedicineInfo(name: String) {
        viewModelScope.launch {
            val med = medicineReferenceRepository.findByName(name)
            if (med != null) {
                update { it.copy(medicineInfoTarget = med) }
            }
        }
    }

    fun loadFromOcrResults(results: List<OcrMedResult>) {
        if (results.isEmpty()) return
        val drafts = results.map { ocr -> ocr.toDraft() }
        update { it.copy(drafts = drafts) }
    }

    fun submit() {
        val s = _state.value
        val validatedDrafts = s.drafts.map { draft ->
            val nameOk = draft.name.isNotBlank()
            val dosageOk = draft.dosageAmount.toDoubleOrNull()?.let { it > 0.0 } == true
            draft.copy(nameError = !nameOk, dosageError = !dosageOk)
        }
        val courseOk = s.isEditMode || s.selectedCourseId != null
        if (validatedDrafts.any { it.nameError || it.dosageError } || !courseOk) {
            update { it.copy(drafts = validatedDrafts, courseError = !courseOk) }
            return
        }
        if (s.saving) return
        update { it.copy(saving = true) }
        viewModelScope.launch {
            val pets = petRepository.observeAll().firstOrNull().orEmpty()
            val petId = pets.firstOrNull()?.id ?: 0L
            val firstDraft = validatedDrafts.first()
            validatedDrafts.forEachIndexed { index, draft ->
                val effectiveDraft = if (index > 0 && draft.useScheduleFromFirst) {
                    draft.copy(
                        frequencyOption = firstDraft.frequencyOption,
                        dailyTimes = firstDraft.dailyTimes,
                        intervalHours = firstDraft.intervalHours,
                        intervalAnchor = firstDraft.intervalAnchor,
                        startDate = firstDraft.startDate,
                        endDate = firstDraft.endDate,
                    )
                } else draft
                val dosage = effectiveDraft.dosageAmount.toDouble()
                val med = Medication(
                    id = if (s.isEditMode) editingId else 0L,
                    petId = petId,
                    courseId = s.selectedCourseId,
                    name = effectiveDraft.name.trim(),
                    dosageAmount = dosage,
                    dosageUnit = effectiveDraft.dosageUnit.ifBlank { "dose" },
                    form = effectiveDraft.form,
                    schedule = effectiveDraft.toSchedule(),
                    startDate = effectiveDraft.startDate,
                    endDate = effectiveDraft.endDate,
                    notes = effectiveDraft.notes.takeIf { it.isNotBlank() },
                    status = LifecycleStatus.ACTIVE,
                    completedAt = null,
                    createdAt = if (s.isEditMode) createdAt else clock.now(),
                )
                val newId = medicationRepository.upsert(med)
                alarmScheduler.rescheduleFor(newId)
            }
            update { it.copy(saving = false, saved = true) }
        }
    }
}

private fun todayLocal(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun stripTrailingZeros(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

private fun Medication.toDraft(): MedDraft = MedDraft(
    name = name,
    dosageAmount = stripTrailingZeros(dosageAmount),
    dosageUnit = dosageUnit,
    dosageUnitIsCustom = dosageUnit !in unitOptionsFor(form).filterNot { it == CUSTOM_UNIT_LABEL },
    form = form,
    frequencyOption = when (schedule) {
        is ScheduleConfig.DailyTimes -> FrequencyOption.DAILY_TIMES
        is ScheduleConfig.IntervalHours -> FrequencyOption.INTERVAL_HOURS
        is ScheduleConfig.SpecificDays -> FrequencyOption.DAILY_TIMES
        ScheduleConfig.Prn -> FrequencyOption.PRN
    },
    dailyTimes = (schedule as? ScheduleConfig.DailyTimes)?.times ?: listOf(LocalTimeStr.of(8, 0)),
    intervalHours = (schedule as? ScheduleConfig.IntervalHours)?.intervalHours ?: 8,
    intervalAnchor = (schedule as? ScheduleConfig.IntervalHours)?.anchor ?: LocalTimeStr.of(8, 0),
    startDate = startDate,
    endDate = endDate,
    notes = notes.orEmpty(),
)

private fun MedDraft.toSchedule(): ScheduleConfig = when (frequencyOption) {
    FrequencyOption.DAILY_TIMES -> ScheduleConfig.DailyTimes(
        dailyTimes.ifEmpty { listOf(LocalTimeStr.of(9, 0)) },
    )
    FrequencyOption.INTERVAL_HOURS -> ScheduleConfig.IntervalHours(
        intervalHours = intervalHours.coerceIn(1, 24),
        anchor = intervalAnchor,
    )
    FrequencyOption.PRN -> ScheduleConfig.Prn
}

private fun OcrMedResult.toDraft(): MedDraft {
    val resolvedForm = when (form.lowercase()) {
        "pill" -> MedForm.PILL
        "liquid" -> MedForm.LIQUID
        "eye_drop" -> MedForm.DROP_EYE
        "ear_drop" -> MedForm.DROP_EAR
        "topical" -> MedForm.TOPICAL
        else -> MedForm.PILL
    }
    val standardUnits = unitOptionsFor(resolvedForm).filterNot { it == CUSTOM_UNIT_LABEL }
    val unitIsCustom = dosageUnit.isNotBlank() && dosageUnit !in standardUnits
    val resolvedUnit = when {
        dosageUnit.isBlank() -> standardUnits.firstOrNull() ?: "dose"
        else -> dosageUnit
    }
    val freqOption = when (frequencyType) {
        "interval" -> FrequencyOption.INTERVAL_HOURS
        "prn" -> FrequencyOption.PRN
        else -> FrequencyOption.DAILY_TIMES
    }
    val resolvedTimes = buildDefaultTimes(timesPerDay)
    return MedDraft(
        name = name,
        dosageAmount = dosageAmount.ifBlank { "1" },
        dosageUnit = resolvedUnit,
        dosageUnitIsCustom = unitIsCustom,
        form = resolvedForm,
        frequencyOption = freqOption,
        dailyTimes = resolvedTimes,
        intervalHours = intervalHours,
        notes = notes,
    )
}

private fun buildDefaultTimes(timesPerDay: Int): List<LocalTimeStr> = when (timesPerDay) {
    1 -> listOf(LocalTimeStr.of(8, 0))
    2 -> listOf(LocalTimeStr.of(8, 0), LocalTimeStr.of(20, 0))
    3 -> listOf(LocalTimeStr.of(8, 0), LocalTimeStr.of(14, 0), LocalTimeStr.of(20, 0))
    4 -> listOf(LocalTimeStr.of(8, 0), LocalTimeStr.of(12, 0), LocalTimeStr.of(18, 0), LocalTimeStr.of(22, 0))
    else -> listOf(LocalTimeStr.of(8, 0))
}
