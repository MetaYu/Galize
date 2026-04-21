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
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.galize.app.R
import com.galize.app.ai.CloudAiClient
import com.galize.app.ai.LocalAiClient
import com.galize.app.ai.PromptBuilder
import com.galize.app.model.ChoiceResult
import com.galize.app.model.ConversationContext
import com.galize.app.ocr.ChatMessageParser
import com.galize.app.ocr.OcrEngine
import com.galize.app.ui.overlay.FloatingBubbleContent
import com.galize.app.utils.GalizeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FloatingBubbleService : Service() {

    companion object {
        const val CHANNEL_ID = "galize_floating_channel"
        const val NOTIFICATION_ID = 1001
        var screenCaptureIntent: Intent? = null
        var screenCaptureResultCode: Int = 0
    }

    private val logger = GalizeLogger("FloatingBubbleService")
    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var panelView: ComposeView? = null
    private var screenCaptureManager: ScreenCaptureManager? = null
    
    // Coroutine scope for background tasks
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // AI clients
    private lateinit var cloudAiClient: CloudAiClient
    private lateinit var localAiClient: LocalAiClient
    private val promptBuilder = PromptBuilder()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        logger.I("FloatingBubbleService onCreate")
        
        // 检查必要的权限
        if (!checkRequiredPermissions()) {
            logger.E("Required permissions not granted, stopping service")
            stopSelf()
            return
        }
        
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            // Initialize AI clients
            cloudAiClient = CloudAiClient(promptBuilder)
            localAiClient = LocalAiClient()
            
            createNotificationChannel()
            
            // Android 14+ 需要指定前台服务类型
            // 使用 FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION 以支持 MediaProjection API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ 使用 mediaProjection 类型
                startForeground(
                    NOTIFICATION_ID, 
                    createNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            
            showBubble()
            logger.I("FloatingBubbleService started successfully")
        } catch (e: Exception) {
            logger.E("Failed to create FloatingBubbleService", e)
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
                logger.W("Notification permission not granted")
                return false
            }
        }
        
        // 注意：MediaProjection 权限暂时不检查，后续实现截图功能时需要
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { ... }
        
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.D("FloatingBubbleService onStartCommand")
        // Initialize screen capture if intent data is available
        screenCaptureIntent?.let { data ->
            logger.I("Initializing screen capture with intent data")
            screenCaptureManager = ScreenCaptureManager(this, screenCaptureResultCode, data)
        }
        // Return START_NOT_STICKY to prevent auto-restart when killed
        // This avoids restart when permissions are not granted
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.I("FloatingBubbleService onDestroy")
        serviceScope.cancel()
        removeBubble()
        removePanel()
        screenCaptureManager?.release()
    }

    private fun showBubble() {
        logger.D("Showing floating bubble")
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
        logger.D("Floating bubble added to window")
    }

    /**
     * Called when the floating bubble is tapped.
     * Triggers the full pipeline: Screenshot -> OCR -> AI -> Display
     */
    private fun onBubbleTapped() {
        logger.I("Bubble tapped - starting Galize pipeline")
        
        // 检查屏幕截图权限
        if (!checkScreenCapturePermission()) {
            logger.W("Screen capture permission not available")
            Toast.makeText(
                this,
                "请先授予屏幕截图权限以使用此功能",
                Toast.LENGTH_LONG
            ).show()
            // 这里可以发送广播或事件通知主Activity显示权限请求对话框
            // 但由于Service无法直接启动Activity，我们只提示用户
            return
        }
        
        val captureManager = screenCaptureManager
        if (captureManager == null) {
            logger.E("Screen capture manager not initialized")
            Toast.makeText(
                this,
                "请先在 Galize 应用中授权屏幕截图权限",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Show processing notification
        Toast.makeText(this, "正在分析对话...", Toast.LENGTH_SHORT).show()

        // Execute pipeline in background
        serviceScope.launch {
            try {
                // Step 1: Capture screen
                logger.D("Step 1: Capturing screen")
                captureManager.captureScreen { bitmap ->
                    if (bitmap == null) {
                        logger.E("Screen capture failed")
                        serviceScope.launch(Dispatchers.Main) {
                            Toast.makeText(this@FloatingBubbleService, "截图失败", Toast.LENGTH_SHORT).show()
                        }
                        return@captureScreen
                    }

                    // Continue pipeline with captured bitmap
                    serviceScope.launch(Dispatchers.IO) {
                        processCapturedScreen(bitmap)
                    }
                }
            } catch (e: Exception) {
                logger.E("Pipeline failed: ${e.message}", e)
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@FloatingBubbleService, "处理失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * 检查屏幕截图权限是否可用
     */
    private fun checkScreenCapturePermission(): Boolean {
        // 检查是否有 MediaProjection 权限
        // 由于 MediaProjection 权限无法直接检查，我们检查 screenCaptureManager 是否已初始化
        // 以及是否有有效的 screenCaptureIntent
        return screenCaptureIntent != null && screenCaptureResultCode != 0
    }

    /**
     * Processes a captured screen through OCR and AI pipeline.
     */
    private suspend fun processCapturedScreen(bitmap: android.graphics.Bitmap) {
        try {
            // Step 2: OCR
            logger.D("Step 2: Running OCR")
            val ocrEngine = OcrEngine()
            val textBlocks = ocrEngine.recognizeText(bitmap)
            
            if (textBlocks.isEmpty()) {
                logger.W("No text detected in screenshot")
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@FloatingBubbleService, "未检测到文字", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // Step 3: Parse chat messages
            logger.D("Step 3: Parsing chat messages")
            val screenWidth = resources.displayMetrics.widthPixels
            val parser = ChatMessageParser()
            val messages = parser.parse(textBlocks, screenWidth)

            if (messages.isEmpty()) {
                logger.W("No chat messages parsed")
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@FloatingBubbleService, "未识别到对话消息", Toast.LENGTH_SHORT).show()
                }
                return
            }

            logger.I("Parsed ${messages.size} messages from OCR")

            // Step 4: AI generates choices
            logger.D("Step 4: Generating AI choices")
            val context = ConversationContext(messages = messages)
            
            val choiceResult: ChoiceResult = if (cloudAiClient.isAvailable()) {
                logger.D("Using cloud AI")
                cloudAiClient.generateChoices(context).getOrNull() 
                    ?: run {
                        logger.W("Cloud AI failed, falling back to local")
                        localAiClient.generateChoices(context).getOrNull()
                            ?: throw Exception("AI generation failed")
                    }
            } else {
                logger.D("Using local AI fallback")
                localAiClient.generateChoices(context).getOrNull()
                    ?: throw Exception("AI generation failed")
            }

            // Step 5: Show result panel
            logger.D("Step 5: Showing choice panel")
            serviceScope.launch(Dispatchers.Main) {
                showChoicePanel(choiceResult)
            }

        } catch (e: Exception) {
            logger.E("Pipeline processing failed: ${e.message}", e)
            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(this@FloatingBubbleService, "处理失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Shows the choice panel with AI-generated responses.
     */
    fun showChoicePanel(choiceResult: ChoiceResult) {
        logger.D("Showing choice panel")
        
        // Remove existing panel if any
        removePanel()
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 100 // Offset from bottom
        }

        panelView = ComposeView(this).apply {
            setContent {
                com.galize.app.ui.overlay.ChoicePanel(
                    choiceResult = choiceResult,
                    currentAffinity = 50, // TODO: load from settings
                    onDismiss = { removePanel() }
                )
            }
            // Setup lifecycle for Compose
            val lifecycleOwner = ServiceLifecycleOwner()
            lifecycleOwner.performStart()
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        windowManager.addView(panelView!!, params)
        logger.D("Choice panel added to window")
    }

    private fun removeBubble() {
        bubbleView?.let {
            logger.D("Removing floating bubble")
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                logger.E("Error removing bubble: ${e.message}", e)
            }
            bubbleView = null
        }
    }

    private fun removePanel() {
        panelView?.let {
            logger.D("Removing choice panel")
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                logger.E("Error removing panel: ${e.message}", e)
            }
            panelView = null
        }
    }

    private fun createNotificationChannel() {
        logger.D("Creating notification channel")
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
