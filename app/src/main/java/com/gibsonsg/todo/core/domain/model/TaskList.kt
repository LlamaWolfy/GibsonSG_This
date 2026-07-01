package com.gibsonsg.todo.core.domain.model

data class TaskList(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int,
    val isArchived: Boolean
)
