package com.jtkehler.ajisai.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.jtkehler.ajisai.R
import com.jtkehler.ajisai.input.FloatingBubbleTriggerSource
import com.jtkehler.ajisai.input.OverlayActionCallbacks
import com.jtkehler.ajisai.input.OverlayTriggerRouter
import com.jtkehler.ajisai.ocr.OcrDependencies
import com.jtkehler.ajisai.ocr.OcrRunner
import com.jtkehler.ajisai.ocrbox.AndroidOcrBoxEditorController
import com.jtkehler.ajisai.ocrbox.OcrBoxDependencies
import com.jtkehler.ajisai.ocrbox.OcrBoxEditorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var triggerSource: FloatingBubbleTriggerSource? = null
    private var bubbleController: FloatingBubbleController? = null
    private var panelController: OverlayPanelController? = null
    private var ocrResultPanelController: OcrResultPanelController? = null
    private var ocrBoxEditorController: OcrBoxEditorController? = null
    private var ocrRunner: OcrRunner? = null
    private var preserveError = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startOverlay()
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bubbleController?.clampToCurrentBounds()
    }

    override fun onDestroy() {
        ocrBoxEditorController?.dismiss(notifyClosed = false)
        ocrRunner?.clear()
        ocrResultPanelController?.hide()
        panelController?.hide()
        bubbleController?.hide()
        triggerSource?.stop()
        ocrBoxEditorController = null
        ocrRunner = null
        ocrResultPanelController = null
        panelController = null
        bubbleController = null
        triggerSource = null
        val permission = if (Settings.canDrawOverlays(this)) {
            OverlayPermissionState.GRANTED
        } else {
            OverlayPermissionState.MISSING
        }
        if (!preserveError) {
            OverlayRuntime.update(OverlayState(permission, OverlayServiceState.STOPPED))
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun startOverlay() {
        if (bubbleController != null) return
        if (!Settings.canDrawOverlays(this)) {
            preserveError = true
            OverlayRuntime.update(
                OverlayState(
                    permission = OverlayPermissionState.MISSING,
                    service = OverlayServiceState.STOPPED,
                    error = OverlayError.PERMISSION_MISSING,
                ),
            )
            stopSelf()
            return
        }

        val windowManager = getSystemService(WindowManager::class.java)
        lateinit var panel: OverlayPanelController
        lateinit var resultPanel: OcrResultPanelController
        lateinit var bubble: FloatingBubbleController
        lateinit var editor: OcrBoxEditorController
        val runner = OcrDependencies.runner(this, scope)
        resultPanel = OcrResultPanelController(
            context = this,
            windowManager = windowManager,
            onRetry = runner::retry,
            onTextChanged = runner::updateText,
            onClearClose = runner::clear,
        )
        val router = OverlayTriggerRouter(
            OverlayActionCallbacks(
                toggleOverlay = {
                    if (resultPanel.isShowing) {
                        runner.clear()
                    } else if (!panel.toggle()) {
                        showPlaceholder(R.string.overlay_panel_failed)
                    }
                },
                runOcr = {
                    panel.hide()
                    runner.run()
                },
                configureOcrBox = {
                    runner.clear()
                    panel.hide()
                    bubble.hide()
                    if (!editor.show()) {
                        bubble.show()
                        showPlaceholder(R.string.ocr_box_editor_failed)
                    }
                },
                closeOverlay = { stopSelf() },
            ),
        )
        val source = FloatingBubbleTriggerSource(router)
        panel = OverlayPanelController(this, windowManager, source)
        bubble = FloatingBubbleController(
            context = this,
            windowManager = windowManager,
            triggerSource = source,
            positionStore = BubblePositionStore(this),
            onPositionChanged = { position, size ->
                panel.updateAnchor(position, size)
                resultPanel.updateAnchor(position, size)
            },
        )
        editor = AndroidOcrBoxEditorController(
            context = this,
            windowManager = windowManager,
            repository = OcrBoxDependencies.repository(this),
            onClosed = {
                if (!bubble.show()) showPlaceholder(R.string.overlay_bubble_restore_failed)
            },
            onSaveFailed = { showPlaceholder(R.string.ocr_box_save_failed) },
        )
        source.start()
        if (!bubble.show()) {
            source.stop()
            preserveError = true
            OverlayRuntime.update(
                OverlayState(
                    permission = OverlayPermissionState.GRANTED,
                    service = OverlayServiceState.STOPPED,
                    error = OverlayError.SERVICE_UNAVAILABLE,
                ),
            )
            stopSelf()
            return
        }

        triggerSource = source
        panelController = panel
        ocrResultPanelController = resultPanel
        bubbleController = bubble
        ocrBoxEditorController = editor
        ocrRunner = runner
        scope.launch {
            runner.state.collect { state ->
                if (!resultPanel.render(state)) {
                    showPlaceholder(R.string.ocr_result_panel_failed)
                    runner.clear()
                }
            }
        }
        preserveError = false
        OverlayRuntime.update(
            OverlayState(OverlayPermissionState.GRANTED, OverlayServiceState.RUNNING),
        )
    }

    private fun showPlaceholder(messageRes: Int) {
        val message = getString(messageRes)
        Log.i(LOG_TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ACTION_START = "com.jtkehler.ajisai.overlay.START"
        private const val ACTION_STOP = "com.jtkehler.ajisai.overlay.STOP"
        private const val LOG_TAG = "AjisaiOverlay"

        fun startIntent(context: Context) = Intent(context, OverlayService::class.java).apply {
            action = ACTION_START
        }

        fun stopIntent(context: Context) = Intent(context, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
