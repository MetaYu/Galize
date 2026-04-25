package com.galize.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import com.galize.app.utils.GalizeLogger

/**
 * Manages screen capture using MediaProjection API.
 * 
 * ⚠️ IMPORTANT NOTE ABOUT PERMISSION:
 * - System shows "Screen Recording" permission dialog
 * - BUT we ONLY capture a SINGLE SCREENSHOT, NOT continuous recording
 * - This is an Android limitation: no direct screenshot API for third-party apps
 * - MediaProjection is the only way to capture screen content
 * 
 * Usage:
 * 1. User grants permission via system dialog (shows as "screen recording")
 * 2. We get resultCode and Intent data from the permission result
 * 3. Create this manager instance with those credentials
 * 4. Call [captureScreen] to capture a SINGLE screenshot (not video!)
 * 5. Call [release] when done to free resources
 * 
 * Privacy guarantee:
 * - We only capture ONE frame when user taps the floating bubble
 * - No continuous monitoring or recording
 * - Captured image is used immediately for OCR and then discarded
 * - No screenshots are saved to storage
 * 
 * Lifecycle:
 * 1. Create instance with valid resultCode and data from permission request
 * 2. Call [captureScreen] to capture a single screenshot
 * 3. Call [release] when done to free resources
 */
class ScreenCaptureManager(
    private val context: Context,
    private val resultCode: Int,
    private val data: Intent
) {
    private val logger = GalizeLogger("ScreenCapture")
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isCapturing = false

    init {
        // 初始化时创建 MediaProjection（只能创建一次）
        initializeMediaProjection()
    }

    private fun initializeMediaProjection() {
        try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            if (mediaProjection == null) {
                logger.E("Failed to create MediaProjection")
                return
            }

            // 注册回调来管理 MediaProjection 状态
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    logger.D("MediaProjection stopped")
                    release()
                }
            }
            mediaProjection?.registerCallback(callback, handler)
            
            // 初始化 ImageReader（只需创建一次）
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            
            // 创建 VirtualDisplay（只需创建一次）
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "GalizeCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null, handler
            )
            
            logger.I("MediaProjection initialized successfully: ${width}x${height}@${density}")
        } catch (e: Exception) {
            logger.E("Failed to initialize MediaProjection: ${e.message}", e)
        }
    }

    /**
     * Captures the current screen and returns a Bitmap.
     * 
     * @param callback Called with the captured bitmap, or null if capture failed
     */
    @SuppressLint("WrongConstant")
    fun captureScreen(callback: (Bitmap?) -> Unit) {
        if (isCapturing) {
            logger.W("Capture already in progress, ignoring")
            callback(null)
            return
        }

        if (mediaProjection == null || virtualDisplay == null || imageReader == null) {
            logger.E("Screen capture not initialized")
            callback(null)
            return
        }

        isCapturing = true
        logger.D("Starting screen capture")

        try {
            // 直接使用已初始化的 VirtualDisplay 和 ImageReader
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            // Delay slightly to allow frame to render
            handler.postDelayed({
                try {
                    val image = imageReader?.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        val bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)
                        image.close()

                        // Crop to actual screen width
                        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                        if (croppedBitmap != bitmap) bitmap.recycle()

                        logger.D("Screen capture successful, bitmap size: ${croppedBitmap.width}x${croppedBitmap.height}")
                        callback(croppedBitmap)
                    } else {
                        logger.E("Failed to acquire image from ImageReader")
                        callback(null)
                    }
                } catch (e: Exception) {
                    logger.E("Error during capture: ${e.message}", e)
                    callback(null)
                } finally {
                    releaseCapture()
                    isCapturing = false
                }
            }, 150)
        } catch (e: Exception) {
            logger.E("Failed to setup capture: ${e.message}", e)
            isCapturing = false
            callback(null)
        }
    }

    private fun releaseCapture() {
        // 注意：不要释放 virtualDisplay 和 imageReader，因为它们需要复用
        // 只需要清理 ImageReader 中的 pending images
        try {
            imageReader?.let { reader ->
                // 清除所有待处理的 images
                while (true) {
                    val image = reader.acquireNextImage()
                    if (image == null) break
                    image.close()
                }
            }
        } catch (e: Exception) {
            logger.E("Error clearing images: ${e.message}", e)
        }
    }

    /**
     * Releases all resources and stops the MediaProjection.
     * Call this when the service is destroyed or capture is no longer needed.
     */
    fun release() {
        logger.I("Releasing ScreenCaptureManager")
        releaseCapture()
        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            logger.E("Error stopping media projection: ${e.message}", e)
        }
        mediaProjection = null
        isCapturing = false
    }
}
