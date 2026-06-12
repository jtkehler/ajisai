package com.jtkehler.ajisai.dictionary

import java.io.File

class DictionaryStorage(filesDir: File) {
    private val rootDirectory = File(filesDir, "Dictionaries")

    val importDirectory: File
        get() = File(rootDirectory, ".imports").apply { mkdirs() }

    fun typeDirectory(type: DictionaryType): File = File(rootDirectory, type.directoryName).apply { mkdirs() }

    fun dictionaryDirectory(dictionary: ImportedDictionary): File =
        File(typeDirectory(dictionary.type), dictionary.directoryName)

    fun delete(dictionary: ImportedDictionary): Boolean = !dictionaryDirectory(dictionary).exists() ||
        dictionaryDirectory(dictionary).deleteRecursively()
}
