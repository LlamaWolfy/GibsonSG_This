package com.gibsonsg.todo.core.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gibsonsg.todo.core.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY sortOrder ASC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND listId = :listId ORDER BY sortOrder ASC")
    fun observeActiveTasksForList(listId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND status = :status ORDER BY sortOrder ASC")
    fun observeActiveTasksByStatus(status: Int): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE isArchived = 0 AND " +
            "(title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') " +
            "ORDER BY updatedAt DESC"
    )
    fun searchActiveTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: String): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("UPDATE tasks SET isArchived = 1, archivedAt = :archivedAt WHERE id = :taskId")
    suspend fun archiveTask(taskId: String, archivedAt: Long)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setStatus(taskId: String, status: Int, completedAt: Long?, updatedAt: Long)
}
