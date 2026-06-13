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
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jtkehler.ajisai.capture.CaptureClient
import com.jtkehler.ajisai.capture.CaptureDependencies
import com.jtkehler.ajisai.capture.CaptureError
import com.jtkehler.ajisai.capture.CapturePermissionLauncher
import com.jtkehler.ajisai.capture.CapturePhase
import com.jtkehler.ajisai.capture.CaptureState
import com.jtkehler.ajisai.capture.CapturedFrame
import com.jtkehler.ajisai.ocrbox.NormalizedRect
import com.jtkehler.ajisai.ocrbox.OcrBoxDefaults
import com.jtkehler.ajisai.ocrbox.OcrBoxDependencies
import com.jtkehler.ajisai.ocrbox.OcrBoxProfile
import com.jtkehler.ajisai.ocrbox.OcrBoxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrBoxDebugUiTest {
    private lateinit var frameBitmap: Bitmap
    private lateinit var repository: FakeOcrBoxRepository
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        frameBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        repository = FakeOcrBoxRepository()
        CaptureDependencies.setClientFactoryForTests { FakeCaptureClient(frameBitmap) }
        CaptureDependencies.setPermissionLauncherFactoryForTests { _, _ -> CapturePermissionLauncher {} }
        OcrBoxDependencies.setRepositoryFactoryForTests { _: Context -> repository }
    }

    @After
    fun tearDown() {
        scenario?.close()
        CaptureDependencies.resetForTests()
        OcrBoxDependencies.resetForTests()
        frameBitmap.takeUnless { it.isRecycled }?.recycle()
    }

    @Test
    fun savedBoxCanPreviewCropAndReset() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.preview_ocr_crop_button)).perform(scrollTo(), click())
        onView(withId(R.id.ocr_crop_preview)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.reset_ocr_box_button)).perform(scrollTo(), click())

        assertEquals(1, repository.resetRequests)
        assertEquals(OcrBoxDefaults.defaultProfile(), repository.getActiveProfile())
    }

    private class FakeOcrBoxRepository : OcrBoxRepository {
        private var profile = OcrBoxProfile(
            id = "test",
            name = "Test",
            normalizedRect = NormalizedRect(0.2f, 0.2f, 0.8f, 0.8f),
        )
        var resetRequests = 0

        override fun getProfiles(): List<OcrBoxProfile> = listOf(profile)

        override fun getActiveProfile(): OcrBoxProfile = profile

        override fun save(profile: OcrBoxProfile) {
            this.profile = profile
        }

        override fun resetToDefault(): OcrBoxProfile {
            resetRequests += 1
            profile = OcrBoxDefaults.defaultProfile()
            return profile
        }
    }

    private class FakeCaptureClient(bitmap: Bitmap) : CaptureClient {
        override val state: StateFlow<CaptureState> = MutableStateFlow(CaptureState(CapturePhase.ACTIVE))
        override val latestFrame: StateFlow<CapturedFrame?> = MutableStateFlow(
            CapturedFrame(bitmap, capturedAtElapsedRealtimeNanos = 123L),
        )

        override fun startCapture(resultCode: Int, permissionData: Intent) = Unit
        override fun captureFrame() = Unit
        override fun stopCapture() = Unit
        override fun reportError(error: CaptureError) = Unit
    }
}
