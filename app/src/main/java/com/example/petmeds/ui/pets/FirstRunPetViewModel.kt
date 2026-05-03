package com.example.petmeds.ui.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.prefs.UserPrefsRepository
import com.example.petmeds.data.repo.PetRepository
import com.example.petmeds.domain.model.Species
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

data class FirstRunUiState(
    val parentName: String = "",
    val name: String = "",
    val species: Species = Species.DOG,
    val breed: String = "",
    val weightKg: String = "",
    val birthDate: LocalDate? = null,
    val saving: Boolean = false,
    val nameError: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class FirstRunPetViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val userPrefsRepository: UserPrefsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FirstRunUiState())
    val state: StateFlow<FirstRunUiState> = _state.asStateFlow()

    fun onParentNameChanged(value: String) {
        _state.value = _state.value.copy(parentName = value)
    }

    fun onNameChanged(value: String) {
        _state.value = _state.value.copy(name = value, nameError = false)
    }

    fun onSpeciesChanged(value: Species) {
        _state.value = _state.value.copy(species = value)
    }

    fun onBreedChanged(value: String) {
        _state.value = _state.value.copy(breed = value)
    }

    fun onWeightChanged(value: String) {
        _state.value = _state.value.copy(weightKg = value)
    }

    fun onBirthDateChanged(value: LocalDate?) {
        _state.value = _state.value.copy(birthDate = value)
    }

    fun submit() {
        val current = _state.value
        if (current.name.isBlank()) {
            _state.value = current.copy(nameError = true); return
        }
        if (current.saving) return
        _state.value = current.copy(saving = true)
        viewModelScope.launch {
            petRepository.create(
                name = current.name.trim(),
                species = current.species,
                breed = current.breed.trim().takeIf { it.isNotBlank() },
                weightKg = current.weightKg.trim().toFloatOrNull(),
                birthDate = current.birthDate,
            )
            userPrefsRepository.setParentName(current.parentName.trim().takeIf { it.isNotBlank() })
            _state.value = _state.value.copy(saving = false, saved = true)
        }
    }
}
