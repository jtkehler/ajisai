package com.jtkehler.ajisai.dictionary

/** Boundary for imported dictionary metadata and priority ordering. */
interface ImportedDictionaryRepository {
    fun getAll(): List<ImportedDictionary>

    fun getEnabledInPriorityOrder(type: DictionaryType): List<ImportedDictionary>

    fun find(id: String): ImportedDictionary?

    fun save(dictionary: ImportedDictionary)

    fun setEnabled(id: String, enabled: Boolean)

    fun delete(id: String)

    fun move(id: String, newPriority: Int)
}

/** Process-local fake used by tests and previews. */
class InMemoryImportedDictionaryRepository(
    initialDictionaries: List<ImportedDictionary> = emptyList(),
) : ImportedDictionaryRepository {
    private val dictionariesById = initialDictionaries.associateByTo(linkedMapOf()) { it.id }

    override fun getAll(): List<ImportedDictionary> = dictionariesById.values
        .sortedWith(compareBy(ImportedDictionary::type, ImportedDictionary::priority, ImportedDictionary::title))

    override fun getEnabledInPriorityOrder(type: DictionaryType): List<ImportedDictionary> = dictionariesById.values
        .filter { it.type == type && it.enabled }
        .sortedBy { it.priority }

    override fun find(id: String): ImportedDictionary? = dictionariesById[id]

    override fun save(dictionary: ImportedDictionary) {
        dictionariesById[dictionary.id] = dictionary
        normalizePriorities(dictionary.type)
    }

    override fun setEnabled(id: String, enabled: Boolean) {
        dictionariesById[id]?.let { dictionariesById[id] = it.copy(enabled = enabled) }
    }

    override fun delete(id: String) {
        val type = dictionariesById.remove(id)?.type ?: return
        normalizePriorities(type)
    }

    override fun move(id: String, newPriority: Int) {
        val dictionary = dictionariesById[id] ?: return
        val ordered = dictionariesById.values.filter { it.type == dictionary.type }.sortedBy { it.priority }.toMutableList()
        val fromIndex = ordered.indexOfFirst { it.id == id }
        if (fromIndex < 0) return
        val moved = ordered.removeAt(fromIndex)
        ordered.add(newPriority.coerceIn(0, ordered.size), moved)
        ordered.forEachIndexed { index, item -> dictionariesById[item.id] = item.copy(priority = index) }
    }

    private fun normalizePriorities(type: DictionaryType) {
        dictionariesById.values
            .filter { it.type == type }
            .sortedWith(compareBy(ImportedDictionary::priority, ImportedDictionary::title))
            .forEachIndexed { index, item -> dictionariesById[item.id] = item.copy(priority = index) }
    }
}
