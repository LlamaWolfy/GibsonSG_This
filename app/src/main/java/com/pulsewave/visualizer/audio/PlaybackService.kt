package com.pulsewave.visualizer.audio

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pulsewave.visualizer.MainActivity

/**
 * Owns the real [LibraryAudioEngine] (and therefore the ExoPlayer instance)
 * so playback -- and the visualizer reacting to it -- survives the app being
 * backgrounded or swiped from recents, and gets a real system media
 * notification with lock-screen / Bluetooth / headset controls.
 *
 * The UI binds to this service (see [LocalBinder]) rather than owning the
 * engine itself; [MediaSession] wires the same player up to the rest of the
 * system so external controls (lock screen, notification, wearables) act on
 * the exact instance the visualizer is reading from.
 */
class PlaybackService : MediaSessionService() {

    lateinit var engine: LibraryAudioEngine
        private set

    private var mediaSession: MediaSession? = null
    private val localBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        engine = LibraryAudioEngine(this)
        engine.start()

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, engine.player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onBind(intent: Intent?): IBinder? =
        if (intent?.action == ACTION_LOCAL_BIND) localBinder else super.onBind(intent)

    override fun onDestroy() {
        mediaSession?.let {
            engine.release()
            it.release()
            mediaSession = null
        }
        super.onDestroy()
    }

    /** Same-process direct access for our own UI, alongside the system-facing [MediaSession]. */
    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    companion object {
        const val ACTION_LOCAL_BIND = "com.pulsewave.visualizer.action.LOCAL_BIND"

        fun localBindIntent(context: android.content.Context): Intent =
            Intent(context, PlaybackService::class.java).apply { action = ACTION_LOCAL_BIND }
    }
}
