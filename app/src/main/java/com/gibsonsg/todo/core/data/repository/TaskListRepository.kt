package com.gibsonsg.todo.core.data.repository

import com.gibsonsg.todo.core.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface TaskListRepository {
    fun observeActiveLists(): Flow<List<TaskList>>
    suspend fun createList(name: String, colorArgb: Int): TaskList
    suspend fun archiveList(id: String)
}
