package com.pulsewave.visualizer.ui.player

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.pulsewave.visualizer.audio.AudioEngine
import com.pulsewave.visualizer.audio.AudioFrame
import com.pulsewave.visualizer.audio.AudioSourceType
import com.pulsewave.visualizer.audio.CaptureService
import com.pulsewave.visualizer.audio.LibraryAudioEngine
import com.pulsewave.visualizer.audio.MicAudioEngine
import com.pulsewave.visualizer.audio.SILENT_FRAME
import com.pulsewave.visualizer.audio.SystemCaptureBus
import com.pulsewave.visualizer.audio.VisualizerStateProcessor
import com.pulsewave.visualizer.data.MediaStoreRepository
import com.pulsewave.visualizer.data.Track
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaStoreRepository(application)
    private val processor = VisualizerStateProcessor()

    val libraryEngine = LibraryAudioEngine(application)
    val micEngine = MicAudioEngine(application)

    private val _settings = MutableStateFlow(VisualizerSettings())
    val settings: StateFlow<VisualizerSettings> = _settings.asStateFlow()

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

    private var frameCollectJob: Job? = null
    private var positionPollJob: Job? = null

    init {
        libraryEngine.addPlayerListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentTrackIndex = libraryEngine.player.currentMediaItemIndex
                    .takeIf { it in _tracks.value.indices }
            }
        })
        applySource(AudioSourceType.LIBRARY)
        startPositionPolling()
    }

    fun updateSettings(update: (VisualizerSettings) -> VisualizerSettings) {
        _settings.value = update(_settings.value)
        processor.sensitivity = _settings.value.sensitivity
        processor.smoothing = _settings.value.smoothing
    }

    fun selectSource(source: AudioSourceType) {
        if (_selectedSource.value == source) return
        _selectedSource.value = source
        applySource(source)
    }

    private fun applySource(source: AudioSourceType) {
        processor.reset()
        val engine: AudioEngine = when (source) {
            AudioSourceType.LIBRARY -> libraryEngine
            AudioSourceType.MICROPHONE -> micEngine
            AudioSourceType.SYSTEM_CAPTURE -> SystemCaptureBus
        }
        if (source == AudioSourceType.MICROPHONE) micEngine.start()
        else micEngine.stop()

        if (source == AudioSourceType.LIBRARY) libraryEngine.start()

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
            libraryEngine.player.setMediaItems(loaded.map { MediaItem.fromUri(it.uri) })
            libraryEngine.player.prepare()
        }
    }

    fun playTrackAt(index: Int) {
        libraryEngine.player.seekTo(index, 0)
        libraryEngine.player.playWhenReady = true
        currentTrackIndex = index
    }

    fun togglePlayPause() {
        libraryEngine.togglePlayPause()
    }

    fun skipNext() {
        if (libraryEngine.player.hasNextMediaItem()) libraryEngine.player.seekToNext()
    }

    fun skipPrevious() {
        if (libraryEngine.player.hasPreviousMediaItem()) libraryEngine.player.seekToPrevious()
        else libraryEngine.seekTo(0)
    }

    fun seekTo(positionMs: Long) {
        libraryEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        libraryEngine.player.shuffleModeEnabled = shuffleEnabled
    }

    fun toggleRepeat() {
        repeatAll = !repeatAll
        libraryEngine.player.repeatMode = if (repeatAll) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
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
                positionMs = libraryEngine.player.currentPosition.coerceAtLeast(0)
                val d = libraryEngine.player.duration
                durationMs = if (d > 0) d else (currentTrack()?.durationMs ?: 0L)
                delay(250)
            }
        }
    }

    override fun onCleared() {
        frameCollectJob?.cancel()
        positionPollJob?.cancel()
        libraryEngine.release()
        micEngine.release()
        super.onCleared()
    }
}
