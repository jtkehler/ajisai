package com.jtkehler.ajisai.capture

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/** Activity-facing capture commands. Platform service details remain behind this boundary. */
interface CaptureClient {
    val state: StateFlow<CaptureState>
    val latestFrame: StateFlow<CapturedFrame?>

    fun startCapture(resultCode: Int, permissionData: Intent)
    fun captureFrame()
    fun stopCapture()
    fun reportError(error: CaptureError)
}
