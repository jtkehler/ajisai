package com.jtkehler.ajisai.ocr

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleLensOcrEngineTest {
    @Test
    fun fakeTransportProducesProviderNeutralResultAndOptionalDebugArtifacts() = runBlocking {
        val debug = RecordingDebugLogger()
        val engine = GoogleLensOcrEngine(
            preprocessor = FixedPreprocessor,
            requestBuilder = LensRequestBuilder(requestId = { 1L }),
            transport = FixedTransport(LensResponse(200, responseWithText("日本語"))),
            debugLogger = debug,
        )
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val withoutDebug = engine.recognize(bitmap)
        assertEquals("日本語", withoutDebug.text)
        assertNull(withoutDebug.debugArtifacts.cropPath)
        assertEquals(0, debug.cropSaves)

        val withDebug = engine.recognize(bitmap, options = OcrOptions(saveDebugArtifacts = true))
        assertEquals("crop.png", withDebug.debugArtifacts.cropPath)
        assertEquals("response.bin", withDebug.debugArtifacts.rawResponsePath)
        assertEquals(1, debug.cropSaves)
        assertEquals(1, debug.responseSaves)
        bitmap.recycle()
    }

    @Test
    fun httpAndNoTextResponsesAreTypedFailures() = runBlocking {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val httpEngine = GoogleLensOcrEngine(
            preprocessor = FixedPreprocessor,
            transport = FixedTransport(LensResponse(503, byteArrayOf())),
        )
        val httpError = runCatching { httpEngine.recognize(bitmap) }.exceptionOrNull() as OcrException
        assertEquals(OcrErrorType.HTTP, httpError.type)

        val emptyEngine = GoogleLensOcrEngine(
            preprocessor = FixedPreprocessor,
            transport = FixedTransport(LensResponse(200, byteArrayOf())),
        )
        val emptyError = runCatching { emptyEngine.recognize(bitmap) }.exceptionOrNull() as OcrException
        assertEquals(OcrErrorType.NO_TEXT, emptyError.type)
        bitmap.recycle()
    }

    private object FixedPreprocessor : OcrImagePreprocessor {
        override fun preprocess(image: Bitmap, region: android.graphics.Rect?) =
            ProcessedOcrImage(byteArrayOf(9), 1, 1)
    }

    private class FixedTransport(private val response: LensResponse) : LensTransport {
        override suspend fun execute(request: LensRequest): LensResponse = response
    }

    private class RecordingDebugLogger : OcrDebugLogger {
        var cropSaves = 0
        var responseSaves = 0

        override fun saveCrop(image: ProcessedOcrImage): String {
            cropSaves += 1
            return "crop.png"
        }

        override fun saveRawResponse(bytes: ByteArray): String {
            responseSaves += 1
            return "response.bin"
        }
    }

    private fun responseWithText(text: String): ByteArray {
        val word = message(fieldString(2, text))
        val line = message(fieldMessage(1, word))
        val paragraph = message(fieldMessage(2, line))
        val layout = message(fieldMessage(1, paragraph))
        val textPayload = message(fieldMessage(1, layout), fieldString(2, "ja"))
        val objects = message(fieldMessage(3, textPayload))
        return message(fieldMessage(2, objects))
    }

    private fun fieldString(number: Int, value: String) = fieldMessage(number, value.encodeToByteArray())

    private fun fieldMessage(number: Int, value: ByteArray) = message(
        varint(((number shl 3) or 2).toLong()),
        varint(value.size.toLong()),
        value,
    )

    private fun message(vararg values: ByteArray) =
        ByteArrayOutputStream().apply { values.forEach { write(it) } }.toByteArray()

    private fun varint(value: Long): ByteArray {
        var remaining = value
        return ByteArrayOutputStream().apply {
            while (remaining and -128L != 0L) {
                write(((remaining and 0x7f) or 0x80).toInt())
                remaining = remaining ushr 7
            }
            write(remaining.toInt())
        }.toByteArray()
    }
}
