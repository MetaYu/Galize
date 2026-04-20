package com.example.galize.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "galize_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        private val KEY_PERSONA = stringPreferencesKey("persona")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    val apiBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_BASE_URL] ?: "https://api.openai.com/v1"
    }

    val persona: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PERSONA] ?: "default"
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[KEY_API_BASE_URL] = url }
    }

    suspend fun setPersona(persona: String) {
        context.dataStore.edit { it[KEY_PERSONA] = persona }
    }
}
