package com.galize.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 权限管理器 - 统一处理应用所需的各种权限检测和引导
 */
object PermissionManager {
    
    /**
     * 权限类型枚举
     */
    enum class PermissionType {
        OVERLAY,          // 悬浮窗权限
        NOTIFICATION,     // 通知权限
        MEDIA_PROJECTION  // 屏幕截图权限
    }
    
    /**
     * 权限检查结果
     */
    data class PermissionResult(
        val type: PermissionType,
        val isGranted: Boolean,
        val title: String,
        val message: String,
        val settingsAction: String? = null
    )
    
    /**
     * 检查所有必要权限
     */
    fun checkAllPermissions(context: Context): List<PermissionResult> {
        return listOf(
            checkOverlayPermission(context),
            checkNotificationPermission(context),
            checkMediaProjectionPermission(context)
        )
    }
    
    /**
     * 检查悬浮窗权限
     */
    fun checkOverlayPermission(context: Context): PermissionResult {
        val isGranted = Settings.canDrawOverlays(context)
        return PermissionResult(
            type = PermissionType.OVERLAY,
            isGranted = isGranted,
            title = "悬浮窗权限",
            message = "Galize 需要悬浮窗权限来显示悬浮球和对话选项面板",
            settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
        )
    }
    
    /**
     * 检查通知权限
     */
    fun checkNotificationPermission(context: Context): PermissionResult {
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 以下不需要运行时权限
        }
        
        return PermissionResult(
            type = PermissionType.NOTIFICATION,
            isGranted = isGranted,
            title = "通知权限",
            message = "Galize 需要通知权限来运行前台服务",
        )
    }
    
    /**
     * 检查媒体投影权限（屏幕截图）
     */
    fun checkMediaProjectionPermission(context: Context): PermissionResult {
        // MediaProjection 权限比较特殊，不能直接检查是否已授权
        // 只能通过是否有有效的 MediaProjection token 来判断
        // 这里我们检查 FloatingBubbleService 中是否有有效的 intent 和 resultCode
        val isGranted = try {
            val hasValidIntent = com.galize.app.service.FloatingBubbleService.screenCaptureIntent != null
            val hasValidResultCode = com.galize.app.service.FloatingBubbleService.screenCaptureResultCode != 0
            hasValidIntent && hasValidResultCode
        } catch (e: Exception) {
            // 如果访问静态变量失败，返回未授权
            false
        }
        
        return PermissionResult(
            type = PermissionType.MEDIA_PROJECTION,
            isGranted = isGranted,
            title = "屏幕截图权限",
            message = "Galize 需要截取当前屏幕来分析游戏对话内容。\n\n" +
                     "⚠️ 系统会显示'屏幕录制'权限，但Galize只会：\n" +
                     "• 截取单张屏幕截图\n" +
                     "• 不会录制视频\n" +
                     "• 不会持续监控屏幕\n" +
                     "• 截图仅在分析时使用，不会保存\n\n" +
                     "请在首页点击'启动Galize'后，在系统弹窗中点击'立即开始'。",
        )
    }
    
    /**
     * 创建跳转到权限设置页面的 Intent
     */
    fun createPermissionSettingIntent(
        context: Context, 
        permissionType: PermissionType
    ): Intent? {
        return when (permissionType) {
            PermissionType.OVERLAY -> {
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    // 确保在新任务中打开
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            PermissionType.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    null
                }
            }
            PermissionType.MEDIA_PROJECTION -> {
                // MediaProjection 权限需要通过 MediaProjectionManager 请求
                val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
                    as MediaProjectionManager
                projectionManager.createScreenCaptureIntent()
            }
        }
    }
    
    /**
     * 获取未授权的权限列表
     */
    fun getDeniedPermissions(context: Context): List<PermissionResult> {
        return checkAllPermissions(context).filter { !it.isGranted }
    }
    
    /**
     * 检查是否所有权限都已授予
     */
    fun hasAllPermissions(context: Context): Boolean {
        return getDeniedPermissions(context).isEmpty()
    }
}
