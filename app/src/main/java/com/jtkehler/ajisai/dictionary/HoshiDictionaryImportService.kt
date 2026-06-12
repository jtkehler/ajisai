package com.jtkehler.ajisai.dictionary

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

class HoshiDictionaryImportService internal constructor(
    private val contentResolver: ContentResolver,
    private val archiveImporter: DictionaryArchiveImporter,
) : DictionaryImportService {
    override suspend fun importDictionary(
        request: DictionaryImportRequest,
        onProgress: (DictionaryImportStage) -> Unit,
    ): DictionaryImportResult = try {
        val input = requireNotNull(contentResolver.openInputStream(Uri.parse(request.sourceUri))) {
            "Unable to open ${request.displayName}."
        }
        DictionaryImportResult.Success(input.use { archiveImporter.import(it, onProgress) })
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        DictionaryImportResult.Failure(error.message ?: "Dictionary import failed.")
    }
}

internal data class DictionaryArchiveIndex(
    val title: String,
    val revision: String?,
)

internal fun interface DictionaryIndexReader {
    fun read(zipFile: File): DictionaryArchiveIndex
}

internal class ZipDictionaryIndexReader : DictionaryIndexReader {
    override fun read(zipFile: File): DictionaryArchiveIndex = ZipFile(zipFile).use { zip ->
        val entry = zip.getEntry("index.json") ?: error("This zip does not contain a Yomitan index.json file.")
        val index = zip.getInputStream(entry).bufferedReader().use { JSONObject(it.readText()) }
        DictionaryArchiveIndex(
            title = index.getString("title"),
            revision = index.optString("revision").ifBlank { null },
        )
    }
}

/** Stages bridge output before committing it to type-specific app storage. */
internal class DictionaryArchiveImporter(
    private val storage: DictionaryStorage,
    private val metadataRepository: ImportedDictionaryRepository,
    private val queryRepository: HoshiDictionaryRepository,
    private val nativeBridge: DictionaryNativeBridge = HoshiDictionaryNativeBridge,
    private val indexReader: DictionaryIndexReader = ZipDictionaryIndexReader(),
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    fun import(
        input: InputStream,
        onProgress: (DictionaryImportStage) -> Unit = {},
    ): List<ImportedDictionary> {
        onProgress(DictionaryImportStage.PREPARING)
        val importId = newId()
        val zipFile = File(storage.importDirectory, "$importId.zip")
        val stagingRoot = File(storage.importDirectory, importId)
        try {
            zipFile.outputStream().use { output -> input.copyTo(output) }
            val index = indexReader.read(zipFile)
            validateTitle(index.title)

            onProgress(DictionaryImportStage.IMPORTING)
            stagingRoot.mkdirs()
            val nativeResult = nativeBridge.importDictionary(zipFile.absolutePath, stagingRoot.absolutePath, false)
            require(nativeResult.success) { "hoshidicts could not import this dictionary." }
            require(nativeResult.title == index.title) { "Dictionary title changed during import." }

            val types = nativeResult.detectedTypes()
            require(types.isNotEmpty()) { "Dictionary contains no supported term, frequency, or pitch data." }
            val stagedDictionary = File(stagingRoot, nativeResult.title)
            require(stagedDictionary.isDirectory) { "hoshidicts produced no dictionary data." }

            onProgress(DictionaryImportStage.SAVING)
            val imported = types.map { type ->
                commitDictionary(type, index, nativeResult, stagedDictionary)
            }
            queryRepository.rebuildQuery()
            return imported
        } finally {
            zipFile.delete()
            stagingRoot.deleteRecursively()
        }
    }

    private fun commitDictionary(
        type: DictionaryType,
        index: DictionaryArchiveIndex,
        result: NativeDictionaryImportResult,
        stagedDictionary: File,
    ): ImportedDictionary {
        val existing = metadataRepository.getAll().firstOrNull { it.type == type && it.title == index.title }
        val id = existing?.id ?: "${type.name}:${newId()}"
        val directoryName = existing?.directoryName ?: newId()
        val target = File(storage.typeDirectory(type), directoryName)
        val copy = File(storage.typeDirectory(type), ".$directoryName-copy-${newId()}")
        copyDirectory(stagedDictionary, copy)
        replaceDirectory(copy, target)

        return ImportedDictionary(
            id = id,
            title = index.title,
            revision = index.revision,
            type = type,
            enabled = existing?.enabled ?: true,
            priority = existing?.priority ?: metadataRepository.getAll().count { it.type == type },
            importedAtMs = nowMs(),
            directoryName = directoryName,
            termCount = result.termCount,
            frequencyCount = result.frequencyCount,
            pitchCount = result.pitchCount,
            mediaCount = result.mediaCount,
        ).also(metadataRepository::save)
    }

    private fun validateTitle(title: String) {
        require(title.isNotBlank()) { "Dictionary title is empty." }
        require(title != "." && title != ".." && '/' !in title && '\\' !in title) {
            "Dictionary title contains invalid path characters."
        }
    }

    private fun copyDirectory(source: File, target: File) {
        require(source.isDirectory)
        target.mkdirs()
        source.listFiles().orEmpty().forEach { child ->
            val destination = File(target, child.name)
            if (child.isDirectory) copyDirectory(child, destination) else child.copyTo(destination, overwrite = true)
        }
    }

    private fun replaceDirectory(source: File, target: File) {
        val backup = target.takeIf(File::exists)?.let {
            File(target.parentFile, ".${target.name}-backup-${newId()}").also { backup -> move(it, backup) }
        }
        try {
            move(source, target)
            backup?.deleteRecursively()
        } catch (error: Throwable) {
            target.deleteRecursively()
            if (backup?.exists() == true) move(backup, target)
            throw error
        }
    }

    private fun move(source: File, target: File) {
        runCatching {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }.recoverCatching { error ->
            if (error !is AtomicMoveNotSupportedException) throw error
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }.getOrThrow()
    }

    private fun NativeDictionaryImportResult.detectedTypes(): List<DictionaryType> = buildList {
        if (termCount > 0) add(DictionaryType.TERM)
        if (frequencyCount > 0) add(DictionaryType.FREQUENCY)
        if (pitchCount > 0) add(DictionaryType.PITCH)
    }
}
