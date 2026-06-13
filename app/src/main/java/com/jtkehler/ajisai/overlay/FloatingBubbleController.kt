package com.jtkehler.ajisai.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.jtkehler.ajisai.R
import com.jtkehler.ajisai.input.FloatingBubbleTriggerSource
import com.jtkehler.ajisai.input.OverlayAction
import kotlin.math.abs
import kotlin.math.roundToInt

internal class FloatingBubbleController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val triggerSource: FloatingBubbleTriggerSource,
    private val positionStore: BubblePositionStore,
    private val onPositionChanged: (BubblePosition, Int) -> Unit,
) {
    private var bubbleView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    fun show(): Boolean {
        if (bubbleView != null) return true
        val size = context.resources.getDimensionPixelSize(R.dimen.overlay_bubble_size)
        val bounds = context.overlayWindowBounds(windowManager)
        val availableWidth = (bounds.width - size).coerceAtLeast(0)
        val availableHeight = (bounds.height - size).coerceAtLeast(0)
        val position = positionStore.load(availableWidth, availableHeight)
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = position.x
            y = position.y
        }
        val view = LayoutInflater.from(context).inflate(R.layout.overlay_bubble, null)
        view.setOnClickListener { triggerSource.emit(OverlayAction.ToggleOverlay) }
        installDragHandler(view, params, size)

        return runCatching { windowManager.addView(view, params) }
            .onSuccess {
                bubbleView = view
                layoutParams = params
                onPositionChanged(position, size)
            }
            .isSuccess
    }

    fun hide() {
        bubbleView?.let { view -> runCatching { windowManager.removeView(view) } }
        bubbleView = null
        layoutParams = null
    }

    fun clampToCurrentBounds() {
        val view = bubbleView ?: return
        val params = layoutParams ?: return
        val size = params.width
        val bounds = context.overlayWindowBounds(windowManager)
        params.x = params.x.coerceIn(0, (bounds.width - size).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (bounds.height - size).coerceAtLeast(0))
        runCatching { windowManager.updateViewLayout(view, params) }
        onPositionChanged(BubblePosition(params.x, params.y), size)
    }

    private fun installDragHandler(
        view: View,
        params: WindowManager.LayoutParams,
        size: Int,
    ) {
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).roundToInt()
                    val dy = (event.rawY - downRawY).roundToInt()
                    moved = moved || abs(dx) > touchSlop || abs(dy) > touchSlop
                    val bounds = context.overlayWindowBounds(windowManager)
                    params.x = (startX + dx).coerceIn(0, (bounds.width - size).coerceAtLeast(0))
                    params.y = (startY + dy).coerceIn(0, (bounds.height - size).coerceAtLeast(0))
                    runCatching { windowManager.updateViewLayout(view, params) }
                    onPositionChanged(BubblePosition(params.x, params.y), size)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        savePosition(params, size)
                    } else {
                        view.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    savePosition(params, size)
                    true
                }
                else -> false
            }
        }
    }

    private fun savePosition(params: WindowManager.LayoutParams, size: Int) {
        val bounds = context.overlayWindowBounds(windowManager)
        positionStore.save(
            position = BubblePosition(params.x, params.y),
            availableWidth = (bounds.width - size).coerceAtLeast(0),
            availableHeight = (bounds.height - size).coerceAtLeast(0),
        )
    }
}
