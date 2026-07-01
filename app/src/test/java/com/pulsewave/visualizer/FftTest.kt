package com.pulsewave.visualizer

import com.pulsewave.visualizer.audio.Fft
import com.pulsewave.visualizer.audio.bucketizeSpectrum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FftTest {

    @Test
    fun `pure tone peaks at the expected bin`() {
        val n = 1024
        val sampleRate = 44100
        val toneHz = 1000.0
        val real = FloatArray(n) { i -> sin(2.0 * PI * toneHz * i / sampleRate).toFloat() }
        val imag = FloatArray(n)

        Fft.forward(real, imag)
        val mags = Fft.magnitudes(real, imag)

        val expectedBin = (toneHz * n / sampleRate).toInt()
        val peakBin = mags.indices.maxBy { mags[it] }

        assertTrue("expected peak near bin $expectedBin, got $peakBin", kotlin.math.abs(peakBin - expectedBin) <= 1)
    }

    @Test
    fun `bucketize produces requested band count of non-negative averages`() {
        val magnitudes = FloatArray(512) { it.toFloat() }
        val bands = bucketizeSpectrum(magnitudes, bandCount = 48, sampleRate = 44100)

        assertEquals(48, bands.size)
        assertTrue(bands.all { it >= 0f })
    }
}
