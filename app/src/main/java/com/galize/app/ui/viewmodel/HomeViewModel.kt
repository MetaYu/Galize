package com.galize.app.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import com.galize.app.service.FloatingBubbleService
import com.galize.app.utils.GalizeLogger
import com.galize.app.utils.PermissionManager
import com.galize.app.utils.PermissionManager.PermissionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class PermissionDialogState(
    val showDialog: Boolean = false,
    val permissionResult: PermissionResult? = null
)

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
    
    // 权限对话框状态
    private val _permissionDialogState = MutableStateFlow(PermissionDialogState())
    val permissionDialogState: StateFlow<PermissionDialogState> = _permissionDialogState
    
    // 媒体投影权限请求标记
    private val _shouldRequestMediaProjection = MutableStateFlow(false)
    val shouldRequestMediaProjection: StateFlow<Boolean> = _shouldRequestMediaProjection

    fun checkPermissions(context: Context) {
        val overlayResult = PermissionManager.checkOverlayPermission(context)
        val notificationResult = PermissionManager.checkNotificationPermission(context)
        val mediaProjectionResult = PermissionManager.checkMediaProjectionPermission(context)
        
        _hasOverlayPermission.value = overlayResult.isGranted
        _hasNotificationPermission.value = notificationResult.isGranted
        _hasMediaProjectionPermission.value = mediaProjectionResult.isGranted
        
        logger.D("Permissions: overlay=${_hasOverlayPermission.value}, notification=${_hasNotificationPermission.value}, mediaProjection=${_hasMediaProjectionPermission.value}")
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearPermissionDialog() {
        _permissionDialogState.value = PermissionDialogState()
    }
    
    fun showPermissionDialog(permissionResult: PermissionResult) {
        _permissionDialogState.value = PermissionDialogState(
            showDialog = true,
            permissionResult = permissionResult
        )
    }
    
    /**
     * 请求特定权限
     */
    fun requestPermission(context: Context, permissionType: PermissionManager.PermissionType) {
        when (permissionType) {
            PermissionManager.PermissionType.OVERLAY -> {
                showPermissionDialog(PermissionManager.checkOverlayPermission(context))
            }
            PermissionManager.PermissionType.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    showPermissionDialog(PermissionManager.checkNotificationPermission(context))
                }
            }
            PermissionManager.PermissionType.MEDIA_PROJECTION -> {
                // 媒体投影权限需要特殊处理，通过 ActivityResultLauncher 请求
                _shouldRequestMediaProjection.value = true
            }
        }
    }
    
    /**
     * 处理媒体投影权限请求结果
     */
    fun onMediaProjectionPermissionResult(granted: Boolean, data: android.content.Intent? = null) {
        _shouldRequestMediaProjection.value = false
        _hasMediaProjectionPermission.value = granted
        
        if (granted && data != null) {
            // 保存intent和resultCode到Service的静态变量
            // 注意：这里 resultCode 应该从 ActivityResult 中获取，但我们暂时用 RESULT_OK
            com.galize.app.service.FloatingBubbleService.screenCaptureIntent = data
            com.galize.app.service.FloatingBubbleService.screenCaptureResultCode = android.app.Activity.RESULT_OK
            logger.I("Media projection permission granted, intent saved")
        } else if (!granted) {
            _errorMessage.value = "屏幕截图权限被拒绝"
        }
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
