package com.gibsonsg.todo.core.data.repository

import com.gibsonsg.todo.core.data.db.dao.TaskListDao
import com.gibsonsg.todo.core.data.db.entity.TaskListEntity
import com.gibsonsg.todo.core.domain.model.TaskList
import com.gibsonsg.todo.util.IdGenerator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TaskListRepositoryImpl @Inject constructor(
    private val taskListDao: TaskListDao
) : TaskListRepository {

    override fun observeActiveLists(): Flow<List<TaskList>> =
        taskListDao.observeActiveLists().map { lists -> lists.map { it.toDomain() } }

    override suspend fun createList(name: String, colorArgb: Int): TaskList {
        val entity = TaskListEntity(
            id = IdGenerator.newId(),
            name = name,
            colorArgb = colorArgb,
            sortOrder = System.currentTimeMillis().toInt()
        )
        taskListDao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun archiveList(id: String) {
        taskListDao.archive(id)
    }
}

fun TaskListEntity.toDomain(): TaskList = TaskList(
    id = id,
    name = name,
    colorArgb = colorArgb,
    sortOrder = sortOrder,
    isArchived = isArchived
)
