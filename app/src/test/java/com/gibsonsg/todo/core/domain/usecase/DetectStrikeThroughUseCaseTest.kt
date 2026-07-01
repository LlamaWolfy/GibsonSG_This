package com.gibsonsg.todo.core.domain.usecase

import com.gibsonsg.todo.core.domain.model.Stroke
import com.gibsonsg.todo.core.domain.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectStrikeThroughUseCaseTest {

    private val useCase = DetectStrikeThroughUseCase()

    // A typical task row: 200pt wide, 60pt tall, top-left at the origin.
    private val rowBounds = TaskRowBounds(left = 0f, top = 0f, width = 200f, height = 60f)

    private fun stroke(points: List<StrokePoint>) = Stroke(points = points, colorArgb = 0xFF000000, widthDp = 3f)

    private fun pt(x: Float, y: Float, t: Long) = StrokePoint(x = x, y = y, pressure = 0.5f, tOffsetMs = t)

    @Test
    fun `clean horizontal swipe across the row is detected as strike-through`() {
        val points = (0..20).map { i ->
            val x = 20f + i * 8.5f // 20 -> ~190
            val y = 28f + (i % 2) * 2f // tiny 2pt wobble, stays near the middle
            pt(x, y, i * 5L)
        }
        val result = useCase(stroke(points), rowBounds)
        assertEquals(StrikeThroughResult.StrikeThrough, result)
    }

    @Test
    fun `circle is not a strike-through - fails the flat aspect-ratio check`() {
        val center = 30f
        val radius = 85f
        val points = (0..36).map { i ->
            val angle = Math.toRadians((i * 10).toDouble())
            pt(
                x = (100 + radius * kotlin.math.cos(angle)).toFloat(),
                y = (center + radius * kotlin.math.sin(angle)).toFloat(),
                t = i * 10L
            )
        }
        val result = useCase(stroke(points), rowBounds)
        assertEquals(StrikeThroughResult.NotStrikeThrough, result)
    }

    @Test
    fun `short scribble is not a strike-through - fails width coverage`() {
        val points = listOf(
            pt(90f, 28f, 0L),
            pt(95f, 32f, 10L),
            pt(100f, 27f, 20L),
            pt(97f, 30f, 30L)
        )
        val result = useCase(stroke(points), rowBounds)
        assertEquals(StrikeThroughResult.NotStrikeThrough, result)
    }

    @Test
    fun `vertical line is not a strike-through`() {
        val points = (0..10).map { i -> pt(100f, 5f + i * 5f, i * 10L) }
        val result = useCase(stroke(points), rowBounds)
        assertEquals(StrikeThroughResult.NotStrikeThrough, result)
    }

    @Test
    fun `wide zigzag scribble is not a strike-through - fails the single-pass check`() {
        // Net motion is left-to-right across the full row, but heavy up-down
        // oscillation on the way inflates path length relative to displacement.
        val points = (0..40).map { i ->
            val x = 20f + i * 4.25f // 20 -> ~190
            val y = if (i % 2 == 0) 12f else 48f
            pt(x, y, i * 5L)
        }
        val result = useCase(stroke(points), rowBounds)
        assertEquals(StrikeThroughResult.NotStrikeThrough, result)
    }

    @Test
    fun `stroke entirely outside the row vertically is not a strike-through`() {
        val points = (0..10).map { i -> pt(20f + i * 17f, 500f, i * 10L) }
        val result = useCase(stroke(points), rowBounds)
        assertEquals(StrikeThroughResult.NotStrikeThrough, result)
    }

    @Test
    fun `stroke covering only 40 percent of row width is not a strike-through`() {
        val points = (0..10).map { i -> pt(20f + i * 6f, 28f + (i % 2), i * 10L) }
        val result = useCase(stroke(points), rowBounds)
        assertEquals(StrikeThroughResult.NotStrikeThrough, result)
    }

    @Test
    fun `single point stroke is never a strike-through`() {
        val result = useCase(stroke(listOf(pt(100f, 30f, 0L))), rowBounds)
        assertEquals(StrikeThroughResult.NotStrikeThrough, result)
    }

    @Test
    fun `right-to-left swipe is also detected as strike-through`() {
        val points = (0..20).map { i ->
            val x = 190f - i * 8.5f // 190 -> ~20
            val y = 30f
            pt(x, y, i * 5L)
        }
        val result = useCase(stroke(points), rowBounds)
        assertEquals(StrikeThroughResult.StrikeThrough, result)
    }
}
