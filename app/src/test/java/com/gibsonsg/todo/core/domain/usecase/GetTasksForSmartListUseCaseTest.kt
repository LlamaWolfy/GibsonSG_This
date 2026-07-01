package com.gibsonsg.todo.core.domain.usecase

import com.gibsonsg.todo.core.domain.model.Priority
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.model.TaskStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTasksForSmartListUseCaseTest {

    // Fixed "now" = 2026-07-01T12:00:00Z so every test bucket is deterministic.
    private val fixedClock = Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneOffset.UTC)
    private val useCase = GetTasksForSmartListUseCase(fixedClock)

    private fun task(
        id: String,
        dueAt: Instant? = null,
        status: TaskStatus = TaskStatus.TODO
    ) = Task(
        id = id,
        title = "Task $id",
        description = null,
        dueAt = dueAt,
        priority = Priority.NONE,
        listId = null,
        status = status,
        isArchived = false,
        archivedAt = null,
        recurrenceRule = null,
        recurrenceParentId = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        completedAt = null,
        sortOrder = 0
    )

    @Test
    fun `task due today lands in TODAY bucket only`() {
        val dueToday = task("today", dueAt = Instant.parse("2026-07-01T18:00:00Z"))
        val tasks = listOf(dueToday)

        assertEquals(listOf(dueToday), useCase(tasks, SmartList.TODAY))
        assertTrue(useCase(tasks, SmartList.OVERDUE).isEmpty())
        assertTrue(useCase(tasks, SmartList.UPCOMING).isEmpty())
    }

    @Test
    fun `task due yesterday lands in OVERDUE only`() {
        val overdue = task("late", dueAt = Instant.parse("2026-06-30T09:00:00Z"))
        val tasks = listOf(overdue)

        assertEquals(listOf(overdue), useCase(tasks, SmartList.OVERDUE))
        assertTrue(useCase(tasks, SmartList.TODAY).isEmpty())
        assertTrue(useCase(tasks, SmartList.UPCOMING).isEmpty())
    }

    @Test
    fun `task due tomorrow lands in UPCOMING only`() {
        val upcoming = task("soon", dueAt = Instant.parse("2026-07-02T09:00:00Z"))
        val tasks = listOf(upcoming)

        assertEquals(listOf(upcoming), useCase(tasks, SmartList.UPCOMING))
        assertTrue(useCase(tasks, SmartList.TODAY).isEmpty())
        assertTrue(useCase(tasks, SmartList.OVERDUE).isEmpty())
    }

    @Test
    fun `task with no due date lands in NO_DATE only`() {
        val noDate = task("nodate", dueAt = null)
        val tasks = listOf(noDate)

        assertEquals(listOf(noDate), useCase(tasks, SmartList.NO_DATE))
        assertTrue(useCase(tasks, SmartList.TODAY).isEmpty())
        assertTrue(useCase(tasks, SmartList.OVERDUE).isEmpty())
        assertTrue(useCase(tasks, SmartList.UPCOMING).isEmpty())
    }

    @Test
    fun `midnight boundary - due at start of today counts as TODAY not OVERDUE`() {
        val dueAtMidnight = task("midnight", dueAt = Instant.parse("2026-07-01T00:00:00Z"))

        assertEquals(listOf(dueAtMidnight), useCase(listOf(dueAtMidnight), SmartList.TODAY))
        assertTrue(useCase(listOf(dueAtMidnight), SmartList.OVERDUE).isEmpty())
    }

    @Test
    fun `midnight boundary - due at end of today counts as TODAY not UPCOMING`() {
        val dueAtEndOfDay = task("endofday", dueAt = Instant.parse("2026-07-01T23:59:59Z"))

        assertEquals(listOf(dueAtEndOfDay), useCase(listOf(dueAtEndOfDay), SmartList.TODAY))
        assertTrue(useCase(listOf(dueAtEndOfDay), SmartList.UPCOMING).isEmpty())
    }

    @Test
    fun `completed tasks are excluded from TODAY OVERDUE UPCOMING NO_DATE but included in ALL`() {
        val doneToday = task("done", dueAt = Instant.parse("2026-07-01T09:00:00Z"), status = TaskStatus.DONE)

        assertTrue(useCase(listOf(doneToday), SmartList.TODAY).isEmpty())
        assertEquals(listOf(doneToday), useCase(listOf(doneToday), SmartList.ALL))
    }

    @Test
    fun `ALL returns every task regardless of due date or status`() {
        val tasks = listOf(
            task("a", dueAt = Instant.parse("2026-07-01T09:00:00Z")),
            task("b", dueAt = null),
            task("c", dueAt = Instant.parse("2026-06-01T09:00:00Z"), status = TaskStatus.DONE)
        )

        assertEquals(tasks, useCase(tasks, SmartList.ALL))
    }
}
