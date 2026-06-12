package com.jtkehler.ajisai.anki

import com.jtkehler.ajisai.mining.MiningCandidate

/** Fields already mapped for the selected Anki note model. */
data class MappedNoteFields(
    val values: Map<String, String>,
)

/** Result of checking whether the configured export backend is ready. */
data class AnkiSetupResult(
    val isReady: Boolean,
    val message: String? = null,
)

/** Placeholder result returned after an export attempt. */
data class AddNoteResult(
    val noteId: Long? = null,
    val errorMessage: String? = null,
)

/** Boundary for direct AnkiDroid export without exposing its API to mining code. */
interface AnkiExporter {
    suspend fun validateSetup(): AnkiSetupResult

    suspend fun add(
        candidate: MiningCandidate,
        fields: MappedNoteFields,
    ): AddNoteResult
}
