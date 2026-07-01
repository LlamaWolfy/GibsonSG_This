package com.gibsonsg.todo.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index("listId"),
        Index("isArchived"),
        Index("dueAt"),
        Index("status")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val dueAt: Long?,
    val priority: Int,
    val listId: String?,
    val status: Int,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val recurrenceRule: String? = null,
    val recurrenceParentId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val sortOrder: Long
)
