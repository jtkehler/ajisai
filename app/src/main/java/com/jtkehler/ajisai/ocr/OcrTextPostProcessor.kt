package com.jtkehler.ajisai.ocr

interface OcrTextPostProcessor {
    fun process(lines: List<String>): List<String>
}

class JapaneseOcrTextPostProcessor : OcrTextPostProcessor {
    override fun process(lines: List<String>): List<String> = lines
        .flatMap { it.replace("\r\n", "\n").replace('\r', '\n').split('\n') }
        .map { line ->
            line
                .replace('\u0000', ' ')
                .replace('\u00a0', ' ')
                .replace('\u3000', ' ')
                .replace(HORIZONTAL_WHITESPACE, " ")
                .trim()
        }
        .filter(String::isNotBlank)

    private companion object {
        val HORIZONTAL_WHITESPACE = Regex("[\\t\\x0B\\f ]+")
    }
}
