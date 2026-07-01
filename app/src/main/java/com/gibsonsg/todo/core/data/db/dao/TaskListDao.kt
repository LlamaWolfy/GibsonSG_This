package com.gibsonsg.todo.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gibsonsg.todo.core.data.db.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {

    @Query("SELECT * FROM task_lists WHERE isArchived = 0 ORDER BY sortOrder ASC")
    fun observeActiveLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun getById(id: String): TaskListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(taskList: TaskListEntity)

    @Update
    suspend fun update(taskList: TaskListEntity)

    @Query("UPDATE task_lists SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: String)
}
