package com.jtkehler.ajisai.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class BubblePositionMathTest {
    @Test
    fun restoresAndClampsNormalizedPosition() {
        assertEquals(
            BubblePosition(x = 900, y = 0),
            BubblePositionMath.restore(
                normalizedX = 1.5f,
                normalizedY = -0.5f,
                availableWidth = 900,
                availableHeight = 1400,
            ),
        )
    }

    @Test
    fun normalizesClampedPixelPosition() {
        assertEquals(1f, BubblePositionMath.normalize(position = 1200, availableSize = 800))
        assertEquals(0f, BubblePositionMath.normalize(position = -20, availableSize = 800))
        assertEquals(0f, BubblePositionMath.normalize(position = 100, availableSize = 0))
    }
}
