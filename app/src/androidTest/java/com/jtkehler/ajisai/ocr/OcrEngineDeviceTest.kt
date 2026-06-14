package com.jtkehler.ajisai.ocr

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrEngineDeviceTest {
    @Test
    fun fakeEngineReturnsConfiguredSuccessAndFailure() = runBlocking {
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        val expected = OcrResult("成功", listOf(OcrTextLine("成功")), "Fake")
        val engine = FakeOcrEngine(result = expected)

        assertSame(expected, engine.recognize(bitmap))
        assertEquals(1, engine.calls)

        val failure = OcrException(OcrErrorType.NETWORK, "offline")
        engine.failure = failure
        val thrown = runCatching { engine.recognize(bitmap) }.exceptionOrNull()
        assertSame(failure, thrown)
        assertEquals(2, engine.calls)
        bitmap.recycle()
    }
}
