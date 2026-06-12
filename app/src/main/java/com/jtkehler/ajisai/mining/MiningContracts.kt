package com.jtkehler.ajisai.mining

/** OCR state retained for editing, lookup, and timestamp-aligned mining. */
data class OcrSession(
    val id: String,
    val capturedAtMs: Long,
    val recognizedText: String,
)

/** Input to mining and export boundaries, independent of overlay UI state. */
data class MiningCandidate(
    val session: OcrSession,
    val sentence: String,
)

/** Placeholder result for the future mining workflow. */
sealed interface MiningResult {
    data object Success : MiningResult

    data class Failure(val message: String) : MiningResult
}

/** Boundary coordinating user-triggered mining work. */
interface MiningCoordinator {
    suspend fun mine(candidate: MiningCandidate): MiningResult
}
