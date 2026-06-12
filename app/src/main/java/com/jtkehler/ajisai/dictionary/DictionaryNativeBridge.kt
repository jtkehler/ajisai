package com.jtkehler.ajisai.dictionary

import de.manhhao.hoshi.HoshiDicts

data class NativeDictionaryImportResult(
    val success: Boolean,
    val title: String,
    val termCount: Long,
    val frequencyCount: Long,
    val pitchCount: Long,
    val mediaCount: Long,
)

interface DictionaryNativeBridge {
    fun importDictionary(zipPath: String, outputDir: String, lowRam: Boolean): NativeDictionaryImportResult

    fun rebuildQuery(termPaths: Array<String>, frequencyPaths: Array<String>, pitchPaths: Array<String>)

    fun lookup(text: String, maxResults: Int, scanLength: Int): List<DictionaryEntry>
}

object HoshiDictionaryNativeBridge : DictionaryNativeBridge {
    private val lock = Any()

    override fun importDictionary(zipPath: String, outputDir: String, lowRam: Boolean): NativeDictionaryImportResult =
        synchronized(lock) {
            HoshiDicts.importDictionary(zipPath, outputDir, lowRam).let { result ->
                NativeDictionaryImportResult(
                    success = result.success,
                    title = result.title,
                    termCount = result.termCount,
                    frequencyCount = result.freqCount,
                    pitchCount = result.pitchCount,
                    mediaCount = result.mediaCount,
                )
            }
        }

    override fun rebuildQuery(
        termPaths: Array<String>,
        frequencyPaths: Array<String>,
        pitchPaths: Array<String>,
    ) = synchronized(lock) {
        HoshiDicts.rebuildQuery(HoshiDicts.lookupObject, termPaths, frequencyPaths, pitchPaths)
    }

    override fun lookup(text: String, maxResults: Int, scanLength: Int): List<DictionaryEntry> = synchronized(lock) {
        HoshiDicts.lookup(HoshiDicts.lookupObject, text, maxResults, scanLength).map { result ->
            DictionaryEntry(
                expression = result.term.expression,
                reading = result.term.reading,
                definitions = result.term.glossaries.map { it.glossary },
            )
        }
    }
}
