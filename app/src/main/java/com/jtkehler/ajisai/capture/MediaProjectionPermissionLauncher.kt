package com.jtkehler.ajisai.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

sealed interface CapturePermissionResult {
    data class Granted(val resultCode: Int, val data: Intent) : CapturePermissionResult
    data object Denied : CapturePermissionResult
    data object Unavailable : CapturePermissionResult
}

fun interface CapturePermissionLauncher {
    fun launch()
}

class MediaProjectionPermissionLauncher(
    private val activity: AppCompatActivity,
    private val onResult: (CapturePermissionResult) -> Unit,
) : CapturePermissionLauncher {
    private val projectionManager =
        activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    private val projectionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            onResult(CapturePermissionResult.Granted(result.resultCode, data))
        } else {
            onResult(CapturePermissionResult.Denied)
        }
    }
    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        launchProjectionPermission()
    }

    override fun launch() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        launchProjectionPermission()
    }

    private fun launchProjectionPermission() {
        val manager = projectionManager
        if (manager == null) {
            onResult(CapturePermissionResult.Unavailable)
            return
        }
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }
}
