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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A retro analog VU meter: a spring-damped needle sweeping across a dial in
 * response to overall signal level, with a brief peak-hold marker, tick
 * marks, and a green/yellow/red danger-zone arc.
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

        drawZoneArc(center, radius)
        drawTickMarks(center, radius)

        drawArc(
            color = accent.copy(alpha = 0.2f),
            startAngle = angleDeg(MIN_ANGLE),
            sweepAngle = angleDeg(MAX_ANGLE) - angleDeg(MIN_ANGLE),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 4f),
        )

        drawNeedleGlow(center, radius, peakAngle, Color.Red.copy(alpha = 0.55f), width = 3f)
        drawNeedleGlow(center, radius, needleAngle, accent, width = 6f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent, accent.copy(alpha = 0.3f)),
                center = center,
                radius = 16f,
            ),
            radius = 14f,
            center = center,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawZoneArc(center: Offset, radius: Float) {
    val zoneWidth = 10f
    val topLeft = Offset(center.x - radius, center.y - radius)
    val size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
    val totalSweep = angleDeg(MAX_ANGLE) - angleDeg(MIN_ANGLE)

    val zones = listOf(
        0f to 0.6f to Color(0xFF2ECC71),
        0.6f to 0.85f to Color(0xFFF1C40F),
        0.85f to 1f to Color(0xFFE74C3C),
    )
    for ((range, color) in zones) {
        val (from, to) = range
        drawArc(
            color = color.copy(alpha = 0.55f),
            startAngle = angleDeg(MIN_ANGLE) + totalSweep * from,
            sweepAngle = totalSweep * (to - from),
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = zoneWidth, cap = StrokeCap.Butt),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTickMarks(center: Offset, radius: Float) {
    val tickCount = 11
    for (i in 0 until tickCount) {
        val angle = MIN_ANGLE + (MAX_ANGLE - MIN_ANGLE) * i / (tickCount - 1)
        val major = i % 2 == 0
        val innerR = radius - (if (major) 18f else 12f)
        val outerR = radius - 4f
        val from = Offset(center.x + cos(angle) * innerR, center.y + sin(angle) * innerR)
        val to = Offset(center.x + cos(angle) * outerR, center.y + sin(angle) * outerR)
        drawLine(
            color = Color.White.copy(alpha = if (major) 0.6f else 0.35f),
            start = from,
            end = to,
            strokeWidth = if (major) 2.5f else 1.5f,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeedleGlow(
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
    drawGlowLine(color = color, start = center, end = end, coreWidth = width, glowWidth = width * 3.5f)
}

private fun angleDeg(radians: Float): Float = radians * 180f / PI.toFloat()

// Needle pivots at the bottom center and sweeps through the top: -150deg
// (up-left, quiet) to -30deg (up-right, loud), passing through straight up.
private val MIN_ANGLE = Math.toRadians(-150.0).toFloat()
private val MAX_ANGLE = Math.toRadians(-30.0).toFloat()
