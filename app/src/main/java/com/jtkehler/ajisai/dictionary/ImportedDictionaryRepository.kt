package com.jtkehler.ajisai.dictionary

/** Boundary for imported dictionary metadata and priority ordering. */
interface ImportedDictionaryRepository {
    fun getAll(): List<ImportedDictionary>

    fun save(dictionary: ImportedDictionary)
}

/** Process-local store used until persistent hoshidicts-backed metadata is added. */
class InMemoryImportedDictionaryRepository(
    initialDictionaries: List<ImportedDictionary> = emptyList(),
) : ImportedDictionaryRepository {
    private val dictionariesById = initialDictionaries.associateByTo(linkedMapOf()) { it.id }

    override fun getAll(): List<ImportedDictionary> = dictionariesById.values.sortedBy { it.priority }

    override fun save(dictionary: ImportedDictionary) {
        dictionariesById[dictionary.id] = dictionary
    }
}
