package com.pulsewave.visualizer.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reactive visualizer driven directly by the device microphone. */
class MicAudioEngine(private val context: Context) : AudioEngine {

    private val _frame = MutableStateFlow(SILENT_FRAME)
    override val frame: StateFlow<AudioFrame> = _frame.asStateFlow()

    private var recordThread: Thread? = null
    @Volatile private var running = false

    private val sampleRate = 44100
    private val fftSize = 1024

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override fun start() {
        if (running || !hasPermission()) return
        running = true
        recordThread = Thread(::captureLoop, "PulseWave-Mic").apply {
            isDaemon = true
            start()
        }
    }

    override fun stop() {
        running = false
        recordThread?.join(200)
        recordThread = null
    }

    override fun release() = stop()

    @SuppressLint("MissingPermission")
    private fun captureLoop() {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) {
            running = false
            return
        }
        val bufferSize = maxOf(minBuf, fftSize * 2)
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        } catch (e: SecurityException) {
            running = false
            return
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            running = false
            return
        }

        val pcm = ShortArray(fftSize)
        try {
            record.startRecording()
            while (running) {
                val read = record.read(pcm, 0, fftSize)
                if (read <= 0) continue
                _frame.value = analyzeBuffer(pcm, sampleRate)
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }

    private fun analyzeBuffer(pcm: ShortArray, sampleRate: Int): AudioFrame {
        val n = pcm.size
        val real = FloatArray(n) { pcm[it] / 32768f }
        val waveform = downsampleWaveform(real)

        Fft.applyHannWindow(real)
        val imag = FloatArray(n)
        Fft.forward(real, imag)
        val mags = Fft.magnitudes(real, imag)
        val bands = bucketizeSpectrum(mags, BAND_COUNT, sampleRate)

        return AudioFrame(spectrum = bands, waveform = waveform)
    }

    private fun downsampleWaveform(samples: FloatArray): FloatArray {
        val out = FloatArray(WAVEFORM_POINTS)
        for (i in out.indices) {
            out[i] = samples[i * samples.size / out.size]
        }
        return out
    }
}
