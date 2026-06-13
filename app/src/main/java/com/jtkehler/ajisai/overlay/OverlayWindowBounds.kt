package com.jtkehler.ajisai.overlay

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

internal data class OverlayWindowBounds(
    val width: Int,
    val height: Int,
)

@Suppress("DEPRECATION")
internal fun Context.overlayWindowBounds(windowManager: WindowManager): OverlayWindowBounds {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.maximumWindowMetrics.bounds
        OverlayWindowBounds(bounds.width(), bounds.height())
    } else {
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        OverlayWindowBounds(point.x, point.y)
    }
}
