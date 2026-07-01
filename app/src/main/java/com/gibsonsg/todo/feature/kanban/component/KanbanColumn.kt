package com.gibsonsg.todo.feature.kanban.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.model.TaskStatus

@Composable
fun KanbanColumn(
    status: TaskStatus,
    label: String,
    tasks: List<Task>,
    draggingTaskId: String?,
    dragOffset: Offset,
    onColumnPositioned: (TaskStatus, Offset, Size) -> Unit,
    onCardPositioned: (String, Offset, Size) -> Unit,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .padding(8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .onGloballyPositioned { coordinates ->
                onColumnPositioned(status, coordinates.positionInRoot(), coordinates.size.toSize())
            }
    ) {
        Text(
            text = "$label (${tasks.size})",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp)
        )
        LazyColumn {
            items(tasks, key = { it.id }) { task ->
                KanbanCard(
                    task = task,
                    onPositioned = { position, size -> onCardPositioned(task.id, position, size) },
                    onDragStart = { onDragStart(task.id, it) },
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    isDragging = task.id == draggingTaskId,
                    dragOffset = dragOffset
                )
            }
        }
    }
}
