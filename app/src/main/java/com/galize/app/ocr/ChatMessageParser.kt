package com.galize.app.ocr

import com.galize.app.model.ChatMessage

/**
 * Parses OCR text blocks into structured ChatMessage objects.
 * Uses position heuristics to determine message ownership:
 * - Messages on the right side = from me
 * - Messages on the left side = from the other party
 */
class ChatMessageParser {

    companion object {
        // Threshold: if a text block's center X is past this ratio of screen width, it's "mine"
        private const val RIGHT_SIDE_THRESHOLD = 0.55f
    }

    /**
     * Parse OCR blocks into chat messages.
     * @param blocks List of recognized text blocks with position info
     * @param screenWidth Width of the captured screen in pixels
     */
    fun parse(blocks: List<OcrTextBlock>, screenWidth: Int = 1080): List<ChatMessage> {
        if (blocks.isEmpty()) return emptyList()

        return blocks
            .filter { it.text.isNotBlank() }
            .sortedBy { it.lines.firstOrNull()?.top ?: 0 }
            .map { block ->
                val centerX = block.boundingBox?.centerX() ?: 0
                val isFromMe = centerX > screenWidth * RIGHT_SIDE_THRESHOLD

                ChatMessage(
                    text = block.text.trim(),
                    isFromMe = isFromMe
                )
            }
    }

    /**
     * Overload for simplified pipeline usage.
     */
    fun parse(blocks: List<OcrTextBlock>): List<ChatMessage> {
        return parse(blocks, 1080)
    }
}
