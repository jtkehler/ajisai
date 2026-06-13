package com.jtkehler.ajisai.overlay

import android.content.Context
import kotlin.math.roundToInt

data class BubblePosition(
    val x: Int,
    val y: Int,
)

object BubblePositionMath {
    fun restore(
        normalizedX: Float,
        normalizedY: Float,
        availableWidth: Int,
        availableHeight: Int,
    ) = BubblePosition(
        x = (normalizedX.coerceIn(0f, 1f) * availableWidth.coerceAtLeast(0)).roundToInt(),
        y = (normalizedY.coerceIn(0f, 1f) * availableHeight.coerceAtLeast(0)).roundToInt(),
    )

    fun normalize(position: Int, availableSize: Int): Float =
        if (availableSize <= 0) 0f else position.coerceIn(0, availableSize).toFloat() / availableSize
}

internal class BubblePositionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(availableWidth: Int, availableHeight: Int): BubblePosition = BubblePositionMath.restore(
        normalizedX = preferences.getFloat(KEY_X, DEFAULT_X),
        normalizedY = preferences.getFloat(KEY_Y, DEFAULT_Y),
        availableWidth = availableWidth,
        availableHeight = availableHeight,
    )

    fun save(position: BubblePosition, availableWidth: Int, availableHeight: Int) {
        preferences.edit()
            .putFloat(KEY_X, BubblePositionMath.normalize(position.x, availableWidth))
            .putFloat(KEY_Y, BubblePositionMath.normalize(position.y, availableHeight))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "overlay_bubble"
        const val KEY_X = "normalized_x"
        const val KEY_Y = "normalized_y"
        const val DEFAULT_X = 0.96f
        const val DEFAULT_Y = 0.35f
    }
}
