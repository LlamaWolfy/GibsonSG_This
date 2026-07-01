package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.pulsewave.visualizer.ui.settings.ColorTheme
import kotlin.math.abs

/** Color for the band at [index] out of [count], for the given [theme]. */
fun themeColorForBand(theme: ColorTheme, index: Int, count: Int): Color {
    if (theme == ColorTheme.SPECTRUM_HUE) {
        val hue = 360f * index / count.coerceAtLeast(1)
        return Color.hsv(hue, 0.85f, 1f)
    }
    val t = if (count <= 1) 0f else index / (count - 1).toFloat()
    return lerp(theme.primary, theme.secondary, t)
}

/** A single accent color representative of the theme, for single-color styles. */
fun themeAccent(theme: ColorTheme, phase: Float = 0f): Color {
    if (theme == ColorTheme.SPECTRUM_HUE) {
        val hue = (phase * 360f) % 360f
        return Color.hsv(hue, 0.85f, 1f)
    }
    val t = (abs(phase % 1f))
    return lerp(theme.primary, theme.secondary, t)
}
