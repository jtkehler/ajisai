package com.jtkehler.ajisai.overlay

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal object OverlayRuntime {
    private val mutableState = MutableStateFlow(OverlayState())
    val state: StateFlow<OverlayState> = mutableState

    fun update(state: OverlayState) {
        mutableState.value = state
    }

    fun reset() {
        mutableState.value = OverlayState()
    }
}

internal class AndroidOverlayClient(
    private val context: Context,
) : OverlayClient {
    override val state: StateFlow<OverlayState> = OverlayRuntime.state

    init {
        refreshPermission()
    }

    override fun refreshPermission() {
        val permission = if (Settings.canDrawOverlays(context)) {
            OverlayPermissionState.GRANTED
        } else {
            OverlayPermissionState.MISSING
        }
        val service = if (permission == OverlayPermissionState.GRANTED) {
            state.value.service
        } else {
            OverlayServiceState.STOPPED
        }
        OverlayRuntime.update(OverlayState(permission, service))
    }

    override fun startOverlay() {
        refreshPermission()
        if (state.value.permission != OverlayPermissionState.GRANTED) {
            OverlayRuntime.update(state.value.copy(error = OverlayError.PERMISSION_MISSING))
            return
        }
        runCatching { context.startService(OverlayService.startIntent(context)) }
            .onFailure {
                OverlayRuntime.update(state.value.copy(error = OverlayError.SERVICE_UNAVAILABLE))
            }
    }

    override fun stopOverlay() {
        runCatching { context.startService(OverlayService.stopIntent(context)) }
            .onFailure {
                OverlayRuntime.update(state.value.copy(error = OverlayError.SERVICE_UNAVAILABLE))
            }
    }
}
