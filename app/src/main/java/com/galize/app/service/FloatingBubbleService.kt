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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.room.Room
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import com.galize.app.R
import com.galize.app.ai.CloudAiClient
import com.galize.app.ai.LocalAiClient
import com.galize.app.ai.PromptBuilder
import com.galize.app.model.ChatMessage
import com.galize.app.model.ChoiceResult
import com.galize.app.model.ConversationContext
import com.galize.app.model.db.AppDatabase
import com.galize.app.model.db.ChatLogEntity
import com.galize.app.model.db.ConversationEntity
import com.galize.app.ocr.ChatMessageParser
import com.galize.app.ocr.OcrEngine
import com.galize.app.repository.ChatRepository
import com.galize.app.ui.overlay.FloatingBubbleContent
import com.galize.app.utils.GalizeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
    
    // Processing state drives bubble animation
    private val isProcessing = MutableStateFlow(false)
    private var screenCaptureManager: ScreenCaptureManager? = null
    
    // Coroutine scope for background tasks
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // AI clients
    private lateinit var cloudAiClient: CloudAiClient
    private lateinit var localAiClient: LocalAiClient
    private val promptBuilder = PromptBuilder()
    
    // Repository for saving chat history
    private lateinit var chatRepository: ChatRepository
    
    // Repository for reading AI settings
    private lateinit var settingsRepository: com.galize.app.repository.SettingsRepository

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
            
            // Initialize settings repository
            settingsRepository = com.galize.app.repository.SettingsRepository(applicationContext)
            
            // Load AI configuration from settings
            serviceScope.launch {
                try {
                    val apiKey = settingsRepository.apiKey.first()
                    val baseUrl = settingsRepository.apiBaseUrl.first()
                    val model = settingsRepository.aiModel.first()
                    val customPrompt = settingsRepository.customSystemPrompt.first()
                    
                    if (customPrompt.isNotBlank()) {
                        promptBuilder.setCustomSystemPrompt(customPrompt)
                        logger.I("Custom system prompt loaded")
                    }
                    
                    if (apiKey.isNotBlank()) {
                        cloudAiClient.configure(apiKey, baseUrl, model)
                        logger.I("AI client configured with model=$model")
                    } else {
                        logger.W("API key not configured, will use local AI fallback")
                    }
                } catch (e: Exception) {
                    logger.E("Failed to load AI configuration: ${e.message}", e)
                }
            }
            
            // Initialize chat repository for saving history
            val database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "galize_database"
            )
                .fallbackToDestructiveMigration(true)
                .build()
            chatRepository = ChatRepository(database)
            
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
                val processing by isProcessing.collectAsState()
                FloatingBubbleContent(
                    isProcessing = processing,
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

        // Set processing state to drive bubble animation
        isProcessing.value = true

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
        var conversationId: Long? = null
        
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

            // Step 2.5: Detect foreground app and extract contact name
            logger.D("Step 2.5: Detecting app and contact name")
            val appDetector = com.galize.app.utils.ForegroundAppDetector(this)
            val packageName = appDetector.getForegroundPackageName()
            val appType = appDetector.detectAppType(packageName)
            val appName = appDetector.getAppName(packageName)
            
            val screenHeight = resources.displayMetrics.heightPixels
            val screenWidth = resources.displayMetrics.widthPixels
            val parser = ChatMessageParser()
            val contactName = parser.extractContactName(textBlocks, screenHeight, screenWidth)
            
            logger.I("Detected app: $appName ($packageName), contact: $contactName")

            // Step 3: Parse chat messages with contact name and screen dimensions
            logger.D("Step 3: Parsing chat messages")
            val messages = parser.parse(textBlocks, screenWidth, screenHeight, contactName)

            if (messages.isEmpty()) {
                logger.W("No chat messages parsed")
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@FloatingBubbleService, "未识别到对话消息", Toast.LENGTH_SHORT).show()
                }
                return
            }

            logger.I("Parsed ${messages.size} messages from OCR")
            
            // Find or create conversation (reuse existing for same contact)
            conversationId = chatRepository.findOrCreateConversation(
                contactName = contactName,
                packageName = packageName,
                appType = appType.name
            )

            // Build ChatLogEntity list with sender name and display time
            val chatLogs = messages.map { message ->
                ChatLogEntity(
                    conversationId = conversationId,
                    text = message.text,
                    isFromMe = message.isFromMe,
                    senderName = message.senderName,
                    displayTime = message.displayTime
                )
            }

            // Append only new messages (auto-dedup against existing)
            val appendedCount = chatRepository.appendNewMessages(conversationId, chatLogs)
            logger.D("Appended $appendedCount new messages to conversation $conversationId")

            // Step 4: AI generates choices
            logger.D("Step 4: Generating AI choices")
            
            // Reload settings before AI call to pick up any changes
            try {
                val apiKey = settingsRepository.apiKey.first()
                val baseUrl = settingsRepository.apiBaseUrl.first()
                val model = settingsRepository.aiModel.first()
                val customPrompt = settingsRepository.customSystemPrompt.first()
                promptBuilder.setCustomSystemPrompt(customPrompt)
                if (apiKey.isNotBlank()) {
                    cloudAiClient.configure(apiKey, baseUrl, model)
                }
            } catch (e: Exception) {
                logger.W("Failed to reload settings: ${e.message}")
            }
            
            val context = ConversationContext(
                messages = messages,
                appType = appType
            )
            
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
            isProcessing.value = false
            serviceScope.launch(Dispatchers.Main) {
                showChoicePanel(choiceResult)
            }
            
            // Update conversation metadata
            conversationId?.let { id ->
                val totalCount = chatRepository.getMessageCount(id)
                val summary = messages.takeLast(3).joinToString(" | ") { 
                    "[${it.senderName}] ${it.text.take(30)}" 
                }
                chatRepository.updateConversation(
                    ConversationEntity(
                        id = id,
                        endedAt = System.currentTimeMillis(),
                        appType = appType.name,
                        packageName = packageName,
                        contactName = contactName,
                        totalAffinity = 50, // TODO: load from settings
                        messageCount = totalCount,
                        summary = summary
                    )
                )
            }

        } catch (e: Exception) {
            logger.E("Pipeline processing failed: ${e.message}", e)
            isProcessing.value = false
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
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
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
