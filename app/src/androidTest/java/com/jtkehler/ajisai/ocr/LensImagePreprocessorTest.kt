package com.jtkehler.ajisai.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LensImagePreprocessorTest {
    @Test
    fun cropsRequestedRegionAndPreservesPixels() {
        val source = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        for (y in 0 until 4) {
            for (x in 0 until 4) source.setPixel(x, y, Color.rgb(x * 30, y * 30, x + y))
        }

        val processed = LensImagePreprocessor().preprocess(source, Rect(1, 1, 3, 3))
        val decoded = BitmapFactory.decodeByteArray(processed.bytes, 0, processed.bytes.size)

        assertEquals(2, processed.width)
        assertEquals(2, processed.height)
        assertEquals(source.getPixel(1, 1), decoded.getPixel(0, 0))
        assertEquals(source.getPixel(2, 2), decoded.getPixel(1, 1))
        decoded.recycle()
        source.recycle()
    }

    @Test
    fun scalesLargeBitmapWithinConfiguredLimits() {
        val source = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)

        val processed = LensImagePreprocessor(maxDimension = 100).preprocess(source)

        assertEquals(100, processed.width)
        assertEquals(50, processed.height)
        source.recycle()
    }
}
