package com.jtkehler.ajisai.overlay

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

fun interface OverlayPermissionLauncher {
    fun launch()
}

class AndroidOverlayPermissionLauncher(
    private val activity: AppCompatActivity,
    private val onReturn: () -> Unit,
) : OverlayPermissionLauncher {
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        onReturn()
    }

    override fun launch() {
        if (Settings.canDrawOverlays(activity)) {
            onReturn()
            return
        }
        val packageUri = Uri.parse("package:${activity.packageName}")
        val overlayIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        runCatching { launcher.launch(overlayIntent) }
            .recoverCatching { launcher.launch(fallbackIntent) }
            .onFailure { onReturn() }
    }
}
