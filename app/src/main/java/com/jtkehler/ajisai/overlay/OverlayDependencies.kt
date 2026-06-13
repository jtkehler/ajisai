package com.jtkehler.ajisai.overlay

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

object OverlayDependencies {
    private val productionClientFactory: (Context) -> OverlayClient = { context ->
        AndroidOverlayClient(context.applicationContext)
    }
    private val productionPermissionLauncherFactory:
        (AppCompatActivity, () -> Unit) -> OverlayPermissionLauncher = { activity, callback ->
            AndroidOverlayPermissionLauncher(activity, callback)
        }

    @Volatile
    private var clientFactory = productionClientFactory

    @Volatile
    private var permissionLauncherFactory = productionPermissionLauncherFactory

    fun client(context: Context): OverlayClient = clientFactory(context)

    fun permissionLauncher(
        activity: AppCompatActivity,
        onReturn: () -> Unit,
    ): OverlayPermissionLauncher = permissionLauncherFactory(activity, onReturn)

    fun setClientFactoryForTests(factory: (Context) -> OverlayClient) {
        clientFactory = factory
    }

    fun setPermissionLauncherFactoryForTests(
        factory: (AppCompatActivity, () -> Unit) -> OverlayPermissionLauncher,
    ) {
        permissionLauncherFactory = factory
    }

    fun resetForTests() {
        clientFactory = productionClientFactory
        permissionLauncherFactory = productionPermissionLauncherFactory
        OverlayRuntime.reset()
    }
}
