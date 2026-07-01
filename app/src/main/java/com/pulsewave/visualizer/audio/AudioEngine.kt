package com.pulsewave.visualizer.audio

import kotlinx.coroutines.flow.StateFlow

/** Number of frequency bands every engine buckets its spectrum into. */
const val BAND_COUNT = 48

/** Number of points every engine exposes for the raw waveform trace. */
const val WAVEFORM_POINTS = 128

enum class AudioSourceType {
    LIBRARY,
    MICROPHONE,
    SYSTEM_CAPTURE,
}

/**
 * A single slice of audio analysis data. [spectrum] holds per-band magnitudes
 * normalized to roughly 0..1 (log-bucketed, low to high frequency).
 * [waveform] holds [WAVEFORM_POINTS] raw amplitude samples normalized to -1..1.
 */
data class AudioFrame(
    val spectrum: FloatArray = FloatArray(BAND_COUNT),
    val waveform: FloatArray = FloatArray(WAVEFORM_POINTS),
) {
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}

val SILENT_FRAME = AudioFrame()

/**
 * Common contract for anything that can feed the visualizers: local playback,
 * the microphone, or captured system audio.
 */
interface AudioEngine {
    val frame: StateFlow<AudioFrame>

    fun start()
    fun stop()
    fun release()
}
