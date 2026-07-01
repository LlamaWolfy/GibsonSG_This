package com.pulsewave.visualizer.audio

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Visualizer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Plays local library tracks with ExoPlayer and reacts to that same playback
 * via [android.media.audiofx.Visualizer] attached to the player's own audio
 * session -- no extra recording permission needed beyond RECORD_AUDIO, which
 * the platform requires simply to instantiate a Visualizer.
 */
class LibraryAudioEngine(context: Context) : AudioEngine {

    private val _frame = MutableStateFlow(SILENT_FRAME)
    override val frame: StateFlow<AudioFrame> = _frame.asStateFlow()

    private val audioSessionId: Int =
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).generateAudioSessionId()

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioSessionId(audioSessionId)
    }

    private var visualizer: Visualizer? = null
    private var lastWaveform = FloatArray(WAVEFORM_POINTS)

    fun playUri(uri: android.net.Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        player.playWhenReady = !player.playWhenReady
    }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun addPlayerListener(listener: Player.Listener) = player.addListener(listener)

    override fun start() {
        setUpVisualizer()
    }

    override fun stop() {
        visualizer?.setEnabled(false)
        player.pause()
    }

    override fun release() {
        visualizer?.release()
        visualizer = null
        player.release()
    }

    private fun setUpVisualizer() {
        if (visualizer != null) {
            visualizer?.setEnabled(true)
            return
        }
        runCatching {
            val range = Visualizer.getCaptureSizeRange()
            var captureSize = 1024
            while (captureSize > range[1]) captureSize /= 2
            if (captureSize < range[0]) captureSize = range[0]

            visualizer = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int,
                        ) {
                            waveform ?: return
                            lastWaveform = decodeWaveform(waveform)
                            publish()
                        }

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            fft ?: return
                            val mags = decodeFftMagnitudes(fft)
                            val bands = bucketizeSpectrum(mags, BAND_COUNT, samplingRate / 1000)
                            _frame.value = AudioFrame(spectrum = bands, waveform = lastWaveform)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true,
                )
                setEnabled(true)
            }
        }
    }

    private fun publish() {
        _frame.value = _frame.value.copy(waveform = lastWaveform)
    }

    private fun decodeWaveform(bytes: ByteArray): FloatArray {
        val out = FloatArray(WAVEFORM_POINTS)
        for (i in out.indices) {
            val srcIndex = i * bytes.size / out.size
            val unsigned = bytes[srcIndex].toInt() and 0xFF
            out[i] = (unsigned - 128) / 128f
        }
        return out
    }

    /**
     * Visualizer.getFft layout: [dc, nyquist, re1, im1, re2, im2, ...].
     * See android.media.audiofx.Visualizer#getFft javadoc.
     */
    private fun decodeFftMagnitudes(fft: ByteArray): FloatArray {
        val n = fft.size
        val bins = n / 2
        if (bins < 2) return FloatArray(1)
        val mags = FloatArray(bins)
        mags[0] = abs(fft[0].toFloat())
        mags[bins - 1] = abs(fft[1].toFloat())
        var bin = 1
        var idx = 2
        while (idx + 1 < n && bin < bins - 1) {
            val re = fft[idx].toFloat()
            val im = fft[idx + 1].toFloat()
            mags[bin] = sqrt(re * re + im * im)
            idx += 2
            bin += 1
        }
        return mags
    }
}
