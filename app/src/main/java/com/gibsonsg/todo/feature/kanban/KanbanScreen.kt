package com.gibsonsg.todo.feature.kanban

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.hilt.navigation.compose.hiltViewModel
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.model.TaskStatus
import com.gibsonsg.todo.feature.kanban.component.KanbanColumn

@Composable
fun KanbanRoute(
    viewModel: KanbanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    KanbanScreen(
        columns = uiState.columns,
        onMoveTask = viewModel::moveTask
    )
}

private fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.TODO -> "To do"
    TaskStatus.IN_PROGRESS -> "In progress"
    TaskStatus.DONE -> "Done"
}

@Composable
fun KanbanScreen(
    columns: Map<TaskStatus, List<Task>>,
    onMoveTask: (String, TaskStatus) -> Unit
) {
    var draggingTaskId by remember { mutableStateOf<String?>(null) }
    var draggingFromStatus by remember { mutableStateOf<TaskStatus?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var draggedCardOrigin by remember { mutableStateOf(Offset.Zero) }
    var draggedCardSize by remember { mutableStateOf(Size.Zero) }
    val columnBounds = remember { mutableMapOf<TaskStatus, Rect>() }
    val cardPositions = remember { mutableMapOf<String, Rect>() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
    ) {
        TaskStatus.entries.forEach { status ->
            KanbanColumn(
                status = status,
                label = statusLabel(status),
                tasks = columns[status].orEmpty(),
                draggingTaskId = draggingTaskId,
                dragOffset = dragOffset,
                onColumnPositioned = { columnStatus, position, size ->
                    columnBounds[columnStatus] = Rect(position, size)
                },
                onCardPositioned = { taskId, position, size ->
                    if (draggingTaskId != taskId) {
                        cardPositions[taskId] = Rect(position, size)
                    }
                },
                onDragStart = { taskId, _ ->
                    val origin = cardPositions[taskId]
                    draggingTaskId = taskId
                    draggingFromStatus = status
                    dragOffset = Offset.Zero
                    draggedCardOrigin = origin?.topLeft ?: Offset.Zero
                    draggedCardSize = origin?.size ?: Size.Zero
                },
                onDrag = { delta ->
                    dragOffset += delta
                },
                onDragEnd = {
                    val taskId = draggingTaskId
                    val fromStatus = draggingFromStatus
                    if (taskId != null && fromStatus != null) {
                        val cardCenter = Offset(
                            x = draggedCardOrigin.x + draggedCardSize.width / 2 + dragOffset.x,
                            y = draggedCardOrigin.y + draggedCardSize.height / 2 + dragOffset.y
                        )
                        val targetStatus = columnBounds.entries.firstOrNull { (_, rect) ->
                            rect.contains(cardCenter)
                        }?.key
                        if (targetStatus != null && targetStatus != fromStatus) {
                            onMoveTask(taskId, targetStatus)
                        }
                    }
                    draggingTaskId = null
                    draggingFromStatus = null
                    dragOffset = Offset.Zero
                }
            )
        }
    }
}
