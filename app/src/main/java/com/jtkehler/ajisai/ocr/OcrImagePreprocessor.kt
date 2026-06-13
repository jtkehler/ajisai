package com.jtkehler.ajisai.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.sqrt

data class ProcessedOcrImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val mimeType: String = "image/png",
)

interface OcrImagePreprocessor {
    fun preprocess(image: Bitmap, region: Rect? = null): ProcessedOcrImage
}

class LensImagePreprocessor(
    private val maxDimension: Int = 1_500,
    private val maxTotalPixels: Int = 3_000_000,
) : OcrImagePreprocessor {
    override fun preprocess(image: Bitmap, region: Rect?): ProcessedOcrImage {
        if (image.isRecycled || image.width <= 0 || image.height <= 0) {
            throw OcrException(OcrErrorType.PREPROCESSING, "OCR image is unavailable.")
        }

        var cropped: Bitmap? = null
        var scaled: Bitmap? = null
        try {
            val source = if (region == null) {
                image
            } else {
                val safeRegion = Rect(region).apply {
                    intersect(0, 0, image.width, image.height)
                }
                if (safeRegion.width() <= 0 || safeRegion.height() <= 0) {
                    throw OcrException(OcrErrorType.PREPROCESSING, "OCR crop is empty.")
                }
                Bitmap.createBitmap(
                    image,
                    safeRegion.left,
                    safeRegion.top,
                    safeRegion.width(),
                    safeRegion.height(),
                ).also { cropped = it }
            }

            val scale = scaleFor(source.width, source.height)
            val prepared = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                    true,
                ).also { scaled = it }
            } else {
                source
            }

            val output = ByteArrayOutputStream()
            if (!prepared.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw OcrException(OcrErrorType.PREPROCESSING, "OCR image encoding failed.")
            }
            return ProcessedOcrImage(output.toByteArray(), prepared.width, prepared.height)
        } catch (error: OcrException) {
            throw error
        } catch (error: Throwable) {
            throw OcrException(
                OcrErrorType.PREPROCESSING,
                "OCR image preprocessing failed.",
                error,
            )
        } finally {
            scaled?.takeUnless { it.isRecycled }?.recycle()
            cropped?.takeUnless { it.isRecycled }?.recycle()
        }
    }

    private fun scaleFor(width: Int, height: Int): Float {
        val dimensionScale = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val pixelScale = sqrt(maxTotalPixels.toDouble() / (width.toDouble() * height)).toFloat()
        return min(1f, min(dimensionScale, pixelScale))
    }
}
