package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
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

    val strokeWidth = (3f + 4f * settings.density).coerceAtLeast(1f)
    drawPath(
        path = path,
        color = themeAccent(settings.colorTheme),
        style = Stroke(width = strokeWidth),
    )

    if (settings.mirror) {
        val mirrored = Path()
        for (i in waveform.indices) {
            val x = i * stepX
            val y = midY + waveform[i] * amplitude
            if (i == 0) mirrored.moveTo(x, y) else mirrored.lineTo(x, y)
        }
        drawPath(
            path = mirrored,
            color = themeAccent(settings.colorTheme, phase = 0.5f).copy(alpha = 0.5f),
            style = Stroke(width = strokeWidth * 0.7f),
        )
    }

    drawLine(
        color = themeAccent(settings.colorTheme).copy(alpha = 0.15f),
        start = Offset(0f, midY),
        end = Offset(size.width, midY),
    )
}
