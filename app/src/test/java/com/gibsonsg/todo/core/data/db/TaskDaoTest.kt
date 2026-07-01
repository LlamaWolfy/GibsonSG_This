package com.gibsonsg.todo.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gibsonsg.todo.core.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {

    private lateinit var database: TodoDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun task(id: String, isArchived: Boolean = false, sortOrder: Long = 0) = TaskEntity(
        id = id,
        title = "Task $id",
        description = null,
        dueAt = null,
        priority = 0,
        listId = null,
        status = 0,
        isArchived = isArchived,
        createdAt = 0,
        updatedAt = 0,
        sortOrder = sortOrder
    )

    @Test
    fun `active tasks excludes archived tasks`() = runBlocking {
        val dao = database.taskDao()
        dao.upsert(task("active-1"))
        dao.upsert(task("archived-1", isArchived = true))

        val activeTasks = dao.observeActiveTasks().first()

        assertEquals(1, activeTasks.size)
        assertEquals("active-1", activeTasks.first().id)
    }

    @Test
    fun `archiveTask flags task and preserves its data`() = runBlocking {
        val dao = database.taskDao()
        dao.upsert(task("t1"))

        dao.archiveTask("t1", archivedAt = 1234L)

        val stored = dao.getById("t1")
        assertTrue(stored!!.isArchived)
        assertEquals(1234L, stored.archivedAt)
        assertEquals("Task t1", stored.title)
    }

    @Test
    fun `setStatus updates status and completedAt`() = runBlocking {
        val dao = database.taskDao()
        dao.upsert(task("t1"))

        dao.setStatus("t1", status = 2, completedAt = 999L, updatedAt = 999L)

        val stored = dao.getById("t1")
        assertEquals(2, stored!!.status)
        assertEquals(999L, stored.completedAt)
    }
}
