package com.gibsonsg.todo.core.domain.model

import java.time.Instant

data class InkPage(
    val id: String,
    val anchorType: AnchorType,
    val anchorId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val strokes: List<Stroke>,
    val isArchived: Boolean,
    val archivedAt: Instant?
)
