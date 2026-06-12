package com.jtkehler.ajisai.dictionary

/** Dictionary content categories exposed by Yomitan archives. */
enum class DictionaryType {
    TERM,
    FREQUENCY,
    PITCH,
    UNKNOWN,
    ;

    val directoryName: String
        get() = name.lowercase()
}

/** App-owned metadata; hoshidicts implementation details stay behind repositories. */
data class ImportedDictionary(
    val id: String,
    val title: String,
    val revision: String? = null,
    val type: DictionaryType = DictionaryType.UNKNOWN,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val importedAtMs: Long,
    val directoryName: String = id,
    val termCount: Long = 0,
    val frequencyCount: Long = 0,
    val pitchCount: Long = 0,
    val mediaCount: Long = 0,
)
