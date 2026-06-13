package com.jtkehler.ajisai.input

class FloatingBubbleTriggerSource(
    private val router: OverlayTriggerRouter,
) : OverlayTriggerSource {
    private var started = false

    override fun start() {
        started = true
    }

    override fun stop() {
        started = false
    }

    fun emit(action: OverlayAction) {
        if (started) router.route(action)
    }
}
