package com.gibsonsg.todo.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gibsonsg.todo.feature.calendar.component.DayDetailSheet
import com.gibsonsg.todo.feature.calendar.component.MonthGrid
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarRoute(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDayStrokes by viewModel.selectedDayStrokes.collectAsState()

    CalendarScreen(
        uiState = uiState,
        selectedDate = selectedDate,
        selectedDayStrokes = selectedDayStrokes,
        onPreviousMonth = viewModel::goToPreviousMonth,
        onNextMonth = viewModel::goToNextMonth,
        onToday = viewModel::goToToday,
        onDaySelected = viewModel::selectDay,
        onStrokeCompleted = viewModel::onDayStrokeCompleted,
        onTaskCompleted = viewModel::completeTask,
        onTaskUncompleted = viewModel::uncompleteTask
    )
}

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    selectedDate: LocalDate?,
    selectedDayStrokes: List<com.gibsonsg.todo.core.domain.model.Stroke>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onDaySelected: (LocalDate?) -> Unit,
    onStrokeCompleted: (com.gibsonsg.todo.core.domain.model.Stroke) -> Unit,
    onTaskCompleted: (String) -> Unit,
    onTaskUncompleted: (String) -> Unit
) {
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = DateTimeFormatter.ofPattern("MMMM yyyy").format(uiState.yearMonth),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }
        TextButton(onClick = onToday) { Text("Today") }

        MonthGrid(
            days = uiState.days,
            today = today,
            tasksByDate = uiState.tasksByDate,
            datesWithInk = uiState.datesWithInk,
            onDayTap = { date -> onDaySelected(date) },
            onDayLongPress = { date -> onDaySelected(date) },
            modifier = Modifier.fillMaxSize()
        )
    }

    if (selectedDate != null) {
        DayDetailSheet(
            date = selectedDate,
            tasks = uiState.tasksByDate[selectedDate].orEmpty(),
            committedStrokes = selectedDayStrokes,
            onStrokeCompleted = onStrokeCompleted,
            onTaskCompleted = onTaskCompleted,
            onTaskUncompleted = onTaskUncompleted,
            onDismiss = { onDaySelected(null) }
        )
    }
}
