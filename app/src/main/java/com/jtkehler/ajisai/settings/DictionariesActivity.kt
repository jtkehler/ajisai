package com.jtkehler.ajisai.settings

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.jtkehler.ajisai.R
import com.jtkehler.ajisai.dictionary.DictionaryDependencies
import com.jtkehler.ajisai.dictionary.DictionaryImportStage
import com.jtkehler.ajisai.dictionary.DictionaryImportWorker
import com.jtkehler.ajisai.dictionary.ImportedDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class DictionariesActivity : AppCompatActivity() {
    private val metadataRepository by lazy { DictionaryDependencies.metadataRepository(this) }
    private val dictionaryStorage by lazy { DictionaryDependencies.storage(this) }
    private val queryRepository by lazy { DictionaryDependencies.queryRepository(this) }
    private val workManager by lazy { WorkManager.getInstance(this) }

    private lateinit var importButton: MaterialButton
    private lateinit var importProgress: ProgressBar
    private lateinit var importStatus: TextView
    private lateinit var emptyState: View
    private lateinit var listContainer: LinearLayout
    private var observedWorkId: UUID? = null

    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) enqueueImport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictionaries)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        importButton = findViewById(R.id.import_dictionary_button)
        importProgress = findViewById(R.id.dictionary_import_progress)
        importStatus = findViewById(R.id.dictionary_import_status)
        emptyState = findViewById(R.id.dictionaries_empty_state)
        listContainer = findViewById(R.id.dictionary_list_container)

        importButton.setOnClickListener {
            documentPicker.launch(DICTIONARY_MIME_TYPES)
        }

        restoreImportObservation()
        renderDictionaries()
    }

    override fun onResume() {
        super.onResume()
        renderDictionaries()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun enqueueImport(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val request = OneTimeWorkRequestBuilder<DictionaryImportWorker>()
            .setInputData(DictionaryImportWorker.inputData(uri.toString(), displayName(uri)))
            .build()
        getPreferences(MODE_PRIVATE).edit().putString(KEY_IMPORT_WORK_ID, request.id.toString()).apply()
        workManager.enqueueUniqueWork(
            DictionaryImportWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        observeImport(request.id)
    }

    private fun restoreImportObservation() {
        val storedId = getPreferences(MODE_PRIVATE).getString(KEY_IMPORT_WORK_ID, null) ?: return
        val workId = runCatching { UUID.fromString(storedId) }.getOrNull()
        if (workId == null) {
            getPreferences(MODE_PRIVATE).edit().remove(KEY_IMPORT_WORK_ID).apply()
        } else {
            lifecycleScope.launch {
                val workInfo = withContext(Dispatchers.IO) {
                    runCatching { workManager.getWorkInfoById(workId).get() }.getOrNull()
                }
                if (workInfo == null || workInfo.state.isFinished) {
                    clearImportObservation(workId)
                } else {
                    observeImport(workId)
                }
            }
        }
    }

    private fun observeImport(workId: UUID) {
        if (observedWorkId == workId) return
        observedWorkId = workId
        workManager.getWorkInfoByIdLiveData(workId).observe(this) { workInfo ->
            workInfo ?: return@observe
            if (observedWorkId != workId) return@observe
            when (workInfo.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> showImportState(true, getString(R.string.dictionary_import_queued))
                WorkInfo.State.RUNNING -> {
                    val stage = workInfo.progress.getString(DictionaryImportWorker.KEY_STAGE)
                    showImportState(true, stageMessage(stage))
                }
                WorkInfo.State.SUCCEEDED -> {
                    val title = workInfo.outputData.getString(DictionaryImportWorker.KEY_IMPORTED_TITLE).orEmpty()
                    showImportState(false, getString(R.string.dictionary_import_succeeded, title))
                    renderDictionaries()
                    clearImportObservation(workId)
                }
                WorkInfo.State.FAILED -> {
                    showImportState(
                        false,
                        workInfo.outputData.getString(DictionaryImportWorker.KEY_ERROR)
                            ?: getString(R.string.dictionary_import_failed),
                    )
                    clearImportObservation(workId)
                }
                WorkInfo.State.CANCELLED -> {
                    showImportState(false, getString(R.string.dictionary_import_cancelled))
                    clearImportObservation(workId)
                }
            }
        }
    }

    private fun clearImportObservation(workId: UUID) {
        val preferences = getPreferences(MODE_PRIVATE)
        if (preferences.getString(KEY_IMPORT_WORK_ID, null) == workId.toString()) {
            preferences.edit().remove(KEY_IMPORT_WORK_ID).apply()
        }
        if (observedWorkId == workId) observedWorkId = null
    }

    private fun stageMessage(stageName: String?): String = when (
        stageName?.let { runCatching { DictionaryImportStage.valueOf(it) }.getOrNull() }
    ) {
        DictionaryImportStage.PREPARING -> getString(R.string.dictionary_import_preparing)
        DictionaryImportStage.IMPORTING -> getString(R.string.dictionary_import_running)
        DictionaryImportStage.SAVING -> getString(R.string.dictionary_import_saving)
        null -> getString(R.string.dictionary_import_running)
    }

    private fun showImportState(importing: Boolean, message: String) {
        importButton.isEnabled = !importing
        importProgress.visibility = if (importing) View.VISIBLE else View.GONE
        importStatus.visibility = View.VISIBLE
        importStatus.text = message
    }

    private fun renderDictionaries() {
        val dictionaries = metadataRepository.getAll()
        emptyState.visibility = if (dictionaries.isEmpty()) View.VISIBLE else View.GONE
        listContainer.removeAllViews()
        dictionaries.forEach { dictionary ->
            listContainer.addView(dictionaryCard(dictionary, dictionaries))
        }
    }

    private fun dictionaryCard(
        dictionary: ImportedDictionary,
        allDictionaries: List<ImportedDictionary>,
    ): View {
        val sameType = allDictionaries.filter { it.type == dictionary.type }.sortedBy { it.priority }
        val card = MaterialCardView(this).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(1).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(12) }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(12))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = dictionary.title
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Subtitle1)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(MaterialSwitch(this).apply {
            showText = false
            textOn = ""
            textOff = ""
            isChecked = dictionary.enabled
            contentDescription = getString(R.string.dictionary_enabled_description, dictionary.title)
            setOnCheckedChangeListener { _, enabled ->
                runDictionaryMutation {
                    metadataRepository.setEnabled(dictionary.id, enabled)
                }
            }
        })
        content.addView(header)
        content.addView(TextView(this).apply {
            text = getString(
                R.string.dictionary_metadata_summary,
                dictionary.type.displayName(),
                dictionary.priority + 1,
                dictionary.revision ?: getString(R.string.dictionary_revision_unknown),
            )
            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body2)
        })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        actions.addView(actionButton(R.string.move_up) {
            runDictionaryMutation { metadataRepository.move(dictionary.id, dictionary.priority - 1) }
        }.apply { isEnabled = dictionary.priority > 0 })
        actions.addView(actionButton(R.string.move_down) {
            runDictionaryMutation { metadataRepository.move(dictionary.id, dictionary.priority + 1) }
        }.apply { isEnabled = dictionary.priority < sameType.lastIndex })
        actions.addView(actionButton(R.string.delete) { confirmDelete(dictionary) })
        content.addView(actions)
        card.addView(content)
        return card
    }

    private fun actionButton(textResource: Int, onClick: () -> Unit) = MaterialButton(this).apply {
        setText(textResource)
        setOnClickListener { onClick() }
    }

    private fun confirmDelete(dictionary: ImportedDictionary) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_dictionary_title)
            .setMessage(getString(R.string.delete_dictionary_message, dictionary.title))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                runDictionaryMutation {
                    check(dictionaryStorage.delete(dictionary)) { "Unable to delete dictionary files." }
                    metadataRepository.delete(dictionary.id)
                }
            }
            .show()
    }

    private fun runDictionaryMutation(mutation: () -> Unit) {
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    mutation()
                    queryRepository.rebuildQuery()
                }
            }
            if (result.isFailure) {
                showImportState(false, result.exceptionOrNull()?.message ?: getString(R.string.dictionary_update_failed))
            }
            renderDictionaries()
        }
    }

    private fun displayName(uri: Uri): String {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) cursor.getString(0) else uri.lastPathSegment.orEmpty()
        } finally {
            cursor?.close()
        }.ifBlank { "dictionary.zip" }
    }

    private fun com.jtkehler.ajisai.dictionary.DictionaryType.displayName(): String = when (this) {
        com.jtkehler.ajisai.dictionary.DictionaryType.TERM -> getString(R.string.dictionary_type_term)
        com.jtkehler.ajisai.dictionary.DictionaryType.FREQUENCY -> getString(R.string.dictionary_type_frequency)
        com.jtkehler.ajisai.dictionary.DictionaryType.PITCH -> getString(R.string.dictionary_type_pitch)
        com.jtkehler.ajisai.dictionary.DictionaryType.UNKNOWN -> getString(R.string.dictionary_type_unknown)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val KEY_IMPORT_WORK_ID = "dictionary_import_work_id"
        val DICTIONARY_MIME_TYPES = arrayOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream",
        )
    }
}
