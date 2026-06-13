package com.jtkehler.ajisai.ocrbox

data class OcrBoxProfile(
    val id: String,
    val name: String,
    val normalizedRect: NormalizedRect,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val lastUsedAtMs: Long? = null,
)

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class PixelRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top
}

enum class OcrBoxResizeHandle {
    TOP_LEFT,
    TOP,
    TOP_RIGHT,
    RIGHT,
    BOTTOM_RIGHT,
    BOTTOM,
    BOTTOM_LEFT,
    LEFT,
}

object OcrBoxDefaults {
    const val DEFAULT_ID = "default"
    const val MIN_WIDTH = 0.08f
    const val MIN_HEIGHT = 0.06f

    val defaultRect = NormalizedRect(
        left = 0.08f,
        top = 0.62f,
        right = 0.92f,
        bottom = 0.90f,
    )

    fun defaultProfile() = OcrBoxProfile(
        id = DEFAULT_ID,
        name = "Default OCR box",
        normalizedRect = defaultRect,
    )
}
