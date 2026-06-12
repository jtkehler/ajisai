package com.jtkehler.ajisai.dictionary

/** Persistable Android document reference selected by the user. */
data class DictionaryImportRequest(
    val sourceUri: String,
    val displayName: String,
)

enum class DictionaryImportStage {
    PREPARING,
    IMPORTING,
    SAVING,
}

sealed interface DictionaryImportResult {
    data class Success(val dictionaries: List<ImportedDictionary>) : DictionaryImportResult

    data class Failure(val message: String) : DictionaryImportResult

}

/** Boundary for importing Yomitan archives without exposing hoshidicts to the UI. */
interface DictionaryImportService {
    suspend fun importDictionary(
        request: DictionaryImportRequest,
        onProgress: (DictionaryImportStage) -> Unit = {},
    ): DictionaryImportResult
}

/** Configurable fake for unit and UI tests. */
class FakeDictionaryImportService(
    private val result: DictionaryImportResult,
) : DictionaryImportService {
    override suspend fun importDictionary(
        request: DictionaryImportRequest,
        onProgress: (DictionaryImportStage) -> Unit,
    ): DictionaryImportResult = result
}
