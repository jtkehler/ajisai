package com.jtkehler.ajisai.dictionary

/** Minimal source description; Android document access is added with real import. */
data class DictionaryImportRequest(
    val displayName: String,
)

sealed interface DictionaryImportResult {
    data class Success(val dictionary: ImportedDictionary) : DictionaryImportResult

    data class Failure(val message: String) : DictionaryImportResult

    data object NotImplemented : DictionaryImportResult
}

/** Boundary for the future hoshidicts-backed Yomitan zip importer. */
interface DictionaryImportService {
    suspend fun importDictionary(request: DictionaryImportRequest): DictionaryImportResult
}

/** Configurable fake that deliberately performs no file or hoshidicts work. */
class FakeDictionaryImportService(
    private val result: DictionaryImportResult = DictionaryImportResult.NotImplemented,
) : DictionaryImportService {
    override suspend fun importDictionary(request: DictionaryImportRequest): DictionaryImportResult = result
}
