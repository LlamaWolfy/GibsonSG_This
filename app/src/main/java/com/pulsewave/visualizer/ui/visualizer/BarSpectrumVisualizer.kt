package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BarSpectrumVisualizer(
    spectrum: FloatArray,
    settings: VisualizerSettings,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawBarSpectrum(spectrum, settings)
    }
}

internal fun DrawScope.drawBarSpectrum(spectrum: FloatArray, settings: VisualizerSettings) {
    val barCount = max(8, (spectrum.size * settings.density).roundToInt()).coerceAtMost(spectrum.size)
    val step = spectrum.size / barCount
    val gap = size.width * 0.006f
    val barWidth = (size.width / barCount) - gap
    val baseline = if (settings.mirror) size.height / 2f else size.height
    val maxBarHeight = if (settings.mirror) size.height / 2f else size.height

    for (i in 0 until barCount) {
        val level = spectrum[(i * step).coerceAtMost(spectrum.size - 1)]
        val barHeight = level * maxBarHeight
        val x = i * (barWidth + gap)
        val color = themeColorForBand(settings.colorTheme, i, barCount)

        drawRect(
            color = color,
            topLeft = Offset(x, baseline - barHeight),
            size = Size(barWidth, barHeight),
        )
        if (settings.mirror) {
            drawRect(
                color = color.copy(alpha = 0.45f),
                topLeft = Offset(x, baseline),
                size = Size(barWidth, barHeight),
            )
        }
    }
}
