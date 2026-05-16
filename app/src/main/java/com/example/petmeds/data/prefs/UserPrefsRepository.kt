package com.example.petmeds.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface UserPrefsRepository {
    fun observeParentName(): Flow<String?>
    suspend fun setParentName(name: String?)

    fun observeHighScore(): Flow<Int>
    /** Persists `score` only if strictly greater than the stored value. Returns true on write. */
    suspend fun setHighScoreIfBetter(score: Int): Boolean

    fun observeGameSoundEnabled(): Flow<Boolean>
    suspend fun setGameSoundEnabled(enabled: Boolean)

    fun observeGameHapticsEnabled(): Flow<Boolean>
    suspend fun setGameHapticsEnabled(enabled: Boolean)
}

@Singleton
class UserPrefsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPrefsRepository {

    override fun observeParentName(): Flow<String?> =
        dataStore.data.map { prefs -> prefs[KEY_PARENT_NAME]?.takeIf { it.isNotBlank() } }

    override suspend fun setParentName(name: String?) {
        dataStore.edit { prefs ->
            val trimmed = name?.trim()
            if (trimmed.isNullOrBlank()) prefs.remove(KEY_PARENT_NAME)
            else prefs[KEY_PARENT_NAME] = trimmed
        }
    }

    override fun observeHighScore(): Flow<Int> =
        dataStore.data.map { prefs -> prefs[KEY_HIGH_SCORE] ?: 0 }

    override suspend fun setHighScoreIfBetter(score: Int): Boolean {
        var wrote = false
        dataStore.edit { prefs ->
            val current = prefs[KEY_HIGH_SCORE] ?: 0
            if (score > current) {
                prefs[KEY_HIGH_SCORE] = score
                wrote = true
            }
        }
        return wrote
    }

    override fun observeGameSoundEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_GAME_SOUND_ENABLED] ?: true }

    override suspend fun setGameSoundEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_GAME_SOUND_ENABLED] = enabled }
    }

    override fun observeGameHapticsEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_GAME_HAPTICS_ENABLED] ?: true }

    override suspend fun setGameHapticsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_GAME_HAPTICS_ENABLED] = enabled }
    }

    private companion object {
        val KEY_PARENT_NAME = stringPreferencesKey("parent_name")
        val KEY_HIGH_SCORE = intPreferencesKey("game_high_score")
        val KEY_GAME_SOUND_ENABLED = booleanPreferencesKey("game_sound_enabled")
        val KEY_GAME_HAPTICS_ENABLED = booleanPreferencesKey("game_haptics_enabled")
    }
}
