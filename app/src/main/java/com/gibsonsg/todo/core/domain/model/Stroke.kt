package com.gibsonsg.todo.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val tOffsetMs: Long
)

@Serializable
data class Stroke(
    val points: List<StrokePoint>,
    val colorArgb: Long,
    val widthDp: Float
) {
    val firstPoint: StrokePoint? get() = points.firstOrNull()
    val lastPoint: StrokePoint? get() = points.lastOrNull()

    val boundsLeft: Float get() = points.minOf { it.x }
    val boundsRight: Float get() = points.maxOf { it.x }
    val boundsTop: Float get() = points.minOf { it.y }
    val boundsBottom: Float get() = points.maxOf { it.y }
    val boundsWidth: Float get() = boundsRight - boundsLeft
    val boundsHeight: Float get() = boundsBottom - boundsTop

    /** Sum of the Euclidean distance between consecutive points. */
    val pathLength: Float
        get() = points.zipWithNext().sumOf { (a, b) ->
            val dx = (b.x - a.x).toDouble()
            val dy = (b.y - a.y).toDouble()
            kotlin.math.sqrt(dx * dx + dy * dy)
        }.toFloat()
}
