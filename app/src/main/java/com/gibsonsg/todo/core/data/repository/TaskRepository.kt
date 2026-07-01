package com.gibsonsg.todo.core.data.repository

import com.gibsonsg.todo.core.domain.model.Priority
import com.gibsonsg.todo.core.domain.model.RecurrenceRule
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.model.TaskStatus
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeActiveTasks(): Flow<List<Task>>
    fun observeActiveTasksForList(listId: String): Flow<List<Task>>
    fun observeActiveTasksByStatus(status: TaskStatus): Flow<List<Task>>
    fun searchActiveTasks(query: String): Flow<List<Task>>
    fun observeTask(taskId: String): Flow<Task?>
    suspend fun getTask(taskId: String): Task?

    suspend fun createTask(
        title: String,
        description: String? = null,
        dueAt: Instant? = null,
        priority: Priority = Priority.NONE,
        listId: String? = null,
        recurrenceRule: RecurrenceRule? = null
    ): Task

    suspend fun updateTask(task: Task)
    suspend fun setStatus(taskId: String, status: TaskStatus)
    suspend fun completeTask(taskId: String)
    suspend fun archiveTask(taskId: String)

    suspend fun addSubtask(taskId: String, title: String): com.gibsonsg.todo.core.domain.model.Subtask
    suspend fun setSubtaskDone(subtaskId: String, isDone: Boolean)
    suspend fun deleteSubtask(subtaskId: String)
}
