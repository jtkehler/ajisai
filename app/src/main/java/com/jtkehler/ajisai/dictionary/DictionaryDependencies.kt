package com.jtkehler.ajisai.dictionary

import android.content.Context

object DictionaryDependencies {
    fun metadataRepository(context: Context): ImportedDictionaryRepository =
        SharedPreferencesImportedDictionaryRepository(
            context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        )

    fun storage(context: Context) = DictionaryStorage(context.applicationContext.filesDir)

    fun queryRepository(context: Context): HoshiDictionaryRepository {
        val metadata = metadataRepository(context)
        return HoshiDictionaryRepository(metadata, storage(context))
    }

    fun importService(context: Context): DictionaryImportService {
        val metadata = metadataRepository(context)
        val storage = storage(context)
        val query = HoshiDictionaryRepository(metadata, storage)
        return HoshiDictionaryImportService(
            contentResolver = context.applicationContext.contentResolver,
            archiveImporter = DictionaryArchiveImporter(storage, metadata, query),
        )
    }

    private const val PREFERENCES_NAME = "dictionary_metadata"
}
