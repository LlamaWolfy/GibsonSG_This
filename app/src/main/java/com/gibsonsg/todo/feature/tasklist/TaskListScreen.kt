package com.gibsonsg.todo.feature.tasklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.usecase.SmartList
import com.gibsonsg.todo.feature.inkoverlay.GestureClassifier
import com.gibsonsg.todo.feature.inkoverlay.InkGestureResult
import com.gibsonsg.todo.feature.inkoverlay.InkOverlay
import com.gibsonsg.todo.feature.inkoverlay.rememberInkOverlayState
import com.gibsonsg.todo.feature.inkoverlay.rememberRowBoundsTracker
import com.gibsonsg.todo.feature.quickcapture.QuickCaptureSheet
import kotlinx.coroutines.launch

@Composable
fun TaskListRoute(
    onOpenTask: (String) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    TaskListScreen(
        uiState = uiState,
        onSelectSmartList = viewModel::selectSmartList,
        onAddTask = viewModel::addTask,
        onToggleDone = viewModel::toggleDone,
        onArchiveTask = viewModel::archiveTask,
        onOpenTask = onOpenTask
    )
}

@Composable
fun TaskListScreen(
    uiState: TaskListUiState,
    onSelectSmartList: (SmartList) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleDone: (String, Boolean) -> Unit,
    onArchiveTask: (String) -> Unit,
    onOpenTask: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val inkState = rememberInkOverlayState()
    val rowBoundsTracker = rememberRowBoundsTracker()
    val gestureClassifier = remember { GestureClassifier() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(onClick = { inkState.toggleInkMode() }) {
                    Icon(
                        imageVector = if (inkState.isInkModeActive) Icons.Default.PanTool else Icons.Default.Draw,
                        contentDescription = if (inkState.isInkModeActive) "Exit ink mode" else "Enter ink mode"
                    )
                }
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add task")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { rowBoundsTracker.onBoxPositioned(it.positionInRoot()) }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SmartListTabs(selected = uiState.selectedSmartList, onSelect = onSelectSmartList)

                if (uiState.tasks.isEmpty() && !uiState.isLoading) {
                    EmptyTaskList(padding)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding
                    ) {
                        items(uiState.tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggleDone = { onToggleDone(task.id, task.isDone) },
                                onArchive = { onArchiveTask(task.id) },
                                onOpen = { onOpenTask(task.id) },
                                onPositioned = { position, size ->
                                    rowBoundsTracker.onRowPositioned(task.id, position, size)
                                }
                            )
                        }
                    }
                }
            }

            // Strike-through-to-complete only in v1 for list view: a matching stroke
            // completes a task, everything else is discarded (list rows have no ink
            // annotation storage - only calendar days and tasks do).
            InkOverlay(
                committedStrokes = emptyList(),
                state = inkState,
                onStrokeCompleted = { stroke ->
                    when (val result = gestureClassifier.classify(stroke, rowBoundsTracker.snapshot())) {
                        is InkGestureResult.CompletesTask -> {
                            onToggleDone(result.taskId, false)
                            scope.launch {
                                val outcome = snackbarHostState.showSnackbar(
                                    message = "Task marked complete",
                                    actionLabel = "Undo"
                                )
                                if (outcome == SnackbarResult.ActionPerformed) {
                                    onToggleDone(result.taskId, true)
                                }
                            }
                        }
                        InkGestureResult.Annotation -> Unit
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showAddDialog) {
        QuickCaptureSheet(
            onDismiss = { showAddDialog = false },
            onConfirm = { title ->
                onAddTask(title)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SmartListTabs(
    selected: SmartList,
    onSelect: (SmartList) -> Unit
) {
    val tabs = SmartList.entries.toList()
    ScrollableTabRow(selectedTabIndex = tabs.indexOf(selected)) {
        tabs.forEach { smartList ->
            Tab(
                selected = smartList == selected,
                onClick = { onSelect(smartList) },
                text = { Text(smartList.label()) }
            )
        }
    }
}

private fun SmartList.label(): String = when (this) {
    SmartList.TODAY -> "Today"
    SmartList.OVERDUE -> "Overdue"
    SmartList.UPCOMING -> "Upcoming"
    SmartList.NO_DATE -> "No date"
    SmartList.ALL -> "All"
}

@Composable
private fun EmptyTaskList(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("No tasks here — tap + to add one")
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggleDone: () -> Unit,
    onArchive: () -> Unit,
    onOpen: () -> Unit,
    onPositioned: (androidx.compose.ui.geometry.Offset, Size) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = task.title,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null
            )
        },
        leadingContent = {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone() })
        },
        trailingContent = {
            TextButton(onClick = onArchive) { Text("Archive") }
        },
        modifier = Modifier
            .clickable(onClick = onOpen)
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.positionInRoot(), coordinates.size.toSize())
            }
    )
}
