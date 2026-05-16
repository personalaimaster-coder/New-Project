package com.example.petmeds.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.prefs.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
) : ViewModel() {

    val parentName: StateFlow<String?> = userPrefsRepository.observeParentName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val gameSoundEnabled: StateFlow<Boolean> = userPrefsRepository.observeGameSoundEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val gameHapticsEnabled: StateFlow<Boolean> = userPrefsRepository.observeGameHapticsEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun updateParentName(value: String?) {
        viewModelScope.launch {
            userPrefsRepository.setParentName(value)
        }
    }

    fun setGameSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefsRepository.setGameSoundEnabled(enabled) }
    }

    fun setGameHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefsRepository.setGameHapticsEnabled(enabled) }
    }
}
