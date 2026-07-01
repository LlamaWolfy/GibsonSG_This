package com.gibsonsg.todo.feature.inkoverlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gibsonsg.todo.core.domain.model.Stroke
import com.gibsonsg.todo.core.domain.model.StrokePoint

/**
 * Transparent drawing layer meant to sit above board content (e.g. a calendar day, a task
 * list) in a Box. Only S-Pen / stylus input (MotionEvent.TOOL_TYPE_STYLUS, surfaced by
 * Compose as PointerType.Stylus) is captured and consumed while ink mode is active; finger
 * touches are left unconsumed so they fall through to the underlying scroll/tap content -
 * this is the palm-rejection / non-stylus-pass-through mechanism. When ink mode is off, the
 * overlay still renders committed strokes (read-only) but intercepts nothing.
 */
@Composable
fun InkOverlay(
    committedStrokes: List<Stroke>,
    state: InkOverlayState,
    onStrokeCompleted: (Stroke) -> Unit,
    modifier: Modifier = Modifier,
    strokeColorArgb: Long = DEFAULT_STROKE_COLOR_ARGB,
    strokeWidthDp: Float = DEFAULT_STROKE_WIDTH_DP
) {
    val density = LocalDensity.current
    val baseWidthPx = with(density) { strokeWidthDp.dp.toPx() }

    Canvas(
        modifier = modifier.pointerInput(state.isInkModeActive) {
            if (!state.isInkModeActive) return@pointerInput

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.type != PointerType.Stylus) {
                    // Not a stylus: leave unconsumed so finger scroll/tap still works.
                    return@awaitEachGesture
                }

                down.consume()
                val strokeStartMs = System.currentTimeMillis()
                state.beginStroke(down.position.toStrokePoint(down.pressure, 0L))

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        change.consume()
                        break
                    }
                    change.consume()
                    state.appendPoint(
                        change.position.toStrokePoint(change.pressure, System.currentTimeMillis() - strokeStartMs)
                    )
                }

                if (state.currentStrokePoints.isNotEmpty()) {
                    val stroke = buildLiveStroke(
                        points = state.currentStrokePoints.toList(),
                        colorArgb = strokeColorArgb,
                        widthDp = strokeWidthDp
                    )
                    onStrokeCompleted(stroke)
                }
                state.clearCurrentStroke()
            }
        }
    ) {
        drawStrokes(committedStrokes, baseWidthPx)
        if (state.isDrawing) {
            val liveStroke = buildLiveStroke(state.currentStrokePoints.toList(), strokeColorArgb, strokeWidthDp)
            drawStroke(liveStroke, baseWidthPx)
        }
    }
}

private fun androidx.compose.ui.geometry.Offset.toStrokePoint(pressure: Float, tOffsetMs: Long) =
    StrokePoint(x = x, y = y, pressure = pressure, tOffsetMs = tOffsetMs)

private const val DEFAULT_STROKE_COLOR_ARGB = 0xFF1A1C1EL
private const val DEFAULT_STROKE_WIDTH_DP = 3f
