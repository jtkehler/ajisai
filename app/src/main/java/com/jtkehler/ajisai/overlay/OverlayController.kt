package com.jtkehler.ajisai.overlay

/** Boundary for controlling overlay visibility without exposing Android window APIs. */
interface OverlayController {
    fun show()

    fun hide()
}
