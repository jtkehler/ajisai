package com.jtkehler.ajisai.input

/** User actions that input sources may send to the overlay workflow. */
enum class OverlayTriggerAction {
    TOGGLE_OVERLAY,
    RUN_OCR,
    MINE,
}

/** Boundary for floating-bubble and future hardware input sources. */
interface OverlayTriggerSource {
    fun start(onTrigger: (OverlayTriggerAction) -> Unit)

    fun stop()
}
