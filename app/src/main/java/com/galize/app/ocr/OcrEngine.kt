package com.galize.app.ocr

import android.graphics.Bitmap
import android.graphics.Color
import com.galize.app.utils.GalizeLogger
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

/**
 * OCR engine using Google ML Kit for text recognition.
 * Supports both English and Chinese text recognition.
 *
 * Precision-first strategy:
 * - Never downscale: ML Kit benefits from high-resolution input
 * - Upscale small images to ensure minimum 2160px width
 * - Convert to grayscale: color adds noise for text OCR
 * - Adaptive binarization: local thresholding preserves fine stroke details
 *   even across varying background colors (dark mode / light mode mix)
 * - No global contrast stretch (causes stroke distortion on similar chars)
 */
class OcrEngine {
    private val logger = GalizeLogger("OcrEngine")

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    /**
     * Recognizes text from a bitmap image.
     * Returns structured text blocks with position information.
     *
     * @param bitmap The image to recognize text from
     * @return List of [OcrTextBlock] containing recognized text and position data
     */
    suspend fun recognizeText(bitmap: Bitmap): List<OcrTextBlock> {
        logger.D("Starting text recognition, bitmap: ${bitmap.width}x${bitmap.height}")

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Preprocess image for better OCR accuracy
        val processedBitmap = preprocessImage(bitmap)

        // Compute inverse scale factor: map preprocessed coordinates → original coordinates.
        // This is critical because ChatMessageParser uses screen-space coordinates
        // for left/right ownership detection, and ML Kit returns bounding boxes
        // in the preprocessed image's coordinate space.
        val scaleX = originalWidth.toFloat() / processedBitmap.width
        val scaleY = originalHeight.toFloat() / processedBitmap.height
        logger.D("Coordinate scale: processed=${processedBitmap.width}x${processedBitmap.height}, " +
                "scaleBack=($scaleX, $scaleY)")

        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(processedBitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val blockCount = visionText.textBlocks.size
                    val lineCount = visionText.textBlocks.sumOf { it.lines.size }
                    logger.D("Recognition successful: $blockCount blocks, $lineCount lines")
                    logger.D("Recognized text length: ${visionText.text.length} chars")

                    val blocks = visionText.textBlocks.map { block ->
                        OcrTextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            confidence = null, // ML Kit TextRecognition doesn't provide confidence
                            lines = block.lines.map { line ->
                                // Scale bounding box back to original bitmap coordinates
                                val box = line.boundingBox
                                OcrTextLine(
                                    text = line.text,
                                    left = ((box?.left ?: 0) * scaleX).toInt(),
                                    right = ((box?.right ?: 0) * scaleX).toInt(),
                                    top = ((box?.top ?: 0) * scaleY).toInt(),
                                    bottom = ((box?.bottom ?: 0) * scaleY).toInt(),
                                    confidence = null
                                )
                            }
                        )
                    }

                    if (processedBitmap != bitmap) {
                        processedBitmap.recycle()
                    }

                    if (continuation.isActive) {
                        continuation.resume(blocks)
                    }
                }
                .addOnFailureListener { e ->
                    logger.E("Text recognition failed: ${e.message}", e)
                    if (processedBitmap != bitmap) {
                        processedBitmap.recycle()
                    }
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
        }
    }

    /**
     * Preprocesses image for maximum OCR precision.
     *
     * Pipeline:
     * 1. Upscale small images (never downscale — more pixels = more precision)
     * 2. Convert to grayscale (color is noise for text recognition)
     * 3. Adaptive binarization (local threshold per block, preserves fine strokes)
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        var current = bitmap

        try {
            // Step 1: Upscale only — NEVER downscale. High-res = high precision.
            val minWidth = 2160
            if (current.width < minWidth) {
                val scale = minWidth.toFloat() / current.width
                val newW = minWidth
                val newH = (current.height * scale).toInt()
                logger.D("Upscaling from ${current.width}x${current.height} to ${newW}x${newH}")
                val scaled = Bitmap.createScaledBitmap(current, newW, newH, true)
                if (scaled != current && current != bitmap) current.recycle()
                current = scaled
            }

            // Step 2: Convert to grayscale — removes color noise,
            // prevents the channel-mixing artifacts that confuse similar chars.
            val gray = toGrayscale(current)
            if (current != bitmap) current.recycle()
            current = gray

            // Step 3: Adaptive binarization — local thresholding per block.
            // Each region gets its own black/white cutoff, so dark-mode and
            // light-mode areas within the same screenshot are both handled.
            // Produces razor-sharp stroke edges without global distortion.
            val binary = adaptiveBinarize(current, blockSize = 31, cOffset = 10)
            if (current != bitmap) current.recycle()
            current = binary

            logger.D("Preprocessing complete: ${current.width}x${current.height}")
            return current

        } catch (e: Exception) {
            logger.E("Error during preprocessing: ${e.message}", e)
            return bitmap
        }
    }

    /**
     * Convert to grayscale using standard BT.601 luminance weights.
     * Returns a new ARGB_8888 bitmap where R=G=B=luma for every pixel.
     */
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val px = pixels[i]
            // BT.601: 0.299R + 0.587G + 0.114B, using integer math for speed
            val lum = (77 * Color.red(px) + 150 * Color.green(px) + 29 * Color.blue(px)) shr 8
            val g = lum.coerceIn(0, 255)
            pixels[i] = Color.argb(255, g, g, g)
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * Adaptive binarization (similar to OpenCV adaptiveThreshold / Sauvola-like).
     *
     * For each pixel, the threshold is the mean luminance of its [blockSize x blockSize]
     * neighborhood minus [cOffset]. Pixels darker than the local threshold become black (0),
     * otherwise white (255).
     *
     * This handles mixed backgrounds (dark mode + light mode, gradient headers, etc.)
     * far better than any global threshold or contrast stretch.
     *
     * Uses an integral image (summed area table) for O(1) per-pixel mean lookup.
     */
    private fun adaptiveBinarize(bitmap: Bitmap, blockSize: Int, cOffset: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // Extract luminance (already grayscale, so just take red channel)
        val lum = IntArray(w * h) { Color.red(pixels[it]) }

        // Build integral image (using Long to avoid overflow on large images)
        val integral = LongArray((w + 1) * (h + 1))
        val iw = w + 1
        for (y in 0 until h) {
            var rowSum = 0L
            for (x in 0 until w) {
                rowSum += lum[y * w + x]
                integral[(y + 1) * iw + (x + 1)] = rowSum + integral[y * iw + (x + 1)]
            }
        }

        // Helper: sum of a rectangle [x1, y1) to [x2, y2) from integral image
        fun rectSum(x1: Int, y1: Int, x2: Int, y2: Int): Long {
            return integral[y2 * iw + x2] - integral[y1 * iw + x2] -
                    integral[y2 * iw + x1] + integral[y1 * iw + x1]
        }

        val half = blockSize / 2
        val output = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                // Clamp neighborhood to image bounds
                val x1 = max(0, x - half)
                val y1 = max(0, y - half)
                val x2 = min(w, x + half + 1)
                val y2 = min(h, y + half + 1)

                val area = (x2 - x1) * (y2 - y1)
                val mean = (rectSum(x1, y1, x2, y2) / area).toInt()
                val threshold = mean - cOffset

                val v = if (lum[y * w + x] < threshold) 0 else 255
                output[y * w + x] = Color.argb(255, v, v, v)
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, w, 0, 0, w, h)
        logger.D("Adaptive binarization complete: blockSize=$blockSize, cOffset=$cOffset")
        return result
    }
}

data class OcrTextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val confidence: Float? = null,
    val lines: List<OcrTextLine>
)

data class OcrTextLine(
    val text: String,
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int,
    val confidence: Float? = null
)
