package com.jtkehler.ajisai.ocr

import java.io.ByteArrayOutputStream
import kotlin.random.Random

class LensRequestBuilder(
    private val config: LensConfig = LensConfig(),
    private val requestId: () -> Long = { Random.nextLong().ushr(1) },
) {
    fun build(image: ProcessedOcrImage): LensRequest {
        val requestContext = message(
            fieldMessage(3, message(
                fieldVarint(1, requestId()),
                fieldVarint(2, 1),
                fieldVarint(3, 1),
            )),
            fieldMessage(4, message(
                fieldVarint(1, PLATFORM_WEB),
                fieldVarint(2, SURFACE_CHROMIUM),
                fieldMessage(4, message(
                    fieldString(1, config.language),
                    fieldString(2, config.region),
                    fieldString(3, config.timeZone),
                )),
            )),
        )
        val imageData = message(
            fieldMessage(1, message(fieldBytes(1, image.bytes))),
            fieldMessage(3, message(
                fieldVarint(1, image.width.toLong()),
                fieldVarint(2, image.height.toLong()),
            )),
        )
        val body = message(
            fieldMessage(1, message(
                fieldMessage(1, requestContext),
                fieldMessage(3, imageData),
            )),
        )
        return LensRequest(
            endpoint = config.endpoint,
            headers = mapOf(
                "Content-Type" to "application/x-protobuf",
                "X-Goog-Api-Key" to config.apiKey,
                "User-Agent" to config.userAgent,
            ),
            body = body,
        )
    }

    private fun message(vararg fields: ByteArray): ByteArray =
        ByteArrayOutputStream().apply { fields.forEach(::write) }.toByteArray()

    private fun fieldString(number: Int, value: String) = fieldBytes(number, value.encodeToByteArray())

    private fun fieldMessage(number: Int, value: ByteArray) = fieldBytes(number, value)

    private fun fieldBytes(number: Int, value: ByteArray) = message(
        varint(((number shl 3) or WIRE_LENGTH_DELIMITED).toLong()),
        varint(value.size.toLong()),
        value,
    )

    private fun fieldVarint(number: Int, value: Long) = message(
        varint(((number shl 3) or WIRE_VARINT).toLong()),
        varint(value),
    )

    private fun varint(value: Long): ByteArray {
        var remaining = value
        return ByteArrayOutputStream().apply {
            while (remaining and -128L != 0L) {
                write(((remaining and 0x7f) or 0x80).toInt())
                remaining = remaining ushr 7
            }
            write(remaining.toInt())
        }.toByteArray()
    }

    private companion object {
        const val WIRE_VARINT = 0
        const val WIRE_LENGTH_DELIMITED = 2
        const val PLATFORM_WEB = 3L
        const val SURFACE_CHROMIUM = 4L
    }
}
