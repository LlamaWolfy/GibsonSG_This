package com.gibsonsg.todo.core.data.di

import com.gibsonsg.todo.core.data.repository.InkRepository
import com.gibsonsg.todo.core.data.repository.InkRepositoryImpl
import com.gibsonsg.todo.core.data.repository.TaskListRepository
import com.gibsonsg.todo.core.data.repository.TaskListRepositoryImpl
import com.gibsonsg.todo.core.data.repository.TaskRepository
import com.gibsonsg.todo.core.data.repository.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindTaskListRepository(impl: TaskListRepositoryImpl): TaskListRepository

    @Binds
    @Singleton
    abstract fun bindInkRepository(impl: InkRepositoryImpl): InkRepository
}
