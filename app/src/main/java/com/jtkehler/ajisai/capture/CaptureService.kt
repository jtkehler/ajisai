package com.jtkehler.ajisai.capture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import com.jtkehler.ajisai.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var captureJob: Job? = null
    private var preserveStartupError = false
    private lateinit var controller: MediaProjectionController

    override fun onCreate() {
        super.onCreate()
        controller = MediaProjectionController(this) { stopCaptureService() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProjection(intent)
            ACTION_CAPTURE -> captureOnce()
            ACTION_STOP -> stopCaptureService()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        captureJob?.cancel()
        controller.release()
        scope.cancel()
        if (!preserveStartupError) {
            CaptureRuntime.updateState(CaptureState())
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startProjection(intent: Intent) {
        startCaptureForeground()
        val permissionData = IntentCompat.getParcelableExtra(
            intent,
            EXTRA_PERMISSION_DATA,
            Intent::class.java,
        )
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        if (permissionData == null || resultCode == Int.MIN_VALUE) {
            preserveStartupError = true
            CaptureRuntime.updateState(
                CaptureState(CapturePhase.INACTIVE, CaptureError.PERMISSION_DENIED),
            )
            stopSelf()
            return
        }

        if (controller.start(resultCode, permissionData)) {
            preserveStartupError = false
            CaptureRuntime.updateState(CaptureState(CapturePhase.ACTIVE))
        } else {
            preserveStartupError = true
            CaptureRuntime.updateState(
                CaptureState(CapturePhase.INACTIVE, CaptureError.CAPTURE_UNAVAILABLE),
            )
            stopSelf()
        }
    }

    private fun captureOnce() {
        if (CaptureRuntime.state.value.phase != CapturePhase.ACTIVE || captureJob?.isActive == true) {
            CaptureRuntime.updateState(
                CaptureState(CaptureRuntime.state.value.phase, CaptureError.CAPTURE_UNAVAILABLE),
            )
            return
        }

        CaptureRuntime.updateState(CaptureState(CapturePhase.CAPTURING))
        captureJob = scope.launch {
            when (val result = controller.captureFrame()) {
                is CaptureFrameResult.Success -> {
                    CaptureRuntime.updateFrame(result.frame)
                    CaptureRuntime.updateState(CaptureState(CapturePhase.ACTIVE))
                }
                is CaptureFrameResult.Failure -> {
                    CaptureRuntime.updateState(CaptureState(CapturePhase.ACTIVE, result.error))
                }
            }
        }
    }

    private fun stopCaptureService() {
        CaptureRuntime.updateState(CaptureState())
        stopSelf()
    }

    private fun startCaptureForeground() {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_screen_capture)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.capture_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.capture_notification_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_START = "com.jtkehler.ajisai.capture.START"
        private const val ACTION_CAPTURE = "com.jtkehler.ajisai.capture.CAPTURE"
        private const val ACTION_STOP = "com.jtkehler.ajisai.capture.STOP"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_PERMISSION_DATA = "permission_data"
        private const val NOTIFICATION_CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 1201

        fun startIntent(context: Context, resultCode: Int, permissionData: Intent) =
            Intent(context, CaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_PERMISSION_DATA, permissionData)
            }

        fun captureIntent(context: Context) = Intent(context, CaptureService::class.java).apply {
            action = ACTION_CAPTURE
        }

        fun stopIntent(context: Context) = Intent(context, CaptureService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
