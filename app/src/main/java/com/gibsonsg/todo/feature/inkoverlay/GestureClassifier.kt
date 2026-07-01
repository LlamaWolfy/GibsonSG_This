package com.gibsonsg.todo.feature.inkoverlay

import androidx.compose.ui.geometry.Rect
import com.gibsonsg.todo.core.domain.model.Stroke
import com.gibsonsg.todo.core.domain.usecase.DetectStrikeThroughUseCase
import com.gibsonsg.todo.core.domain.usecase.StrikeThroughResult
import com.gibsonsg.todo.core.domain.usecase.TaskRowBounds

sealed interface InkGestureResult {
    data class CompletesTask(val taskId: String) : InkGestureResult
    data object Annotation : InkGestureResult
}

/**
 * Bridges Compose geometry (row bounds tracked via onGloballyPositioned, in the ink
 * overlay's own local coordinate space) into the pure [DetectStrikeThroughUseCase]. A
 * stroke only ever becomes a gesture candidate if its bounding box overlaps exactly one
 * tracked task row - anything else (zero or multiple rows) is always treated as plain ink.
 */
class GestureClassifier(
    private val detectStrikeThrough: DetectStrikeThroughUseCase = DetectStrikeThroughUseCase()
) {
    fun classify(stroke: Stroke, rowBoundsByTaskId: Map<String, Rect>): InkGestureResult {
        val strokeBounds = Rect(
            left = stroke.boundsLeft,
            top = stroke.boundsTop,
            right = stroke.boundsRight,
            bottom = stroke.boundsBottom
        )

        val overlapping = rowBoundsByTaskId.filterValues { it.overlaps(strokeBounds) }
        if (overlapping.size != 1) return InkGestureResult.Annotation

        val (taskId, rowRect) = overlapping.entries.first()
        val rowBounds = TaskRowBounds(
            left = rowRect.left,
            top = rowRect.top,
            width = rowRect.width,
            height = rowRect.height
        )

        return when (detectStrikeThrough(stroke, rowBounds)) {
            StrikeThroughResult.StrikeThrough -> InkGestureResult.CompletesTask(taskId)
            StrikeThroughResult.NotStrikeThrough -> InkGestureResult.Annotation
        }
    }
}
