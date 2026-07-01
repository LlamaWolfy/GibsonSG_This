package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Cheap, API-independent "glow": draw the same shape a few times with
 * increasing width and decreasing alpha to fake a soft bloom, without
 * needing RenderEffect blur (API 31+ only). Used to make the thin,
 * single-stroke visualizer styles read as more polished/refined.
 */
fun DrawScope.drawGlowLine(color: Color, start: Offset, end: Offset, coreWidth: Float, glowWidth: Float) {
    drawLine(color.copy(alpha = color.alpha * 0.16f), start, end, glowWidth, cap = StrokeCap.Round)
    drawLine(color.copy(alpha = color.alpha * 0.35f), start, end, glowWidth * 0.5f, cap = StrokeCap.Round)
    drawLine(color, start, end, coreWidth, cap = StrokeCap.Round)
}

fun DrawScope.drawGlowPath(color: Color, path: Path, coreWidth: Float, glowWidth: Float) {
    drawPath(path, color.copy(alpha = color.alpha * 0.16f), style = Stroke(glowWidth, cap = StrokeCap.Round))
    drawPath(path, color.copy(alpha = color.alpha * 0.35f), style = Stroke(glowWidth * 0.5f, cap = StrokeCap.Round))
    drawPath(path, color, style = Stroke(coreWidth, cap = StrokeCap.Round))
}

fun DrawScope.drawGlowCircle(color: Color, center: Offset, radius: Float) {
    if (radius <= 0f) return
    drawCircle(color.copy(alpha = color.alpha * 0.16f), radius * 2.4f, center)
    drawCircle(color.copy(alpha = color.alpha * 0.35f), radius * 1.5f, center)
    drawCircle(color, radius, center)
}
