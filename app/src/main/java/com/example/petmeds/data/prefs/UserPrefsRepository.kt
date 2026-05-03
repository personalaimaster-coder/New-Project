package com.example.petmeds.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface UserPrefsRepository {
    fun observeParentName(): Flow<String?>
    suspend fun setParentName(name: String?)
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

    private companion object {
        val KEY_PARENT_NAME = stringPreferencesKey("parent_name")
    }
}
