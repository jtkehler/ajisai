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

internal class OverlayPanelController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val triggerSource: FloatingBubbleTriggerSource,
) {
    private var panelView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var bubblePosition = BubblePosition(0, 0)
    private var bubbleSize = 0

    val isShowing: Boolean
        get() = panelView != null

    fun toggle(): Boolean {
        if (isShowing) {
            hide()
            return true
        }
        return show()
    }

    fun show(): Boolean {
        if (panelView != null) return true
        val panelWidth = context.resources.getDimensionPixelSize(R.dimen.overlay_panel_width)
        val params = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        updatePanelPosition(params, panelWidth)
        return runCatching {
            val view = LayoutInflater.from(context).inflate(R.layout.overlay_panel, null)
            view.findViewById<View>(R.id.overlay_ocr_action).setOnClickListener {
                triggerSource.emit(OverlayAction.RunOcr)
            }
            view.findViewById<View>(R.id.overlay_configure_action).setOnClickListener {
                triggerSource.emit(OverlayAction.ConfigureOcrBox)
            }
            view.findViewById<View>(R.id.overlay_close_action).setOnClickListener {
                triggerSource.emit(OverlayAction.CloseOverlay)
            }
            windowManager.addView(view, params)
            view
        }
            .onSuccess {
                panelView = it
                layoutParams = params
            }
            .isSuccess
    }

    fun hide() {
        panelView?.let { view -> runCatching { windowManager.removeView(view) } }
        panelView = null
        layoutParams = null
    }

    fun updateAnchor(position: BubblePosition, size: Int) {
        bubblePosition = position
        bubbleSize = size
        val view = panelView ?: return
        val params = layoutParams ?: return
        updatePanelPosition(params, params.width)
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun updatePanelPosition(params: WindowManager.LayoutParams, panelWidth: Int) {
        val margin = context.resources.getDimensionPixelSize(R.dimen.overlay_panel_margin)
        val panelHeight = context.resources.getDimensionPixelSize(
            R.dimen.overlay_panel_estimated_height,
        )
        val bounds = context.overlayWindowBounds(windowManager)
        val leftOfBubble = bubblePosition.x - panelWidth - margin
        params.x = if (leftOfBubble >= 0) {
            leftOfBubble
        } else {
            (bubblePosition.x + bubbleSize + margin)
                .coerceAtMost((bounds.width - panelWidth).coerceAtLeast(0))
        }
        params.y = bubblePosition.y.coerceIn(
            0,
            (bounds.height - panelHeight).coerceAtLeast(0),
        )
    }
}
