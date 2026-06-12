package com.jtkehler.ajisai.audio

/** Timestamped PCM window used for later sentence-audio extraction. */
data class AudioWindow(
    val pcm: ByteArray,
    val sampleRateHz: Int,
    val channelCount: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
)

/** Boundary for platform audio capture and the future ring buffer. */
interface AudioRecorder {
    suspend fun start()

    suspend fun stop()

    suspend fun readWindow(startTimeMs: Long, endTimeMs: Long): AudioWindow?
}

/** Boundary for VAD that will be invoked only after the user chooses Mine. */
interface VadSegmenter {
    suspend fun segment(window: AudioWindow): AudioWindow?
}
