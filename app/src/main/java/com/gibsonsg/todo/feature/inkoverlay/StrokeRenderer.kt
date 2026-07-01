package com.gibsonsg.todo.feature.inkoverlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.gibsonsg.todo.core.domain.model.Stroke
import com.gibsonsg.todo.core.domain.model.StrokePoint

private const val PRESSURE_WIDTH_FLOOR = 0.5f

/**
 * Renders a stroke as a sequence of connected line segments, each segment's width derived
 * from the average pressure of its two endpoints. Simpler and fast enough for real-time
 * rendering than building a single variable-width filled Path outline.
 */
fun DrawScope.drawStroke(stroke: Stroke, baseWidthPx: Float) {
    val color = Color(stroke.colorArgb)
    val points = stroke.points
    if (points.size < 2) {
        points.firstOrNull()?.let { point ->
            drawCircle(
                color = color,
                radius = baseWidthPx * (PRESSURE_WIDTH_FLOOR + point.pressure) / 2f,
                center = Offset(point.x, point.y)
            )
        }
        return
    }

    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        val segmentWidth = baseWidthPx * (PRESSURE_WIDTH_FLOOR + (a.pressure + b.pressure) / 2f)
        drawLine(
            color = color,
            start = Offset(a.x, a.y),
            end = Offset(b.x, b.y),
            strokeWidth = segmentWidth,
            cap = StrokeCap.Round
        )
    }
}

fun DrawScope.drawStrokes(strokes: List<Stroke>, baseWidthPx: Float) {
    strokes.forEach { drawStroke(it, baseWidthPx) }
}

fun buildLiveStroke(points: List<StrokePoint>, colorArgb: Long, widthDp: Float): Stroke =
    Stroke(points = points, colorArgb = colorArgb, widthDp = widthDp)
