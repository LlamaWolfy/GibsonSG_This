package com.gibsonsg.todo.feature.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gibsonsg.todo.core.data.repository.TaskRepository
import com.gibsonsg.todo.core.domain.model.TaskStatus
import com.gibsonsg.todo.core.domain.usecase.GetTasksForSmartListUseCase
import com.gibsonsg.todo.core.domain.usecase.SmartList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val getTasksForSmartList: GetTasksForSmartListUseCase
) : ViewModel() {

    private val selectedSmartList = MutableStateFlow(SmartList.ALL)

    val uiState: StateFlow<TaskListUiState> = combine(
        taskRepository.observeActiveTasks(),
        selectedSmartList
    ) { tasks, smartList ->
        TaskListUiState(
            tasks = getTasksForSmartList(tasks, smartList),
            selectedSmartList = smartList,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListUiState()
    )

    fun selectSmartList(smartList: SmartList) {
        selectedSmartList.value = smartList
    }

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.createTask(title = title.trim())
        }
    }

    fun toggleDone(taskId: String, isCurrentlyDone: Boolean) {
        viewModelScope.launch {
            taskRepository.setStatus(taskId, if (isCurrentlyDone) TaskStatus.TODO else TaskStatus.DONE)
        }
    }

    fun archiveTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.archiveTask(taskId)
        }
    }
}
