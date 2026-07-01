package com.gibsonsg.todo.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gibsonsg.todo.core.data.repository.TaskRepository
import com.gibsonsg.todo.core.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    taskRepository: TaskRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    val results: StateFlow<List<Task>> = query
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList()) else taskRepository.searchActiveTasks(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentQuery: StateFlow<String> = query

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }
}
