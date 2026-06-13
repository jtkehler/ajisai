package com.jtkehler.ajisai.overlay

import kotlinx.coroutines.flow.StateFlow

interface OverlayClient {
    val state: StateFlow<OverlayState>

    fun refreshPermission()
    fun startOverlay()
    fun stopOverlay()
}
