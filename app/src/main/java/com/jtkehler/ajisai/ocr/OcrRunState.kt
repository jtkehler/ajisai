package com.jtkehler.ajisai.ocr

sealed interface OcrRunState {
    data object Idle : OcrRunState

    data object Capturing : OcrRunState

    data class Recognizing(
        val capturedAtElapsedRealtimeNanos: Long,
    ) : OcrRunState

    data class Success(
        val text: String,
        val capturedAtElapsedRealtimeNanos: Long,
        val providerName: String? = null,
        val lines: List<OcrTextLine> = emptyList(),
        val debugArtifacts: OcrDebugArtifacts = OcrDebugArtifacts(),
    ) : OcrRunState

    data class Error(
        val type: OcrRunError,
        val capturedAtElapsedRealtimeNanos: Long? = null,
    ) : OcrRunState
}

enum class OcrRunError {
    CAPTURE_INACTIVE,
    CAPTURE_UNAVAILABLE,
    CAPTURE_TIMEOUT,
    CROP_FAILED,
    PREPROCESSING,
    NETWORK,
    HTTP,
    PARSE,
    NO_TEXT,
    UNKNOWN,
}
