package com.gibsonsg.todo.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ink_pages",
    indices = [
        Index(value = ["anchorType", "anchorId"]),
        Index("anchorId")
    ]
)
data class InkPageEntity(
    @PrimaryKey val id: String,
    val anchorType: String,
    val anchorId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val strokesJson: String,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null
)
