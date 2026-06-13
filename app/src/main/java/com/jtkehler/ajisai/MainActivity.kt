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

        findViewById<MaterialButton>(R.id.settings_button).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        startCaptureButton.setOnClickListener { permissionLauncher.launch() }
        captureScreenshotButton.setOnClickListener { captureClient.captureFrame() }
        stopCaptureButton.setOnClickListener { captureClient.stopCapture() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { captureClient.state.collect(::renderCaptureState) }
                launch { captureClient.latestFrame.collect(::renderCapturedFrame) }
            }
        }
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
            return
        }
        capturePreview.setImageBitmap(frame.bitmap)
        capturePreview.visibility = View.VISIBLE
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
