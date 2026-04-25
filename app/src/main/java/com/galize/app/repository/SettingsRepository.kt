package com.galize.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "galize_settings")

/**
 * Repository for managing app settings using DataStore.
 * 
 * Stored settings:
 * - API Key for cloud AI service
 * - API Base URL (supports custom endpoints)
 * - AI Model name
 * - User persona configuration
 * - Current affinity score
 */
@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        private val KEY_AI_MODEL = stringPreferencesKey("ai_model")
        private val KEY_PERSONA = stringPreferencesKey("persona")
        private val KEY_AFFINITY = intPreferencesKey("affinity")
        private val KEY_CUSTOM_SYSTEM_PROMPT = stringPreferencesKey("custom_system_prompt")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    val apiBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_BASE_URL] ?: "https://api.openai.com/v1"
    }

    val aiModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_MODEL] ?: "gpt-4o-mini"
    }

    val persona: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PERSONA] ?: "default"
    }

    val affinity: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_AFFINITY] ?: 50
    }

    val customSystemPrompt: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_SYSTEM_PROMPT] ?: ""
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[KEY_API_BASE_URL] = url }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { it[KEY_AI_MODEL] = model }
    }

    suspend fun setPersona(persona: String) {
        context.dataStore.edit { it[KEY_PERSONA] = persona }
    }

    suspend fun setAffinity(value: Int) {
        context.dataStore.edit { it[KEY_AFFINITY] = value.coerceIn(0, 100) }
    }

    suspend fun updateAffinity(delta: Int) {
        context.dataStore.data.map { prefs ->
            prefs[KEY_AFFINITY] ?: 50
        }.collect { current ->
            setAffinity(current + delta)
        }
    }

    suspend fun setCustomSystemPrompt(prompt: String) {
        context.dataStore.edit { it[KEY_CUSTOM_SYSTEM_PROMPT] = prompt }
    }
}
