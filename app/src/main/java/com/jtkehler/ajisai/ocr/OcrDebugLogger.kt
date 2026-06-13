package com.jtkehler.ajisai.ocr

import android.content.Context
import java.io.File
import java.util.concurrent.atomic.AtomicLong

interface OcrDebugLogger {
    fun saveCrop(image: ProcessedOcrImage): String?
    fun saveRawResponse(bytes: ByteArray): String?
}

object NoOpOcrDebugLogger : OcrDebugLogger {
    override fun saveCrop(image: ProcessedOcrImage): String? = null
    override fun saveRawResponse(bytes: ByteArray): String? = null
}

class FileOcrDebugLogger(context: Context) : OcrDebugLogger {
    private val directory = File(context.filesDir, "ocr-debug")

    override fun saveCrop(image: ProcessedOcrImage): String? =
        save("crop", "png", image.bytes)

    override fun saveRawResponse(bytes: ByteArray): String? =
        save("response", "bin", bytes)

    private fun save(prefix: String, extension: String, bytes: ByteArray): String? = runCatching {
        directory.mkdirs()
        File(directory, "${prefix}_${System.currentTimeMillis()}_${sequence.incrementAndGet()}.$extension")
            .apply { writeBytes(bytes) }
            .absolutePath
    }.getOrNull()

    private companion object {
        val sequence = AtomicLong()
    }
}
