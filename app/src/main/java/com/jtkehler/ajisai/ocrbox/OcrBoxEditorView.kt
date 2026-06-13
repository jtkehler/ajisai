package com.jtkehler.ajisai.ocrbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.jtkehler.ajisai.R
import kotlin.math.hypot

class OcrBoxEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val density = resources.displayMetrics.density
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hydrangea_blue)
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.FILL
    }
    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hydrangea_blue)
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val scrimPaint = Paint().apply {
        color = 0x66000000
        style = Paint.Style.FILL
    }
    private val handleRadius = 7f * density
    private val handleHitRadius = 28f * density
    private var draftRect = OcrBoxDefaults.defaultRect
    private var gestureStartRect = draftRect
    private var downX = 0f
    private var downY = 0f
    private var moving = false
    private var resizeHandle: OcrBoxResizeHandle? = null

    init {
        isClickable = true
        contentDescription = context.getString(R.string.ocr_box_editor_description)
    }

    fun setNormalizedRect(rect: NormalizedRect) {
        draftRect = OcrBoxGeometry.sanitize(rect)
        invalidate()
    }

    fun getNormalizedRect(): NormalizedRect = draftRect

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val rect = OcrBoxGeometry.toPixelRect(draftRect, width, height)
        val left = rect.left.toFloat()
        val top = rect.top.toFloat()
        val right = rect.right.toFloat()
        val bottom = rect.bottom.toFloat()

        canvas.drawRect(0f, 0f, width.toFloat(), top, scrimPaint)
        canvas.drawRect(0f, bottom, width.toFloat(), height.toFloat(), scrimPaint)
        canvas.drawRect(0f, top, left, bottom, scrimPaint)
        canvas.drawRect(right, top, width.toFloat(), bottom, scrimPaint)
        canvas.drawRect(left, top, right, bottom, outlinePaint)
        handlePoints(rect).forEach { point ->
            canvas.drawCircle(point.x, point.y, handleRadius, handleFillPaint)
            canvas.drawCircle(point.x, point.y, handleRadius, handleStrokePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (width <= 0 || height <= 0) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartRect = draftRect
                downX = event.x
                downY = event.y
                resizeHandle = findHandle(event.x, event.y)
                moving = resizeHandle == null && containsPoint(event.x, event.y)
                return resizeHandle != null || moving
            }
            MotionEvent.ACTION_MOVE -> {
                if (!moving && resizeHandle == null) return false
                val deltaX = (event.x - downX) / width
                val deltaY = (event.y - downY) / height
                draftRect = resizeHandle?.let { handle ->
                    OcrBoxGeometry.resize(gestureStartRect, handle, deltaX, deltaY)
                } ?: OcrBoxGeometry.move(gestureStartRect, deltaX, deltaY)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val handled = moving || resizeHandle != null
                moving = false
                resizeHandle = null
                if (handled) performClick()
                return handled
            }
            MotionEvent.ACTION_CANCEL -> {
                moving = false
                resizeHandle = null
                return true
            }
            else -> return false
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun containsPoint(x: Float, y: Float): Boolean {
        val rect = OcrBoxGeometry.toPixelRect(draftRect, width, height)
        return x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
    }

    private fun findHandle(x: Float, y: Float): OcrBoxResizeHandle? {
        val rect = OcrBoxGeometry.toPixelRect(draftRect, width, height)
        return handlePoints(rect)
            .minByOrNull { point -> hypot(x - point.x, y - point.y) }
            ?.takeIf { point -> hypot(x - point.x, y - point.y) <= handleHitRadius }
            ?.handle
    }

    private fun handlePoints(rect: PixelRect): List<HandlePoint> {
        val left = rect.left.toFloat()
        val top = rect.top.toFloat()
        val right = rect.right.toFloat()
        val bottom = rect.bottom.toFloat()
        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        return listOf(
            HandlePoint(OcrBoxResizeHandle.TOP_LEFT, left, top),
            HandlePoint(OcrBoxResizeHandle.TOP, centerX, top),
            HandlePoint(OcrBoxResizeHandle.TOP_RIGHT, right, top),
            HandlePoint(OcrBoxResizeHandle.RIGHT, right, centerY),
            HandlePoint(OcrBoxResizeHandle.BOTTOM_RIGHT, right, bottom),
            HandlePoint(OcrBoxResizeHandle.BOTTOM, centerX, bottom),
            HandlePoint(OcrBoxResizeHandle.BOTTOM_LEFT, left, bottom),
            HandlePoint(OcrBoxResizeHandle.LEFT, left, centerY),
        )
    }

    private data class HandlePoint(
        val handle: OcrBoxResizeHandle,
        val x: Float,
        val y: Float,
    )
}
