package com.gibsonsg.todo.core.domain.model

data class Subtask(
    val id: String,
    val taskId: String,
    val title: String,
    val isDone: Boolean,
    val sortOrder: Int
)
