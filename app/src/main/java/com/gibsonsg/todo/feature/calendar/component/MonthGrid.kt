package com.gibsonsg.todo.feature.calendar.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.usecase.CalendarDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val WEEKDAY_LABELS = (0 until 7).map { offset ->
    DayOfWeek.SUNDAY.plus(offset.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

@Composable
fun MonthGrid(
    days: List<CalendarDay>,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    datesWithInk: Set<LocalDate>,
    onDayTap: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        date = day.date,
                        isInCurrentMonth = day.isInCurrentMonth,
                        isToday = day.date == today,
                        tasks = tasksByDate[day.date].orEmpty(),
                        hasInk = day.date in datesWithInk,
                        onTap = { onDayTap(day.date) },
                        onLongPress = { onDayLongPress(day.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
