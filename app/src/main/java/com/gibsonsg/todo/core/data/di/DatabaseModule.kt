package com.gibsonsg.todo.core.data.di

import android.content.Context
import androidx.room.Room
import com.gibsonsg.todo.core.data.db.TodoDatabase
import com.gibsonsg.todo.core.data.db.dao.InkPageDao
import com.gibsonsg.todo.core.data.db.dao.SubtaskDao
import com.gibsonsg.todo.core.data.db.dao.TaskDao
import com.gibsonsg.todo.core.data.db.dao.TaskListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTodoDatabase(@ApplicationContext context: Context): TodoDatabase =
        Room.databaseBuilder(context, TodoDatabase::class.java, TodoDatabase.DATABASE_NAME).build()

    @Provides
    fun provideTaskDao(database: TodoDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideSubtaskDao(database: TodoDatabase): SubtaskDao = database.subtaskDao()

    @Provides
    fun provideTaskListDao(database: TodoDatabase): TaskListDao = database.taskListDao()

    @Provides
    fun provideInkPageDao(database: TodoDatabase): InkPageDao = database.inkPageDao()
}
