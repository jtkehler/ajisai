package com.jtkehler.ajisai.ocr

import android.content.Intent
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jtkehler.ajisai.capture.CaptureClient
import com.jtkehler.ajisai.capture.CaptureError
import com.jtkehler.ajisai.capture.CapturePhase
import com.jtkehler.ajisai.capture.CaptureState
import com.jtkehler.ajisai.capture.CapturedFrame
import com.jtkehler.ajisai.ocrbox.NormalizedRect
import com.jtkehler.ajisai.ocrbox.OcrBoxProfile
import com.jtkehler.ajisai.ocrbox.OcrBoxRepository
import com.jtkehler.ajisai.ocrbox.OcrFrameCropper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrRunnerTest {
    @Test
    fun requestsOneCaptureWaitsForFreshFrameCropsAndStopsAfterSuccess() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val stale = frame(10L, 8, 8)
        val fresh = frame(20L, 10, 10)
        val later = frame(30L, 12, 12)
        val capture = FakeCaptureClient(stale)
        val cropper = RecordingCropper()
        val engine = FakeOcrEngine(OcrResult("結果", listOf("結果"), "Fake"))
        val runner = DefaultOcrRunner(
            scope,
            capture,
            FakeRepository,
            cropper,
            engine,
            captureTimeoutMs = 1_000,
        )

        runner.run()
        assertEquals(OcrRunState.Capturing, runner.state.value)
        assertEquals(1, capture.captureRequests)
        assertEquals(0, engine.calls)

        capture.emit(fresh)
        val success = withTimeout(1_000) {
            runner.state.filterIsInstance<OcrRunState.Success>().first()
        }

        assertEquals("結果", success.text)
        assertEquals(20L, success.capturedAtElapsedRealtimeNanos)
        assertSame(fresh, cropper.receivedFrame)
        assertEquals(FakeRepository.getActiveProfile(), cropper.receivedProfile)
        assertEquals(4, engine.receivedWidth)
        assertEquals(3, engine.receivedHeight)
        assertEquals(1, engine.calls)

        capture.emit(later)
        delay(50)
        assertEquals(1, capture.captureRequests)
        assertEquals(1, engine.calls)

        runner.clear()
        scope.cancel()
        listOf(stale, fresh, later).forEach { it.bitmap.recycle() }
    }

    @Test
    fun emitsRecognizingBeforeEngineCompletesAndMapsEngineError() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val capture = FakeCaptureClient(null)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val engine = object : OcrEngine {
            override suspend fun recognize(
                image: Bitmap,
                region: android.graphics.Rect?,
                options: OcrOptions,
            ): OcrResult {
                entered.complete(Unit)
                release.await()
                throw OcrException(OcrErrorType.NETWORK, "offline")
            }
        }
        val runner = DefaultOcrRunner(
            scope,
            capture,
            FakeRepository,
            RecordingCropper(),
            engine,
            captureTimeoutMs = 1_000,
        )

        runner.run()
        val fresh = frame(50L, 5, 5)
        capture.emit(fresh)
        withTimeout(1_000) { entered.await() }
        assertEquals(OcrRunState.Recognizing(50L), runner.state.value)
        release.complete(Unit)
        val error = withTimeout(1_000) {
            runner.state.filterIsInstance<OcrRunState.Error>().first()
        }
        assertEquals(OcrRunError.NETWORK, error.type)

        runner.clear()
        scope.cancel()
        fresh.bitmap.recycle()
    }

    @Test
    fun inactiveAndTimeoutStatesDoNotUseStaleFrame() = runBlocking {
        val inactiveScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val inactiveCapture = FakeCaptureClient(null).apply {
            mutableState.value = CaptureState(CapturePhase.INACTIVE)
        }
        val inactiveRunner = DefaultOcrRunner(
            inactiveScope,
            inactiveCapture,
            FakeRepository,
            RecordingCropper(),
            FakeOcrEngine(),
            captureTimeoutMs = 50,
        )
        inactiveRunner.run()
        val inactive = withTimeout(1_000) {
            inactiveRunner.state.filterIsInstance<OcrRunState.Error>().first()
        }
        assertEquals(OcrRunError.CAPTURE_INACTIVE, inactive.type)
        assertEquals(0, inactiveCapture.captureRequests)
        inactiveScope.cancel()

        val timeoutScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val stale = frame(70L, 3, 3)
        val timeoutCapture = FakeCaptureClient(stale)
        val timeoutRunner = DefaultOcrRunner(
            timeoutScope,
            timeoutCapture,
            FakeRepository,
            RecordingCropper(),
            FakeOcrEngine(),
            captureTimeoutMs = 50,
        )
        timeoutRunner.run()
        val timeout = withTimeout(1_000) {
            timeoutRunner.state.filterIsInstance<OcrRunState.Error>().first()
        }
        assertEquals(OcrRunError.CAPTURE_TIMEOUT, timeout.type)
        assertEquals(1, timeoutCapture.captureRequests)
        timeoutScope.cancel()
        stale.bitmap.recycle()
    }

    private class FakeCaptureClient(initialFrame: CapturedFrame?) : CaptureClient {
        val mutableState = MutableStateFlow(CaptureState(CapturePhase.ACTIVE))
        private val mutableFrame = MutableStateFlow(initialFrame)
        override val state: StateFlow<CaptureState> = mutableState
        override val latestFrame: StateFlow<CapturedFrame?> = mutableFrame
        var captureRequests = 0

        fun emit(frame: CapturedFrame) {
            mutableFrame.value = frame
        }

        override fun startCapture(resultCode: Int, permissionData: Intent) = Unit
        override fun captureFrame() { captureRequests += 1 }
        override fun stopCapture() = Unit
        override fun reportError(error: CaptureError) = Unit
    }

    private class RecordingCropper : OcrFrameCropper {
        var receivedFrame: CapturedFrame? = null
        var receivedProfile: OcrBoxProfile? = null

        override fun crop(frame: CapturedFrame, box: OcrBoxProfile): Bitmap {
            receivedFrame = frame
            receivedProfile = box
            return Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
        }
    }

    private object FakeRepository : OcrBoxRepository {
        private val profile = OcrBoxProfile(
            id = "test",
            name = "Test",
            normalizedRect = NormalizedRect(0.1f, 0.2f, 0.8f, 0.9f),
        )

        override fun getProfiles() = listOf(profile)
        override fun getActiveProfile() = profile
        override fun save(profile: OcrBoxProfile) = Unit
        override fun resetToDefault() = profile
    }

    private fun frame(timestamp: Long, width: Int, height: Int) = CapturedFrame(
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888),
        timestamp,
    )
}
