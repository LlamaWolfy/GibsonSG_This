package com.pulsewave.visualizer.ui.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.pulsewave.visualizer.audio.AudioEngine
import com.pulsewave.visualizer.audio.AudioFrame
import com.pulsewave.visualizer.audio.AudioSourceType
import com.pulsewave.visualizer.audio.CaptureService
import com.pulsewave.visualizer.audio.LibraryAudioEngine
import com.pulsewave.visualizer.audio.MicAudioEngine
import com.pulsewave.visualizer.audio.PlaybackService
import com.pulsewave.visualizer.audio.SILENT_FRAME
import com.pulsewave.visualizer.audio.SystemCaptureBus
import com.pulsewave.visualizer.audio.VisualizerStateProcessor
import com.pulsewave.visualizer.data.MediaStoreRepository
import com.pulsewave.visualizer.data.PresetRepository
import com.pulsewave.visualizer.data.Track
import com.pulsewave.visualizer.ui.settings.VisualizerPreset
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Library playback itself lives in [PlaybackService] (a MediaSessionService)
 * so it survives the app being backgrounded or swiped from recents, and gets
 * a real lock-screen / notification / Bluetooth media session. This
 * ViewModel binds to that service and forwards UI commands to its engine.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaStoreRepository(application)
    private val presetRepository = PresetRepository(application)
    private val processor = VisualizerStateProcessor()

    val micEngine = MicAudioEngine(application)

    private var libraryEngine: LibraryAudioEngine? = null
    private var boundToService = false

    private val _settings = MutableStateFlow(VisualizerSettings())
    val settings: StateFlow<VisualizerSettings> = _settings.asStateFlow()

    private val _presets = MutableStateFlow(presetRepository.loadPresets())
    val presets: StateFlow<List<VisualizerPreset>> = _presets.asStateFlow()

    private val _selectedSource = MutableStateFlow(AudioSourceType.LIBRARY)
    val selectedSource: StateFlow<AudioSourceType> = _selectedSource.asStateFlow()

    private val _displayFrame = MutableStateFlow(SILENT_FRAME)
    val displayFrame: StateFlow<AudioFrame> = _displayFrame.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    var isPlaying by mutableStateOf(false)
        private set
    var currentTrackIndex by mutableStateOf<Int?>(null)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var shuffleEnabled by mutableStateOf(false)
        private set
    var repeatAll by mutableStateOf(true)
        private set
    var albumArt by mutableStateOf<Bitmap?>(null)
        private set

    private var frameCollectJob: Job? = null
    private var positionPollJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as PlaybackService.LocalBinder).getService()
            onEngineBound(service.engine)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundToService = false
        }
    }

    init {
        val context = getApplication<Application>()
        context.startService(Intent(context, PlaybackService::class.java))
        context.bindService(
            PlaybackService.localBindIntent(context),
            serviceConnection,
            Context.BIND_AUTO_CREATE,
        )
        startPositionPolling()
    }

    private fun onEngineBound(engine: LibraryAudioEngine) {
        libraryEngine = engine
        boundToService = true

        engine.addPlayerListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentTrackIndex = engine.player.currentMediaItemIndex
                    .takeIf { it in _tracks.value.indices }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                updateAlbumArt(mediaMetadata.artworkData)
            }
        })
        isPlaying = engine.player.isPlaying
        currentTrackIndex = engine.player.currentMediaItemIndex.takeIf { it in _tracks.value.indices }

        if (_selectedSource.value == AudioSourceType.LIBRARY) {
            collectFrom(engine)
        }
    }

    fun updateSettings(update: (VisualizerSettings) -> VisualizerSettings) {
        _settings.value = update(_settings.value)
        processor.sensitivity = _settings.value.sensitivity
        processor.smoothing = _settings.value.smoothing
    }

    fun saveCurrentAsPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val updated = _presets.value.filterNot { it.name == trimmed } + VisualizerPreset(trimmed, _settings.value)
        _presets.value = updated
        presetRepository.savePresets(updated)
    }

    fun loadPreset(preset: VisualizerPreset) {
        updateSettings { preset.settings }
    }

    fun deletePreset(name: String) {
        val updated = _presets.value.filterNot { it.name == name }
        _presets.value = updated
        presetRepository.savePresets(updated)
    }

    private fun updateAlbumArt(artworkData: ByteArray?) {
        if (artworkData == null) {
            albumArt = null
            return
        }
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                runCatching { BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size) }.getOrNull()
            }
            albumArt = bitmap
        }
    }

    fun selectSource(source: AudioSourceType) {
        if (_selectedSource.value == source) return
        _selectedSource.value = source
        applySource(source)
    }

    private fun applySource(source: AudioSourceType) {
        processor.reset()
        if (source == AudioSourceType.MICROPHONE) micEngine.start() else micEngine.stop()

        val engine: AudioEngine? = when (source) {
            AudioSourceType.LIBRARY -> libraryEngine
            AudioSourceType.MICROPHONE -> micEngine
            AudioSourceType.SYSTEM_CAPTURE -> SystemCaptureBus
        }
        engine?.let { collectFrom(it) }
    }

    private fun collectFrom(engine: AudioEngine) {
        frameCollectJob?.cancel()
        frameCollectJob = viewModelScope.launch {
            engine.frame.collect { raw ->
                _displayFrame.value = processor.process(raw)
            }
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            val loaded = mediaRepository.loadTracks()
            _tracks.value = loaded
            libraryEngine?.let { engine ->
                engine.player.setMediaItems(loaded.map { MediaItem.fromUri(it.uri) })
                engine.player.prepare()
            }
        }
    }

    fun playTrackAt(index: Int) {
        val engine = libraryEngine ?: return
        engine.player.seekTo(index, 0)
        engine.player.playWhenReady = true
        currentTrackIndex = index
    }

    fun togglePlayPause() {
        libraryEngine?.togglePlayPause()
    }

    fun skipNext() {
        val engine = libraryEngine ?: return
        if (engine.player.hasNextMediaItem()) engine.player.seekToNext()
    }

    fun skipPrevious() {
        val engine = libraryEngine ?: return
        if (engine.player.hasPreviousMediaItem()) engine.player.seekToPrevious() else engine.seekTo(0)
    }

    fun seekTo(positionMs: Long) {
        libraryEngine?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        val engine = libraryEngine ?: return
        shuffleEnabled = !shuffleEnabled
        engine.player.shuffleModeEnabled = shuffleEnabled
    }

    fun toggleRepeat() {
        val engine = libraryEngine ?: return
        repeatAll = !repeatAll
        engine.player.repeatMode = if (repeatAll) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    fun currentTrack(): Track? = currentTrackIndex?.let { idx -> _tracks.value.getOrNull(idx) }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onSystemCaptureGranted(resultCode: Int, data: Intent) {
        val context = getApplication<Application>()
        ContextCompat.startForegroundService(context, CaptureService.startIntent(context, resultCode, data))
        selectSource(AudioSourceType.SYSTEM_CAPTURE)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun stopSystemCapture() {
        val context = getApplication<Application>()
        context.startService(CaptureService.stopIntent(context))
    }

    private fun startPositionPolling() {
        positionPollJob = viewModelScope.launch {
            while (true) {
                libraryEngine?.player?.let { player ->
                    positionMs = player.currentPosition.coerceAtLeast(0)
                    val d = player.duration
                    durationMs = if (d > 0) d else (currentTrack()?.durationMs ?: 0L)
                }
                delay(250)
            }
        }
    }

    /**
     * Playback itself deliberately keeps running in [PlaybackService] after
     * this ViewModel is cleared -- that's what makes it survive the app
     * being backgrounded. Only the mic engine (which has no independent
     * lifecycle of its own) and this ViewModel's bindings are torn down.
     */
    override fun onCleared() {
        frameCollectJob?.cancel()
        positionPollJob?.cancel()
        micEngine.release()
        if (boundToService) {
            runCatching { getApplication<Application>().unbindService(serviceConnection) }
        }
        super.onCleared()
    }
}
