package com.jtkehler.ajisai.ocrbox

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.jtkehler.ajisai.capture.CapturedFrame

interface OcrFrameCropper {
    fun crop(frame: CapturedFrame, box: OcrBoxProfile): Bitmap?
}

class BitmapOcrFrameCropper : OcrFrameCropper {
    override fun crop(frame: CapturedFrame, box: OcrBoxProfile): Bitmap? {
        val source = frame.bitmap
        if (!box.enabled || source.isRecycled || source.width <= 0 || source.height <= 0) return null
        val cropRect = OcrBoxGeometry.toPixelRect(
            rect = box.normalizedRect,
            width = source.width,
            height = source.height,
        )
        if (cropRect.width <= 0 || cropRect.height <= 0) return null

        var output: Bitmap? = null
        return runCatching {
            val config = source.config
                ?.takeUnless { it == Bitmap.Config.HARDWARE }
                ?: Bitmap.Config.ARGB_8888
            Bitmap.createBitmap(cropRect.width, cropRect.height, config).also { bitmap ->
                output = bitmap
                Canvas(bitmap).drawBitmap(
                    source,
                    Rect(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom),
                    Rect(0, 0, cropRect.width, cropRect.height),
                    Paint(Paint.FILTER_BITMAP_FLAG),
                )
            }
        }.onFailure {
            output?.recycle()
        }.getOrNull()
    }
}
