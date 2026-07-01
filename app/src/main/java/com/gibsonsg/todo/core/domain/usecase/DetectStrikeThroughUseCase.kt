package com.gibsonsg.todo.core.domain.usecase

import com.gibsonsg.todo.core.domain.model.Stroke
import kotlin.math.abs

data class TaskRowBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

sealed interface StrikeThroughResult {
    data object StrikeThrough : StrikeThroughResult
    data object NotStrikeThrough : StrikeThroughResult
}

/**
 * Decides whether a completed stylus stroke drawn over a single task row is a deliberate
 * "cross this off" gesture (-> mark complete) versus ordinary ink annotation. Callers are
 * expected to have already confirmed the stroke's bounding box overlaps exactly one task's
 * row - a stroke spanning zero or multiple rows is never a candidate.
 *
 * All five signals below must agree (AND, not OR): false-positive completions are worse
 * than the rare missed strike-through (which the user can always finish via checkbox tap).
 */
class DetectStrikeThroughUseCase {

    operator fun invoke(stroke: Stroke, rowBounds: TaskRowBounds): StrikeThroughResult {
        if (stroke.points.size < 2) return StrikeThroughResult.NotStrikeThrough

        val strokeWidth = stroke.boundsWidth
        val strokeHeight = stroke.boundsHeight

        val coversRowWidth = rowBounds.width > 0f && strokeWidth >= MIN_WIDTH_COVERAGE * rowBounds.width
        val isFlatAndWide = strokeHeight <= 0f || strokeWidth / strokeHeight >= MIN_ASPECT_RATIO
        val isHorizontalSweep = isPredominantlyHorizontal(stroke, strokeWidth)
        val isVerticallyCentered = isInMiddleBand(stroke, rowBounds)
        val isSinglePass = isSingleConfidentPass(stroke)

        return if (coversRowWidth && isFlatAndWide && isHorizontalSweep && isVerticallyCentered && isSinglePass) {
            StrikeThroughResult.StrikeThrough
        } else {
            StrikeThroughResult.NotStrikeThrough
        }
    }

    private fun isPredominantlyHorizontal(stroke: Stroke, strokeWidth: Float): Boolean {
        val first = stroke.firstPoint ?: return false
        val last = stroke.lastPoint ?: return false
        if (strokeWidth <= 0f) return false
        val netHorizontalDisplacement = abs(last.x - first.x)
        return netHorizontalDisplacement >= MIN_HORIZONTAL_DISPLACEMENT_RATIO * strokeWidth
    }

    private fun isInMiddleBand(stroke: Stroke, rowBounds: TaskRowBounds): Boolean {
        if (rowBounds.height <= 0f) return false
        val averageY = stroke.points.map { it.y }.average().toFloat()
        val relativeY = (averageY - rowBounds.top) / rowBounds.height
        return relativeY in MIDDLE_BAND_MIN..MIDDLE_BAND_MAX
    }

    private fun isSingleConfidentPass(stroke: Stroke): Boolean {
        val first = stroke.firstPoint ?: return false
        val last = stroke.lastPoint ?: return false
        val straightLineDistance = kotlin.math.sqrt(
            (last.x - first.x) * (last.x - first.x) + (last.y - first.y) * (last.y - first.y)
        )
        if (straightLineDistance <= 0f) return false
        return stroke.pathLength / straightLineDistance <= MAX_PATH_LENGTH_RATIO
    }

    companion object {
        const val MIN_WIDTH_COVERAGE = 0.6f
        const val MIN_ASPECT_RATIO = 3.0f
        const val MIN_HORIZONTAL_DISPLACEMENT_RATIO = 0.7f
        const val MIDDLE_BAND_MIN = 0.25f
        const val MIDDLE_BAND_MAX = 0.75f
        const val MAX_PATH_LENGTH_RATIO = 1.8f
    }
}
