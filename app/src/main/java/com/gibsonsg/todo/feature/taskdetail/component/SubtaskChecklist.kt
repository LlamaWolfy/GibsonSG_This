package com.gibsonsg.todo.feature.taskdetail.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.gibsonsg.todo.core.domain.model.Subtask

@Composable
fun SubtaskChecklist(
    subtasks: List<Subtask>,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newSubtaskTitle by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        subtasks.forEach { subtask ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = subtask.isDone,
                    onCheckedChange = { onToggle(subtask.id, it) }
                )
                Text(
                    text = subtask.title,
                    textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onDelete(subtask.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove subtask")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newSubtaskTitle,
                onValueChange = { newSubtaskTitle = it },
                label = { Text("Add subtask") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    onAdd(newSubtaskTitle)
                    newSubtaskTitle = ""
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add subtask")
            }
        }
    }
}
