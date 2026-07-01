package com.gibsonsg.todo.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_lists")
data class TaskListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int,
    val isArchived: Boolean = false
)
