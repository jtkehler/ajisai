package com.jtkehler.ajisai.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.jtkehler.ajisai.R
import com.jtkehler.ajisai.input.FloatingBubbleTriggerSource
import com.jtkehler.ajisai.input.OverlayAction

internal class QuickControlsController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val triggerSource: FloatingBubbleTriggerSource,
) : OverlayHudSurface {
    private var controlsView: View? = null

    override val isShowing: Boolean
        get() = controlsView != null

    override fun show(): Boolean {
        if (controlsView != null) return true
        val margin = context.resources.getDimensionPixelSize(R.dimen.overlay_panel_margin)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = margin
            y = margin
        }
        return runCatching {
            val view = LayoutInflater.from(context).inflate(R.layout.overlay_quick_controls, null)
            view.findViewById<View>(R.id.overlay_quick_ocr_box).setOnClickListener {
                triggerSource.emit(OverlayAction.ConfigureOcrBox)
            }
            view.findViewById<View>(R.id.overlay_quick_settings).setOnClickListener {
                triggerSource.emit(OverlayAction.OpenSettings)
            }
            view.findViewById<View>(R.id.overlay_quick_hide).setOnClickListener {
                triggerSource.emit(OverlayAction.ToggleOverlay)
            }
            windowManager.addView(view, params)
            controlsView = view
        }.isSuccess
    }

    override fun hide() {
        controlsView?.let { view -> runCatching { windowManager.removeView(view) } }
        controlsView = null
    }
}
