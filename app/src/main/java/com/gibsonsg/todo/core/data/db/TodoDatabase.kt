package com.gibsonsg.todo.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gibsonsg.todo.core.data.db.dao.InkPageDao
import com.gibsonsg.todo.core.data.db.dao.SubtaskDao
import com.gibsonsg.todo.core.data.db.dao.TaskDao
import com.gibsonsg.todo.core.data.db.dao.TaskListDao
import com.gibsonsg.todo.core.data.db.entity.InkPageEntity
import com.gibsonsg.todo.core.data.db.entity.SubtaskEntity
import com.gibsonsg.todo.core.data.db.entity.TaskEntity
import com.gibsonsg.todo.core.data.db.entity.TaskListEntity

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        TaskListEntity::class,
        InkPageEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun inkPageDao(): InkPageDao

    companion object {
        const val DATABASE_NAME = "scribble_todo.db"
    }
}
