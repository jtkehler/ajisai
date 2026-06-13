package com.jtkehler.ajisai.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextPostProcessorTest {
    private val processor = JapaneseOcrTextPostProcessor()

    @Test
    fun normalizesWhitespaceLineEndingsAndBlankLines() {
        val result = processor.process(
            listOf("  日本\t語\r\n", "\u3000", "次\u00a0の 行 "),
        )

        assertEquals(listOf("日本 語", "次 の 行"), result)
    }
}
