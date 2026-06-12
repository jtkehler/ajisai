package com.jtkehler.ajisai.dictionary

/** hoshidicts lookup adapter that only loads enabled dictionaries in priority order. */
class HoshiDictionaryRepository(
    private val metadataRepository: ImportedDictionaryRepository,
    private val storage: DictionaryStorage,
    private val nativeBridge: DictionaryNativeBridge = HoshiDictionaryNativeBridge,
) : DictionaryRepository {
    @Volatile
    private var queryFingerprint: String? = null

    override suspend fun lookup(request: LookupRequest): List<DictionaryEntry> {
        rebuildQueryIfNeeded()
        val text = request.text.drop(request.characterOffset)
        return nativeBridge.lookup(text, DEFAULT_MAX_RESULTS, text.length)
    }

    fun rebuildQuery() {
        val paths = queryPaths()
        nativeBridge.rebuildQuery(paths.termPaths, paths.frequencyPaths, paths.pitchPaths)
        queryFingerprint = paths.fingerprint
    }

    private fun rebuildQueryIfNeeded() {
        val paths = queryPaths()
        if (queryFingerprint == paths.fingerprint) return
        nativeBridge.rebuildQuery(paths.termPaths, paths.frequencyPaths, paths.pitchPaths)
        queryFingerprint = paths.fingerprint
    }

    internal fun queryPaths(): DictionaryQueryPaths {
        fun paths(type: DictionaryType) = metadataRepository.getEnabledInPriorityOrder(type)
            .map { storage.dictionaryDirectory(it).absolutePath }
            .toTypedArray()

        val termPaths = paths(DictionaryType.TERM)
        val frequencyPaths = paths(DictionaryType.FREQUENCY)
        val pitchPaths = paths(DictionaryType.PITCH)
        return DictionaryQueryPaths(termPaths, frequencyPaths, pitchPaths)
    }

    private companion object {
        const val DEFAULT_MAX_RESULTS = 20
    }
}

internal data class DictionaryQueryPaths(
    val termPaths: Array<String>,
    val frequencyPaths: Array<String>,
    val pitchPaths: Array<String>,
) {
    val fingerprint: String = listOf(termPaths, frequencyPaths, pitchPaths)
        .joinToString("|") { it.joinToString(";") }
}
