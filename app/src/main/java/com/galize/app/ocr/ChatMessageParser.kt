package com.galize.app.ocr

import com.galize.app.model.ChatMessage
import com.galize.app.utils.GalizeLogger

/**
 * Parses OCR text blocks into structured ChatMessage objects.
 * 
 * Uses position heuristics to determine message ownership:
 * - Messages on the right side = from me (user)
 * - Messages on the left side = from the other party
 * 
 * The parser assumes typical chat app layouts where:
 * - User's messages are aligned to the right
 * - Other party's messages are aligned to the left
 * - Messages are ordered vertically by time
 * 
 * Limitations:
 * - Does not handle group chats (only 2 parties)
 * - May misclassify centered messages (e.g., timestamps, system messages)
 * - Assumes standard left-to-right, top-to-bottom reading order
 */
class ChatMessageParser {
    private val logger = GalizeLogger("ChatMessageParser")

    companion object {
        // Threshold: if a text block's center X is past this ratio of screen width, it's "mine"
        // 0.55 means 55% from the left edge (slightly right of center)
        private const val RIGHT_SIDE_THRESHOLD = 0.55f
        
        // Minimum text length to consider as a valid message
        private const val MIN_MESSAGE_LENGTH = 2
    }

    /**
     * Parse OCR blocks into chat messages.
     * 
     * @param blocks List of recognized text blocks with position info
     * @param screenWidth Width of the captured screen in pixels
     * @return List of parsed [ChatMessage] objects, sorted by vertical position
     */
    fun parse(blocks: List<OcrTextBlock>, screenWidth: Int = 1080): List<ChatMessage> {
        if (blocks.isEmpty()) {
            logger.D("No blocks to parse")
            return emptyList()
        }

        logger.D("Parsing ${blocks.size} blocks with screen width $screenWidth")

        val messages = blocks
            .filter { it.text.isNotBlank() && it.text.length >= MIN_MESSAGE_LENGTH }
            .sortedBy { it.lines.firstOrNull()?.top ?: 0 }
            .mapNotNull { block ->
                val centerX = block.boundingBox?.centerX() ?: 0
                val isFromMe = centerX > screenWidth * RIGHT_SIDE_THRESHOLD

                val message = ChatMessage(
                    text = block.text.trim(),
                    isFromMe = isFromMe,
                    positionX = centerX,
                    positionY = block.boundingBox?.top ?: 0
                )

                logger.D("Parsed: '${message.text.take(20)}...' from=${if (isFromMe) "me" else "other"} x=$centerX")
                message
            }

        logger.I("Successfully parsed ${messages.size} messages from ${blocks.size} blocks")
        return messages
    }

    /**
     * Overload for simplified pipeline usage.
     * Uses default screen width of 1080px.
     */
    fun parse(blocks: List<OcrTextBlock>): List<ChatMessage> {
        return parse(blocks, 1080)
    }
}
