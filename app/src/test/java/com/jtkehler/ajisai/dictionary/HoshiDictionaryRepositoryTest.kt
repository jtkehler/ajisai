package com.jtkehler.ajisai.dictionary

import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HoshiDictionaryRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun rebuildLoadsOnlyEnabledDictionariesInPriorityOrder() {
        val metadata = InMemoryImportedDictionaryRepository(
            listOf(
                dictionary("term-low", DictionaryType.TERM, priority = 2),
                dictionary("term-disabled", DictionaryType.TERM, priority = 0, enabled = false),
                dictionary("term-high", DictionaryType.TERM, priority = 1),
                dictionary("frequency", DictionaryType.FREQUENCY, priority = 0),
                dictionary("pitch", DictionaryType.PITCH, priority = 0),
            ),
        )
        val storage = DictionaryStorage(temporaryFolder.root)
        val bridge = RecordingBridge()
        val repository = HoshiDictionaryRepository(metadata, storage, bridge)

        repository.rebuildQuery()

        assertArrayEquals(
            arrayOf(
                storage.dictionaryDirectory(metadata.find("term-high")!!).absolutePath,
                storage.dictionaryDirectory(metadata.find("term-low")!!).absolutePath,
            ),
            bridge.termPaths,
        )
        assertArrayEquals(
            arrayOf(storage.dictionaryDirectory(metadata.find("frequency")!!).absolutePath),
            bridge.frequencyPaths,
        )
        assertArrayEquals(
            arrayOf(storage.dictionaryDirectory(metadata.find("pitch")!!).absolutePath),
            bridge.pitchPaths,
        )
    }

    private fun dictionary(
        id: String,
        type: DictionaryType,
        priority: Int,
        enabled: Boolean = true,
    ) = ImportedDictionary(
        id = id,
        title = id,
        type = type,
        enabled = enabled,
        priority = priority,
        importedAtMs = 0L,
        directoryName = id,
    )

    private class RecordingBridge : DictionaryNativeBridge {
        var termPaths: Array<String> = emptyArray()
        var frequencyPaths: Array<String> = emptyArray()
        var pitchPaths: Array<String> = emptyArray()

        override fun importDictionary(zipPath: String, outputDir: String, lowRam: Boolean) =
            error("Import is not used by this test.")

        override fun rebuildQuery(
            termPaths: Array<String>,
            frequencyPaths: Array<String>,
            pitchPaths: Array<String>,
        ) {
            this.termPaths = termPaths
            this.frequencyPaths = frequencyPaths
            this.pitchPaths = pitchPaths
        }

        override fun lookup(text: String, maxResults: Int, scanLength: Int): List<DictionaryEntry> = emptyList()
    }
}
