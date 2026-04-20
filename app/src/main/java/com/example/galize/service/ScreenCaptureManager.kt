package com.example.galize.service

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

class ScreenCaptureManager(
    private val context: Context,
    private val resultCode: Int,
    private val data: Intent
) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("WrongConstant")
    fun captureScreen(callback: (Bitmap?) -> Unit) {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "GalizeCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null, handler
        )

        // Delay slightly to allow frame to render
        handler.postDelayed({
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

                callback(croppedBitmap)
            } else {
                callback(null)
            }
            releaseCapture()
        }, 150)
    }

    private fun releaseCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
    }

    fun release() {
        releaseCapture()
        mediaProjection?.stop()
        mediaProjection = null
    }
}
