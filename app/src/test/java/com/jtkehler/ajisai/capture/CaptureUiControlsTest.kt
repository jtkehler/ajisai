package com.jtkehler.ajisai.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureUiControlsTest {
    @Test
    fun inactiveOnlyAllowsStartingCapture() {
        assertEquals(
            CaptureUiControls(
                startEnabled = true,
                captureEnabled = false,
                stopEnabled = false,
                progressVisible = false,
            ),
            CaptureState(CapturePhase.INACTIVE).toUiControls(),
        )
    }

    @Test
    fun activeAllowsOnDemandCaptureAndStop() {
        assertEquals(
            CaptureUiControls(
                startEnabled = false,
                captureEnabled = true,
                stopEnabled = true,
                progressVisible = false,
            ),
            CaptureState(CapturePhase.ACTIVE).toUiControls(),
        )
    }

    @Test
    fun capturingDisablesDuplicateCaptureAndShowsProgress() {
        assertEquals(
            CaptureUiControls(
                startEnabled = false,
                captureEnabled = false,
                stopEnabled = true,
                progressVisible = true,
            ),
            CaptureState(CapturePhase.CAPTURING).toUiControls(),
        )
    }
}
