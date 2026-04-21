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

        isCapturing = true
        logger.D("Starting screen capture")

        try {
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            if (mediaProjection == null) {
                logger.E("Failed to create MediaProjection")
                isCapturing = false
                callback(null)
                return
            }

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "GalizeCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null, handler
            )

            logger.D("Virtual display created: ${width}x${height}@${density}")

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
        logger.D("Releasing capture resources")
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            logger.E("Error releasing capture: ${e.message}", e)
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
