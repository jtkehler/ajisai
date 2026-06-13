package com.jtkehler.ajisai.ocrbox

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jtkehler.ajisai.capture.CapturedFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BitmapOcrFrameCropperTest {
    @Test
    fun cropReturnsNewBitmapWithExpectedDimensionsAndPixels() {
        val source = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                source.setPixel(x, y, Color.rgb(x * 40, y * 40, x + y))
            }
        }
        val frame = CapturedFrame(source, capturedAtElapsedRealtimeNanos = 123L)
        val profile = OcrBoxProfile(
            id = "test",
            name = "Test",
            normalizedRect = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f),
        )

        val crop = requireNotNull(BitmapOcrFrameCropper().crop(frame, profile))

        assertNotSame(source, crop)
        assertEquals(2, crop.width)
        assertEquals(2, crop.height)
        assertEquals(source.getPixel(1, 1), crop.getPixel(0, 0))
        assertEquals(source.getPixel(2, 2), crop.getPixel(1, 1))
        crop.recycle()
        source.recycle()
    }
}
