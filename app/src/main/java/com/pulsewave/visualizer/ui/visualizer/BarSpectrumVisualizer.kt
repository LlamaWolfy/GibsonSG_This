package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pulsewave.visualizer.audio.BAND_COUNT
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BarSpectrumVisualizer(
    spectrum: FloatArray,
    settings: VisualizerSettings,
    modifier: Modifier = Modifier,
) {
    val peaks = remember { FloatArray(BAND_COUNT) }
    Canvas(modifier = modifier) {
        drawBarSpectrum(spectrum, settings, peaks)
    }
}

private const val PEAK_DECAY_PER_FRAME = 0.02f

internal fun DrawScope.drawBarSpectrum(spectrum: FloatArray, settings: VisualizerSettings, peaks: FloatArray) {
    val barCount = max(8, (spectrum.size * settings.density).roundToInt()).coerceAtMost(spectrum.size)
    val step = spectrum.size / barCount
    val gap = size.width * 0.006f
    val barWidth = (size.width / barCount) - gap
    val corner = CornerRadius(barWidth * 0.4f, barWidth * 0.4f)
    val baseline = if (settings.mirror) size.height / 2f else size.height
    val maxBarHeight = if (settings.mirror) size.height / 2f else size.height

    for (i in 0 until barCount) {
        val level = spectrum[(i * step).coerceAtMost(spectrum.size - 1)]

        val peakIndex = i.coerceAtMost(peaks.size - 1)
        peaks[peakIndex] = if (level >= peaks[peakIndex]) {
            level
        } else {
            (peaks[peakIndex] - PEAK_DECAY_PER_FRAME).coerceAtLeast(level)
        }

        // Coerced to a minimum so the gradient never degenerates to a
        // zero-length start/end (which throws on silence, level == 0).
        val barHeight = (level * maxBarHeight).coerceAtLeast(1f)
        val x = i * (barWidth + gap)
        val color = themeColorForBand(settings.colorTheme, i, barCount)
        val brush = Brush.verticalGradient(
            colors = listOf(color, color.copy(alpha = 0.35f)),
            startY = baseline - barHeight,
            endY = baseline,
        )

        drawRoundRect(
            brush = brush,
            topLeft = Offset(x, baseline - barHeight),
            size = Size(barWidth, barHeight),
            cornerRadius = corner,
        )
        if (settings.mirror) {
            val mirrorBrush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.05f)),
                startY = baseline,
                endY = baseline + barHeight,
            )
            drawRoundRect(
                brush = mirrorBrush,
                topLeft = Offset(x, baseline),
                size = Size(barWidth, barHeight),
                cornerRadius = corner,
            )
        }

        val peakHeight = peaks[peakIndex] * maxBarHeight
        val capThickness = (barWidth * 0.28f).coerceIn(2f, 6f)
        drawRoundRect(
            color = color,
            topLeft = Offset(x, (baseline - peakHeight - capThickness).coerceAtMost(baseline - capThickness)),
            size = Size(barWidth, capThickness),
            cornerRadius = CornerRadius(capThickness / 2f, capThickness / 2f),
        )
    }
}
