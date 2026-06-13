package com.jtkehler.ajisai.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

internal class MediaProjectionController(
    private val context: Context,
    private val onProjectionStopped: () -> Unit,
) : ScreenFrameSource {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseResources(stopProjection = false)
            onProjectionStopped()
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            if (width <= 0 || height <= 0 || projection == null) return
            resizeCaptureSurface(width, height, displayMetrics().densityDpi)
        }
    }

    fun start(resultCode: Int, permissionData: Intent): Boolean {
        release()
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
            as? MediaProjectionManager ?: return false
        val mediaProjection = runCatching {
            manager.getMediaProjection(resultCode, permissionData)
        }.getOrNull() ?: return false

        projection = mediaProjection
        mediaProjection.registerCallback(projectionCallback, mainHandler)
        val metrics = displayMetrics()
        val started = runCatching {
            createCaptureSurface(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        }.getOrDefault(false)
        if (!started) release()
        return started
    }

    override suspend fun captureFrame(): CaptureFrameResult {
        val reader = imageReader
            ?: return CaptureFrameResult.Failure(CaptureError.CAPTURE_UNAVAILABLE)

        drainImages(reader)
        val image = withTimeoutOrNull(FRAME_TIMEOUT_MS) { awaitLatestImage(reader) }
            ?: return CaptureFrameResult.Failure(CaptureError.NO_FRAME_AVAILABLE)

        return runCatching {
            image.use {
                val bitmap = withContext(Dispatchers.Default) { it.toBitmap() }
                CapturedFrame(bitmap, SystemClock.elapsedRealtimeNanos())
            }
        }.fold(
            onSuccess = { CaptureFrameResult.Success(it) },
            onFailure = { CaptureFrameResult.Failure(CaptureError.CAPTURE_FAILED) },
        )
    }

    fun release() {
        releaseResources(stopProjection = true)
    }

    private fun createCaptureSurface(width: Int, height: Int, densityDpi: Int): Boolean {
        val reader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            MAX_IMAGES,
        )
        imageReader = reader
        virtualDisplay = projection?.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            mainHandler,
        )
        if (virtualDisplay == null) {
            reader.close()
            imageReader = null
            return false
        }
        return true
    }

    private fun resizeCaptureSurface(width: Int, height: Int, densityDpi: Int) {
        val display = virtualDisplay ?: return
        val previousReader = imageReader
        val replacementReader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            MAX_IMAGES,
        )
        display.resize(width, height, densityDpi)
        display.surface = replacementReader.surface
        imageReader = replacementReader
        previousReader?.setOnImageAvailableListener(null, null)
        previousReader?.close()
    }

    private suspend fun awaitLatestImage(reader: ImageReader): Image =
        suspendCancellableCoroutine { continuation ->
            reader.setOnImageAvailableListener(
                { source ->
                    val image = runCatching { source.acquireLatestImage() }.getOrNull()
                        ?: return@setOnImageAvailableListener
                    source.setOnImageAvailableListener(null, null)
                    if (continuation.isActive) {
                        continuation.resume(image)
                    } else {
                        image.close()
                    }
                },
                mainHandler,
            )
            continuation.invokeOnCancellation {
                mainHandler.post { reader.setOnImageAvailableListener(null, null) }
            }
        }

    private fun drainImages(reader: ImageReader) {
        while (true) {
            val image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: break
            image.close()
        }
    }

    private fun Image.toBitmap(): Bitmap {
        val plane = planes.first()
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val paddedWidth = width + rowPadding / pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        plane.buffer.rewind()
        padded.copyPixelsFromBuffer(plane.buffer)
        if (paddedWidth == width) return padded

        return Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
    }

    @Suppress("DEPRECATION")
    private fun displayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi = context.resources.displayMetrics.densityDpi
        } else {
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }
        return metrics
    }

    private fun releaseResources(stopProjection: Boolean) {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        val currentProjection = projection
        projection = null
        currentProjection?.unregisterCallback(projectionCallback)
        if (stopProjection) currentProjection?.stop()
    }

    private companion object {
        const val VIRTUAL_DISPLAY_NAME = "AjisaiScreenCapture"
        const val MAX_IMAGES = 2
        const val FRAME_TIMEOUT_MS = 2_000L
    }
}
