package com.jtkehler.ajisai.dictionary

/** Provider-neutral lookup request originating from editable OCR text. */
data class LookupRequest(
    val text: String,
    val characterOffset: Int = 0,
)

/** Minimal lookup result that does not expose hoshidicts internals. */
data class DictionaryEntry(
    val expression: String,
    val reading: String? = null,
    val definitions: List<String> = emptyList(),
)

/** Boundary for dictionary lookup and future hoshidicts-backed storage. */
interface DictionaryRepository {
    suspend fun lookup(request: LookupRequest): List<DictionaryEntry>
}

/** In-memory lookup fake for tests and UI development before hoshidicts integration. */
class InMemoryDictionaryRepository(
    private val entriesByText: Map<String, List<DictionaryEntry>> = emptyMap(),
) : DictionaryRepository {
    override suspend fun lookup(request: LookupRequest): List<DictionaryEntry> =
        entriesByText[request.text].orEmpty()
}
