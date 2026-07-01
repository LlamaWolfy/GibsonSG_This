package com.gibsonsg.todo.core.data.repository

import androidx.room.withTransaction
import com.gibsonsg.todo.core.data.db.TodoDatabase
import com.gibsonsg.todo.core.data.db.dao.InkPageDao
import com.gibsonsg.todo.core.data.db.dao.SubtaskDao
import com.gibsonsg.todo.core.data.db.dao.TaskDao
import com.gibsonsg.todo.core.data.db.entity.SubtaskEntity
import com.gibsonsg.todo.core.data.db.entity.TaskEntity
import com.gibsonsg.todo.core.domain.model.Priority
import com.gibsonsg.todo.core.domain.model.RecurrenceRule
import com.gibsonsg.todo.core.domain.model.Subtask
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.model.TaskStatus
import com.gibsonsg.todo.core.reminder.ReminderScheduler
import com.gibsonsg.todo.util.IdGenerator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val database: TodoDatabase,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val inkPageDao: InkPageDao,
    private val reminderScheduler: ReminderScheduler
) : TaskRepository {

    override fun observeActiveTasks(): Flow<List<Task>> =
        taskDao.observeActiveTasks().map { list -> list.map { it.toDomain() } }

    override fun observeActiveTasksForList(listId: String): Flow<List<Task>> =
        taskDao.observeActiveTasksForList(listId).map { list -> list.map { it.toDomain() } }

    override fun observeActiveTasksByStatus(status: TaskStatus): Flow<List<Task>> =
        taskDao.observeActiveTasksByStatus(status.ordinal).map { list -> list.map { it.toDomain() } }

    override fun searchActiveTasks(query: String): Flow<List<Task>> =
        taskDao.searchActiveTasks(query).map { list -> list.map { it.toDomain() } }

    override fun observeTask(taskId: String): Flow<Task?> =
        combine(
            taskDao.observeById(taskId),
            subtaskDao.observeForTask(taskId)
        ) { entity, subtasks ->
            entity?.toDomain(subtasks.map { it.toDomain() })
        }

    override suspend fun getTask(taskId: String): Task? = taskDao.getById(taskId)?.toDomain()

    override suspend fun createTask(
        title: String,
        description: String?,
        dueAt: Instant?,
        priority: Priority,
        listId: String?,
        recurrenceRule: RecurrenceRule?
    ): Task {
        val now = Instant.now()
        val entity = TaskEntity(
            id = IdGenerator.newId(),
            title = title,
            description = description,
            dueAt = dueAt?.toEpochMilli(),
            priority = priority.sortWeight,
            listId = listId,
            status = TaskStatus.TODO.ordinal,
            recurrenceRule = recurrenceRule?.serialize(),
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
            sortOrder = now.toEpochMilli()
        )
        taskDao.upsert(entity)
        val task = entity.toDomain()
        rescheduleReminder(task)
        return task
    }

    override suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity(updatedAt = Instant.now()))
        rescheduleReminder(task)
    }

    override suspend fun setStatus(taskId: String, status: TaskStatus) {
        val completedAt = if (status == TaskStatus.DONE) Instant.now().toEpochMilli() else null
        taskDao.setStatus(taskId, status.ordinal, completedAt, Instant.now().toEpochMilli())

        if (status == TaskStatus.DONE) {
            reminderScheduler.cancel(taskId)
            spawnNextRecurrenceIfNeeded(taskId)
        } else {
            taskDao.getById(taskId)?.toDomain()?.let { rescheduleReminder(it) }
        }
    }

    override suspend fun completeTask(taskId: String) = setStatus(taskId, TaskStatus.DONE)

    override suspend fun archiveTask(taskId: String) {
        val archivedAt = Instant.now().toEpochMilli()
        database.withTransaction {
            taskDao.archiveTask(taskId, archivedAt)
            inkPageDao.archivePagesForTask(taskId, archivedAt)
        }
        reminderScheduler.cancel(taskId)
    }

    override suspend fun addSubtask(taskId: String, title: String): Subtask {
        val entity = SubtaskEntity(
            id = IdGenerator.newId(),
            taskId = taskId,
            title = title,
            isDone = false,
            sortOrder = Instant.now().toEpochMilli().toInt()
        )
        subtaskDao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun setSubtaskDone(subtaskId: String, isDone: Boolean) {
        subtaskDao.setDone(subtaskId, isDone)
    }

    override suspend fun deleteSubtask(subtaskId: String) {
        subtaskDao.deleteById(subtaskId)
    }

    private fun rescheduleReminder(task: Task) {
        val dueAt = task.dueAt
        if (dueAt != null && !task.isDone && !task.isArchived) {
            reminderScheduler.schedule(task.id, task.title, dueAt)
        } else {
            reminderScheduler.cancel(task.id)
        }
    }

    /**
     * v1 recurrence: spawn a fresh task instance when a recurring task is completed,
     * rather than expanding the full RRULE up front.
     */
    private suspend fun spawnNextRecurrenceIfNeeded(completedTaskId: String) {
        val completed = taskDao.getById(completedTaskId) ?: return
        val rule = RecurrenceRule.parse(completed.recurrenceRule) ?: return
        val currentDueAt = completed.dueAt?.let { Instant.ofEpochMilli(it) } ?: Instant.now()
        val zone = ZoneId.systemDefault()
        val nextDueDate: LocalDate = rule.nextDueDate(currentDueAt.atZone(zone).toLocalDate())
        val nextDueAt = nextDueDate.atTime(currentDueAt.atZone(zone).toLocalTime()).atZone(zone).toInstant()

        val now = Instant.now()
        val nextEntity = TaskEntity(
            id = IdGenerator.newId(),
            title = completed.title,
            description = completed.description,
            dueAt = nextDueAt.toEpochMilli(),
            priority = completed.priority,
            listId = completed.listId,
            status = TaskStatus.TODO.ordinal,
            recurrenceRule = completed.recurrenceRule,
            recurrenceParentId = completed.recurrenceParentId ?: completed.id,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
            sortOrder = now.toEpochMilli()
        )
        taskDao.upsert(nextEntity)
        rescheduleReminder(nextEntity.toDomain())
    }
}

fun TaskEntity.toDomain(subtasks: List<Subtask> = emptyList()): Task = Task(
    id = id,
    title = title,
    description = description,
    dueAt = dueAt?.let { Instant.ofEpochMilli(it) },
    priority = Priority.entries.firstOrNull { it.sortWeight == priority } ?: Priority.NONE,
    listId = listId,
    status = TaskStatus.entries.getOrElse(status) { TaskStatus.TODO },
    isArchived = isArchived,
    archivedAt = archivedAt?.let { Instant.ofEpochMilli(it) },
    recurrenceRule = RecurrenceRule.parse(recurrenceRule),
    recurrenceParentId = recurrenceParentId,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
    sortOrder = sortOrder,
    subtasks = subtasks
)

fun Task.toEntity(updatedAt: Instant = this.updatedAt): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    dueAt = dueAt?.toEpochMilli(),
    priority = priority.sortWeight,
    listId = listId,
    status = status.ordinal,
    isArchived = isArchived,
    archivedAt = archivedAt?.toEpochMilli(),
    recurrenceRule = recurrenceRule?.serialize(),
    recurrenceParentId = recurrenceParentId,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    completedAt = completedAt?.toEpochMilli(),
    sortOrder = sortOrder
)

fun SubtaskEntity.toDomain(): Subtask = Subtask(
    id = id,
    taskId = taskId,
    title = title,
    isDone = isDone,
    sortOrder = sortOrder
)
