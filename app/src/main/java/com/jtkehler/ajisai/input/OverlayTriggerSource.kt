package com.jtkehler.ajisai.input

/** Boundary for sources that emit overlay actions without executing feature logic. */
interface OverlayTriggerSource {
    fun start()

    fun stop()
}

// TODO(Stage later): AccessibilityVolumeTriggerSource.
// TODO(Stage later): QuickSettingsTriggerSource.
// TODO(Stage later): GamepadTriggerSource.
