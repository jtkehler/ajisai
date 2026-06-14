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
    fun removesSmallKanaOnlyLineWhenMuchSmallerThanMainText() {
        val result = processor.process(
            listOf(
                line("にほんご", box(0.2f, 0.20f, 0.6f, 0.25f)),
                line("ありがとう", box(0.1f, 0.45f, 0.8f, 0.56f)),
                line("日本語", box(0.15f, 0.70f, 0.65f, 0.82f)),
            ),
        )

        assertEquals(listOf("ありがとう", "日本語"), result.map(OcrTextLine::text))
    }

    @Test
    fun keepsKanaOnlyLineWhenSimilarSizeToMainText() {
        val result = processor.process(
            listOf(
                line("ありがとう", box(0.1f, 0.2f, 0.8f, 0.30f)),
                line("日本語", box(0.1f, 0.34f, 0.8f, 0.46f)),
            ),
        )

        assertEquals(listOf("ありがとう", "日本語"), result.map(OcrTextLine::text))
    }

    @Test
    fun neverRemovesKanjiContainingLine() {
        val result = processor.process(
            listOf(
                line("日本", box(0.2f, 0.20f, 0.6f, 0.23f)),
                line("日本語", box(0.15f, 0.27f, 0.65f, 0.39f)),
            ),
        )

        assertEquals(listOf("日本", "日本語"), result.map(OcrTextLine::text))
    }

    @Test
    fun neverRemovesGeometryLessLine() {
        val result = processor.process(
            listOf(
                OcrTextLine("にほんご"),
                line("日本語", box(0.15f, 0.27f, 0.65f, 0.39f)),
            ),
        )

        assertEquals(listOf("にほんご", "日本語"), result.map(OcrTextLine::text))
    }

    @Test
    fun verticalFontSizeUsesBoundingBoxWidth() {
        val result = processor.process(
            listOf(
                line(
                    "しゅうかん",
                    box(0.2f, 0.1f, 0.23f, 0.6f),
                    OcrWritingDirection.VERTICAL,
                ),
                line(
                    "一週間が",
                    box(0.4f, 0.1f, 0.49f, 0.6f),
                    OcrWritingDirection.VERTICAL,
                ),
            ),
        )

        assertEquals(listOf("一週間が"), result.map(OcrTextLine::text))
    }

    private fun line(
        text: String,
        box: OcrBoundingBox,
        direction: OcrWritingDirection = OcrWritingDirection.HORIZONTAL,
    ) = OcrTextLine(
        text = text,
        boundingBox = box,
        writingDirection = direction,
    )

    private fun box(left: Float, top: Float, right: Float, bottom: Float) =
        OcrBoundingBox(left, top, right, bottom)
}
