package com.galize.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.galize.app.R
import com.galize.app.ui.overlay.FloatingBubbleContent

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
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Initialize screen capture if intent data is available
        screenCaptureIntent?.let { data ->
            screenCaptureManager = ScreenCaptureManager(this, screenCaptureResultCode, data)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBubble()
        removePanel()
        screenCaptureManager?.release()
    }

    private fun showBubble() {
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
                    onTap = { onBubbleTapped() }
                )
            }
            // Setup lifecycle for Compose
            val lifecycleOwner = ServiceLifecycleOwner()
            lifecycleOwner.performStart()
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        // Make bubble draggable
        setupDragListener(bubbleView!!, params)
        windowManager.addView(bubbleView, params)
    }

    private fun setupDragListener(view: ComposeView, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 25) isMoved = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun onBubbleTapped() {
        // Trigger screen capture and process
        screenCaptureManager?.captureScreen { bitmap ->
            if (bitmap != null) {
                // Send to OCR -> AI pipeline
                GalizePipeline.process(this, bitmap)
            }
        }
    }

    fun showChoicePanel() {
        // Will be implemented in Task 5
    }

    private fun removeBubble() {
        bubbleView?.let {
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
