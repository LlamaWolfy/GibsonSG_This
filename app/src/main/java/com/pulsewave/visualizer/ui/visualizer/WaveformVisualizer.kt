package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pulsewave.visualizer.ui.settings.VisualizerSettings

@Composable
fun WaveformVisualizer(
    waveform: FloatArray,
    settings: VisualizerSettings,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawWaveform(waveform, settings)
    }
}

internal fun DrawScope.drawWaveform(waveform: FloatArray, settings: VisualizerSettings) {
    if (waveform.isEmpty()) return
    val midY = size.height / 2f
    val amplitude = size.height / 2f * 0.9f
    val stepX = size.width / (waveform.size - 1).coerceAtLeast(1)

    val path = Path()
    for (i in waveform.indices) {
        val x = i * stepX
        val y = midY - waveform[i] * amplitude
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    val coreWidth = (3f + 4f * settings.density).coerceAtLeast(1.5f)
    val brush = Brush.horizontalGradient(
        listOf(
            themeAccent(settings.colorTheme, phase = 0f),
            themeAccent(settings.colorTheme, phase = 0.5f),
            themeAccent(settings.colorTheme, phase = 1f),
        ),
    )

    // Glow pass (flat color, gradients don't matter for a soft halo).
    val glowColor = themeAccent(settings.colorTheme, phase = 0.5f)
    drawPath(path, glowColor.copy(alpha = 0.18f), style = Stroke(coreWidth * 3.2f, cap = StrokeCap.Round))
    drawPath(path, glowColor.copy(alpha = 0.35f), style = Stroke(coreWidth * 1.8f, cap = StrokeCap.Round))
    drawPath(path, brush = brush, style = Stroke(coreWidth, cap = StrokeCap.Round))

    if (settings.mirror) {
        val mirrored = Path()
        for (i in waveform.indices) {
            val x = i * stepX
            val y = midY + waveform[i] * amplitude
            if (i == 0) mirrored.moveTo(x, y) else mirrored.lineTo(x, y)
        }
        drawPath(
            path = mirrored,
            brush = brush,
            alpha = 0.4f,
            style = Stroke(width = coreWidth * 0.7f, cap = StrokeCap.Round),
        )
    }

    drawLine(
        color = glowColor.copy(alpha = 0.15f),
        start = Offset(0f, midY),
        end = Offset(size.width, midY),
    )
}
