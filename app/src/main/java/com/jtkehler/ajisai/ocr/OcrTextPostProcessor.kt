package com.jtkehler.ajisai.ocr

interface OcrTextPostProcessor {
    fun process(lines: List<OcrTextLine>): List<OcrTextLine>
}

class JapaneseOcrTextPostProcessor(
    private val furiganaFilter: FuriganaFilter = FuriganaFilter(),
) : OcrTextPostProcessor {
    override fun process(lines: List<OcrTextLine>): List<OcrTextLine> {
        val cleaned = lines.flatMap { line ->
            val textLines = line.text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
            textLines.map { text ->
                line.copy(
                    text = text
                        .replace('\u0000', ' ')
                        .replace('\u00a0', ' ')
                        .replace('\u3000', ' ')
                        .replace(HORIZONTAL_WHITESPACE, " ")
                        .trim(),
                    boundingBox = line.boundingBox.takeIf { textLines.size == 1 },
                    writingDirection = line.writingDirection.takeIf { textLines.size == 1 }
                        ?: OcrWritingDirection.UNKNOWN,
                )
            }
        }
            .filter { it.text.isNotBlank() }
        return furiganaFilter.filter(cleaned)
    }

    private companion object {
        val HORIZONTAL_WHITESPACE = Regex("[\\t\\x0B\\f ]+")
    }
}

class FuriganaFilter(
    private val sizeThreshold: Float = 0.58f,
) {
    fun filter(lines: List<OcrTextLine>): List<OcrTextLine> {
        val referenceSize = referenceSize(lines) ?: return lines
        return lines.filter { line ->
            !isFurigana(line, referenceSize)
        }
    }

    private fun isFurigana(line: OcrTextLine, referenceSize: Float): Boolean {
        if (line.text.containsKanji() || !line.text.containsKana()) return false
        val fontSize = line.estimatedFontSize() ?: return false
        return fontSize < referenceSize * sizeThreshold
    }

    private fun referenceSize(lines: List<OcrTextLine>): Float? {
        val japaneseLines = lines.mapNotNull { line ->
            if (!line.text.containsJapanese()) return@mapNotNull null
            line.estimatedFontSize()?.let { size -> line to size }
        }
        val preferred = japaneseLines.filter { (line, _) -> line.text.containsKanji() }
        return (preferred.ifEmpty { japaneseLines })
            .map { (_, size) -> size }
            .medianOrNull()
    }

    private fun OcrTextLine.estimatedFontSize(): Float? {
        val box = boundingBox ?: return null
        val size = when (writingDirection) {
            OcrWritingDirection.HORIZONTAL -> box.height
            OcrWritingDirection.VERTICAL -> box.width
            OcrWritingDirection.UNKNOWN -> minOf(box.width, box.height)
        }
        return size.takeIf { it.isFinite() && it > 0f }
    }

    private fun List<Float>.medianOrNull(): Float? {
        if (isEmpty()) return null
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2f
        }
    }

    private fun String.containsKanji(): Boolean = any { character ->
        character in '\u3400'..'\u4dbf' ||
            character in '\u4e00'..'\u9fff' ||
            character in '\uf900'..'\ufaff'
    }

    private fun String.containsKana(): Boolean = any { character ->
        character in '\u3040'..'\u309f' ||
            character in '\u30a0'..'\u30ff' ||
            character in '\uff66'..'\uff9f'
    }

    private fun String.containsJapanese(): Boolean = containsKana() || containsKanji()
}

class JapaneseOcrTextAssembler {
    fun assemble(lines: List<OcrTextLine>): String = buildString {
        lines.forEachIndexed { index, line ->
            if (index > 0 && !lines[index - 1].isVerticalWith(line)) append('\n')
            append(line.text)
        }
    }

    private fun OcrTextLine.isVerticalWith(other: OcrTextLine): Boolean =
        writingDirection == OcrWritingDirection.VERTICAL &&
            other.writingDirection == OcrWritingDirection.VERTICAL
}
