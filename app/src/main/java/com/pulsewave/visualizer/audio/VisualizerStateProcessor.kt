package com.pulsewave.visualizer.audio

import kotlin.math.max

/**
 * Turns raw [AudioFrame]s from any [AudioEngine] into display-ready,
 * normalized values. This is the one place sensitivity/smoothing tweaks live
 * so every visualizer style and every audio source behaves consistently.
 *
 * - Auto-gain: tracks a slowly decaying running max per band so quiet and
 *   loud sources both fill the display without a fixed magic constant.
 * - Sensitivity: a user multiplier applied on top of auto-gain.
 * - Smoothing: attack/decay easing so bars don't jitter frame to frame.
 */
class VisualizerStateProcessor(bandCount: Int = BAND_COUNT) {

    var sensitivity: Float = 1f
    /** 0 = instant/jittery, 1 = very smooth/slow. */
    var smoothing: Float = 0.55f

    private val runningMax = FloatArray(bandCount) { 1f }
    private val displayedSpectrum = FloatArray(bandCount)
    private val displayedWaveform = FloatArray(WAVEFORM_POINTS)

    private companion object {
        const val GAIN_DECAY = 0.992f
        const val MIN_RUNNING_MAX = 1f
        const val ATTACK_RATE = 0.6f
        const val MIN_DECAY_RATE = 0.05f
    }

    fun process(raw: AudioFrame): AudioFrame {
        val spectrum = raw.spectrum
        val decayRate = (1f - smoothing).coerceIn(MIN_DECAY_RATE, 1f)
        for (i in spectrum.indices) {
            runningMax[i] = max(runningMax[i] * GAIN_DECAY, max(spectrum[i], MIN_RUNNING_MAX))
            val target = ((spectrum[i] / runningMax[i]) * sensitivity).coerceIn(0f, 1f)
            val rate = if (target > displayedSpectrum[i]) ATTACK_RATE else decayRate
            displayedSpectrum[i] += (target - displayedSpectrum[i]) * rate
        }

        val waveformRate = (1f - smoothing * 0.5f).coerceIn(0.1f, 1f)
        val waveform = raw.waveform
        for (i in waveform.indices) {
            val target = (waveform[i] * sensitivity).coerceIn(-1f, 1f)
            displayedWaveform[i] += (target - displayedWaveform[i]) * waveformRate
        }

        return AudioFrame(
            spectrum = displayedSpectrum.copyOf(),
            waveform = displayedWaveform.copyOf(),
        )
    }

    fun reset() {
        runningMax.fill(1f)
        displayedSpectrum.fill(0f)
        displayedWaveform.fill(0f)
    }
}
