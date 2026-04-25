package com.galize.app.ocr

import com.galize.app.model.ChatMessage
import com.galize.app.utils.GalizeLogger

/**
 * Parses OCR text blocks into structured ChatMessage objects.
 *
 * WeChat-specific heuristics:
 * - Title bar (top 8-12%): contact name, centered
 * - Timestamps: centered, short text matching time patterns (e.g. "18:47")
 * - Right-side green bubbles: from me (right boundary near screen edge 85-95%)
 * - Left-side white bubbles: from the other party
 * - Quote/reply messages: "XX：content" pattern, smaller font, filtered out
 * - Bottom input bar (bottom 8%): filtered out
 * - System messages: centered, matching known patterns
 */
class ChatMessageParser {
    private val logger = GalizeLogger("ChatMessageParser")

    companion object {
        // If a message's right edge is past this ratio, it's likely "from me" (green bubble)
        private const val RIGHT_EDGE_THRESHOLD = 0.82f

        // If a message's left edge is before this ratio, it's likely "from other" (white bubble)
        private const val LEFT_EDGE_THRESHOLD = 0.18f

        // Minimum text length to consider as a valid message
        private const val MIN_MESSAGE_LENGTH = 1

        // Title bar occupies roughly top 8-12% of screen
        private const val TITLE_BAR_RATIO = 0.12f

        // Bottom input bar occupies roughly bottom 8%
        private const val BOTTOM_BAR_RATIO = 0.08f

        // Time pattern: "HH:mm", "昨天 HH:mm", "星期X HH:mm", "MM月DD日", "YYYY年MM月DD日" etc.
        private val TIME_PATTERNS = listOf(
            Regex("^\\d{1,2}:\\d{2}$"),                                    // 18:47
            Regex("^昨天\\s*\\d{1,2}:\\d{2}$"),                            // 昨天 18:47
            Regex("^前天\\s*\\d{1,2}:\\d{2}$"),                            // 前天 18:47
            Regex("^星期[一二三四五六日天]\\s*\\d{1,2}:\\d{2}$"),            // 星期三 18:47
            Regex("^周[一二三四五六日天]\\s*\\d{1,2}:\\d{2}$"),             // 周三 18:47
            Regex("^\\d{1,2}月\\d{1,2}日\\s*(\\d{1,2}:\\d{2})?$"),         // 3月15日 18:47
            Regex("^\\d{4}年\\d{1,2}月\\d{1,2}日\\s*(\\d{1,2}:\\d{2})?$"), // 2024年3月15日 18:47
            Regex("^\\d{4}/\\d{1,2}/\\d{1,2}\\s*(\\d{1,2}:\\d{2})?$"),     // 2024/3/15 18:47
            Regex("^\\d{1,2}-\\d{1,2}\\s*\\d{1,2}:\\d{2}$"),               // 3-15 18:47
        )

        // System message patterns to filter out
        private val SYSTEM_MESSAGE_PATTERNS = listOf(
            Regex(".*撤回了一条消息.*"),
            Regex("^以下为新消息$"),
            Regex("^你已添加了.*"),
            Regex("^你已成为.*"),
            Regex("^消息已发出.*"),
            Regex("^你邀请.*"),
            Regex("^\".*\"邀请.*"),
            Regex("^对方开启了朋友验证.*"),
            Regex("^你已打开了.*"),
            Regex("^你和.*成为了朋友.*"),
        )

        // Quote/reply pattern: "名字：引用内容" — typically shown smaller below the message
        private val QUOTE_PATTERN = Regex("^.{1,10}[：:]\\s*.+")
    }

    /**
     * Parse OCR blocks into chat messages with WeChat-optimized filtering.
     * Lines from the same chat bubble are merged into a single message.
     *
     * @param blocks List of recognized text blocks with position info
     * @param screenWidth Width of the captured screen in pixels
     * @param screenHeight Height of the captured screen in pixels
     * @param contactName Contact name to assign to other's messages
     * @return List of parsed [ChatMessage] objects, sorted by vertical position
     */
    fun parse(
        blocks: List<OcrTextBlock>,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400,
        contactName: String = ""
    ): List<ChatMessage> {
        if (blocks.isEmpty()) {
            logger.D("No blocks to parse")
            return emptyList()
        }

        logger.D("Parsing ${blocks.size} blocks (screen: ${screenWidth}x${screenHeight})")

        val titleBarBottom = (screenHeight * TITLE_BAR_RATIO).toInt()
        val bottomBarTop = (screenHeight * (1 - BOTTOM_BAR_RATIO)).toInt()
        // Max vertical gap between lines to consider them part of the same bubble
        val mergeGapThreshold = (screenHeight * 0.025).toInt() // ~2.5% of screen height

        // Step 1: Flatten all blocks to lines, filter, and classify
        data class ClassifiedLine(
            val text: String,
            val isFromMe: Boolean,
            val left: Int,
            val right: Int,
            val top: Int,
            val bottom: Int,
            val isTimestamp: Boolean = false,
            val timestampText: String = ""
        )

        val classifiedLines = mutableListOf<ClassifiedLine>()
        val allLines = blocks.flatMap { block ->
            block.lines.map { line -> line to block }
        }.sortedBy { it.first.top }

        for ((line, _) in allLines) {
            val text = line.text.trim()
            if (text.isEmpty()) continue

            val top = line.top
            val bottom = line.bottom
            val left = line.left
            val right = line.right
            val centerX = (left + right) / 2
            val lineHeight = bottom - top

            // Filter: skip title bar area
            if (top < titleBarBottom) {
                logger.D("Skipped title bar text: '$text'")
                continue
            }

            // Filter: skip bottom input bar area
            if (top > bottomBarTop) {
                logger.D("Skipped bottom bar text: '$text'")
                continue
            }

            // Check if this is a timestamp (centered + matches time pattern)
            val isCentered = centerX > screenWidth * 0.3 && centerX < screenWidth * 0.7
            if (isCentered && isTimestamp(text)) {
                classifiedLines.add(ClassifiedLine(
                    text = text, isFromMe = false,
                    left = left, right = right, top = top, bottom = bottom,
                    isTimestamp = true, timestampText = text
                ))
                logger.D("Detected timestamp: '$text'")
                continue
            }

            // Filter: system messages (centered + matches pattern)
            if (isCentered && isSystemMessage(text)) {
                logger.D("Skipped system message: '$text'")
                continue
            }

            // Filter: quote/reply references
            if (isQuoteMessage(text, lineHeight, screenHeight)) {
                logger.D("Skipped quote/reply: '$text'")
                continue
            }

            // Filter: too short
            if (text.length < MIN_MESSAGE_LENGTH) continue

            val isFromMe = determineOwnership(left, right, screenWidth)
            classifiedLines.add(ClassifiedLine(
                text = text, isFromMe = isFromMe,
                left = left, right = right, top = top, bottom = bottom
            ))
        }

        // Step 2: Merge consecutive lines from the same side into single messages
        val messages = mutableListOf<ChatMessage>()
        var currentDisplayTime = ""
        var pendingTexts = mutableListOf<String>()
        var pendingIsFromMe = false
        var pendingTop = 0
        var pendingBottom = 0
        var pendingLeft = 0
        var pendingRight = 0
        var pendingTime = ""

        fun flushPending() {
            if (pendingTexts.isEmpty()) return
            val mergedText = pendingTexts.joinToString("")
            val senderName = if (pendingIsFromMe) "我" else contactName.ifEmpty { "对方" }
            val centerX = (pendingLeft + pendingRight) / 2
            messages.add(ChatMessage(
                text = mergedText,
                isFromMe = pendingIsFromMe,
                senderName = senderName,
                displayTime = pendingTime,
                positionX = centerX,
                positionY = pendingTop
            ))
            logger.D("Merged ${pendingTexts.size} lines -> [$senderName] '${mergedText.take(30)}' time='$pendingTime'")
            pendingTexts = mutableListOf()
        }

        for (cl in classifiedLines) {
            if (cl.isTimestamp) {
                // Flush any pending message before timestamp
                flushPending()
                currentDisplayTime = cl.timestampText
                continue
            }

            if (pendingTexts.isEmpty()) {
                // Start new pending message
                pendingTexts.add(cl.text)
                pendingIsFromMe = cl.isFromMe
                pendingTop = cl.top
                pendingBottom = cl.bottom
                pendingLeft = cl.left
                pendingRight = cl.right
                pendingTime = currentDisplayTime
            } else if (cl.isFromMe == pendingIsFromMe && (cl.top - pendingBottom) < mergeGapThreshold) {
                // Same side and vertically close -> merge into same message
                pendingTexts.add(cl.text)
                pendingBottom = cl.bottom
                pendingLeft = minOf(pendingLeft, cl.left)
                pendingRight = maxOf(pendingRight, cl.right)
            } else {
                // Different side or too far apart -> flush and start new
                flushPending()
                pendingTexts.add(cl.text)
                pendingIsFromMe = cl.isFromMe
                pendingTop = cl.top
                pendingBottom = cl.bottom
                pendingLeft = cl.left
                pendingRight = cl.right
                pendingTime = currentDisplayTime
            }
        }
        flushPending()

        logger.I("Successfully parsed ${messages.size} messages from ${blocks.size} blocks")
        return messages
    }

    /**
     * Determine message ownership using left/right edge heuristics.
     *
     * Key insight: LEFT edge is the most reliable indicator in WeChat:
     * - "My" green bubbles start from mid-screen (left edge > 35%)
     * - "Other's" white bubbles start from near screen left (left edge < 18%)
     * - Right edge varies with text length and is less reliable
     *
     * When a long message from the other person spans the full width,
     * right edge may exceed 82% — but left edge still starts near 5-15%.
     */
    private fun determineOwnership(left: Int, right: Int, screenWidth: Int): Boolean {
        val rightEdgeRatio = right.toFloat() / screenWidth
        val leftEdgeRatio = left.toFloat() / screenWidth

        val isNearLeft = leftEdgeRatio < LEFT_EDGE_THRESHOLD   // < 18%
        val isNearRight = rightEdgeRatio > RIGHT_EDGE_THRESHOLD // > 82%

        // Case 1: Left edge near screen left → from other (most reliable signal)
        // This catches long messages from other that also reach the right edge
        if (isNearLeft) {
            logger.D("Ownership: FROM_OTHER (left=${leftEdgeRatio}, right=${rightEdgeRatio}) — left near edge")
            return false
        }

        // Case 2: Right edge near screen right and left NOT near left → from me
        if (isNearRight) {
            logger.D("Ownership: FROM_ME (left=${leftEdgeRatio}, right=${rightEdgeRatio}) — right near edge")
            return true
        }

        // Case 3: Neither edge is definitive
        // "My" messages have left edge > ~35% (bubble starts mid-screen)
        // "Other's" messages have left edge < ~25%
        val fromMe = leftEdgeRatio > 0.30f
        logger.D("Ownership: ${if (fromMe) "FROM_ME" else "FROM_OTHER"} (left=${leftEdgeRatio}, right=${rightEdgeRatio}) — fallback")
        return fromMe
    }

    /**
     * Check if a text matches timestamp patterns.
     */
    private fun isTimestamp(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > 20) return false  // Timestamps are short
        return TIME_PATTERNS.any { it.matches(trimmed) }
    }

    /**
     * Check if text is a system message.
     */
    private fun isSystemMessage(text: String): Boolean {
        return SYSTEM_MESSAGE_PATTERNS.any { it.matches(text) }
    }

    /**
     * Check if text is a quote/reply reference.
     * Quote messages in WeChat are typically:
     * - Format: "名字：引用的消息内容"
     * - Displayed with smaller font (smaller line height)
     */
    private fun isQuoteMessage(text: String, lineHeight: Int, screenHeight: Int): Boolean {
        // Quote messages typically have smaller font, less than 2% of screen height
        val isSmallFont = lineHeight < screenHeight * 0.02
        val matchesQuotePattern = QUOTE_PATTERN.matches(text) && text.length > 3
        return isSmallFont && matchesQuotePattern
    }

    /**
     * Extract contact name from the top area of the screen.
     * In WeChat, the contact name is displayed centered in the title bar.
     *
     * @param blocks List of recognized text blocks
     * @param screenHeight Height of the screen
     * @param screenWidth Width of the screen
     * @return Extracted contact name, or empty string if not found
     */
    fun extractContactName(
        blocks: List<OcrTextBlock>,
        screenHeight: Int,
        screenWidth: Int
    ): String {
        if (blocks.isEmpty()) return ""

        // Title bar area: top 8-12%
        val topAreaHeight = (screenHeight * TITLE_BAR_RATIO).toInt()

        // Flatten to lines for more precise matching
        val topLines = blocks.flatMap { it.lines }
            .filter { line ->
                val centerX = (line.left + line.right) / 2
                // Must be in title bar and roughly centered (30%-70% of width)
                line.top < topAreaHeight &&
                centerX > screenWidth * 0.3 &&
                centerX < screenWidth * 0.7
            }
            .sortedBy { it.top }

        if (topLines.isEmpty()) {
            logger.D("No contact name found in top area")
            return ""
        }

        // Find the best candidate: filter out common non-name text
        val contactLine = topLines.firstOrNull { line ->
            val text = line.text.trim()
            text.isNotEmpty() &&
            text.length <= 20 &&                           // Names are usually short
            !text.contains(":") &&                         // Not a timestamp
            !text.contains("：") &&
            !text.all { it.isDigit() } &&                  // Not just numbers
            !isTimestamp(text) &&                           // Not a time pattern
            !text.startsWith("返回") &&
            !text.startsWith("<")
        }

        val contactName = contactLine?.text?.trim() ?: ""
        logger.I("Extracted contact name: '$contactName'")
        return contactName
    }

    /**
     * Overload for simplified pipeline usage.
     */
    fun parse(blocks: List<OcrTextBlock>): List<ChatMessage> {
        return parse(blocks, 1080, 2400, "")
    }
}
