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
    private val maximumHeightRatio: Float = 0.75f,
    private val minimumHorizontalOverlap: Float = 0.5f,
) {
    fun filter(lines: List<OcrTextLine>): List<OcrTextLine> = lines.filterIndexed { index, candidate ->
        val base = lines.getOrNull(index + 1)
        !isFurigana(candidate, base)
    }

    private fun isFurigana(candidate: OcrTextLine, base: OcrTextLine?): Boolean {
        if (base == null || candidate.text.containsKanji() || !candidate.text.containsKana()) return false
        if (!base.text.containsKanji()) return false
        if (candidate.writingDirection != OcrWritingDirection.HORIZONTAL ||
            base.writingDirection != OcrWritingDirection.HORIZONTAL
        ) {
            return false
        }
        val candidateBox = candidate.boundingBox ?: return false
        val baseBox = base.boundingBox ?: return false
        if (candidateBox.height <= 0f || baseBox.height <= 0f ||
            candidateBox.width <= 0f || baseBox.width <= 0f
        ) {
            return false
        }
        if (candidateBox.height >= baseBox.height * maximumHeightRatio) return false

        val verticalDistance = baseBox.centerY - candidateBox.centerY
        val minimumDistance = kotlin.math.abs(baseBox.height - candidateBox.height) / 2f
        val maximumDistance = baseBox.height + candidateBox.height / 2f
        if (verticalDistance <= minimumDistance || verticalDistance >= maximumDistance) return false

        val overlap = (minOf(candidateBox.right, baseBox.right) -
            maxOf(candidateBox.left, baseBox.left)).coerceAtLeast(0f)
        val overlapRatio = overlap / minOf(candidateBox.width, baseBox.width)
        return overlapRatio >= minimumHorizontalOverlap
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
}
