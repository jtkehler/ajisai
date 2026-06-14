package com.jtkehler.ajisai.ocr

import android.graphics.Bitmap
import com.jtkehler.ajisai.capture.CaptureClient
import com.jtkehler.ajisai.capture.CaptureError
import com.jtkehler.ajisai.capture.CapturePhase
import com.jtkehler.ajisai.capture.CapturedFrame
import com.jtkehler.ajisai.ocrbox.OcrBoxRepository
import com.jtkehler.ajisai.ocrbox.OcrFrameCropper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

interface OcrRunner {
    val state: StateFlow<OcrRunState>

    fun run()
    fun clear()
}

class DefaultOcrRunner(
    private val scope: CoroutineScope,
    private val captureClient: CaptureClient,
    private val ocrBoxRepository: OcrBoxRepository,
    private val cropper: OcrFrameCropper,
    private val ocrEngine: OcrEngine,
    private val optionsProvider: () -> OcrOptions = { OcrOptions() },
    private val captureTimeoutMs: Long = DEFAULT_CAPTURE_TIMEOUT_MS,
) : OcrRunner {
    private val mutableState = MutableStateFlow<OcrRunState>(OcrRunState.Idle)
    override val state: StateFlow<OcrRunState> = mutableState
    private var runJob: Job? = null

    override fun run() {
        runJob?.cancel()
        runJob = scope.launch { runOnce() }
    }

    override fun clear() {
        runJob?.cancel()
        runJob = null
        mutableState.value = OcrRunState.Idle
    }

    private suspend fun runOnce() {
        var crop: Bitmap? = null
        try {
            if (captureClient.state.value.phase != CapturePhase.ACTIVE) {
                mutableState.value = OcrRunState.Error(OcrRunError.CAPTURE_INACTIVE)
                return
            }

            mutableState.value = OcrRunState.Capturing
            val previousFrame = captureClient.latestFrame.value
            captureClient.captureFrame()
            val frame = awaitFreshFrame(previousFrame)
            mutableState.value = OcrRunState.Recognizing(frame.capturedAtElapsedRealtimeNanos)

            crop = cropper.crop(frame, ocrBoxRepository.getActiveProfile())
            if (crop == null) {
                mutableState.value = OcrRunState.Error(
                    OcrRunError.CROP_FAILED,
                    frame.capturedAtElapsedRealtimeNanos,
                )
                return
            }

            val result = ocrEngine.recognize(crop, options = optionsProvider())
            mutableState.value = OcrRunState.Success(
                text = result.text,
                capturedAtElapsedRealtimeNanos = frame.capturedAtElapsedRealtimeNanos,
                providerName = result.providerName,
                lines = result.lines,
                debugArtifacts = result.debugArtifacts,
            )
        } catch (error: CaptureWaitFailure) {
            mutableState.value = OcrRunState.Error(error.error.toRunError())
        } catch (_: TimeoutCancellationException) {
            mutableState.value = OcrRunState.Error(OcrRunError.CAPTURE_TIMEOUT)
        } catch (error: CancellationException) {
            throw error
        } catch (error: OcrException) {
            mutableState.value = OcrRunState.Error(error.type.toRunError())
        } catch (_: Throwable) {
            mutableState.value = OcrRunState.Error(OcrRunError.UNKNOWN)
        } finally {
            crop?.takeUnless { it.isRecycled }?.recycle()
        }
    }

    private suspend fun awaitFreshFrame(previousFrame: CapturedFrame?): CapturedFrame =
        withTimeout(captureTimeoutMs) {
            val previousTimestamp = previousFrame?.capturedAtElapsedRealtimeNanos ?: Long.MIN_VALUE
            merge(
                captureClient.latestFrame
                    .filterNotNull()
                    .filter { frame ->
                        frame !== previousFrame &&
                            frame.capturedAtElapsedRealtimeNanos > previousTimestamp
                    }
                    .map<CapturedFrame, CaptureWaitEvent>(CaptureWaitEvent::Frame),
                captureClient.state
                    .filter { state ->
                        state.error != null || state.phase == CapturePhase.INACTIVE
                    }
                    .map { state ->
                        CaptureWaitEvent.Failure(
                            state.error ?: CaptureError.CAPTURE_UNAVAILABLE,
                        )
                    },
            ).first().let { event ->
                when (event) {
                    is CaptureWaitEvent.Frame -> event.frame
                    is CaptureWaitEvent.Failure -> throw CaptureWaitFailure(event.error)
                }
            }
        }

    private sealed interface CaptureWaitEvent {
        data class Frame(val frame: CapturedFrame) : CaptureWaitEvent
        data class Failure(val error: CaptureError) : CaptureWaitEvent
    }

    private class CaptureWaitFailure(val error: CaptureError) : Exception()

    private fun CaptureError.toRunError(): OcrRunError = when (this) {
        CaptureError.PERMISSION_DENIED,
        CaptureError.SERVICE_UNAVAILABLE,
        CaptureError.CAPTURE_UNAVAILABLE,
        -> OcrRunError.CAPTURE_INACTIVE
        CaptureError.NO_FRAME_AVAILABLE,
        CaptureError.CAPTURE_FAILED,
        -> OcrRunError.CAPTURE_UNAVAILABLE
    }

    private fun OcrErrorType.toRunError(): OcrRunError = when (this) {
        OcrErrorType.PREPROCESSING -> OcrRunError.PREPROCESSING
        OcrErrorType.NETWORK -> OcrRunError.NETWORK
        OcrErrorType.HTTP -> OcrRunError.HTTP
        OcrErrorType.PARSE -> OcrRunError.PARSE
        OcrErrorType.NO_TEXT -> OcrRunError.NO_TEXT
    }

    private companion object {
        const val DEFAULT_CAPTURE_TIMEOUT_MS = 4_000L
    }
}
