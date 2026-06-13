package com.jtkehler.ajisai.capture

import android.graphics.Bitmap

/** A captured screen frame and its monotonic capture timestamp. */
data class CapturedFrame(
    val bitmap: Bitmap,
    val capturedAtElapsedRealtimeNanos: Long,
)

/** Boundary for obtaining frames without exposing MediaProjection details. */
interface ScreenFrameSource {
    suspend fun captureFrame(): CaptureFrameResult
}

sealed interface CaptureFrameResult {
    data class Success(val frame: CapturedFrame) : CaptureFrameResult

    data class Failure(val error: CaptureError) : CaptureFrameResult
}

enum class CaptureError {
    PERMISSION_DENIED,
    SERVICE_UNAVAILABLE,
    CAPTURE_UNAVAILABLE,
    NO_FRAME_AVAILABLE,
    CAPTURE_FAILED,
}

enum class CapturePhase {
    INACTIVE,
    STARTING,
    ACTIVE,
    CAPTURING,
}

data class CaptureState(
    val phase: CapturePhase = CapturePhase.INACTIVE,
    val error: CaptureError? = null,
)
