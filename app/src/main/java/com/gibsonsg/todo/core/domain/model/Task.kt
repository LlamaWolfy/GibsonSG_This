package com.gibsonsg.todo.core.domain.model

import java.time.Instant

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val dueAt: Instant?,
    val priority: Priority,
    val listId: String?,
    val status: TaskStatus,
    val isArchived: Boolean,
    val archivedAt: Instant?,
    val recurrenceRule: RecurrenceRule?,
    val recurrenceParentId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant?,
    val sortOrder: Long,
    val subtasks: List<Subtask> = emptyList()
) {
    val isDone: Boolean get() = status == TaskStatus.DONE
}
