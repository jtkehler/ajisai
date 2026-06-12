package com.jtkehler.ajisai.dictionary

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DictionaryArchiveImporterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun importPersistsDetectedTypesAndCommitsDictionaryData() {
        val metadata = InMemoryImportedDictionaryRepository()
        val storage = DictionaryStorage(temporaryFolder.root)
        val bridge = RecordingNativeBridge(
            importResult = NativeDictionaryImportResult(
                success = true,
                title = "Test Dictionary",
                termCount = 12,
                frequencyCount = 4,
                pitchCount = 2,
                mediaCount = 1,
            ),
        )
        val queryRepository = HoshiDictionaryRepository(metadata, storage, bridge)
        var id = 0
        val importer = DictionaryArchiveImporter(
            storage = storage,
            metadataRepository = metadata,
            queryRepository = queryRepository,
            nativeBridge = bridge,
            indexReader = DictionaryIndexReader { DictionaryArchiveIndex("Test Dictionary", "2026.06") },
            nowMs = { 42L },
            newId = { "id-${id++}" },
        )
        val stages = mutableListOf<DictionaryImportStage>()

        val imported = importer.import(ByteArrayInputStream("fixture".toByteArray()), stages::add)

        assertEquals(listOf(DictionaryType.TERM, DictionaryType.FREQUENCY, DictionaryType.PITCH), imported.map { it.type })
        assertEquals(listOf(DictionaryImportStage.PREPARING, DictionaryImportStage.IMPORTING, DictionaryImportStage.SAVING), stages)
        assertEquals(imported, metadata.getAll())
        imported.forEach { dictionary ->
            assertTrue(File(storage.dictionaryDirectory(dictionary), "dictionary-data").isFile)
            assertEquals("2026.06", dictionary.revision)
            assertEquals(42L, dictionary.importedAtMs)
        }
        assertEquals(1, bridge.rebuildCalls.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun importRejectsArchivesWithoutSupportedDictionaryData() {
        val metadata = InMemoryImportedDictionaryRepository()
        val storage = DictionaryStorage(temporaryFolder.root)
        val bridge = RecordingNativeBridge(
            NativeDictionaryImportResult(true, "Unsupported", 0, 0, 0, 0),
        )
        val importer = DictionaryArchiveImporter(
            storage = storage,
            metadataRepository = metadata,
            queryRepository = HoshiDictionaryRepository(metadata, storage, bridge),
            nativeBridge = bridge,
            indexReader = DictionaryIndexReader { DictionaryArchiveIndex("Unsupported", null) },
        )

        importer.import(ByteArrayInputStream("fixture".toByteArray()))
    }

    private class RecordingNativeBridge(
        private val importResult: NativeDictionaryImportResult,
    ) : DictionaryNativeBridge {
        val rebuildCalls = mutableListOf<DictionaryQueryPaths>()

        override fun importDictionary(zipPath: String, outputDir: String, lowRam: Boolean): NativeDictionaryImportResult {
            File(outputDir, importResult.title).apply {
                mkdirs()
                File(this, "dictionary-data").writeText("fixture")
            }
            return importResult
        }

        override fun rebuildQuery(
            termPaths: Array<String>,
            frequencyPaths: Array<String>,
            pitchPaths: Array<String>,
        ) {
            rebuildCalls += DictionaryQueryPaths(termPaths, frequencyPaths, pitchPaths)
        }

        override fun lookup(text: String, maxResults: Int, scanLength: Int): List<DictionaryEntry> = emptyList()
    }
}
