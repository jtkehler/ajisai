package com.jtkehler.ajisai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jtkehler.ajisai.capture.CaptureClient
import com.jtkehler.ajisai.capture.CaptureDependencies
import com.jtkehler.ajisai.capture.CaptureError
import com.jtkehler.ajisai.capture.CapturePermissionLauncher
import com.jtkehler.ajisai.capture.CapturePermissionResult
import com.jtkehler.ajisai.capture.CapturePhase
import com.jtkehler.ajisai.capture.CaptureState
import com.jtkehler.ajisai.capture.CapturedFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureDebugUiTest {
    private lateinit var fakeClient: FakeCaptureClient
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        fakeClient = FakeCaptureClient()
        CaptureDependencies.setClientFactoryForTests { _: Context -> fakeClient }
        CaptureDependencies.setPermissionLauncherFactoryForTests { _, _ ->
            CapturePermissionLauncher { }
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        CaptureDependencies.resetForTests()
    }

    @Test
    fun activeCaptureRendersPreviewAndCanStop() {
        fakeClient.setActive()
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.capture_status))
            .check(matches(withText(R.string.capture_status_active)))
        onView(withId(R.id.capture_screenshot_button))
            .perform(scrollTo())
            .check(matches(isEnabled()))
            .perform(click())
        onView(withId(R.id.capture_preview))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.stop_capture_button)).perform(click())
        onView(withId(R.id.capture_status))
            .check(matches(withText(R.string.capture_status_inactive)))
        assertEquals(1, fakeClient.captureRequests)
        assertEquals(1, fakeClient.stopRequests)
    }

    @Test
    fun deniedPermissionShowsErrorAndDoesNotStartService() {
        CaptureDependencies.setPermissionLauncherFactoryForTests { _, callback ->
            CapturePermissionLauncher { callback(CapturePermissionResult.Denied) }
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.start_capture_button)).perform(scrollTo(), click())

        onView(withId(R.id.capture_status))
            .check(matches(withText(R.string.capture_status_permission_denied)))
        onView(withId(R.id.capture_screenshot_button))
            .check(matches(not(isEnabled())))
        assertEquals(0, fakeClient.startRequests)
    }

    private class FakeCaptureClient : CaptureClient {
        private val mutableState = MutableStateFlow(CaptureState())
        private val mutableLatestFrame = MutableStateFlow<CapturedFrame?>(null)

        override val state: StateFlow<CaptureState> = mutableState
        override val latestFrame: StateFlow<CapturedFrame?> = mutableLatestFrame
        var startRequests = 0
        var captureRequests = 0
        var stopRequests = 0

        fun setActive() {
            mutableState.value = CaptureState(CapturePhase.ACTIVE)
        }

        override fun startCapture(resultCode: Int, permissionData: Intent) {
            startRequests += 1
            mutableState.value = CaptureState(CapturePhase.ACTIVE)
        }

        override fun captureFrame() {
            captureRequests += 1
            mutableLatestFrame.value = CapturedFrame(
                bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888),
                capturedAtElapsedRealtimeNanos = 123L,
            )
        }

        override fun stopCapture() {
            stopRequests += 1
            mutableState.value = CaptureState()
        }

        override fun reportError(error: CaptureError) {
            mutableState.value = CaptureState(mutableState.value.phase, error)
        }
    }
}
