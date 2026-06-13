package com.jtkehler.ajisai.capture

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

object CaptureDependencies {
    private val productionClientFactory: (Context) -> CaptureClient = { context ->
        AndroidCaptureClient(context.applicationContext)
    }
    private val productionPermissionLauncherFactory:
        (AppCompatActivity, (CapturePermissionResult) -> Unit) -> CapturePermissionLauncher =
        { activity, callback -> MediaProjectionPermissionLauncher(activity, callback) }

    @Volatile
    private var clientFactory = productionClientFactory

    @Volatile
    private var permissionLauncherFactory = productionPermissionLauncherFactory

    fun client(context: Context): CaptureClient = clientFactory(context)

    fun permissionLauncher(
        activity: AppCompatActivity,
        callback: (CapturePermissionResult) -> Unit,
    ): CapturePermissionLauncher = permissionLauncherFactory(activity, callback)

    fun setClientFactoryForTests(factory: (Context) -> CaptureClient) {
        clientFactory = factory
    }

    fun setPermissionLauncherFactoryForTests(
        factory: (AppCompatActivity, (CapturePermissionResult) -> Unit) -> CapturePermissionLauncher,
    ) {
        permissionLauncherFactory = factory
    }

    fun resetForTests() {
        clientFactory = productionClientFactory
        permissionLauncherFactory = productionPermissionLauncherFactory
        CaptureRuntime.reset()
    }
}
