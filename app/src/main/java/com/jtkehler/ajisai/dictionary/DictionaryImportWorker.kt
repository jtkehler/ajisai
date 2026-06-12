package com.jtkehler.ajisai.dictionary

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DictionaryImportWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sourceUri = inputData.getString(KEY_SOURCE_URI) ?: return@withContext failure("Missing dictionary URI.")
        val displayName = inputData.getString(KEY_DISPLAY_NAME) ?: "dictionary.zip"
        val result = DictionaryDependencies.importService(applicationContext).importDictionary(
            DictionaryImportRequest(sourceUri, displayName),
        ) { stage ->
            setProgressAsync(workDataOf(KEY_STAGE to stage.name))
        }

        when (result) {
            is DictionaryImportResult.Success -> Result.success(
                workDataOf(
                    KEY_IMPORTED_COUNT to result.dictionaries.size,
                    KEY_IMPORTED_TITLE to result.dictionaries.firstOrNull()?.title.orEmpty(),
                ),
            )
            is DictionaryImportResult.Failure -> failure(result.message)
        }
    }

    private fun failure(message: String): Result = Result.failure(workDataOf(KEY_ERROR to message))

    companion object {
        const val UNIQUE_WORK_NAME = "dictionary-import"
        const val KEY_SOURCE_URI = "source_uri"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_STAGE = "stage"
        const val KEY_ERROR = "error"
        const val KEY_IMPORTED_COUNT = "imported_count"
        const val KEY_IMPORTED_TITLE = "imported_title"

        fun inputData(sourceUri: String, displayName: String): Data = workDataOf(
            KEY_SOURCE_URI to sourceUri,
            KEY_DISPLAY_NAME to displayName,
        )
    }
}
