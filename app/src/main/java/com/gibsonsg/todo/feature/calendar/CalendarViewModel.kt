package com.gibsonsg.todo.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gibsonsg.todo.core.data.repository.InkRepository
import com.gibsonsg.todo.core.data.repository.TaskRepository
import com.gibsonsg.todo.core.domain.model.AnchorType
import com.gibsonsg.todo.core.domain.model.Stroke
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.model.TaskStatus
import com.gibsonsg.todo.core.domain.usecase.CalendarDay
import com.gibsonsg.todo.core.domain.usecase.GenerateMonthGridUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val days: List<CalendarDay> = emptyList(),
    val tasksByDate: Map<LocalDate, List<Task>> = emptyMap(),
    val datesWithInk: Set<LocalDate> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val inkRepository: InkRepository,
    private val generateMonthGrid: GenerateMonthGridUseCase
) : ViewModel() {

    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val zone = ZoneId.systemDefault()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    val uiState: StateFlow<CalendarUiState> = combine(
        visibleMonth,
        taskRepository.observeActiveTasks(),
        inkRepository.observeDatesWithInk()
    ) { month, tasks, datesWithInk ->
        val tasksByDate = tasks
            .filter { it.dueAt != null }
            .groupBy { it.dueAt!!.atZone(zone).toLocalDate() }
        CalendarUiState(
            yearMonth = month,
            days = generateMonthGrid(month),
            tasksByDate = tasksByDate,
            datesWithInk = datesWithInk
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    val selectedDayStrokes: StateFlow<List<Stroke>> = _selectedDate
        .flatMapLatest { date ->
            if (date == null) {
                flowOf(emptyList())
            } else {
                inkRepository.observePagesForAnchor(AnchorType.DAY, date.toString())
                    .map { pages -> pages.firstOrNull()?.strokes.orEmpty() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun goToPreviousMonth() {
        visibleMonth.value = visibleMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        visibleMonth.value = visibleMonth.value.plusMonths(1)
    }

    fun goToToday() {
        visibleMonth.value = YearMonth.now()
    }

    fun selectDay(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun onDayStrokeCompleted(stroke: Stroke) {
        val date = _selectedDate.value ?: return
        viewModelScope.launch {
            inkRepository.appendStrokeToDayPage(date, stroke)
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch { taskRepository.setStatus(taskId, TaskStatus.DONE) }
    }

    fun uncompleteTask(taskId: String) {
        viewModelScope.launch { taskRepository.setStatus(taskId, TaskStatus.TODO) }
    }
}
