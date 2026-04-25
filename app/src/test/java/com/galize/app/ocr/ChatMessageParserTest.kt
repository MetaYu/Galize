package com.galize.app.ocr

import android.graphics.Rect
import com.galize.app.model.ChatMessage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ChatMessageParser.
 * Tests the WeChat-optimized parsing logic for OCR text blocks.
 */
class ChatMessageParserTest {

    private lateinit var parser: ChatMessageParser

    @BeforeEach
    fun setUp() {
        parser = ChatMessageParser()
    }

    // Helper: create a block with a single line at given position
    private fun makeBlock(
        text: String,
        left: Int, top: Int, right: Int, bottom: Int
    ) = OcrTextBlock(
        text = text,
        boundingBox = Rect(left, top, right, bottom),
        lines = listOf(
            OcrTextLine(text = text, left = left, right = right, top = top, bottom = bottom)
        )
    )

    // Screen dimensions used in tests
    private val SW = 1080
    private val SH = 2400

    @Nested
    @DisplayName("Parse Empty Input Tests")
    inner class ParseEmptyInputTests {

        @Test
        fun `should return empty list when no blocks provided`() {
            val result = parser.parse(emptyList(), SW, SH)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `should return empty list when all blocks are blank`() {
            val blankBlocks = listOf(
                OcrTextBlock(text = "", boundingBox = null, lines = listOf()),
                OcrTextBlock(text = "   ", boundingBox = null, lines = listOf()),
            )
            val result = parser.parse(blankBlocks, SW, SH)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Message Ownership Detection Tests")
    inner class MessageOwnershipDetectionTests {

        @Test
        fun `should detect message from me when right edge is near screen right`() {
            // Right edge at 950/1080 = 88% > 82% threshold
            val block = makeBlock("Hello!", 550, 500, 950, 550)
            val result = parser.parse(listOf(block), SW, SH)

            assertEquals(1, result.size)
            assertTrue(result[0].isFromMe)
            assertEquals("Hello!", result[0].text)
        }

        @Test
        fun `should detect message from other when left edge is near screen left`() {
            // Left edge at 80/1080 = 7% < 18% threshold
            val block = makeBlock("I'm good!", 80, 500, 450, 550)
            val result = parser.parse(listOf(block), SW, SH)

            assertEquals(1, result.size)
            assertFalse(result[0].isFromMe)
        }

        @Test
        fun `should handle multiple messages with correct ownership`() {
            // Messages far apart vertically so they don't merge
            val blocks = listOf(
                makeBlock("Hey there!", 80, 400, 400, 450),        // left side
                makeBlock("Hi! How are you?", 550, 600, 950, 650), // right side, >60px gap
                makeBlock("Doing great!", 80, 800, 400, 850),      // left side, >60px gap
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(3, result.size)
            assertFalse(result[0].isFromMe)
            assertTrue(result[1].isFromMe)
            assertFalse(result[2].isFromMe)
        }

        @Test
        fun `should correctly identify long message from other even when right edge exceeds threshold`() {
            // Long text from other person that spans nearly full width
            // left=80 (7%), right=900 (83%) -> old logic would wrongly say "from me"
            val block = makeBlock("This is a very long message from the other person that spans almost the entire screen width", 80, 500, 900, 550)
            val result = parser.parse(listOf(block), SW, SH)

            assertEquals(1, result.size)
            assertFalse(result[0].isFromMe)  // Should be from other because left edge < 18%
        }

        @Test
        fun `should correctly identify short message from me even when right edge is not near screen right`() {
            // Short text from me: left=600 (56%), right=750 (69%) -> neither threshold met
            val block = makeBlock("好的", 600, 500, 750, 550)
            val result = parser.parse(listOf(block), SW, SH)

            assertEquals(1, result.size)
            assertTrue(result[0].isFromMe)  // left > 30% -> from me via fallback
        }
    }

    @Nested
    @DisplayName("Message Sorting Tests")
    inner class MessageSortingTests {

        @Test
        fun `should sort messages by vertical position`() {
            // Messages far apart so they don't merge
            val blocks = listOf(
                makeBlock("Third", 80, 900, 400, 950),
                makeBlock("First", 80, 400, 400, 450),
                makeBlock("Second", 550, 650, 950, 700),
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(3, result.size)
            assertEquals("First", result[0].text)
            assertEquals("Second", result[1].text)
            assertEquals("Third", result[2].text)
        }
    }

    @Nested
    @DisplayName("Line Merging Tests")
    inner class LineMergingTests {

        @Test
        fun `should merge consecutive lines from same side into one message`() {
            // Two lines from the same bubble (right side, close together)
            val blocks = listOf(
                makeBlock("Hello,", 550, 500, 950, 530),
                makeBlock("how are you?", 550, 535, 950, 565),  // 5px gap, same side
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(1, result.size)
            assertEquals("Hello,how are you?", result[0].text)
            assertTrue(result[0].isFromMe)
        }

        @Test
        fun `should not merge lines from different sides`() {
            val blocks = listOf(
                makeBlock("From other", 80, 500, 400, 530),
                makeBlock("From me", 550, 535, 950, 565),
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(2, result.size)
            assertFalse(result[0].isFromMe)
            assertTrue(result[1].isFromMe)
        }

        @Test
        fun `should not merge lines with large vertical gap`() {
            // Two lines from same side but far apart (different bubbles)
            val blocks = listOf(
                makeBlock("First bubble", 80, 400, 400, 430),
                makeBlock("Second bubble", 80, 600, 400, 630), // 170px gap > 60px threshold
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(2, result.size)
            assertEquals("First bubble", result[0].text)
            assertEquals("Second bubble", result[1].text)
        }

        @Test
        fun `should merge three lines in same bubble`() {
            val blocks = listOf(
                makeBlock("Line one", 550, 500, 950, 530),
                makeBlock("Line two", 550, 535, 950, 565),
                makeBlock("Line three", 550, 570, 950, 600),
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(1, result.size)
            assertEquals("Line oneLine twoLine three", result[0].text)
        }
    }

    @Nested
    @DisplayName("Timestamp Filtering Tests")
    inner class TimestampFilteringTests {

        @Test
        fun `should filter centered timestamps`() {
            val blocks = listOf(
                makeBlock("18:47", 400, 350, 680, 380),        // centered timestamp
                makeBlock("Hello!", 550, 500, 950, 550),       // real message
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(1, result.size)
            assertEquals("Hello!", result[0].text)
        }

        @Test
        fun `should extract timestamp and associate with subsequent message`() {
            val blocks = listOf(
                makeBlock("18:47", 400, 350, 680, 380),        // centered timestamp
                makeBlock("Hello!", 550, 500, 950, 550),       // message after timestamp
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(1, result.size)
            assertEquals("18:47", result[0].displayTime)
        }

        @Test
        fun `should recognize various time formats`() {
            val timeTexts = listOf("18:47", "昨天 20:30", "星期三 18:47", "3月15日", "2024年3月15日 18:47")
            timeTexts.forEach { time ->
                val blocks = listOf(
                    makeBlock(time, 400, 350, 680, 380),
                    makeBlock("Test", 550, 500, 950, 550),
                )
                val result = parser.parse(blocks, SW, SH)
                assertEquals(1, result.size, "Time '$time' should be filtered")
            }
        }
    }

    @Nested
    @DisplayName("Title Bar Filtering Tests")
    inner class TitleBarFilteringTests {

        @Test
        fun `should filter text in title bar area`() {
            // Title bar is top 12% = top 288px on 2400px screen
            val blocks = listOf(
                makeBlock("鸭鸭", 400, 80, 680, 120),          // title bar
                makeBlock("Hello!", 550, 500, 950, 550),       // real message
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(1, result.size)
            assertEquals("Hello!", result[0].text)
        }
    }

    @Nested
    @DisplayName("Bottom Bar Filtering Tests")
    inner class BottomBarFilteringTests {

        @Test
        fun `should filter text in bottom input bar area`() {
            // Bottom bar is bottom 8% = below 2208px on 2400px screen
            val blocks = listOf(
                makeBlock("Hello!", 550, 500, 950, 550),       // real message
                makeBlock("输入消息", 200, 2300, 600, 2350),     // bottom input bar
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(1, result.size)
            assertEquals("Hello!", result[0].text)
        }
    }

    @Nested
    @DisplayName("System Message Filtering Tests")
    inner class SystemMessageFilteringTests {

        @Test
        fun `should filter system messages`() {
            val blocks = listOf(
                makeBlock("对方撤回了一条消息", 300, 450, 780, 480), // system msg
                makeBlock("Hello!", 550, 600, 950, 650),
            )
            val result = parser.parse(blocks, SW, SH)

            assertEquals(1, result.size)
            assertEquals("Hello!", result[0].text)
        }
    }

    @Nested
    @DisplayName("Contact Name Extraction Tests")
    inner class ContactNameExtractionTests {

        @Test
        fun `should extract contact name from title bar`() {
            val blocks = listOf(
                makeBlock("鸭鸭", 400, 80, 680, 120),
                makeBlock("Hello!", 550, 500, 950, 550),
            )
            val name = parser.extractContactName(blocks, SH, SW)
            assertEquals("鸭鸭", name)
        }

        @Test
        fun `should ignore timestamps in title area`() {
            val blocks = listOf(
                makeBlock("21:42", 400, 10, 680, 40),   // status bar time
                makeBlock("鸭鸭", 400, 80, 680, 120),   // contact name
            )
            val name = parser.extractContactName(blocks, SH, SW)
            assertEquals("鸭鸭", name)
        }

        @Test
        fun `should return empty when no title found`() {
            val blocks = listOf(
                makeBlock("Hello!", 550, 500, 950, 550),
            )
            val name = parser.extractContactName(blocks, SH, SW)
            assertEquals("", name)
        }
    }

    @Nested
    @DisplayName("Sender Name Assignment Tests")
    inner class SenderNameTests {

        @Test
        fun `should assign contact name to other party messages`() {
            val blocks = listOf(
                makeBlock("你好", 80, 500, 400, 550),
            )
            val result = parser.parse(blocks, SW, SH, "鸭鸭")

            assertEquals(1, result.size)
            assertFalse(result[0].isFromMe)
            assertEquals("鸭鸭", result[0].senderName)
        }

        @Test
        fun `should assign 我 to my messages`() {
            val blocks = listOf(
                makeBlock("Hello!", 550, 500, 950, 550),
            )
            val result = parser.parse(blocks, SW, SH, "鸭鸭")

            assertEquals(1, result.size)
            assertTrue(result[0].isFromMe)
            assertEquals("我", result[0].senderName)
        }

        @Test
        fun `should use 对方 when contact name is empty`() {
            val blocks = listOf(
                makeBlock("你好", 80, 500, 400, 550),
            )
            val result = parser.parse(blocks, SW, SH, "")

            assertEquals("对方", result[0].senderName)
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    inner class EdgeCasesTests {

        @Test
        fun `should handle large number of messages`() {
            // Messages far apart vertically so they don't merge (>60px gap)
            val blocks = (0 until 20).map { i ->
                val isLeft = i % 2 == 0
                val left = if (isLeft) 80 else 550
                val right = if (isLeft) 450 else 950
                val top = 400 + i * 80  // 80px gap > merge threshold
                makeBlock("Message $i", left, top, right, top + 30)
            }
            val result = parser.parse(blocks, SW, SH)
            assertEquals(20, result.size)
        }

        @Test
        fun `should trim whitespace from message text`() {
            val block = makeBlock("  Trimmed message  \n", 550, 500, 950, 550)
            val result = parser.parse(listOf(block), SW, SH)

            assertEquals(1, result.size)
            assertEquals("Trimmed message", result[0].text)
        }
    }
}
