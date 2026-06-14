package com.jtkehler.ajisai.capture

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidCaptureClientTest {
    @Before
    fun setUp() {
        CaptureRuntime.reset()
    }

    @After
    fun tearDown() {
        CaptureRuntime.reset()
    }

    @Test
    fun activeStaleErrorIsClearedBeforeNewCaptureIntentStarts() {
        CaptureRuntime.updateState(
            CaptureState(CapturePhase.ACTIVE, CaptureError.NO_FRAME_AVAILABLE),
        )
        var serviceStarts = 0
        val client = AndroidCaptureClient(
            context = ApplicationProvider.getApplicationContext(),
            serviceStarter = { serviceStarts += 1 },
        )

        client.captureFrame()

        assertEquals(1, serviceStarts)
        assertEquals(CapturePhase.ACTIVE, client.state.value.phase)
        assertNull(client.state.value.error)
    }
}
