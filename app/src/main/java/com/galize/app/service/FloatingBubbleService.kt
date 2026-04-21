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
        logger.i("FloatingBubbleService onCreate")
        
        // 检查必要的权限
        if (!checkRequiredPermissions()) {
            logger.e("Required permissions not granted, stopping service")
            stopSelf()
            return
        }
        
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            // Initialize AI clients
            cloudAiClient = CloudAiClient(promptBuilder)
            localAiClient = LocalAiClient()
            
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
                    logger.w("specialUse permission not available, using legacy startForeground")
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            
            showBubble()
            logger.i("FloatingBubbleService started successfully")
        } catch (e: Exception) {
            logger.e("Failed to create FloatingBubbleService", e)
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
                logger.w("Notification permission not granted")
                return false
            }
        }
        
        // 注意：MediaProjection 权限暂时不检查，后续实现截图功能时需要
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { ... }
        
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d("FloatingBubbleService onStartCommand")
        // Initialize screen capture if intent data is available
        screenCaptureIntent?.let { data ->
            logger.i("Initializing screen capture with intent data")
            screenCaptureManager = ScreenCaptureManager(this, screenCaptureResultCode, data)
        }
        // Return START_NOT_STICKY to prevent auto-restart when killed
        // This avoids restart when permissions are not granted
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.i("FloatingBubbleService onDestroy")
        serviceScope.cancel()
        removeBubble()
        removePanel()
        screenCaptureManager?.release()
    }

    private fun showBubble() {
        logger.d("Showing floating bubble")
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
        logger.d("Floating bubble added to window")
    }

    /**
     * Called when the floating bubble is tapped.
     * Triggers the full pipeline: Screenshot -> OCR -> AI -> Display
     */
    private fun onBubbleTapped() {
        logger.i("Bubble tapped - starting Galize pipeline")
        
        val captureManager = screenCaptureManager
        if (captureManager == null) {
            logger.e("Screen capture manager not initialized")
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
                logger.d("Step 1: Capturing screen")
                captureManager.captureScreen { bitmap ->
                    if (bitmap == null) {
                        logger.e("Screen capture failed")
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@FloatingBubbleService, "截图失败", Toast.LENGTH_SHORT).show()
                        }
                        return@captureScreen
                    }

                    // Continue pipeline with captured bitmap
                    launch(Dispatchers.IO) {
                        processCapturedScreen(bitmap)
                    }
                }
            } catch (e: Exception) {
                logger.e("Pipeline failed: ${e.message}", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@FloatingBubbleService, "处理失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Processes a captured screen through OCR and AI pipeline.
     */
    private suspend fun processCapturedScreen(bitmap: android.graphics.Bitmap) {
        try {
            // Step 2: OCR
            logger.d("Step 2: Running OCR")
            val ocrEngine = OcrEngine()
            val textBlocks = ocrEngine.recognizeText(bitmap)
            
            if (textBlocks.isEmpty()) {
                logger.w("No text detected in screenshot")
                launch(Dispatchers.Main) {
                    Toast.makeText(this@FloatingBubbleService, "未检测到文字", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // Step 3: Parse chat messages
            logger.d("Step 3: Parsing chat messages")
            val screenWidth = resources.displayMetrics.widthPixels
            val parser = ChatMessageParser()
            val messages = parser.parse(textBlocks, screenWidth)

            if (messages.isEmpty()) {
                logger.w("No chat messages parsed")
                launch(Dispatchers.Main) {
                    Toast.makeText(this@FloatingBubbleService, "未识别到对话消息", Toast.LENGTH_SHORT).show()
                }
                return
            }

            logger.i("Parsed ${messages.size} messages from OCR")

            // Step 4: AI generates choices
            logger.d("Step 4: Generating AI choices")
            val context = ConversationContext(messages = messages)
            
            val choiceResult: ChoiceResult = if (cloudAiClient.isAvailable()) {
                logger.d("Using cloud AI")
                cloudAiClient.generateChoices(context).getOrNull() 
                    ?: run {
                        logger.w("Cloud AI failed, falling back to local")
                        localAiClient.generateChoices(context).getOrNull()
                            ?: throw Exception("AI generation failed")
                    }
            } else {
                logger.d("Using local AI fallback")
                localAiClient.generateChoices(context).getOrNull()
                    ?: throw Exception("AI generation failed")
            }

            // Step 5: Show result panel
            logger.d("Step 5: Showing choice panel")
            launch(Dispatchers.Main) {
                showChoicePanel(choiceResult)
            }

        } catch (e: Exception) {
            logger.e("Pipeline processing failed: ${e.message}", e)
            launch(Dispatchers.Main) {
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
        logger.d("Showing choice panel")
        
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
        logger.d("Choice panel added to window")
    }

    private fun removeBubble() {
        bubbleView?.let {
            logger.d("Removing floating bubble")
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                logger.e("Error removing bubble: ${e.message}", e)
            }
            bubbleView = null
        }
    }

    private fun removePanel() {
        panelView?.let {
            logger.d("Removing choice panel")
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                logger.e("Error removing panel: ${e.message}", e)
            }
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
