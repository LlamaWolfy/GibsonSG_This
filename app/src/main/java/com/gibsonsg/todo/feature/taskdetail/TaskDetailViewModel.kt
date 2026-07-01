package com.gibsonsg.todo.feature.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gibsonsg.todo.core.data.repository.InkRepository
import com.gibsonsg.todo.core.data.repository.TaskListRepository
import com.gibsonsg.todo.core.data.repository.TaskRepository
import com.gibsonsg.todo.core.domain.model.AnchorType
import com.gibsonsg.todo.core.domain.model.InkPage
import com.gibsonsg.todo.core.domain.model.Priority
import com.gibsonsg.todo.core.domain.model.RecurrenceRule
import com.gibsonsg.todo.core.domain.model.Stroke
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.model.TaskList
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val inkRepository: InkRepository,
    taskListRepository: TaskListRepository
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    val task: StateFlow<Task?> = taskRepository.observeTask(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val availableLists: StateFlow<List<TaskList>> = taskListRepository.observeActiveLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    val inkPages: StateFlow<List<InkPage>> = inkRepository.observePagesForAnchor(AnchorType.TASK, taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    // Two distinct concepts on purpose: a freshly-started session is writable, while a
    // history page opened for viewing is always read-only, even if its id happens to be
    // reused - old notes must never become silently editable just by opening them.
    private val _activeWritingPageId = MutableStateFlow<String?>(null)
    val activeWritingPageId: StateFlow<String?> = _activeWritingPageId

    private val _viewingHistoryPageId = MutableStateFlow<String?>(null)
    val viewingHistoryPageId: StateFlow<String?> = _viewingHistoryPageId

    fun updateTitle(title: String) = updateCurrent { it.copy(title = title) }

    fun updateDescription(description: String) = updateCurrent { it.copy(description = description) }

    fun updateDueAt(dueAt: Instant?) = updateCurrent { it.copy(dueAt = dueAt) }

    fun updatePriority(priority: Priority) = updateCurrent { it.copy(priority = priority) }

    fun updateListId(listId: String?) = updateCurrent { it.copy(listId = listId) }

    fun updateRecurrence(recurrenceRule: RecurrenceRule?) = updateCurrent { it.copy(recurrenceRule = recurrenceRule) }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { taskRepository.addSubtask(taskId, title.trim()) }
    }

    fun setSubtaskDone(subtaskId: String, isDone: Boolean) {
        viewModelScope.launch { taskRepository.setSubtaskDone(subtaskId, isDone) }
    }

    fun deleteSubtask(subtaskId: String) {
        viewModelScope.launch { taskRepository.deleteSubtask(subtaskId) }
    }

    fun archiveTask() {
        viewModelScope.launch { taskRepository.archiveTask(taskId) }
    }

    /** Always starts a brand-new page - a task's ink history is a stack of sessions, never one merged canvas. */
    fun startNewNote() {
        viewModelScope.launch {
            val page = inkRepository.startNewTaskPage(taskId)
            _activeWritingPageId.value = page.id
        }
    }

    fun openHistoryPage(pageId: String) {
        _viewingHistoryPageId.value = pageId
    }

    fun closeNote() {
        _activeWritingPageId.value = null
        _viewingHistoryPageId.value = null
    }

    fun onNoteStrokeCompleted(stroke: Stroke) {
        val pageId = _activeWritingPageId.value ?: return
        viewModelScope.launch { inkRepository.appendStrokeToPage(pageId, stroke) }
    }

    private fun updateCurrent(transform: (Task) -> Task) {
        val current = task.value ?: return
        viewModelScope.launch { taskRepository.updateTask(transform(current)) }
    }
}
