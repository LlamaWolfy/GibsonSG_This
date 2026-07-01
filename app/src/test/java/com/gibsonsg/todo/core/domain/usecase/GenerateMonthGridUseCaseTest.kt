package com.gibsonsg.todo.core.domain.usecase

import java.time.DayOfWeek
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateMonthGridUseCaseTest {

    private val useCase = GenerateMonthGridUseCase()

    @Test
    fun `grid always has exactly 42 days`() {
        val grid = useCase(YearMonth.of(2026, 7))
        assertEquals(42, grid.size)
    }

    @Test
    fun `grid is contiguous with no gaps or duplicates`() {
        val grid = useCase(YearMonth.of(2026, 7))
        for (i in 1 until grid.size) {
            assertEquals(grid[i - 1].date.plusDays(1), grid[i].date)
        }
    }

    @Test
    fun `every day of the target month is present exactly once`() {
        val month = YearMonth.of(2026, 7)
        val grid = useCase(month)
        val daysInMonth = grid.filter { it.isInCurrentMonth }.map { it.date }

        assertEquals(month.lengthOfMonth(), daysInMonth.size)
        assertEquals(month.atDay(1), daysInMonth.first())
        assertEquals(month.atEndOfMonth(), daysInMonth.last())
    }

    @Test
    fun `first row starts on the configured first day of week`() {
        val grid = useCase(YearMonth.of(2026, 7), firstDayOfWeek = DayOfWeek.MONDAY)
        assertEquals(DayOfWeek.MONDAY, grid.first().date.dayOfWeek)
    }

    @Test
    fun `default first day of week is Sunday`() {
        val grid = useCase(YearMonth.of(2026, 7))
        assertEquals(DayOfWeek.SUNDAY, grid.first().date.dayOfWeek)
    }

    @Test
    fun `February in a non-leap year is handled without gaps`() {
        val grid = useCase(YearMonth.of(2026, 2))
        val febDays = grid.filter { it.isInCurrentMonth }
        assertEquals(28, febDays.size)
        assertTrue(febDays.maxOf { it.date.dayOfMonth } == 28)
    }

    @Test
    fun `leap year February has 29 days in the grid`() {
        val grid = useCase(YearMonth.of(2028, 2))
        val febDays = grid.filter { it.isInCurrentMonth }
        assertEquals(29, febDays.size)
    }

    @Test
    fun `year boundary December to January is contiguous`() {
        val grid = useCase(YearMonth.of(2025, 12))
        for (i in 1 until grid.size) {
            assertEquals(grid[i - 1].date.plusDays(1), grid[i].date)
        }
        val decDays = grid.filter { it.isInCurrentMonth }
        assertEquals(31, decDays.size)
    }
}
