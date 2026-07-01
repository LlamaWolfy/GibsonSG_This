package com.gibsonsg.todo.core.domain.model

enum class Priority(val sortWeight: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3)
}
