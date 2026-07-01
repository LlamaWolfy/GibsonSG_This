package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun PolarRingVisualizer(
    waveform: FloatArray,
    settings: VisualizerSettings,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (waveform.isEmpty()) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = min(size.width, size.height) * 0.28f
        val amplitude = baseRadius * 0.6f * settings.density.coerceIn(0.4f, 2f)
        val accent = themeAccent(settings.colorTheme)

        val path = Path()
        for (i in waveform.indices) {
            val angle = 2.0 * PI * i / waveform.size - PI / 2
            val r = baseRadius + waveform[i] * amplitude
            val x = center.x + cos(angle).toFloat() * r
            val y = center.y + sin(angle).toFloat() * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0f)),
                center = center,
                radius = (baseRadius + amplitude).coerceAtLeast(1f),
            ),
        )

        drawCircle(
            color = accent.copy(alpha = 0.15f),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 2f),
        )

        drawGlowPath(color = accent, path = path, coreWidth = 4f, glowWidth = 16f)
    }
}
