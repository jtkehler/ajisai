package com.jtkehler.ajisai.overlay

enum class OverlayPermissionState {
    MISSING,
    GRANTED,
}

enum class OverlayServiceState {
    STOPPED,
    RUNNING,
}

enum class OverlayError {
    PERMISSION_MISSING,
    SERVICE_UNAVAILABLE,
}

data class OverlayState(
    val permission: OverlayPermissionState = OverlayPermissionState.MISSING,
    val service: OverlayServiceState = OverlayServiceState.STOPPED,
    val error: OverlayError? = null,
)

data class OverlayUiControls(
    val requestPermissionEnabled: Boolean,
    val startEnabled: Boolean,
    val stopEnabled: Boolean,
)

fun OverlayState.toUiControls() = OverlayUiControls(
    requestPermissionEnabled = permission == OverlayPermissionState.MISSING,
    startEnabled = permission == OverlayPermissionState.GRANTED &&
        service == OverlayServiceState.STOPPED,
    stopEnabled = service == OverlayServiceState.RUNNING,
)
