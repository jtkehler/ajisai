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
    val lines: List<String> = emptyList(),
    val providerName: String? = null,
    val debugArtifacts: OcrDebugArtifacts = OcrDebugArtifacts(),
)

data class OcrDebugArtifacts(
    val cropPath: String? = null,
    val rawResponsePath: String? = null,
)

enum class OcrErrorType {
    PREPROCESSING,
    NETWORK,
    HTTP,
    PARSE,
    NO_TEXT,
}

class OcrException(
    val type: OcrErrorType,
    message: String,
    cause: Throwable? = null,
    val httpStatusCode: Int? = null,
) : Exception(message, cause)

/** Boundary for OCR providers such as the planned Google Lens implementation. */
interface OcrEngine {
    suspend fun recognize(
        image: Bitmap,
        region: Rect? = null,
        options: OcrOptions = OcrOptions(),
    ): OcrResult
}
