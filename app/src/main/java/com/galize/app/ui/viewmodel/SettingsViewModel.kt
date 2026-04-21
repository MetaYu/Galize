package com.galize.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galize.app.repository.SettingsRepository
import com.galize.app.utils.PermissionManager
import com.galize.app.utils.PermissionManager.PermissionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionState(
    val permissions: List<PermissionResult> = emptyList(),
    val showDialog: Boolean = false,
    val currentPermission: PermissionResult? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _apiBaseUrl = MutableStateFlow("https://api.openai.com/v1")
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl

    private val _selectedPersona = MutableStateFlow("default")
    val selectedPersona: StateFlow<String> = _selectedPersona
    
    // 权限状态
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState
    
    // 媒体投影权限请求标记
    private val _shouldRequestMediaProjection = MutableStateFlow(false)
    val shouldRequestMediaProjection: StateFlow<Boolean> = _shouldRequestMediaProjection

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
        // 初始化时检查权限
        checkPermissions()
    }
    
    /**
     * 检查所有权限状态
     */
    fun checkPermissions() {
        val permissions = PermissionManager.checkAllPermissions(context)
        _permissionState.value = _permissionState.value.copy(permissions = permissions)
    }
    
    /**
     * 请求特定权限
     */
    fun requestPermission(permissionType: PermissionManager.PermissionType) {
        when (permissionType) {
            PermissionManager.PermissionType.OVERLAY,
            PermissionManager.PermissionType.NOTIFICATION -> {
                val permission = _permissionState.value.permissions.find { it.type == permissionType }
                permission?.let {
                    _permissionState.value = _permissionState.value.copy(
                        showDialog = true,
                        currentPermission = it
                    )
                }
            }
            PermissionManager.PermissionType.MEDIA_PROJECTION -> {
                // 媒体投影权限需要特殊处理
                _shouldRequestMediaProjection.value = true
            }
        }
    }
    
    /**
     * 关闭权限对话框
     */
    fun clearPermissionDialog() {
        _permissionState.value = _permissionState.value.copy(
            showDialog = false,
            currentPermission = null
        )
    }
    
    /**
     * 处理媒体投影权限请求结果
     */
    fun onMediaProjectionPermissionResult(granted: Boolean, data: android.content.Intent? = null) {
        _shouldRequestMediaProjection.value = false
        
        if (granted && data != null) {
            // 保存intent和resultCode到Service的静态变量
            com.galize.app.service.FloatingBubbleService.screenCaptureIntent = data
            com.galize.app.service.FloatingBubbleService.screenCaptureResultCode = android.app.Activity.RESULT_OK
        }
        
        // 重新检查权限状态
        checkPermissions()
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
