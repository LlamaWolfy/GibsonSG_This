package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A retro analog VU meter: a spring-damped needle sweeping across a dial in
 * response to overall signal level, with a brief peak-hold marker.
 */
@Composable
fun VuNeedleVisualizer(
    spectrum: FloatArray,
    settings: VisualizerSettings,
    modifier: Modifier = Modifier,
) {
    var needleAngle by remember { mutableFloatStateOf(MIN_ANGLE) }
    var needleVelocity by remember { mutableFloatStateOf(0f) }
    var peakAngle by remember { mutableFloatStateOf(MIN_ANGLE) }
    var peakHoldTime by remember { mutableFloatStateOf(0f) }
    val latestSpectrum = rememberUpdatedState(spectrum)

    LaunchedEffect(Unit) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = now

                val level = latestSpectrum.value.average().toFloat().coerceIn(0f, 1f)
                val targetAngle = MIN_ANGLE + level * (MAX_ANGLE - MIN_ANGLE)

                // Critically-damped spring toward target for a natural swing.
                val stiffness = 220f
                val damping = 24f
                val accel = (targetAngle - needleAngle) * stiffness - needleVelocity * damping
                needleVelocity += accel * dt
                needleAngle += needleVelocity * dt

                if (needleAngle >= peakAngle) {
                    peakAngle = needleAngle
                    peakHoldTime = 0.8f
                } else {
                    peakHoldTime -= dt
                    if (peakHoldTime <= 0f) {
                        peakAngle = (peakAngle - dt * 0.6f).coerceAtLeast(needleAngle)
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height * 0.82f)
        val radius = min(size.width, size.height) * 0.55f
        val accent = themeAccent(settings.colorTheme)

        drawArc(
            color = accent.copy(alpha = 0.25f),
            startAngle = angleDeg(MIN_ANGLE),
            sweepAngle = angleDeg(MAX_ANGLE) - angleDeg(MIN_ANGLE),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 6f),
        )

        drawNeedle(center, radius, peakAngle, Color.Red.copy(alpha = 0.6f), width = 4f)
        drawNeedle(center, radius, needleAngle, accent, width = 7f)

        drawCircle(color = accent, radius = 10f, center = center)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeedle(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color,
    width: Float,
) {
    val end = Offset(
        center.x + cos(angle) * radius,
        center.y + sin(angle) * radius,
    )
    drawLine(color = color, start = center, end = end, strokeWidth = width)
}

private fun angleDeg(radians: Float): Float = radians * 180f / PI.toFloat()

// Needle pivots at the bottom center and sweeps through the top: -150deg
// (up-left, quiet) to -30deg (up-right, loud), passing through straight up.
private val MIN_ANGLE = Math.toRadians(-150.0).toFloat()
private val MAX_ANGLE = Math.toRadians(-30.0).toFloat()
