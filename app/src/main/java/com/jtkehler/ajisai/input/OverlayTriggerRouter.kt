package com.jtkehler.ajisai.input

data class OverlayActionCallbacks(
    val toggleOverlay: () -> Unit,
    val runOcr: () -> Unit,
    val configureOcrBox: () -> Unit,
    val closeOverlay: () -> Unit,
)

/** Routes generic input actions while keeping sources independent from overlay implementation. */
class OverlayTriggerRouter(
    private val callbacks: OverlayActionCallbacks,
) {
    fun route(action: OverlayAction) {
        when (action) {
            OverlayAction.ToggleOverlay -> callbacks.toggleOverlay()
            OverlayAction.RunOcr -> callbacks.runOcr()
            OverlayAction.ConfigureOcrBox -> callbacks.configureOcrBox()
            OverlayAction.CloseOverlay -> callbacks.closeOverlay()
        }
    }
}
