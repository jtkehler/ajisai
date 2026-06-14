package com.jtkehler.ajisai.capture

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow

internal object CaptureRuntime {
    private val mutableState = kotlinx.coroutines.flow.MutableStateFlow(CaptureState())
    private val mutableLatestFrame = kotlinx.coroutines.flow.MutableStateFlow<CapturedFrame?>(null)

    val state: StateFlow<CaptureState> = mutableState
    val latestFrame: StateFlow<CapturedFrame?> = mutableLatestFrame

    fun updateState(state: CaptureState) {
        mutableState.value = state
    }

    fun updateFrame(frame: CapturedFrame) {
        mutableLatestFrame.value = frame
    }

    fun reset() {
        mutableState.value = CaptureState()
        mutableLatestFrame.value = null
    }
}

internal class AndroidCaptureClient(
    private val context: Context,
    private val serviceStarter: (Intent) -> Unit = { intent -> context.startService(intent) },
) : CaptureClient {
    override val state: StateFlow<CaptureState> = CaptureRuntime.state
    override val latestFrame: StateFlow<CapturedFrame?> = CaptureRuntime.latestFrame

    override fun startCapture(resultCode: Int, permissionData: Intent) {
        CaptureRuntime.updateState(CaptureState(CapturePhase.STARTING))
        val intent = CaptureService.startIntent(context, resultCode, permissionData)
        runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure {
                CaptureRuntime.updateState(
                    CaptureState(CapturePhase.INACTIVE, CaptureError.SERVICE_UNAVAILABLE),
                )
            }
    }

    override fun captureFrame() {
        if (state.value.phase != CapturePhase.ACTIVE) {
            reportError(CaptureError.CAPTURE_UNAVAILABLE)
            return
        }
        CaptureRuntime.updateState(CaptureState(CapturePhase.ACTIVE))
        runCatching { serviceStarter(CaptureService.captureIntent(context)) }
            .onFailure { reportError(CaptureError.SERVICE_UNAVAILABLE) }
    }

    override fun stopCapture() {
        runCatching { context.startService(CaptureService.stopIntent(context)) }
            .onFailure {
                CaptureRuntime.updateState(
                    CaptureState(CapturePhase.INACTIVE, CaptureError.SERVICE_UNAVAILABLE),
                )
            }
    }

    override fun reportError(error: CaptureError) {
        CaptureRuntime.updateState(CaptureState(state.value.phase, error))
    }
}
