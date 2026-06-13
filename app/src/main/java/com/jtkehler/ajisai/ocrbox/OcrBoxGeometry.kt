package com.jtkehler.ajisai.ocrbox

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object OcrBoxGeometry {
    fun sanitize(
        rect: NormalizedRect,
        minimumWidth: Float = OcrBoxDefaults.MIN_WIDTH,
        minimumHeight: Float = OcrBoxDefaults.MIN_HEIGHT,
    ): NormalizedRect {
        val fallback = OcrBoxDefaults.defaultRect
        val rawLeft = rect.left.finiteOr(fallback.left)
        val rawTop = rect.top.finiteOr(fallback.top)
        val rawRight = rect.right.finiteOr(fallback.right)
        val rawBottom = rect.bottom.finiteOr(fallback.bottom)
        val minWidth = minimumWidth.finiteOr(OcrBoxDefaults.MIN_WIDTH).coerceIn(0.0001f, 1f)
        val minHeight = minimumHeight.finiteOr(OcrBoxDefaults.MIN_HEIGHT).coerceIn(0.0001f, 1f)

        val horizontal = enforceMinimumSpan(
            start = min(rawLeft, rawRight).coerceIn(0f, 1f),
            end = max(rawLeft, rawRight).coerceIn(0f, 1f),
            minimumSpan = minWidth,
        )
        val vertical = enforceMinimumSpan(
            start = min(rawTop, rawBottom).coerceIn(0f, 1f),
            end = max(rawTop, rawBottom).coerceIn(0f, 1f),
            minimumSpan = minHeight,
        )
        return NormalizedRect(
            left = horizontal.first,
            top = vertical.first,
            right = horizontal.second,
            bottom = vertical.second,
        )
    }

    fun toPixelRect(
        rect: NormalizedRect,
        width: Int,
        height: Int,
    ): PixelRect {
        if (width <= 0 || height <= 0) return PixelRect(0, 0, 0, 0)
        val safe = sanitize(rect)
        return clampPixelRect(
            rect = PixelRect(
                left = (safe.left * width).roundToInt(),
                top = (safe.top * height).roundToInt(),
                right = (safe.right * width).roundToInt(),
                bottom = (safe.bottom * height).roundToInt(),
            ),
            width = width,
            height = height,
        )
    }

    fun toNormalizedRect(
        rect: PixelRect,
        width: Int,
        height: Int,
    ): NormalizedRect {
        if (width <= 0 || height <= 0) return OcrBoxDefaults.defaultRect
        val safe = clampPixelRect(rect, width, height)
        return sanitize(
            NormalizedRect(
                left = safe.left.toFloat() / width,
                top = safe.top.toFloat() / height,
                right = safe.right.toFloat() / width,
                bottom = safe.bottom.toFloat() / height,
            ),
        )
    }

    fun clampPixelRect(
        rect: PixelRect,
        width: Int,
        height: Int,
        minimumWidth: Int = 1,
        minimumHeight: Int = 1,
    ): PixelRect {
        if (width <= 0 || height <= 0) return PixelRect(0, 0, 0, 0)
        val left = min(rect.left, rect.right).coerceIn(0, width)
        val right = max(rect.left, rect.right).coerceIn(0, width)
        val top = min(rect.top, rect.bottom).coerceIn(0, height)
        val bottom = max(rect.top, rect.bottom).coerceIn(0, height)
        val horizontal = enforceMinimumPixelSpan(
            left,
            right,
            minimumWidth.coerceIn(1, width),
            width,
        )
        val vertical = enforceMinimumPixelSpan(
            top,
            bottom,
            minimumHeight.coerceIn(1, height),
            height,
        )
        return PixelRect(horizontal.first, vertical.first, horizontal.second, vertical.second)
    }

    fun move(rect: NormalizedRect, deltaX: Float, deltaY: Float): NormalizedRect {
        val safe = sanitize(rect)
        val dx = deltaX.finiteOr(0f)
        val dy = deltaY.finiteOr(0f)
        val left = (safe.left + dx).coerceIn(0f, 1f - (safe.right - safe.left))
        val top = (safe.top + dy).coerceIn(0f, 1f - (safe.bottom - safe.top))
        return safe.copy(
            left = left,
            top = top,
            right = left + (safe.right - safe.left),
            bottom = top + (safe.bottom - safe.top),
        )
    }

    fun resize(
        rect: NormalizedRect,
        handle: OcrBoxResizeHandle,
        deltaX: Float,
        deltaY: Float,
        minimumWidth: Float = OcrBoxDefaults.MIN_WIDTH,
        minimumHeight: Float = OcrBoxDefaults.MIN_HEIGHT,
    ): NormalizedRect {
        val safe = sanitize(rect, minimumWidth, minimumHeight)
        val dx = deltaX.finiteOr(0f)
        val dy = deltaY.finiteOr(0f)
        val minWidth = minimumWidth.coerceIn(0.0001f, 1f)
        val minHeight = minimumHeight.coerceIn(0.0001f, 1f)
        var left = safe.left
        var top = safe.top
        var right = safe.right
        var bottom = safe.bottom

        if (handle.movesLeft) left = (left + dx).coerceIn(0f, right - minWidth)
        if (handle.movesRight) right = (right + dx).coerceIn(left + minWidth, 1f)
        if (handle.movesTop) top = (top + dy).coerceIn(0f, bottom - minHeight)
        if (handle.movesBottom) bottom = (bottom + dy).coerceIn(top + minHeight, 1f)

        return NormalizedRect(left, top, right, bottom)
    }

    fun isValid(rect: NormalizedRect): Boolean =
        rect.left.isFinite() &&
            rect.top.isFinite() &&
            rect.right.isFinite() &&
            rect.bottom.isFinite() &&
            rect.left in 0f..1f &&
            rect.top in 0f..1f &&
            rect.right in 0f..1f &&
            rect.bottom in 0f..1f &&
            rect.left < rect.right &&
            rect.top < rect.bottom

    private fun enforceMinimumSpan(start: Float, end: Float, minimumSpan: Float): Pair<Float, Float> {
        if (end - start >= minimumSpan) return start to end
        val center = (start + end) / 2f
        val adjustedStart = (center - minimumSpan / 2f).coerceIn(0f, 1f - minimumSpan)
        return adjustedStart to adjustedStart + minimumSpan
    }

    private fun enforceMinimumPixelSpan(
        start: Int,
        end: Int,
        minimumSpan: Int,
        maximum: Int,
    ): Pair<Int, Int> {
        if (end - start >= minimumSpan) return start to end
        val center = (start + end) / 2
        val adjustedStart = (center - minimumSpan / 2).coerceIn(0, maximum - minimumSpan)
        return adjustedStart to adjustedStart + minimumSpan
    }

    private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback

    private val OcrBoxResizeHandle.movesLeft: Boolean
        get() = this == OcrBoxResizeHandle.LEFT ||
            this == OcrBoxResizeHandle.TOP_LEFT ||
            this == OcrBoxResizeHandle.BOTTOM_LEFT

    private val OcrBoxResizeHandle.movesRight: Boolean
        get() = this == OcrBoxResizeHandle.RIGHT ||
            this == OcrBoxResizeHandle.TOP_RIGHT ||
            this == OcrBoxResizeHandle.BOTTOM_RIGHT

    private val OcrBoxResizeHandle.movesTop: Boolean
        get() = this == OcrBoxResizeHandle.TOP ||
            this == OcrBoxResizeHandle.TOP_LEFT ||
            this == OcrBoxResizeHandle.TOP_RIGHT

    private val OcrBoxResizeHandle.movesBottom: Boolean
        get() = this == OcrBoxResizeHandle.BOTTOM ||
            this == OcrBoxResizeHandle.BOTTOM_LEFT ||
            this == OcrBoxResizeHandle.BOTTOM_RIGHT
}
