package com.jtkehler.ajisai.overlay

import com.jtkehler.ajisai.input.FloatingBubbleTriggerSource
import com.jtkehler.ajisai.input.OverlayAction
import com.jtkehler.ajisai.input.OverlayActionCallbacks
import com.jtkehler.ajisai.input.OverlayTriggerRouter
import com.jtkehler.ajisai.ocr.OcrRunState
import com.jtkehler.ajisai.ocr.OcrRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayHudControllerTest {
    @Test
    fun bubbleTapShowsHudRunsOnceThenCollapsesAndClears() {
        val runner = FakeRunner()
        val quickControls = FakeSurface()
        val ocrPanel = FakeOcrSurface()
        val hud = OverlayHudController(runner, quickControls, ocrPanel)
        val source = FloatingBubbleTriggerSource(
            OverlayTriggerRouter(
                OverlayActionCallbacks(
                    toggleOverlay = { hud.toggle() },
                    runOcr = runner::run,
                    configureOcrBox = {},
                    openSettings = {},
                    closeOverlay = {},
                ),
            ),
        ).also { it.start() }

        source.emit(OverlayAction.ToggleOverlay)

        assertTrue(hud.isShown)
        assertTrue(quickControls.isShowing)
        assertTrue(ocrPanel.isShowing)
        assertEquals(1, runner.runRequests)

        source.emit(OverlayAction.ToggleOverlay)

        assertFalse(hud.isShown)
        assertFalse(quickControls.isShowing)
        assertFalse(ocrPanel.isShowing)
        assertEquals(1, runner.clearRequests)
        assertEquals(1, runner.runRequests)
    }

    @Test
    fun editorTemporarilyHidesHudAndResumeStartsFreshOcr() {
        val runner = FakeRunner()
        val quickControls = FakeSurface()
        val ocrPanel = FakeOcrSurface()
        val hud = OverlayHudController(runner, quickControls, ocrPanel)
        hud.show()

        hud.suspendForEditor()

        assertTrue(hud.isShown)
        assertFalse(quickControls.isShowing)
        assertFalse(ocrPanel.isShowing)
        assertEquals(1, runner.clearRequests)

        assertTrue(hud.resumeAfterEditor())
        assertTrue(quickControls.isShowing)
        assertTrue(ocrPanel.isShowing)
        assertEquals(2, runner.runRequests)
    }

    private open class FakeSurface : OverlayHudSurface {
        override var isShowing = false
        override fun show(): Boolean {
            isShowing = true
            return true
        }

        override fun hide() {
            isShowing = false
        }
    }

    private class FakeOcrSurface : FakeSurface(), OcrHudSurface {
        override fun render(state: OcrRunState) = Unit
    }

    private class FakeRunner : OcrRunner {
        override val state: StateFlow<OcrRunState> = MutableStateFlow(OcrRunState.Idle)
        var runRequests = 0
        var clearRequests = 0

        override fun run() {
            runRequests += 1
        }

        override fun clear() {
            clearRequests += 1
        }
    }
}
