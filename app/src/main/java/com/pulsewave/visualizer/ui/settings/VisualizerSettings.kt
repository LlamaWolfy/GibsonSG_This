package com.pulsewave.visualizer.ui.settings

import androidx.compose.ui.graphics.Color
import com.pulsewave.visualizer.ui.theme.PulseCyan
import com.pulsewave.visualizer.ui.theme.PulsePink
import com.pulsewave.visualizer.ui.theme.PulsePurple

enum class VisualizerStyle(val label: String) {
    BARS("Bar Spectrum"),
    WAVEFORM("Waveform"),
    CIRCULAR("Circular Pulse"),
    PARTICLES("Particle Field"),
    VU_NEEDLE("VU Needle"),
    POLAR_RING("Polar Ring"),
}

enum class ColorTheme(val label: String, val primary: Color, val secondary: Color) {
    NEON("Neon", PulseCyan, PulsePink),
    MONO("Mono", Color(0xFFECECEC), Color(0xFF8A8A8A)),
    SUNSET("Sunset", Color(0xFFFF7A45), Color(0xFFFFC542)),
    ULTRAVIOLET("Ultraviolet", PulsePurple, PulsePink),
    SPECTRUM_HUE("Spectrum Hue", PulseCyan, PulsePurple),
    OCEAN("Ocean", Color(0xFF00BCD4), Color(0xFF1565C0)),
    FIRE("Fire", Color(0xFFFF3D00), Color(0xFFFFC400)),
}

/**
 * All the live tweakable knobs for the visualizer. One shared shape used by
 * every style + every audio source, so a change here (e.g. sensitivity)
 * behaves the same regardless of what's currently selected.
 */
data class VisualizerSettings(
    val style: VisualizerStyle = VisualizerStyle.BARS,
    val colorTheme: ColorTheme = ColorTheme.NEON,
    val sensitivity: Float = 1f,
    val smoothing: Float = 0.55f,
    val mirror: Boolean = true,
    val density: Float = 1f,
    val beatFlash: Boolean = true,
    val showAlbumArt: Boolean = true,
)
