package com.galize.app.ocr

import android.graphics.Bitmap
import com.galize.app.utils.GalizeLogger
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * OCR engine using Google ML Kit for text recognition.
 * Supports both English and Chinese text recognition.
 * 
 * Performance notes:
 * - Recognition typically takes 100-500ms depending on image complexity
 * - Model is downloaded on-demand, first run may be slower
 * - Runs on-device, no network required
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
     * @throws Exception if recognition fails
     */
    suspend fun recognizeText(bitmap: Bitmap): List<OcrTextBlock> {
        logger.D("Starting text recognition, bitmap: ${bitmap.width}x${bitmap.height}")
        
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val blockCount = visionText.textBlocks.size
                    val lineCount = visionText.textBlocks.sumOf { it.lines.size }
                    logger.D("Recognition successful: $blockCount blocks, $lineCount lines")
                    
                    val blocks = visionText.textBlocks.map { block ->
                        OcrTextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            confidence = block.confidence,
                            lines = block.lines.map { line ->
                                OcrTextLine(
                                    text = line.text,
                                    confidence = line.confidence,
                                    left = line.boundingBox?.left ?: 0,
                                    right = line.boundingBox?.right ?: 0,
                                    top = line.boundingBox?.top ?: 0,
                                    bottom = line.boundingBox?.bottom ?: 0
                                )
                            }
                        )
                    }
                    
                    if (continuation.isActive) {
                        continuation.resume(blocks)
                    }
                }
                .addOnFailureListener { e ->
                    logger.E("Text recognition failed: ${e.message}", e)
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
        }
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
    val confidence: Float? = null,
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int
)
