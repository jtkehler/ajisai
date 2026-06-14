package com.jtkehler.ajisai.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class JapaneseOcrTextAssemblerTest {
    private val assembler = JapaneseOcrTextAssembler()

    @Test
    fun joinsVerticalLinesWithoutNewlines() {
        val lines = listOf(
            OcrTextLine("わたしの知らない", writingDirection = OcrWritingDirection.VERTICAL),
            OcrTextLine("一週間が", writingDirection = OcrWritingDirection.VERTICAL),
            OcrTextLine("教室にはあって", writingDirection = OcrWritingDirection.VERTICAL),
        )

        assertEquals(
            "わたしの知らない一週間が教室にはあって",
            assembler.assemble(lines),
        )
    }

    @Test
    fun keepsHorizontalLinesNewlineSeparated() {
        val lines = listOf(
            OcrTextLine("一行目", writingDirection = OcrWritingDirection.HORIZONTAL),
            OcrTextLine("二行目", writingDirection = OcrWritingDirection.HORIZONTAL),
        )

        assertEquals("一行目\n二行目", assembler.assemble(lines))
    }
}
