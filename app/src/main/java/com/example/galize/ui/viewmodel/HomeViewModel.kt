package com.example.galize.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.example.galize.service.FloatingBubbleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission

    fun checkPermissions(context: Context) {
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)
    }

    fun toggleService(context: Context) {
        checkPermissions(context)
        if (!_hasOverlayPermission.value) return

        if (_isServiceRunning.value) {
            context.stopService(Intent(context, FloatingBubbleService::class.java))
            _isServiceRunning.value = false
        } else {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.startForegroundService(intent)
            _isServiceRunning.value = true
        }
    }
}
