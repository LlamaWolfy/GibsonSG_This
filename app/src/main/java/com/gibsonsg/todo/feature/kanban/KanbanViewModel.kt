package com.gibsonsg.todo.feature.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gibsonsg.todo.core.data.repository.TaskRepository
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KanbanUiState(
    val columns: Map<TaskStatus, List<Task>> = emptyMap()
)

@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val uiState: StateFlow<KanbanUiState> = taskRepository.observeActiveTasks()
        .map { tasks -> KanbanUiState(columns = TaskStatus.entries.associateWith { status -> tasks.filter { it.status == status } }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KanbanUiState())

    fun moveTask(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
            taskRepository.setStatus(taskId, newStatus)
        }
    }
}
