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
import com.jtkehler.ajisai.SettingsActivity
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
    private var hudController: OverlayHudController? = null
    private var quickControlsController: QuickControlsController? = null
    private var ocrTextPanelController: BottomOcrTextPanelController? = null
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
        hudController?.collapse()
        ocrTextPanelController?.hide()
        quickControlsController?.hide()
        bubbleController?.hide()
        triggerSource?.stop()
        ocrBoxEditorController = null
        ocrRunner = null
        ocrTextPanelController = null
        quickControlsController = null
        hudController = null
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
        lateinit var hud: OverlayHudController
        lateinit var bubble: FloatingBubbleController
        lateinit var editor: OcrBoxEditorController
        val runner = OcrDependencies.runner(this, scope)
        val router = OverlayTriggerRouter(
            OverlayActionCallbacks(
                toggleOverlay = {
                    if (!hud.toggle()) {
                        showPlaceholder(R.string.overlay_panel_failed)
                    }
                },
                runOcr = runner::run,
                configureOcrBox = {
                    hud.suspendForEditor()
                    if (!editor.show()) {
                        hud.resumeAfterEditor()
                        showPlaceholder(R.string.ocr_box_editor_failed)
                    }
                },
                openSettings = {
                    hud.collapse()
                    startActivity(
                        Intent(this, SettingsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                closeOverlay = { stopSelf() },
            ),
        )
        val source = FloatingBubbleTriggerSource(router)
        val quickControls = QuickControlsController(this, windowManager, source)
        val ocrTextPanel = BottomOcrTextPanelController(this, windowManager)
        hud = OverlayHudController(runner, quickControls, ocrTextPanel)
        bubble = FloatingBubbleController(
            context = this,
            windowManager = windowManager,
            triggerSource = source,
            positionStore = BubblePositionStore(this),
            onPositionChanged = { _, _ -> },
        )
        editor = AndroidOcrBoxEditorController(
            context = this,
            windowManager = windowManager,
            repository = OcrBoxDependencies.repository(this),
            onClosed = {
                if (!hud.resumeAfterEditor()) {
                    showPlaceholder(R.string.ocr_result_panel_failed)
                }
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
        hudController = hud
        quickControlsController = quickControls
        ocrTextPanelController = ocrTextPanel
        bubbleController = bubble
        ocrBoxEditorController = editor
        ocrRunner = runner
        scope.launch {
            runner.state.collect { state ->
                if (!hud.render(state)) {
                    showPlaceholder(R.string.ocr_result_panel_failed)
                    hud.collapse()
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
