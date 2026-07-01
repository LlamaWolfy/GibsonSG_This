package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CircularPulseVisualizer(
    spectrum: FloatArray,
    settings: VisualizerSettings,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = min(size.width, size.height) * 0.22f
        val maxExtra = min(size.width, size.height) * 0.28f

        val barCount = max(16, (spectrum.size * settings.density).roundToInt()).coerceAtMost(spectrum.size)
        val step = spectrum.size / barCount

        drawCircle(
            color = themeAccent(settings.colorTheme).copy(alpha = 0.25f),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 3f),
        )

        for (i in 0 until barCount) {
            val level = spectrum[(i * step).coerceAtMost(spectrum.size - 1)]
            val angle = 2.0 * PI * i / barCount - PI / 2
            val innerR = baseRadius
            val outerR = baseRadius + level * maxExtra
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()

            val from = Offset(center.x + cosA * innerR, center.y + sinA * innerR)
            val to = Offset(center.x + cosA * outerR, center.y + sinA * outerR)
            val color = themeColorForBand(settings.colorTheme, i, barCount)

            drawLine(color = color, start = from, end = to, strokeWidth = 5f)
        }
    }
}
