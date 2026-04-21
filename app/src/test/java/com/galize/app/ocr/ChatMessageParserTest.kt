package com.galize.app.ocr

import android.graphics.Rect
import com.galize.app.model.ChatMessage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ChatMessageParser using JUnit5 + MockK
 * 
 * Tests the parsing logic for OCR text blocks into structured chat messages.
 */
class ChatMessageParserTest {

    private lateinit var parser: ChatMessageParser

    @BeforeEach
    fun setUp() {
        parser = ChatMessageParser()
    }

    @Nested
    @DisplayName("Parse Empty Input Tests")
    inner class ParseEmptyInputTests {

        @Test
        fun `should return empty list when no blocks provided`() {
            // Given
            val emptyBlocks = emptyList<OcrTextBlock>()

            // When
            val result = parser.parse(emptyBlocks)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `should return empty list when all blocks are blank`() {
            // Given
            val blankBlocks = listOf(
                OcrTextBlock(text = "", boundingBox = null, lines = listOf()),
                OcrTextBlock(text = "   ", boundingBox = null, lines = listOf()),
                OcrTextBlock(text = "\n", boundingBox = null, lines = listOf())
            )

            // When
            val result = parser.parse(blankBlocks)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `should filter out messages shorter than minimum length`() {
            // Given
            val shortBlocks = listOf(
                OcrTextBlock(
                    text = "a",
                    boundingBox = Rect(100, 100, 200, 200),
                    lines = listOf(OcrTextLine(text = "a", left = 100, right = 200, top = 100, bottom = 200))
                ),
                OcrTextBlock(
                    text = "ab",
                    boundingBox = Rect(100, 300, 200, 400),
                    lines = listOf(OcrTextLine(text = "ab", left = 100, right = 200, top = 300, bottom = 400))
                )
            )

            // When
            val result = parser.parse(shortBlocks)

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Message Ownership Detection Tests")
    inner class MessageOwnershipDetectionTests {

        @Test
        fun `should detect message from me when on right side`() {
            // Given - Screen width 1080, threshold is 594 (1080 * 0.55)
            // Block with centerX > 594 should be "from me"
            val rightSideBlock = OcrTextBlock(
                text = "Hello, how are you?",
                boundingBox = Rect(700, 100, 900, 200), // centerX = 800
                lines = listOf(
                    OcrTextLine(text = "Hello, how are you?", left = 700, right = 900, top = 100, bottom = 200)
                )
            )

            // When
            val result = parser.parse(listOf(rightSideBlock), 1080)

            // Then
            assertEquals(1, result.size)
            assertTrue(result[0].isFromMe)
            assertEquals("Hello, how are you?", result[0].text)
        }

        @Test
        fun `should detect message from other when on left side`() {
            // Given - Block with centerX < 594 should be "from other"
            val leftSideBlock = OcrTextBlock(
                text = "I'm good, thanks!",
                boundingBox = Rect(100, 300, 400, 400), // centerX = 250
                lines = listOf(
                    OcrTextLine(text = "I'm good, thanks!", left = 100, right = 400, top = 300, bottom = 400)
                )
            )

            // When
            val result = parser.parse(listOf(leftSideBlock), 1080)

            // Then
            assertEquals(1, result.size)
            assertFalse(result[0].isFromMe)
            assertEquals("I'm good, thanks!", result[0].text)
        }

        @Test
        fun `should handle multiple messages with correct ownership`() {
            // Given
            val blocks = listOf(
                OcrTextBlock(
                    text = "Hey there!",
                    boundingBox = Rect(100, 100, 400, 200), // centerX = 250, left side
                    lines = listOf(OcrTextLine(text = "Hey there!", left = 100, right = 400, top = 100, bottom = 200))
                ),
                OcrTextBlock(
                    text = "Hi! How are you?",
                    boundingBox = Rect(600, 250, 900, 350), // centerX = 750, right side
                    lines = listOf(OcrTextLine(text = "Hi! How are you?", left = 600, right = 900, top = 250, bottom = 350))
                ),
                OcrTextBlock(
                    text = "Doing great!",
                    boundingBox = Rect(100, 400, 400, 500), // centerX = 250, left side
                    lines = listOf(OcrTextLine(text = "Doing great!", left = 100, right = 400, top = 400, bottom = 500))
                )
            )

            // When
            val result = parser.parse(blocks, 1080)

            // Then
            assertEquals(3, result.size)
            assertFalse(result[0].isFromMe) // "Hey there!" - left
            assertTrue(result[1].isFromMe)  // "Hi! How are you?" - right
            assertFalse(result[2].isFromMe) // "Doing great!" - left
        }
    }

    @Nested
    @DisplayName("Message Sorting Tests")
    inner class MessageSortingTests {

        @Test
        fun `should sort messages by vertical position`() {
            // Given - Blocks in random vertical order
            val blocks = listOf(
                OcrTextBlock(
                    text = "Third message",
                    boundingBox = Rect(100, 500, 400, 600), // top = 500
                    lines = listOf(OcrTextLine(text = "Third message", left = 100, right = 400, top = 500, bottom = 600))
                ),
                OcrTextBlock(
                    text = "First message",
                    boundingBox = Rect(100, 100, 400, 200), // top = 100
                    lines = listOf(OcrTextLine(text = "First message", left = 100, right = 400, top = 100, bottom = 200))
                ),
                OcrTextBlock(
                    text = "Second message",
                    boundingBox = Rect(600, 300, 900, 400), // top = 300
                    lines = listOf(OcrTextLine(text = "Second message", left = 600, right = 900, top = 300, bottom = 400))
                )
            )

            // When
            val result = parser.parse(blocks, 1080)

            // Then
            assertEquals(3, result.size)
            assertEquals("First message", result[0].text)
            assertEquals("Second message", result[1].text)
            assertEquals("Third message", result[2].text)
        }
    }

    @Nested
    @DisplayName("Position Tracking Tests")
    inner class PositionTrackingTests {

        @Test
        fun `should store position coordinates in message`() {
            // Given
            val block = OcrTextBlock(
                text = "Test message",
                boundingBox = Rect(300, 200, 700, 300),
                lines = listOf(OcrTextLine(text = "Test message", left = 300, right = 700, top = 200, bottom = 300))
            )

            // When
            val result = parser.parse(listOf(block), 1080)

            // Then
            assertEquals(1, result.size)
            assertEquals(500, result[0].positionX) // centerX = (300 + 700) / 2
            assertEquals(200, result[0].positionY) // top = 200
        }

        @Test
        fun `should handle missing bounding box gracefully`() {
            // Given
            val blockWithoutBox = OcrTextBlock(
                text = "No position info",
                boundingBox = null,
                lines = listOf(OcrTextLine(text = "No position info", left = 0, right = 0, top = 0, bottom = 0))
            )

            // When
            val result = parser.parse(listOf(blockWithoutBox), 1080)

            // Then
            assertEquals(1, result.size)
            assertEquals(0, result[0].positionX)
            assertEquals(0, result[0].positionY)
            assertFalse(result[0].isFromMe) // centerX 0 is not > 594
        }
    }

    @Nested
    @DisplayName("Different Screen Width Tests")
    inner class DifferentScreenWidthTests {

        @Test
        fun `should adapt to different screen widths`() {
            // Given - Wider screen (1440px), threshold is 792 (1440 * 0.55)
            val block = OcrTextBlock(
                text = "Wide screen message",
                boundingBox = Rect(800, 100, 1000, 200), // centerX = 900
                lines = listOf(OcrTextLine(text = "Wide screen message", left = 800, right = 1000, top = 100, bottom = 200))
            )

            // When - On 1440px screen
            val result1440 = parser.parse(listOf(block), 1440)
            
            // When - On 1080px screen
            val result1080 = parser.parse(listOf(block), 1080)

            // Then
            // On 1440px: centerX 900 > 792, so isFromMe = true
            assertTrue(result1440[0].isFromMe)
            
            // On 1080px: centerX 900 > 594, so isFromMe = true
            assertTrue(result1080[0].isFromMe)
        }

        @Test
        fun `should use default screen width of 1080`() {
            // Given
            val block = OcrTextBlock(
                text = "Default width test",
                boundingBox = Rect(700, 100, 900, 200), // centerX = 800
                lines = listOf(OcrTextLine(text = "Default width test", left = 700, right = 900, top = 100, bottom = 200))
            )

            // When - Using default screen width
            val result = parser.parse(listOf(block))

            // Then - Should use 1080 as default, centerX 800 > 594
            assertEquals(1, result.size)
            assertTrue(result[0].isFromMe)
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    inner class EdgeCasesTests {

        @Test
        fun `should handle message exactly at threshold boundary`() {
            // Given - centerX exactly at 594 (1080 * 0.55)
            val blockAtBoundary = OcrTextBlock(
                text = "Boundary message",
                boundingBox = Rect(494, 100, 694, 200), // centerX = 594
                lines = listOf(OcrTextLine(text = "Boundary message", left = 494, right = 694, top = 100, bottom = 200))
            )

            // When
            val result = parser.parse(listOf(blockAtBoundary), 1080)

            // Then - centerX 594 is NOT > 594, so should be "from other"
            assertEquals(1, result.size)
            assertFalse(result[0].isFromMe)
        }

        @Test
        fun `should trim whitespace from message text`() {
            // Given
            val blockWithWhitespace = OcrTextBlock(
                text = "  Trimmed message  \n",
                boundingBox = Rect(700, 100, 900, 200),
                lines = listOf(OcrTextLine(text = "  Trimmed message  \n", left = 700, right = 900, top = 100, bottom = 200))
            )

            // When
            val result = parser.parse(listOf(blockWithWhitespace), 1080)

            // Then
            assertEquals(1, result.size)
            assertEquals("Trimmed message", result[0].text)
        }

        @Test
        fun `should handle large number of messages`() {
            // Given - 100 messages alternating left and right
            val blocks = (0 until 100).map { i ->
                val isLeft = i % 2 == 0
                val x = if (isLeft) 100 else 700
                OcrTextBlock(
                    text = "Message $i",
                    boundingBox = Rect(x, i * 100, x + 200, i * 100 + 50),
                    lines = listOf(OcrTextLine(text = "Message $i", left = x, right = x + 200, top = i * 100, bottom = i * 100 + 50))
                )
            }

            // When
            val result = parser.parse(blocks, 1080)

            // Then
            assertEquals(100, result.size)
            // Verify alternating pattern
            for (i in result.indices) {
                if (i % 2 == 0) {
                    assertFalse(result[i].isFromMe)
                } else {
                    assertTrue(result[i].isFromMe)
                }
            }
        }
    }
}
