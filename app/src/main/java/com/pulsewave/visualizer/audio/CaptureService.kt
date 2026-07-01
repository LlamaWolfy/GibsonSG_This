package com.pulsewave.visualizer.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pulsewave.visualizer.MainActivity
import com.pulsewave.visualizer.R

/**
 * Foreground service required to legally run [AudioPlaybackCaptureConfiguration]
 * capture (Android 10+, mediaProjection foreground service type). Owns the
 * MediaProjection + AudioRecord lifecycle and streams analyzed frames to
 * [SystemCaptureBus]. Only ever started on API 29+ (gated in the UI).
 */
@RequiresApi(Build.VERSION_CODES.Q)
class CaptureService : Service() {

    companion object {
        const val ACTION_START = "com.pulsewave.visualizer.action.START_CAPTURE"
        const val ACTION_STOP = "com.pulsewave.visualizer.action.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        private const val CHANNEL_ID = "pulsewave_capture"
        private const val NOTIFICATION_ID = 42
        private const val TAG = "CaptureService"

        fun startIntent(context: Context, resultCode: Int, data: Intent): Intent =
            Intent(context, CaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, CaptureService::class.java).apply { action = ACTION_STOP }
    }

    private var mediaProjection: MediaProjection? = null
    private var captureThread: Thread? = null
    @Volatile private var running = false

    private val sampleRate = 44100
    private val fftSize = 1024

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        @Suppress("DEPRECATION")
        val data: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)
        if (data == null) {
            stopSelf()
            return
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)
        if (projection == null) {
            stopSelf()
            return
        }
        mediaProjection = projection

        // Required: AudioPlaybackCaptureConfiguration.Builder throws
        // IllegalStateException on a MediaProjection with no registered
        // callback. Also lets us clean up if the user stops the capture
        // from the system's screen/audio capture indicator.
        projection.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    stopSelf()
                }
            },
            Handler(Looper.getMainLooper()),
        )

        startCapture(projection)
    }

    private fun startCapture(projection: MediaProjection) {
        if (running) return
        running = true
        SystemCaptureBus.setCapturing(true)
        captureThread = Thread({ captureLoop(projection) }, "PulseWave-SystemCapture").apply {
            isDaemon = true
            start()
        }
    }

    private fun captureLoop(projection: MediaProjection) {
        val record = try {
            val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(android.media.AudioAttributes.USAGE_GAME)
                .addMatchingUsage(android.media.AudioAttributes.USAGE_UNKNOWN)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val bufferSize = maxOf(minBuf, fftSize * 2)

            AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start system audio capture", e)
            running = false
            SystemCaptureBus.setCapturing(false)
            return
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            running = false
            SystemCaptureBus.setCapturing(false)
            return
        }

        val pcm = ShortArray(fftSize)
        try {
            record.startRecording()
            while (running) {
                val read = record.read(pcm, 0, fftSize)
                if (read <= 0) continue
                SystemCaptureBus.publish(analyzeBuffer(pcm))
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }

    private fun analyzeBuffer(pcm: ShortArray): AudioFrame {
        val n = pcm.size
        val real = FloatArray(n) { pcm[it] / 32768f }
        val waveform = FloatArray(WAVEFORM_POINTS) { real[it * n / WAVEFORM_POINTS] }

        Fft.applyHannWindow(real)
        val imag = FloatArray(n)
        Fft.forward(real, imag)
        val mags = Fft.magnitudes(real, imag)
        val bands = bucketizeSpectrum(mags, BAND_COUNT, sampleRate)

        return AudioFrame(spectrum = bands, waveform = waveform)
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_capture),
                NotificationManager.IMPORTANCE_LOW,
            )
            nm.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        running = false
        captureThread?.join(200)
        captureThread = null
        mediaProjection?.stop()
        mediaProjection = null
        SystemCaptureBus.setCapturing(false)
        super.onDestroy()
    }
}
