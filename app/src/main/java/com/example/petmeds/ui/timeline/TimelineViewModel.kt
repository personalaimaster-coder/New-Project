package com.example.petmeds.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.prefs.UserPrefsRepository
import com.example.petmeds.data.repo.DoseLogRepository
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.data.repo.PetRepository
import com.example.petmeds.domain.model.DoseStatus
import com.example.petmeds.domain.model.Medication
import com.example.petmeds.domain.model.Pet
import com.example.petmeds.domain.schedule.DoseScheduler
import com.example.petmeds.notifications.DoseAlarmScheduler
import com.example.petmeds.ui.common.formatDosage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository,
    private val doseScheduler: DoseScheduler,
    private val alarmScheduler: DoseAlarmScheduler,
    private val userPrefsRepository: UserPrefsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val zone get() = TimeZone.currentSystemDefault()
    private val tickFlow = MutableStateFlow(clock.now())

    val state: StateFlow<TimelineUiState> = combine(
        petRepository.observeAll(),
        medicationRepository.observeActive(),
        doseLogRepository.observeWindow(
            from = clock.now() - 1.days,
            to = clock.now() + HORIZON_DAYS.days,
        ),
        tickFlow,
        userPrefsRepository.observeParentName(),
    ) { pets, meds, logs, now, parentName ->
        buildState(pets, meds, logs, now, parentName)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState())

    fun refresh() { tickFlow.value = clock.now() }

    fun markTaken(item: TimelineItem) {
        viewModelScope.launch {
            val id = item.doseLogId
                ?: doseLogRepository.ensurePending(item.medicationId, item.scheduledAt)
            doseLogRepository.markTaken(id)
        }
    }

    fun markTakenAt(item: TimelineItem, at: Instant) {
        viewModelScope.launch {
            val id = item.doseLogId
                ?: doseLogRepository.ensurePending(item.medicationId, item.scheduledAt)
            doseLogRepository.markTaken(id, at)
        }
    }

    fun markSkipped(item: TimelineItem) {
        viewModelScope.launch {
            val id = item.doseLogId
                ?: doseLogRepository.ensurePending(item.medicationId, item.scheduledAt)
            doseLogRepository.markSkipped(id)
        }
    }

    fun rescheduleAlarmsAfterPermissionChange() {
        viewModelScope.launch { alarmScheduler.rescheduleAll() }
    }

    private fun buildState(
        pets: List<Pet>,
        meds: List<Medication>,
        logs: List<com.example.petmeds.domain.model.DoseLog>,
        now: Instant,
        parentName: String?,
    ): TimelineUiState {
        if (pets.isEmpty()) return TimelineUiState(loading = false, hasPet = false, parentName = parentName)

        val primaryPet = pets.first()
        val petById = pets.associateBy { it.id }
        val medById = meds.associateBy { it.id }
        val medColorIndex = meds.mapIndexed { i, m -> m.id to (i % 6) }.toMap()
        val itemsByKey = mutableMapOf<String, TimelineItem>()

        for (log in logs) {
            val med = medById[log.medicationId] ?: continue
            val pet = petById[med.petId] ?: continue
            val sched = log.scheduledAt ?: log.takenAt ?: log.createdAt
            val key = "${log.medicationId}@${sched.toEpochMilliseconds()}"
            itemsByKey[key] = TimelineItem(
                key = key,
                medicationId = med.id,
                medName = med.name,
                dosage = formatDosage(med.dosageAmount, med.dosageUnit),
                notes = med.notes,
                petId = pet.id,
                petName = pet.name,
                petPhotoPath = pet.photoPath,
                scheduledAt = sched,
                doseLogId = log.id,
                status = log.status,
                colorIndex = medColorIndex[med.id] ?: 0,
            )
        }

        for (med in meds) {
            val pet = petById[med.petId] ?: continue
            val instants = doseScheduler.computeUpcoming(med, now, HORIZON_DAYS, zone)
            for (at in instants) {
                val key = "${med.id}@${at.toEpochMilliseconds()}"
                if (itemsByKey.containsKey(key)) continue
                itemsByKey[key] = TimelineItem(
                    key = key,
                    medicationId = med.id,
                    medName = med.name,
                    dosage = formatDosage(med.dosageAmount, med.dosageUnit),
                    notes = med.notes,
                    petId = pet.id,
                    petName = pet.name,
                    petPhotoPath = pet.photoPath,
                    scheduledAt = at,
                    doseLogId = null,
                    status = DoseStatus.PENDING,
                    colorIndex = medColorIndex[med.id] ?: 0,
                )
            }
        }

        val allItems = itemsByKey.values.sortedBy { it.scheduledAt }
        val days = allItems
            .groupBy { it.scheduledAt.toLocalDateTime(zone).date }
            .toSortedMap()
            .map { (date, items) -> TimelineDay(date, items) }

        val today = now.toLocalDateTime(zone).date
        val tomorrow = today.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
        val nowWindow = 15.minutes

        val overdue = allItems.filter {
            it.status == DoseStatus.PENDING && it.scheduledAt < now - nowWindow
        }
        val dueNow = allItems.filter {
            it.status == DoseStatus.PENDING &&
                it.scheduledAt >= now - nowWindow &&
                it.scheduledAt <= now + nowWindow
        }
        val upNext = allItems.filter {
            it.status == DoseStatus.PENDING &&
                it.scheduledAt > now + nowWindow &&
                it.scheduledAt.toLocalDateTime(zone).date == today
        }
        val tomorrowItems = allItems.filter {
            it.status == DoseStatus.PENDING &&
                it.scheduledAt.toLocalDateTime(zone).date == tomorrow
        }
        val doneToday = allItems.filter {
            (it.status == DoseStatus.TAKEN || it.status == DoseStatus.SKIPPED) &&
                it.scheduledAt.toLocalDateTime(zone).date == today
        }
        val todayTotal = overdue.size + dueNow.size + upNext.size + doneToday.size
        val todayDone = doneToday.size

        return TimelineUiState(
            loading = false,
            hasPet = true,
            parentName = parentName,
            petName = primaryPet.name,
            petPhotoPath = primaryPet.photoPath,
            days = days,
            empty = days.isEmpty(),
            todayDone = todayDone,
            todayTotal = todayTotal,
            overdue = overdue,
            dueNow = dueNow,
            upNext = upNext,
            tomorrow = tomorrowItems,
            doneToday = doneToday,
            hasTodaySections = overdue.isNotEmpty() || dueNow.isNotEmpty() ||
                upNext.isNotEmpty() || tomorrowItems.isNotEmpty() || doneToday.isNotEmpty(),
        )
    }

    companion object { const val HORIZON_DAYS = 2 }
}
