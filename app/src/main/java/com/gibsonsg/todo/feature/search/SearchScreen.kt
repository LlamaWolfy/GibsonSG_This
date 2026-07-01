package com.gibsonsg.todo.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gibsonsg.todo.core.domain.model.Task

@Composable
fun SearchRoute(
    onOpenTask: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.currentQuery.collectAsState()
    val results by viewModel.results.collectAsState()
    SearchScreen(
        query = query,
        results = results,
        onQueryChange = viewModel::onQueryChange,
        onOpenTask = onOpenTask
    )
}

@Composable
fun SearchScreen(
    query: String,
    results: List<Task>,
    onQueryChange: (String) -> Unit,
    onOpenTask: (String) -> Unit
) {
    Scaffold { padding ->
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search tasks") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (query.isNotBlank() && results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matches")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.id }) { task ->
                        ListItem(
                            headlineContent = { Text(task.title) },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable { onOpenTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}
