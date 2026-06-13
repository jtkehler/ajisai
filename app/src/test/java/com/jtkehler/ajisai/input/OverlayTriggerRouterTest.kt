package com.jtkehler.ajisai.input

import com.jtkehler.ajisai.ocrbox.OcrBoxEditorController
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

    @Test
    fun configureActionOpensEditorController() {
        val editor = FakeEditorController()
        val router = OverlayTriggerRouter(
            OverlayActionCallbacks(
                toggleOverlay = {},
                runOcr = {},
                configureOcrBox = { editor.show() },
                closeOverlay = {},
            ),
        )

        router.route(OverlayAction.ConfigureOcrBox)

        assertEquals(1, editor.showRequests)
    }

    private class FakeEditorController : OcrBoxEditorController {
        override val isShowing: Boolean = false
        var showRequests = 0

        override fun show(): Boolean {
            showRequests += 1
            return true
        }

        override fun dismiss(notifyClosed: Boolean) = Unit
    }
}
