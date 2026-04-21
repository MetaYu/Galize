package com.galize.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.galize.app.service.FloatingBubbleService
import com.galize.app.utils.GalizeLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val logger = GalizeLogger("HomeViewModel")

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission

    private val _hasNotificationPermission = MutableStateFlow(false)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission

    private val _hasMediaProjectionPermission = MutableStateFlow(false)
    val hasMediaProjectionPermission: StateFlow<Boolean> = _hasMediaProjectionPermission
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun checkPermissions(context: Context) {
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)
        
        // Check notification permission (required for Android 13+)
        _hasNotificationPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Below Android 13, notification permission not required at runtime
        }
        
        // TODO: MediaProjection 权限检查后续实现截图功能时需要
        // 暂时设为 true，不阻塞服务启动
        _hasMediaProjectionPermission.value = true
        
        logger.D("Permissions: overlay=${_hasOverlayPermission.value}, notification=${_hasNotificationPermission.value}")
    }
    
    fun clearError() {
        _errorMessage.value = null
    }

    fun toggleService(context: Context) {
        logger.I("toggleService called, current running state: ${_isServiceRunning.value}")
        checkPermissions(context)
        
        if (!_hasOverlayPermission.value) {
            val msg = "请先授予悬浮窗权限"
            logger.W(msg)
            _errorMessage.value = msg
            return
        }
        
        if (!_hasNotificationPermission.value) {
            val msg = "请先授予通知权限（Android 13+必需）"
            logger.W(msg)
            _errorMessage.value = msg
            return
        }
        
        if (!_hasMediaProjectionPermission.value) {
            val msg = "请先授予媒体投影权限（Android 14+必需）"
            logger.W(msg)
            _errorMessage.value = msg
            return
        }

        if (_isServiceRunning.value) {
            logger.I("Stopping FloatingBubbleService")
            context.stopService(Intent(context, FloatingBubbleService::class.java))
            _isServiceRunning.value = false
        } else {
            logger.I("Starting FloatingBubbleService")
            try {
                val intent = Intent(context, FloatingBubbleService::class.java)
                context.startForegroundService(intent)
                _isServiceRunning.value = true
            } catch (e: SecurityException) {
                val msg = "权限不足：${e.message}"
                logger.E(msg, e)
                _errorMessage.value = msg
            } catch (e: Exception) {
                val msg = "启动失败：${e.message ?: e.javaClass.simpleName}"
                logger.E(msg, e)
                _errorMessage.value = msg
            }
        }
    }
}
