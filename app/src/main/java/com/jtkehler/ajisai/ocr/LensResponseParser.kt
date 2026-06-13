package com.jtkehler.ajisai.ocr

data class ParsedLensResponse(
    val lines: List<String>,
    val contentLanguage: String? = null,
)

class LensResponseParser {
    fun parse(bytes: ByteArray): ParsedLensResponse {
        try {
            val objects = ProtoReader(bytes).messages(2).firstOrNull()
                ?: return ParsedLensResponse(emptyList())
            val text = ProtoReader(objects).messages(3).firstOrNull()
                ?: return ParsedLensResponse(emptyList())
            val textReader = ProtoReader(text)
            val layout = textReader.messages(1).firstOrNull()
                ?: return ParsedLensResponse(emptyList(), textReader.strings(2).firstOrNull())
            val lines = ProtoReader(layout).messages(1).flatMap(::parseParagraph)
            return ParsedLensResponse(lines, textReader.strings(2).firstOrNull())
        } catch (error: Throwable) {
            if (error is OcrException) throw error
            throw OcrException(OcrErrorType.PARSE, "Google Lens returned an invalid response.", error)
        }
    }

    private fun parseParagraph(bytes: ByteArray): List<String> =
        ProtoReader(bytes).messages(2).mapNotNull(::parseLine)

    private fun parseLine(bytes: ByteArray): String? {
        val words = ProtoReader(bytes).messages(1).map { wordBytes ->
            val word = ProtoReader(wordBytes)
            val text = word.strings(2).firstOrNull().orEmpty()
            val separator = word.strings(3).firstOrNull().orEmpty()
            text + separator
        }
        return words.joinToString("").trim().takeIf(String::isNotBlank)
    }
}

internal class ProtoReader(private val bytes: ByteArray) {
    fun messages(fieldNumber: Int): List<ByteArray> = lengthDelimited(fieldNumber)

    fun strings(fieldNumber: Int): List<String> =
        lengthDelimited(fieldNumber).map(ByteArray::decodeToString)

    fun varints(targetField: Int): List<Long> {
        val values = mutableListOf<Long>()
        var offset = 0
        while (offset < bytes.size) {
            val tag = readVarint(offset)
            offset = tag.nextOffset
            val fieldNumber = (tag.value ushr 3).toInt()
            when ((tag.value and 0x7).toInt()) {
                WIRE_VARINT -> {
                    val value = readVarint(offset)
                    if (fieldNumber == targetField) values += value.value
                    offset = value.nextOffset
                }
                WIRE_FIXED_64 -> offset = checkedOffset(offset, 8)
                WIRE_LENGTH_DELIMITED -> {
                    val length = readVarint(offset)
                    offset = checkedOffset(length.nextOffset, length.value.toInt())
                }
                WIRE_FIXED_32 -> offset = checkedOffset(offset, 4)
                else -> throw IllegalArgumentException("Unsupported protobuf wire type")
            }
        }
        return values
    }

    private fun lengthDelimited(targetField: Int): List<ByteArray> {
        val values = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < bytes.size) {
            val tag = readVarint(offset)
            offset = tag.nextOffset
            val fieldNumber = (tag.value ushr 3).toInt()
            when ((tag.value and 0x7).toInt()) {
                WIRE_VARINT -> offset = readVarint(offset).nextOffset
                WIRE_FIXED_64 -> offset = checkedOffset(offset, 8)
                WIRE_LENGTH_DELIMITED -> {
                    val length = readVarint(offset)
                    offset = length.nextOffset
                    val end = checkedOffset(offset, length.value.toInt())
                    if (fieldNumber == targetField) values += bytes.copyOfRange(offset, end)
                    offset = end
                }
                WIRE_FIXED_32 -> offset = checkedOffset(offset, 4)
                else -> throw IllegalArgumentException("Unsupported protobuf wire type")
            }
        }
        return values
    }

    private fun readVarint(start: Int): Varint {
        var result = 0L
        var shift = 0
        var offset = start
        while (offset < bytes.size && shift < 64) {
            val value = bytes[offset++].toInt() and 0xff
            result = result or ((value and 0x7f).toLong() shl shift)
            if (value and 0x80 == 0) return Varint(result, offset)
            shift += 7
        }
        throw IllegalArgumentException("Invalid protobuf varint")
    }

    private fun checkedOffset(offset: Int, length: Int): Int {
        if (length < 0 || offset < 0 || offset + length > bytes.size) {
            throw IllegalArgumentException("Invalid protobuf length")
        }
        return offset + length
    }

    private data class Varint(val value: Long, val nextOffset: Int)

    private companion object {
        const val WIRE_VARINT = 0
        const val WIRE_FIXED_64 = 1
        const val WIRE_LENGTH_DELIMITED = 2
        const val WIRE_FIXED_32 = 5
    }
}
