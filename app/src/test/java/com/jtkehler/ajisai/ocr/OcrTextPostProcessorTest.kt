package com.jtkehler.ajisai.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextPostProcessorTest {
    private val processor = JapaneseOcrTextPostProcessor()

    @Test
    fun normalizesWhitespaceLineEndingsAndBlankLines() {
        val result = processor.process(
            listOf(
                OcrTextLine("  日本\t語\r\n"),
                OcrTextLine("\u3000"),
                OcrTextLine("次\u00a0の 行 "),
            ),
        )

        assertEquals(listOf("日本 語", "次 の 行"), result.map(OcrTextLine::text))
    }

    @Test
    fun removesSmallAlignedKanaAboveKanjiLine() {
        val result = processor.process(
            listOf(
                line("にほんご", box(0.2f, 0.20f, 0.6f, 0.25f)),
                line("日本語", box(0.15f, 0.27f, 0.65f, 0.39f)),
            ),
        )

        assertEquals(listOf("日本語"), result.map(OcrTextLine::text))
    }

    @Test
    fun keepsKanaDialogueWithoutAlignedKanjiBase() {
        val result = processor.process(
            listOf(
                line("ありがとう", box(0.1f, 0.2f, 0.8f, 0.3f)),
                line("そうだね", box(0.1f, 0.34f, 0.8f, 0.44f)),
            ),
        )

        assertEquals(listOf("ありがとう", "そうだね"), result.map(OcrTextLine::text))
    }

    @Test
    fun neverRemovesKanjiLinesOrLinesWithoutGeometry() {
        val result = processor.process(
            listOf(
                line("日本", box(0.2f, 0.20f, 0.6f, 0.25f)),
                line("日本語", box(0.15f, 0.27f, 0.65f, 0.39f)),
                OcrTextLine("にほんご"),
                OcrTextLine("日本語"),
            ),
        )

        assertEquals(listOf("日本", "日本語", "にほんご", "日本語"), result.map(OcrTextLine::text))
    }

    private fun line(text: String, box: OcrBoundingBox) = OcrTextLine(
        text = text,
        boundingBox = box,
        writingDirection = OcrWritingDirection.HORIZONTAL,
    )

    private fun box(left: Float, top: Float, right: Float, bottom: Float) =
        OcrBoundingBox(left, top, right, bottom)
}
