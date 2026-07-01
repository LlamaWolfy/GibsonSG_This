package com.pulsewave.visualizer.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/** Minimal in-place iterative radix-2 Cooley-Tukey FFT. */
object Fft {

    /** [real].size must be a power of two. [imag] is overwritten in place too. */
    fun forward(real: FloatArray, imag: FloatArray) {
        val n = real.size
        require(n and (n - 1) == 0) { "FFT size must be a power of two, was $n" }

        // Bit-reversal permutation.
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                var tmp = real[i]; real[i] = real[j]; real[j] = tmp
                tmp = imag[i]; imag[i] = imag[j]; imag[j] = tmp
            }
        }

        // Iterative butterflies.
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wr = cos(ang).toFloat()
            val wi = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curWr = 1f
                var curWi = 0f
                for (k in 0 until len / 2) {
                    val evenR = real[i + k]
                    val evenI = imag[i + k]
                    val oddR = real[i + k + len / 2]
                    val oddI = imag[i + k + len / 2]
                    val twR = oddR * curWr - oddI * curWi
                    val twI = oddR * curWi + oddI * curWr

                    real[i + k] = evenR + twR
                    imag[i + k] = evenI + twI
                    real[i + k + len / 2] = evenR - twR
                    imag[i + k + len / 2] = evenI - twI

                    val nextWr = curWr * wr - curWi * wi
                    val nextWi = curWr * wi + curWi * wr
                    curWr = nextWr
                    curWi = nextWi
                }
                i += len
            }
            len = len shl 1
        }
    }

    /** Hann window applied in place, reduces spectral leakage before an FFT. */
    fun applyHannWindow(samples: FloatArray) {
        val n = samples.size
        for (i in samples.indices) {
            val w = 0.5f * (1f - cos(2.0 * PI * i / (n - 1)).toFloat())
            samples[i] *= w
        }
    }

    /** Magnitude of each of the first n/2 complex bins after [forward]. */
    fun magnitudes(real: FloatArray, imag: FloatArray): FloatArray {
        val half = real.size / 2
        val out = FloatArray(half)
        for (i in 0 until half) {
            out[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return out
    }
}

/**
 * Groups a linear magnitude spectrum into [bandCount] logarithmically spaced
 * bands (so low frequencies get more visual resolution, matching how a
 * classic EQ / spectrum visualizer looks). Returns raw (unnormalized,
 * non-negative) averages per band -- gain/normalization is applied downstream
 * by [com.pulsewave.visualizer.audio.VisualizerStateProcessor] so it behaves
 * consistently no matter which engine produced the data.
 */
fun bucketizeSpectrum(
    magnitudes: FloatArray,
    bandCount: Int,
    sampleRate: Int,
): FloatArray {
    val bins = magnitudes.size
    val nyquist = sampleRate / 2.0
    val minFreq = 30.0
    val maxFreq = nyquist.coerceAtMost(16000.0)
    val logMin = ln(minFreq)
    val logMax = ln(maxFreq)

    val bands = FloatArray(bandCount)
    for (b in 0 until bandCount) {
        val f0 = Math.exp(logMin + (logMax - logMin) * b / bandCount)
        val f1 = Math.exp(logMin + (logMax - logMin) * (b + 1) / bandCount)
        val bin0 = ((f0 / nyquist) * bins).toInt().coerceIn(0, bins - 1)
        val bin1 = ((f1 / nyquist) * bins).toInt().coerceIn(bin0 + 1, bins)
        var sum = 0f
        for (bin in bin0 until bin1) sum += magnitudes[bin]
        bands[b] = sum / (bin1 - bin0)
    }
    return bands
}
