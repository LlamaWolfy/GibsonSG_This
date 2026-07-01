package com.gibsonsg.todo.feature.taskdetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gibsonsg.todo.core.domain.model.Priority

@Composable
fun PrioritySelector(
    selected: Priority,
    onSelect: (Priority) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Priority.entries.forEach { priority ->
            FilterChip(
                selected = priority == selected,
                onClick = { onSelect(priority) },
                label = { Text(priority.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
