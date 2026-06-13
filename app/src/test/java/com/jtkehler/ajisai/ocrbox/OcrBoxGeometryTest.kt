package com.jtkehler.ajisai.ocrbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrBoxGeometryTest {
    @Test
    fun sanitizeClampsOrdersAndEnforcesMinimumSize() {
        val rect = OcrBoxGeometry.sanitize(
            NormalizedRect(left = 1.2f, top = 0.5f, right = -0.2f, bottom = 0.51f),
        )

        assertEquals(0f, rect.left)
        assertEquals(1f, rect.right)
        assertEquals(OcrBoxDefaults.MIN_HEIGHT, rect.bottom - rect.top, 0.0001f)
        assertTrue(OcrBoxGeometry.isValid(rect))
    }

    @Test
    fun normalizedAndPixelCoordinatesRoundTrip() {
        val normalized = NormalizedRect(0.25f, 0.20f, 0.75f, 0.80f)

        val pixels = OcrBoxGeometry.toPixelRect(normalized, width = 200, height = 100)
        val restored = OcrBoxGeometry.toNormalizedRect(pixels, width = 200, height = 100)

        assertEquals(PixelRect(50, 20, 150, 80), pixels)
        assertEquals(normalized.left, restored.left, 0.0001f)
        assertEquals(normalized.top, restored.top, 0.0001f)
        assertEquals(normalized.right, restored.right, 0.0001f)
        assertEquals(normalized.bottom, restored.bottom, 0.0001f)
    }

    @Test
    fun pixelRectIsClampedAndKeptNonEmpty() {
        assertEquals(
            PixelRect(0, 0, 1, 1),
            OcrBoxGeometry.clampPixelRect(
                rect = PixelRect(-20, -30, -10, -5),
                width = 100,
                height = 50,
            ),
        )
    }

    @Test
    fun movePreservesSizeAndStaysWithinBounds() {
        val moved = OcrBoxGeometry.move(
            NormalizedRect(0.2f, 0.2f, 0.6f, 0.5f),
            deltaX = 0.8f,
            deltaY = -0.8f,
        )

        assertEquals(0.6f, moved.left, 0.0001f)
        assertEquals(0f, moved.top, 0.0001f)
        assertEquals(1f, moved.right, 0.0001f)
        assertEquals(0.3f, moved.bottom, 0.0001f)
    }

    @Test
    fun resizeFromHandleKeepsOppositeCornerAndMinimumSize() {
        val resized = OcrBoxGeometry.resize(
            rect = NormalizedRect(0.2f, 0.2f, 0.8f, 0.8f),
            handle = OcrBoxResizeHandle.TOP_LEFT,
            deltaX = 0.9f,
            deltaY = 0.9f,
        )

        assertEquals(0.8f, resized.right)
        assertEquals(0.8f, resized.bottom)
        assertEquals(OcrBoxDefaults.MIN_WIDTH, resized.right - resized.left, 0.0001f)
        assertEquals(OcrBoxDefaults.MIN_HEIGHT, resized.bottom - resized.top, 0.0001f)
        assertTrue(OcrBoxGeometry.isValid(resized))
    }
}
