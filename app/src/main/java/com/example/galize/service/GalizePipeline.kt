package com.example.galize.service

import android.content.Context
import android.graphics.Bitmap
import com.example.galize.model.ConversationContext
import com.example.galize.ocr.ChatMessageParser
import com.example.galize.ocr.OcrEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Orchestrates the full pipeline: Screenshot -> OCR -> AI -> Display
 */
object GalizePipeline {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun process(context: Context, bitmap: Bitmap) {
        scope.launch {
            // Step 1: OCR - recognize text blocks with position info
            val ocrEngine = OcrEngine()
            val textBlocks = ocrEngine.recognizeText(bitmap)

            // Step 2: Parse chat messages from OCR blocks
            val screenWidth = context.resources.displayMetrics.widthPixels
            val parser = ChatMessageParser()
            val messages = parser.parse(textBlocks, screenWidth)

            if (messages.isEmpty()) {
                bitmap.recycle()
                return@launch
            }

            // Step 3: Build conversation context
            val conversationContext = ConversationContext(messages = messages)

            // Step 4: AI generates choices
            // TODO: inject proper AiClient via DI in production
            // The result will be displayed via FloatingBubbleService.showChoicePanel()

            bitmap.recycle()
        }
    }
}
