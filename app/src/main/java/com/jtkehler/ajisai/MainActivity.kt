package com.jtkehler.ajisai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.jtkehler.ajisai.capture.CaptureClient
import com.jtkehler.ajisai.capture.CaptureDependencies
import com.jtkehler.ajisai.capture.CaptureError
import com.jtkehler.ajisai.capture.CapturePermissionLauncher
import com.jtkehler.ajisai.capture.CapturePermissionResult
import com.jtkehler.ajisai.capture.CapturePhase
import com.jtkehler.ajisai.capture.CaptureState
import com.jtkehler.ajisai.capture.CapturedFrame
import com.jtkehler.ajisai.capture.toUiControls
import com.jtkehler.ajisai.overlay.OverlayClient
import com.jtkehler.ajisai.overlay.OverlayDependencies
import com.jtkehler.ajisai.overlay.OverlayError
import com.jtkehler.ajisai.overlay.OverlayPermissionLauncher
import com.jtkehler.ajisai.overlay.OverlayPermissionState
import com.jtkehler.ajisai.overlay.OverlayServiceState
import com.jtkehler.ajisai.overlay.OverlayState
import com.jtkehler.ajisai.overlay.toUiControls
import com.jtkehler.ajisai.ocrbox.OcrBoxDependencies
import com.jtkehler.ajisai.ocrbox.OcrBoxProfile
import com.jtkehler.ajisai.ocrbox.OcrBoxRepository
import com.jtkehler.ajisai.ocrbox.OcrFrameCropper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var captureClient: CaptureClient
    private lateinit var permissionLauncher: CapturePermissionLauncher
    private lateinit var captureStatus: TextView
    private lateinit var startCaptureButton: MaterialButton
    private lateinit var captureScreenshotButton: MaterialButton
    private lateinit var stopCaptureButton: MaterialButton
    private lateinit var captureProgress: ProgressBar
    private lateinit var capturePreview: ImageView
    private lateinit var ocrBoxRepository: OcrBoxRepository
    private lateinit var ocrFrameCropper: OcrFrameCropper
    private lateinit var ocrBoxCoordinates: TextView
    private lateinit var resetOcrBoxButton: MaterialButton
    private lateinit var previewOcrCropButton: MaterialButton
    private lateinit var ocrCropPreview: ImageView
    private var ocrCropPreviewBitmap: android.graphics.Bitmap? = null
    private lateinit var overlayClient: OverlayClient
    private lateinit var overlayPermissionLauncher: OverlayPermissionLauncher
    private lateinit var overlayPermissionStatus: TextView
    private lateinit var overlayServiceStatus: TextView
    private lateinit var requestOverlayPermissionButton: MaterialButton
    private lateinit var startOverlayButton: MaterialButton
    private lateinit var stopOverlayButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captureClient = CaptureDependencies.client(this)
        permissionLauncher = CaptureDependencies.permissionLauncher(this, ::onCapturePermissionResult)
        captureStatus = findViewById(R.id.capture_status)
        startCaptureButton = findViewById(R.id.start_capture_button)
        captureScreenshotButton = findViewById(R.id.capture_screenshot_button)
        stopCaptureButton = findViewById(R.id.stop_capture_button)
        captureProgress = findViewById(R.id.capture_progress)
        capturePreview = findViewById(R.id.capture_preview)
        ocrBoxRepository = OcrBoxDependencies.repository(this)
        ocrFrameCropper = OcrBoxDependencies.cropper()
        ocrBoxCoordinates = findViewById(R.id.ocr_box_coordinates)
        resetOcrBoxButton = findViewById(R.id.reset_ocr_box_button)
        previewOcrCropButton = findViewById(R.id.preview_ocr_crop_button)
        ocrCropPreview = findViewById(R.id.ocr_crop_preview)
        overlayClient = OverlayDependencies.client(this)
        overlayPermissionLauncher = OverlayDependencies.permissionLauncher(this) {
            overlayClient.refreshPermission()
        }
        overlayPermissionStatus = findViewById(R.id.overlay_permission_status)
        overlayServiceStatus = findViewById(R.id.overlay_service_status)
        requestOverlayPermissionButton = findViewById(R.id.request_overlay_permission_button)
        startOverlayButton = findViewById(R.id.start_overlay_button)
        stopOverlayButton = findViewById(R.id.stop_overlay_button)

        findViewById<MaterialButton>(R.id.settings_button).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        startCaptureButton.setOnClickListener { permissionLauncher.launch() }
        captureScreenshotButton.setOnClickListener { captureClient.captureFrame() }
        stopCaptureButton.setOnClickListener { captureClient.stopCapture() }
        resetOcrBoxButton.setOnClickListener {
            renderOcrBoxProfile(ocrBoxRepository.resetToDefault())
            clearOcrCropPreview()
        }
        previewOcrCropButton.setOnClickListener { previewSavedOcrCrop() }
        requestOverlayPermissionButton.setOnClickListener { overlayPermissionLauncher.launch() }
        startOverlayButton.setOnClickListener { overlayClient.startOverlay() }
        stopOverlayButton.setOnClickListener { overlayClient.stopOverlay() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { captureClient.state.collect(::renderCaptureState) }
                launch { captureClient.latestFrame.collect(::renderCapturedFrame) }
                launch { overlayClient.state.collect(::renderOverlayState) }
            }
        }
        renderOcrBoxProfile(ocrBoxRepository.getActiveProfile())
    }

    override fun onResume() {
        super.onResume()
        if (::overlayClient.isInitialized) overlayClient.refreshPermission()
        if (::ocrBoxRepository.isInitialized) renderOcrBoxProfile(ocrBoxRepository.getActiveProfile())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::ocrBoxRepository.isInitialized) {
            renderOcrBoxProfile(ocrBoxRepository.getActiveProfile())
        }
    }

    override fun onDestroy() {
        clearOcrCropPreview()
        super.onDestroy()
    }

    private fun onCapturePermissionResult(result: CapturePermissionResult) {
        when (result) {
            is CapturePermissionResult.Granted -> {
                captureClient.startCapture(result.resultCode, result.data)
            }
            CapturePermissionResult.Denied -> {
                captureClient.reportError(CaptureError.PERMISSION_DENIED)
            }
            CapturePermissionResult.Unavailable -> {
                captureClient.reportError(CaptureError.SERVICE_UNAVAILABLE)
            }
        }
    }

    private fun renderCaptureState(state: CaptureState) {
        val controls = state.toUiControls()
        startCaptureButton.isEnabled = controls.startEnabled
        captureScreenshotButton.isEnabled = controls.captureEnabled
        stopCaptureButton.isEnabled = controls.stopEnabled
        captureProgress.visibility = if (controls.progressVisible) View.VISIBLE else View.GONE
        captureStatus.setText(state.statusText())
    }

    private fun renderCapturedFrame(frame: CapturedFrame?) {
        if (frame == null) {
            capturePreview.setImageDrawable(null)
            capturePreview.visibility = View.GONE
            previewOcrCropButton.isEnabled = false
            clearOcrCropPreview()
            return
        }
        capturePreview.setImageBitmap(frame.bitmap)
        capturePreview.visibility = View.VISIBLE
        previewOcrCropButton.isEnabled = true
    }

    private fun renderOcrBoxProfile(profile: OcrBoxProfile) {
        val rect = profile.normalizedRect
        ocrBoxCoordinates.text = getString(
            R.string.ocr_box_coordinates,
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
        )
    }

    private fun previewSavedOcrCrop() {
        val frame = captureClient.latestFrame.value ?: return
        val crop = ocrFrameCropper.crop(frame, ocrBoxRepository.getActiveProfile()) ?: return
        clearOcrCropPreview()
        ocrCropPreviewBitmap = crop
        ocrCropPreview.setImageBitmap(crop)
        ocrCropPreview.visibility = View.VISIBLE
    }

    private fun clearOcrCropPreview() {
        if (!::ocrCropPreview.isInitialized) return
        ocrCropPreview.setImageDrawable(null)
        ocrCropPreview.visibility = View.GONE
        ocrCropPreviewBitmap?.takeUnless { it.isRecycled }?.recycle()
        ocrCropPreviewBitmap = null
    }

    private fun renderOverlayState(state: OverlayState) {
        val controls = state.toUiControls()
        requestOverlayPermissionButton.isEnabled = controls.requestPermissionEnabled
        startOverlayButton.isEnabled = controls.startEnabled
        stopOverlayButton.isEnabled = controls.stopEnabled
        overlayPermissionStatus.setText(
            when (state.permission) {
                OverlayPermissionState.MISSING -> R.string.overlay_permission_missing
                OverlayPermissionState.GRANTED -> R.string.overlay_permission_granted
            },
        )
        overlayServiceStatus.setText(
            state.error?.let {
                when (it) {
                    OverlayError.PERMISSION_MISSING -> R.string.overlay_error_permission_missing
                    OverlayError.SERVICE_UNAVAILABLE -> R.string.overlay_error_service_unavailable
                }
            } ?: when (state.service) {
                OverlayServiceState.STOPPED -> R.string.overlay_service_stopped
                OverlayServiceState.RUNNING -> R.string.overlay_service_running
            },
        )
    }

    private fun CaptureState.statusText(): Int = error?.let {
        when (it) {
            CaptureError.PERMISSION_DENIED -> R.string.capture_status_permission_denied
            CaptureError.SERVICE_UNAVAILABLE -> R.string.capture_status_service_unavailable
            CaptureError.CAPTURE_UNAVAILABLE -> R.string.capture_status_unavailable
            CaptureError.NO_FRAME_AVAILABLE -> R.string.capture_status_no_frame
            CaptureError.CAPTURE_FAILED -> R.string.capture_status_failed
        }
    } ?: when (phase) {
        CapturePhase.INACTIVE -> R.string.capture_status_inactive
        CapturePhase.STARTING -> R.string.capture_status_starting
        CapturePhase.ACTIVE -> R.string.capture_status_active
        CapturePhase.CAPTURING -> R.string.capture_status_capturing
    }
}
