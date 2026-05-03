package com.example.petmeds.ui.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.repo.PetRepository
import com.example.petmeds.domain.model.Species
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

data class PetEditorUiState(
    val id: Long = 0,
    val name: String = "",
    val species: Species = Species.DOG,
    val breed: String = "",
    val weightKg: String = "",
    val birthDate: LocalDate? = null,
    val nameError: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class PetEditorViewModel @Inject constructor(
    private val petRepository: PetRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PetEditorUiState())
    val state: StateFlow<PetEditorUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val pet = petRepository.observeAll().first().firstOrNull() ?: return@launch
            _state.value = PetEditorUiState(
                id = pet.id,
                name = pet.name,
                species = pet.species,
                breed = pet.breed.orEmpty(),
                weightKg = pet.weightKg?.let { w ->
                    if (w == w.toLong().toFloat()) w.toLong().toString() else w.toString()
                }.orEmpty(),
                birthDate = pet.birthDate,
            )
        }
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
            _state.value = current.copy(nameError = true)
            return
        }
        if (current.saving) return
        _state.value = current.copy(saving = true)
        viewModelScope.launch {
            val pet = petRepository.observeAll().first()
                .firstOrNull { it.id == current.id } ?: return@launch
            petRepository.update(
                pet.copy(
                    name = current.name.trim(),
                    species = current.species,
                    breed = current.breed.trim().takeIf { it.isNotBlank() },
                    weightKg = current.weightKg.trim().toFloatOrNull(),
                    birthDate = current.birthDate,
                )
            )
            _state.value = _state.value.copy(saving = false, saved = true)
        }
    }
}
