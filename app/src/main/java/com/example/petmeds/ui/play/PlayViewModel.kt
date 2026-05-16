package com.example.petmeds.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmeds.data.prefs.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class PlayViewModel @Inject constructor(
    private val userPrefs: UserPrefsRepository,
    private val audio: GameAudio,
    private val haptics: GameHaptics,
) : ViewModel() {

    private val rng: Random = Random.Default

    private val _state = MutableStateFlow(GameEngine.initial(highScore = 0))
    val state: StateFlow<GameState> = _state.asStateFlow()

    @Volatile
    private var pendingSwipe: Swipe? = null

    init {
        viewModelScope.launch {
            userPrefs.observeHighScore().collect { stored ->
                _state.update { current ->
                    val previousHigh = if (current.phase == GamePhase.IDLE) stored else current.previousHighScore
                    current.copy(highScore = stored, previousHighScore = previousHigh)
                }
            }
        }
        viewModelScope.launch {
            userPrefs.observeGameSoundEnabled().collect(audio::setEnabled)
        }
        viewModelScope.launch {
            userPrefs.observeGameHapticsEnabled().collect(haptics::setEnabled)
        }
    }

    fun start() {
        val result = GameEngine.start(_state.value)
        _state.value = result.state
        dispatchEvents(result.events)
    }

    fun swipe(swipe: Swipe) {
        if (_state.value.phase != GamePhase.PLAYING) return
        pendingSwipe = swipe
    }

    fun onFrame(deltaMs: Long) {
        if (_state.value.phase != GamePhase.PLAYING) return
        val safeDelta = if (deltaMs > 100L) 100L else deltaMs
        val swipe = pendingSwipe
        pendingSwipe = null
        val result = GameEngine.tick(_state.value, safeDelta, swipe, rng)
        _state.value = result.state
        if (result.events.isNotEmpty()) dispatchEvents(result.events)
    }

    private fun dispatchEvents(events: List<GameEvent>) {
        for (event in events) {
            when (event) {
                is GameEvent.Started -> {
                    audio.play(GameSound.START)
                    haptics.start()
                }
                is GameEvent.LaneChanged -> {
                    haptics.lightTick()
                }
                is GameEvent.Caught -> {
                    audio.play(GameSound.CATCH)
                    haptics.lightTick()
                }
                is GameEvent.Hit -> {
                    audio.play(GameSound.HIT)
                    haptics.heavy()
                    persistHighScoreIfBeaten(_state.value.score)
                }
            }
        }
    }

    private fun persistHighScoreIfBeaten(score: Int) {
        viewModelScope.launch {
            userPrefs.setHighScoreIfBetter(score)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audio.release()
    }
}
