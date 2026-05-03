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

    fun updateParentName(value: String?) {
        viewModelScope.launch {
            userPrefsRepository.setParentName(value)
        }
    }
}
