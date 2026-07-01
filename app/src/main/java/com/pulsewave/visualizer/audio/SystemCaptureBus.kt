package com.pulsewave.visualizer.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges [CaptureService] -- which owns the actual MediaProjection-backed
 * AudioRecord, since Android requires that capture run inside a foreground
 * service -- to the rest of the app. The service pushes frames here;
 * anything that wants system-audio-driven visuals just observes [frame].
 */
object SystemCaptureBus : AudioEngine {
    private val _frame = MutableStateFlow(SILENT_FRAME)
    override val frame: StateFlow<AudioFrame> = _frame.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    internal fun publish(frame: AudioFrame) {
        _frame.value = frame
    }

    internal fun setCapturing(capturing: Boolean) {
        _isCapturing.value = capturing
        if (!capturing) _frame.value = SILENT_FRAME
    }

    /** No-op: capture is driven by [CaptureService]'s own lifecycle. */
    override fun start() = Unit
    override fun stop() = Unit
    override fun release() {
        _frame.value = SILENT_FRAME
    }
}
