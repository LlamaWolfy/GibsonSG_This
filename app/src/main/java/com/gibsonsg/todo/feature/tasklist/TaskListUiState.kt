package com.gibsonsg.todo.feature.tasklist

import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.usecase.SmartList

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val selectedSmartList: SmartList = SmartList.ALL,
    val isLoading: Boolean = true
)
