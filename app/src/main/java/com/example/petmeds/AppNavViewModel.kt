package com.example.petmeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.repo.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Decides whether to show the first-run screen or the main timeline. */
@HiltViewModel
class AppNavViewModel @Inject constructor(
    petRepository: PetRepository,
) : ViewModel() {

    val petCount: StateFlow<Int> = petRepository.observeCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1)
}
