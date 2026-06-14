package com.jtkehler.ajisai.ocr

import android.graphics.Bitmap
import android.graphics.Rect

internal class FakeOcrEngine(
    var result: OcrResult = OcrResult("日本語", listOf(OcrTextLine("日本語")), "Fake"),
    var failure: OcrException? = null,
) : OcrEngine {
    var calls = 0
    var receivedWidth: Int? = null
    var receivedHeight: Int? = null
    var receivedRegion: Rect? = null

    override suspend fun recognize(
        image: Bitmap,
        region: Rect?,
        options: OcrOptions,
    ): OcrResult {
        calls += 1
        receivedWidth = image.width
        receivedHeight = image.height
        receivedRegion = region
        failure?.let { throw it }
        return result
    }
}
