package com.jtkehler.ajisai.ocr

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LensProtocolTest {
    @Test
    fun requestBuilderEncodesChromiumContextAndImage() {
        val image = ProcessedOcrImage(byteArrayOf(1, 2, 3), width = 640, height = 360)
        val request = LensRequestBuilder(
            config = LensConfig(
                endpoint = "https://example.test/lens",
                apiKey = "test-key",
                userAgent = "test-agent",
                language = "ja",
                region = "JP",
                timeZone = "Asia/Tokyo",
            ),
            requestId = { 42L },
        ).build(image)

        assertEquals("https://example.test/lens", request.endpoint)
        assertEquals("test-key", request.headers["X-Goog-Api-Key"])
        val objects = ProtoReader(request.body).messages(1).single()
        val objectsReader = ProtoReader(objects)
        val context = ProtoReader(objectsReader.messages(1).single())
        val requestId = ProtoReader(context.messages(3).single())
        assertEquals(listOf(42L), requestId.varints(1))
        val client = ProtoReader(context.messages(4).single())
        assertEquals(listOf(3L), client.varints(1))
        assertEquals(listOf(4L), client.varints(2))
        val locale = ProtoReader(client.messages(4).single())
        assertEquals(listOf("ja"), locale.strings(1))
        assertEquals(listOf("JP"), locale.strings(2))
        assertEquals(listOf("Asia/Tokyo"), locale.strings(3))

        val imageData = ProtoReader(objectsReader.messages(3).single())
        val payload = ProtoReader(imageData.messages(1).single())
        assertArrayEquals(image.bytes, payload.messages(1).single())
        val metadata = ProtoReader(imageData.messages(3).single())
        assertEquals(listOf(640L), metadata.varints(1))
        assertEquals(listOf(360L), metadata.varints(2))
    }

    @Test
    fun responseParserReadsParagraphLinesAndSeparators() {
        val firstLine = line(word("日本", ""), word("語", " "))
        val secondLine = line(word("です", ""))
        val response = fieldMessage(2, fieldMessage(3, message(
            fieldMessage(1, fieldMessage(1, message(
                fieldMessage(2, firstLine),
                fieldMessage(2, secondLine),
            ))),
            fieldString(2, "ja"),
        )))

        val parsed = LensResponseParser().parse(response)

        assertEquals(listOf("日本語", "です"), parsed.lines.map(OcrTextLine::text))
        assertEquals("ja", parsed.contentLanguage)
    }

    @Test
    fun responseParserPreservesNormalizedLineGeometry() {
        val response = fieldMessage(2, fieldMessage(3, message(
            fieldMessage(1, fieldMessage(1, message(
                fieldMessage(2, lineWithGeometry(
                    geometry(centerX = 0.5f, centerY = 0.4f, width = 0.6f, height = 0.1f),
                    word("日本語", ""),
                )),
            ))),
        )))

        val parsedLine = LensResponseParser().parse(response).lines.single()

        assertEquals("日本語", parsedLine.text)
        assertEquals(OcrWritingDirection.HORIZONTAL, parsedLine.writingDirection)
        val box = requireNotNull(parsedLine.boundingBox)
        assertEquals(0.2f, box.left, 0.0001f)
        assertEquals(0.35f, box.top, 0.0001f)
        assertEquals(0.8f, box.right, 0.0001f)
        assertEquals(0.45f, box.bottom, 0.0001f)
    }

    @Test
    fun responseParserReturnsNoLinesWhenTextPayloadIsMissing() {
        assertEquals(emptyList<String>(), LensResponseParser().parse(byteArrayOf()).lines)
    }

    @Test
    fun responseParserMapsMalformedPayloadToParseError() {
        val error = assertThrows(OcrException::class.java) {
            LensResponseParser().parse(byteArrayOf(0x12, 0x7f))
        }

        assertEquals(OcrErrorType.PARSE, error.type)
    }

    private fun line(vararg words: ByteArray): ByteArray =
        message(*words.map { fieldMessage(1, it) }.toTypedArray())

    private fun lineWithGeometry(geometry: ByteArray, vararg words: ByteArray): ByteArray = message(
        *words.map { fieldMessage(1, it) }.toTypedArray(),
        fieldMessage(2, geometry),
    )

    private fun geometry(centerX: Float, centerY: Float, width: Float, height: Float): ByteArray =
        message(fieldMessage(1, message(
            fieldFloat(1, centerX),
            fieldFloat(2, centerY),
            fieldFloat(3, width),
            fieldFloat(4, height),
        )))

    private fun word(text: String, separator: String): ByteArray = message(
        fieldString(2, text),
        fieldString(3, separator),
    )

    private fun fieldString(number: Int, value: String) =
        fieldMessage(number, value.encodeToByteArray())

    private fun fieldMessage(number: Int, value: ByteArray): ByteArray = message(
        varint(((number shl 3) or 2).toLong()),
        varint(value.size.toLong()),
        value,
    )

    private fun fieldFloat(number: Int, value: Float): ByteArray = message(
        varint(((number shl 3) or 5).toLong()),
        byteArrayOf(
            value.toBits().toByte(),
            (value.toBits() ushr 8).toByte(),
            (value.toBits() ushr 16).toByte(),
            (value.toBits() ushr 24).toByte(),
        ),
    )

    private fun message(vararg fields: ByteArray): ByteArray =
        ByteArrayOutputStream().apply { fields.forEach { write(it) } }.toByteArray()

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
}
