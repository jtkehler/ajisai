package com.jtkehler.ajisai.capture

data class CaptureUiControls(
    val startEnabled: Boolean,
    val captureEnabled: Boolean,
    val stopEnabled: Boolean,
    val progressVisible: Boolean,
)

fun CaptureState.toUiControls(): CaptureUiControls = when (phase) {
    CapturePhase.INACTIVE -> CaptureUiControls(
        startEnabled = true,
        captureEnabled = false,
        stopEnabled = false,
        progressVisible = false,
    )
    CapturePhase.STARTING -> CaptureUiControls(
        startEnabled = false,
        captureEnabled = false,
        stopEnabled = true,
        progressVisible = true,
    )
    CapturePhase.ACTIVE -> CaptureUiControls(
        startEnabled = false,
        captureEnabled = true,
        stopEnabled = true,
        progressVisible = false,
    )
    CapturePhase.CAPTURING -> CaptureUiControls(
        startEnabled = false,
        captureEnabled = false,
        stopEnabled = true,
        progressVisible = true,
    )
}
