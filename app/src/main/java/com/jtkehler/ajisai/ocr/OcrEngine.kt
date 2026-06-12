package com.jtkehler.ajisai.ocr

import android.graphics.Bitmap
import android.graphics.Rect

/** Provider-neutral OCR options. Provider-specific request data stays behind [OcrEngine]. */
data class OcrOptions(
    val saveDebugArtifacts: Boolean = false,
)

/** Text returned by an OCR provider. */
data class OcrResult(
    val text: String,
)

/** Boundary for OCR providers such as the planned Google Lens implementation. */
interface OcrEngine {
    suspend fun recognize(
        image: Bitmap,
        region: Rect? = null,
        options: OcrOptions = OcrOptions(),
    ): OcrResult
}
