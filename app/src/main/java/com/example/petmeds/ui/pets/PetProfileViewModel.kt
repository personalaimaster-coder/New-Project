package com.example.petmeds.ui.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.repo.MedicationRepository
import com.example.petmeds.data.repo.PetRepository
import com.example.petmeds.domain.model.Pet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class PetProfileUiState(
    val pet: Pet? = null,
    val activeMeds: Int = 0,
    val daysOnApp: Int = 0,
)

@HiltViewModel
class PetProfileViewModel @Inject constructor(
    petRepository: PetRepository,
    medicationRepository: MedicationRepository,
) : ViewModel() {

    val state: StateFlow<PetProfileUiState> = combine(
        petRepository.observeAll(),
        medicationRepository.observeActive(),
    ) { pets, meds ->
        val pet = pets.firstOrNull()
        val daysOnApp = pet?.let {
            val created = it.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            (today.toEpochDays() - created.toEpochDays()).coerceAtLeast(1).toInt()
        } ?: 0
        PetProfileUiState(
            pet = pet,
            activeMeds = meds.size,
            daysOnApp = daysOnApp,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetProfileUiState())
}
