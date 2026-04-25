package com.galize.app.ocr

import android.graphics.Rect
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for OcrEngine data structures.
 * 
 * Note: Full OcrEngine testing requires Android instrumentation tests due to ML Kit dependencies.
 * This test file validates the data structures (OcrTextBlock, OcrTextLine) and their properties.
 */
class OcrEngineTest {

    @Nested
    @DisplayName("OcrTextBlock Data Structure Tests")
    inner class OcrTextBlockTests {

        @Test
        fun `should create OcrTextBlock with all fields`() {
            // Given
            val lines = listOf(
                OcrTextLine(text = "Line 1", left = 10, right = 100, top = 10, bottom = 30, confidence = 0.95f),
                OcrTextLine(text = "Line 2", left = 10, right = 120, top = 35, bottom = 55, confidence = 0.92f)
            )

            // When
            val block = OcrTextBlock(
                text = "Line 1\nLine 2",
                boundingBox = null,  // Can't test Android Rect in unit tests
                confidence = 0.93f,
                lines = lines
            )

            // Then
            assertEquals("Line 1\nLine 2", block.text)
            assertEquals(0.93f, block.confidence)
            assertEquals(2, block.lines.size)
        }

        @Test
        fun `should handle OcrTextBlock with null bounding box`() {
            // Given & When
            val block = OcrTextBlock(
                text = "Text without position",
                boundingBox = null,
                confidence = null,
                lines = emptyList()
            )

            // Then
            assertEquals("Text without position", block.text)
            assertNull(block.boundingBox)
            assertNull(block.confidence)
            assertTrue(block.lines.isEmpty())
        }

        @Test
        fun `should handle empty text block`() {
            // Given & When
            val block = OcrTextBlock(
                text = "",
                boundingBox = Rect(0, 0, 0, 0),
                confidence = 0.0f,
                lines = emptyList()
            )

            // Then
            assertTrue(block.text.isEmpty())
            assertEquals(0.0f, block.confidence)
        }
    }

    @Nested
    @DisplayName("OcrTextLine Data Structure Tests")
    inner class OcrTextLineTests {

        @Test
        fun `should create OcrTextLine with all fields`() {
            // Given & When
            val line = OcrTextLine(
                text = "Hello World",
                left = 50,
                right = 200,
                top = 100,
                bottom = 130,
                confidence = 0.98f
            )

            // Then
            assertEquals("Hello World", line.text)
            assertEquals(50, line.left)
            assertEquals(200, line.right)
            assertEquals(100, line.top)
            assertEquals(130, line.bottom)
            assertEquals(0.98f, line.confidence)
        }

        @Test
        fun `should calculate line width correctly`() {
            // Given
            val line = OcrTextLine(
                text = "Test",
                left = 100,
                right = 300,
                top = 50,
                bottom = 80,
                confidence = 0.9f
            )

            // Then
            val width = line.right - line.left
            assertEquals(200, width)
        }

        @Test
        fun `should calculate line height correctly`() {
            // Given
            val line = OcrTextLine(
                text = "Test",
                left = 100,
                right = 300,
                top = 50,
                bottom = 80,
                confidence = 0.9f
            )

            // Then
            val height = line.bottom - line.top
            assertEquals(30, height)
        }

        @Test
        fun `should handle line with null confidence`() {
            // Given & When
            val line = OcrTextLine(
                text = "No confidence",
                left = 0,
                right = 100,
                top = 0,
                bottom = 20,
                confidence = null
            )

            // Then
            assertNull(line.confidence)
        }
    }

    @Nested
    @DisplayName("OcrTextBlock and OcrTextLine Integration Tests")
    inner class IntegrationTests {

        @Test
        fun `should create realistic chat message block`() {
            // Given - Simulating a chat message bubble with multiple lines
            val lines = listOf(
                OcrTextLine(text = "Hey! Are you free", left = 700, right = 950, top = 200, bottom = 225, confidence = 0.96f),
                OcrTextLine(text = "tonight?", left = 700, right = 820, top = 230, bottom = 255, confidence = 0.94f)
            )

            // When
            val block = OcrTextBlock(
                text = "Hey! Are you free\ntonight?",
                boundingBox = null,  // Can't test Android Rect in unit tests
                confidence = 0.95f,
                lines = lines
            )

            // Then
            assertEquals(2, block.lines.size)
            assertEquals("Hey! Are you free\ntonight?", block.text)
            assertEquals(0.95f, block.confidence)
            // Verify line properties
            assertEquals(700, block.lines[0].left)
            assertEquals(950, block.lines[0].right)
            assertEquals(200, block.lines[0].top)
            assertEquals(225, block.lines[0].bottom)
        }

        @Test
        fun `should create realistic received message block`() {
            // Given - Simulating a received message on the left side
            val lines = listOf(
                OcrTextLine(text = "Yes, what's up?", left = 100, right = 350, top = 300, bottom = 325, confidence = 0.97f)
            )

            // When
            val block = OcrTextBlock(
                text = "Yes, what's up?",
                boundingBox = null,  // Can't test Android Rect in unit tests
                confidence = 0.97f,
                lines = lines
            )

            // Then
            assertEquals(1, block.lines.size)
            assertEquals(0.97f, block.confidence)
            assertEquals(100, block.lines[0].left)
            assertEquals(350, block.lines[0].right)
            assertEquals(300, block.lines[0].top)
            assertEquals(325, block.lines[0].bottom)
        }

        @Test
        fun `should preserve line order in block`() {
            // Given
            val lines = listOf(
                OcrTextLine(text = "First line", left = 100, right = 200, top = 100, bottom = 120, confidence = 0.9f),
                OcrTextLine(text = "Second line", left = 100, right = 220, top = 125, bottom = 145, confidence = 0.88f),
                OcrTextLine(text = "Third line", left = 100, right = 210, top = 150, bottom = 170, confidence = 0.91f)
            )

            // When
            val block = OcrTextBlock(
                text = "First line\nSecond line\nThird line",
                boundingBox = null,  // Can't test Android Rect in unit tests
                confidence = 0.9f,
                lines = lines
            )

            // Then
            assertEquals(3, block.lines.size)
            assertEquals("First line", block.lines[0].text)
            assertEquals("Second line", block.lines[1].text)
            assertEquals("Third line", block.lines[2].text)
            
            // Verify vertical ordering
            assertTrue(block.lines[0].top < block.lines[1].top)
            assertTrue(block.lines[1].top < block.lines[2].top)
        }

        @Test
        fun `should handle block with mixed confidence values`() {
            // Given
            val lines = listOf(
                OcrTextLine(text = "Clear text", left = 100, right = 200, top = 100, bottom = 120, confidence = 0.98f),
                OcrTextLine(text = "Blurry text", left = 100, right = 200, top = 125, bottom = 145, confidence = 0.65f),
                OcrTextLine(text = "No confidence", left = 100, right = 200, top = 150, bottom = 170, confidence = null)
            )

            // When
            val block = OcrTextBlock(
                text = "Clear text\nBlurry text\nNo confidence",
                boundingBox = null,  // Can't test Android Rect in unit tests
                confidence = null,
                lines = lines
            )

            // Then
            assertNull(block.confidence)
            assertEquals(0.98f, block.lines[0].confidence)
            assertEquals(0.65f, block.lines[1].confidence)
            assertNull(block.lines[2].confidence)
        }
    }

}
