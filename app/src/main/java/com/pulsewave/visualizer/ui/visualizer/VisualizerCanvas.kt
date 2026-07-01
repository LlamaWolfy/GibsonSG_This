package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.pulsewave.visualizer.audio.AudioFrame
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import com.pulsewave.visualizer.ui.settings.VisualizerStyle

@Composable
fun AudioVisualization(
    frame: AudioFrame,
    settings: VisualizerSettings,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (settings.beatFlash) {
            BeatGlow(spectrum = frame.spectrum, settings = settings, modifier = Modifier.fillMaxSize())
        }
        Crossfade(
            targetState = settings.style,
            animationSpec = tween(durationMillis = 350),
            label = "visualizer-style",
        ) { style ->
            when (style) {
                VisualizerStyle.BARS -> BarSpectrumVisualizer(frame.spectrum, settings, Modifier.fillMaxSize())
                VisualizerStyle.WAVEFORM -> WaveformVisualizer(frame.waveform, settings, Modifier.fillMaxSize())
                VisualizerStyle.CIRCULAR -> CircularPulseVisualizer(frame.spectrum, settings, Modifier.fillMaxSize())
                VisualizerStyle.PARTICLES -> ParticleFieldVisualizer(frame.spectrum, settings, Modifier.fillMaxSize())
                VisualizerStyle.VU_NEEDLE -> VuNeedleVisualizer(frame.spectrum, settings, Modifier.fillMaxSize())
                VisualizerStyle.POLAR_RING -> PolarRingVisualizer(frame.waveform, settings, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun BeatGlow(spectrum: FloatArray, settings: VisualizerSettings, modifier: Modifier = Modifier) {
    val bassCount = (spectrum.size * 0.15f).toInt().coerceAtLeast(1)
    val bass = spectrum.take(bassCount).average().toFloat().coerceIn(0f, 1f)
    val accent = themeAccent(settings.colorTheme)

    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = bass * 0.35f), accent.copy(alpha = 0f)),
                radius = size.maxDimension * 0.75f,
            ),
        )
    }
}
