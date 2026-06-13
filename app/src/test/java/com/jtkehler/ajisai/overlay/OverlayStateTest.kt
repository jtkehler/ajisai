package com.jtkehler.ajisai.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayStateTest {
    @Test
    fun missingPermissionOnlyAllowsPermissionRequest() {
        assertEquals(
            OverlayUiControls(
                requestPermissionEnabled = true,
                startEnabled = false,
                stopEnabled = false,
            ),
            OverlayState().toUiControls(),
        )
    }

    @Test
    fun grantedStoppedStateAllowsStarting() {
        assertEquals(
            OverlayUiControls(
                requestPermissionEnabled = false,
                startEnabled = true,
                stopEnabled = false,
            ),
            OverlayState(
                permission = OverlayPermissionState.GRANTED,
                service = OverlayServiceState.STOPPED,
            ).toUiControls(),
        )
    }

    @Test
    fun runningStateOnlyAllowsStopping() {
        assertEquals(
            OverlayUiControls(
                requestPermissionEnabled = false,
                startEnabled = false,
                stopEnabled = true,
            ),
            OverlayState(
                permission = OverlayPermissionState.GRANTED,
                service = OverlayServiceState.RUNNING,
            ).toUiControls(),
        )
    }
}
