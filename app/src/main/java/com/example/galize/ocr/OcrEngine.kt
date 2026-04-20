package com.example.galize.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * OCR engine using Google ML Kit for text recognition.
 */
class OcrEngine {

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    /**
     * Recognizes text from a bitmap image.
     * Returns structured text blocks with position information.
     */
    suspend fun recognizeText(bitmap: Bitmap): List<OcrTextBlock> {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.map { block ->
                        OcrTextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            lines = block.lines.map { line ->
                                OcrTextLine(
                                    text = line.text,
                                    left = line.boundingBox?.left ?: 0,
                                    right = line.boundingBox?.right ?: 0,
                                    top = line.boundingBox?.top ?: 0,
                                    bottom = line.boundingBox?.bottom ?: 0
                                )
                            }
                        )
                    }
                    continuation.resume(blocks)
                }
                .addOnFailureListener {
                    continuation.resume(emptyList())
                }
        }
    }
}

data class OcrTextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val lines: List<OcrTextLine>
)

data class OcrTextLine(
    val text: String,
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int
)
