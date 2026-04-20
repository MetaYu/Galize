package com.example.galize.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galize.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _apiBaseUrl = MutableStateFlow("https://api.openai.com/v1")
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl

    private val _selectedPersona = MutableStateFlow("default")
    val selectedPersona: StateFlow<String> = _selectedPersona

    init {
        viewModelScope.launch {
            settingsRepository.apiKey.collect { _apiKey.value = it }
        }
        viewModelScope.launch {
            settingsRepository.apiBaseUrl.collect { _apiBaseUrl.value = it }
        }
        viewModelScope.launch {
            settingsRepository.persona.collect { _selectedPersona.value = it }
        }
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
        viewModelScope.launch { settingsRepository.setApiKey(key) }
    }

    fun updateApiBaseUrl(url: String) {
        _apiBaseUrl.value = url
        viewModelScope.launch { settingsRepository.setApiBaseUrl(url) }
    }

    fun updatePersona(persona: String) {
        _selectedPersona.value = persona
        viewModelScope.launch { settingsRepository.setPersona(persona) }
    }
}
