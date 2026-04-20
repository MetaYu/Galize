package com.galize.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.galize.app.R
import com.galize.app.ui.overlay.FloatingBubbleContent
import com.galize.app.utils.GalizeLogger

class FloatingBubbleService : Service() {

    companion object {
        const val CHANNEL_ID = "galize_floating_channel"
        const val NOTIFICATION_ID = 1001
        var screenCaptureIntent: Intent? = null
        var screenCaptureResultCode: Int = 0
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var panelView: ComposeView? = null
    private var screenCaptureManager: ScreenCaptureManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        GalizeLogger.i("FloatingBubbleService onCreate")
        
        // 检查必要的权限
        if (!checkRequiredPermissions()) {
            GalizeLogger.e("Required permissions not granted, stopping service")
            stopSelf()
            return
        }
        
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            createNotificationChannel()
            
            // Android 14+ 需要指定前台服务类型，但需要对应权限
            // 暂时使用兼容方式：不指定类型（仅用于测试悬浮球显示）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ 尝试使用 specialUse，如果失败则不使用类型
                try {
                    startForeground(
                        NOTIFICATION_ID, 
                        createNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (e: SecurityException) {
                    GalizeLogger.w("specialUse permission not available, using legacy startForeground")
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            
            showBubble()
            GalizeLogger.i("FloatingBubbleService started successfully")
        } catch (e: Exception) {
            GalizeLogger.e("Failed to create FloatingBubbleService", e)
            stopSelf()
        }
    }
    
    /**
     * 检查必要的权限
     * 简化版：只检查悬浮窗权限，MediaProjection 后续实现
     */
    private fun checkRequiredPermissions(): Boolean {
        // 检查通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasNotificationPermission) {
                GalizeLogger.w("Notification permission not granted")
                return false
            }
        }
        
        // 注意：MediaProjection 权限暂时不检查，后续实现截图功能时需要
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { ... }
        
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        GalizeLogger.d("FloatingBubbleService onStartCommand")
        // Initialize screen capture if intent data is available
        screenCaptureIntent?.let { data ->
            GalizeLogger.i("Initializing screen capture with intent data")
            screenCaptureManager = ScreenCaptureManager(this, screenCaptureResultCode, data)
        }
        // Return START_NOT_STICKY to prevent auto-restart when killed
        // This avoids restart when permissions are not granted
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        GalizeLogger.i("FloatingBubbleService onDestroy")
        removeBubble()
        removePanel()
        screenCaptureManager?.release()
    }

    private fun showBubble() {
        GalizeLogger.d("Showing floating bubble")
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        bubbleView = ComposeView(this).apply {
            setContent {
                FloatingBubbleContent(
                    onTap = { onBubbleTapped() },
                    onDrag = { dx, dy ->
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        windowManager.updateViewLayout(this@apply, params)
                    }
                )
            }
            // Setup lifecycle for Compose
            val lifecycleOwner = ServiceLifecycleOwner()
            lifecycleOwner.performStart()
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        windowManager.addView(bubbleView!!, params)
        GalizeLogger.d("Floating bubble added to window")
    }

    // 删除 setupDragListener，改用 Compose 内部的 pointerInput

    private fun onBubbleTapped() {
        GalizeLogger.i("Bubble tapped - showing test message")
        // TODO: 实现完整的截图 -> OCR -> AI 流程
        // 暂时显示测试提示
        android.widget.Toast.makeText(
            this,
            "Galize 悬浮球已激活！\n截图功能开发中...",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    fun showChoicePanel() {
        // Will be implemented in Task 5
    }

    private fun removeBubble() {
        bubbleView?.let {
            GalizeLogger.d("Removing floating bubble")
            windowManager.removeView(it)
            bubbleView = null
        }
    }

    private fun removePanel() {
        panelView?.let {
            windowManager.removeView(it)
            panelView = null
        }
    }

    private fun createNotificationChannel() {
        GalizeLogger.d("Creating notification channel")
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Galize Floating Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Galize floating bubble active"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Galize")
            .setContentText("Floating bubble is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
