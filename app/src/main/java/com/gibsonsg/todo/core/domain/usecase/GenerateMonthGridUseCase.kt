package com.gibsonsg.todo.core.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarDay(
    val date: LocalDate,
    val isInCurrentMonth: Boolean
)

/**
 * Builds a fixed 6-row x 7-col (42 day) grid for a month view, padded with the tail of
 * the previous month and the head of the next month so every week row is fully populated
 * - the standard calendar UI layout. Pure function of [yearMonth]/[firstDayOfWeek] so it's
 * cheap to unit test without touching any UI or database code.
 */
class GenerateMonthGridUseCase @Inject constructor() {
    operator fun invoke(
        yearMonth: YearMonth,
        firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY
    ): List<CalendarDay> {
        val firstOfMonth = yearMonth.atDay(1)

        // Distance (0-6) from firstDayOfWeek back to the start of firstOfMonth's week.
        val leadingOffset = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val gridStart = firstOfMonth.minusDays(leadingOffset.toLong())

        return (0 until GRID_SIZE).map { offset ->
            val date = gridStart.plusDays(offset.toLong())
            CalendarDay(date = date, isInCurrentMonth = YearMonth.from(date) == yearMonth)
        }
    }

    companion object {
        private const val WEEKS = 6
        private const val DAYS_PER_WEEK = 7
        const val GRID_SIZE = WEEKS * DAYS_PER_WEEK
    }
}
