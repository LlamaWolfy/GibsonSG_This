package com.gibsonsg.todo.core.data.repository

import com.gibsonsg.todo.core.domain.model.AnchorType
import com.gibsonsg.todo.core.domain.model.InkPage
import com.gibsonsg.todo.core.domain.model.Stroke
import kotlinx.coroutines.flow.Flow

interface InkRepository {
    fun observePagesForAnchor(anchorType: AnchorType, anchorId: String): Flow<List<InkPage>>
    fun observeDatesWithInk(): Flow<Set<java.time.LocalDate>>

    /** Appends [stroke] to the day's single accumulating page, creating it if needed. */
    suspend fun appendStrokeToDayPage(date: java.time.LocalDate, stroke: Stroke)

    /** Appends [stroke] to an existing task-anchored session page. */
    suspend fun appendStrokeToPage(pageId: String, stroke: Stroke)

    /** Starts a brand-new page for a task-writing session (never reused across sessions). */
    suspend fun startNewTaskPage(taskId: String): InkPage
}
