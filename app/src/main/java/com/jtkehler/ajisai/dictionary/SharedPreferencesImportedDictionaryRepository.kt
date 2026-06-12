package com.jtkehler.ajisai.dictionary

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** SharedPreferences-backed metadata store. Imported dictionary data remains in app files. */
class SharedPreferencesImportedDictionaryRepository(
    private val preferences: SharedPreferences,
) : ImportedDictionaryRepository {
    private val lock = Any()

    override fun getAll(): List<ImportedDictionary> = synchronized(lock) {
        readAll().sortedWith(compareBy(ImportedDictionary::type, ImportedDictionary::priority, ImportedDictionary::title))
    }

    override fun getEnabledInPriorityOrder(type: DictionaryType): List<ImportedDictionary> = synchronized(lock) {
        readAll().filter { it.type == type && it.enabled }.sortedBy { it.priority }
    }

    override fun find(id: String): ImportedDictionary? = synchronized(lock) {
        readAll().firstOrNull { it.id == id }
    }

    override fun save(dictionary: ImportedDictionary) = mutate { it.save(dictionary) }

    override fun setEnabled(id: String, enabled: Boolean) = mutate { it.setEnabled(id, enabled) }

    override fun delete(id: String) = mutate { it.delete(id) }

    override fun move(id: String, newPriority: Int) = mutate { it.move(id, newPriority) }

    private fun mutate(block: (InMemoryImportedDictionaryRepository) -> Unit) = synchronized(lock) {
        val repository = InMemoryImportedDictionaryRepository(readAll())
        block(repository)
        writeAll(repository.getAll())
    }

    private fun readAll(): List<ImportedDictionary> {
        val encoded = preferences.getString(KEY_DICTIONARIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toDictionary())
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(dictionaries: List<ImportedDictionary>) {
        val array = JSONArray()
        dictionaries.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_DICTIONARIES, array.toString()).apply()
    }

    private fun ImportedDictionary.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("revision", revision)
        .put("type", type.name)
        .put("enabled", enabled)
        .put("priority", priority)
        .put("importedAtMs", importedAtMs)
        .put("directoryName", directoryName)
        .put("termCount", termCount)
        .put("frequencyCount", frequencyCount)
        .put("pitchCount", pitchCount)
        .put("mediaCount", mediaCount)

    private fun JSONObject.toDictionary(): ImportedDictionary {
        val id = getString("id")
        return ImportedDictionary(
            id = id,
            title = getString("title"),
            revision = optString("revision").ifBlank { null },
            type = runCatching { DictionaryType.valueOf(getString("type")) }.getOrDefault(DictionaryType.UNKNOWN),
            enabled = optBoolean("enabled", true),
            priority = optInt("priority", 0),
            importedAtMs = optLong("importedAtMs", 0L),
            directoryName = optString("directoryName", id),
            termCount = optLong("termCount", 0L),
            frequencyCount = optLong("frequencyCount", 0L),
            pitchCount = optLong("pitchCount", 0L),
            mediaCount = optLong("mediaCount", 0L),
        )
    }

    private companion object {
        const val KEY_DICTIONARIES = "imported_dictionaries"
    }
}
