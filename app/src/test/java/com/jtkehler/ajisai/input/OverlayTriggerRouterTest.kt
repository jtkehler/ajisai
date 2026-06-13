package com.jtkehler.ajisai.input

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayTriggerRouterTest {
    @Test
    fun routesEachActionToItsCallback() {
        val routed = mutableListOf<String>()
        val router = OverlayTriggerRouter(
            OverlayActionCallbacks(
                toggleOverlay = { routed += "toggle" },
                runOcr = { routed += "ocr" },
                configureOcrBox = { routed += "configure" },
                closeOverlay = { routed += "close" },
            ),
        )

        OverlayAction.entries.forEach(router::route)

        assertEquals(listOf("toggle", "ocr", "configure", "close"), routed)
    }

    @Test
    fun floatingBubbleSourceOnlyEmitsWhileStarted() {
        var toggleCount = 0
        val source = FloatingBubbleTriggerSource(
            OverlayTriggerRouter(
                OverlayActionCallbacks(
                    toggleOverlay = { toggleCount += 1 },
                    runOcr = {},
                    configureOcrBox = {},
                    closeOverlay = {},
                ),
            ),
        )

        source.emit(OverlayAction.ToggleOverlay)
        source.start()
        source.emit(OverlayAction.ToggleOverlay)
        source.stop()
        source.emit(OverlayAction.ToggleOverlay)

        assertEquals(1, toggleCount)
    }
}
