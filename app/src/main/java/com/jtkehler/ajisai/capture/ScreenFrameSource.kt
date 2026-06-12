package com.jtkehler.ajisai.capture

import android.graphics.Bitmap

/** A captured screen frame and the timestamp used to align later mining media. */
data class CapturedFrame(
    val bitmap: Bitmap,
    val capturedAtMs: Long,
)

/** Boundary for obtaining frames without exposing MediaProjection details. */
interface ScreenFrameSource {
    suspend fun captureFrame(): CapturedFrame
}
