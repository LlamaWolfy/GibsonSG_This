package com.gibsonsg.todo.feature.taskdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gibsonsg.todo.core.domain.model.InkPage
import com.gibsonsg.todo.core.domain.model.RecurrenceFrequency
import com.gibsonsg.todo.core.domain.model.RecurrenceRule
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.core.domain.model.TaskList
import com.gibsonsg.todo.feature.taskdetail.component.InkHistoryList
import com.gibsonsg.todo.feature.taskdetail.component.InkPageDialog
import com.gibsonsg.todo.feature.taskdetail.component.PrioritySelector
import com.gibsonsg.todo.feature.taskdetail.component.SubtaskChecklist
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailRoute(
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val task by viewModel.task.collectAsState()
    val lists by viewModel.availableLists.collectAsState()
    val inkPages by viewModel.inkPages.collectAsState()
    val activeWritingPageId by viewModel.activeWritingPageId.collectAsState()
    val viewingHistoryPageId by viewModel.viewingHistoryPageId.collectAsState()

    task?.let { current ->
        TaskDetailScreen(
            task = current,
            availableLists = lists,
            inkPages = inkPages,
            onBack = onBack,
            onTitleChange = viewModel::updateTitle,
            onDescriptionChange = viewModel::updateDescription,
            onDueAtChange = viewModel::updateDueAt,
            onPriorityChange = viewModel::updatePriority,
            onListChange = viewModel::updateListId,
            onRecurrenceChange = viewModel::updateRecurrence,
            onAddSubtask = viewModel::addSubtask,
            onToggleSubtask = viewModel::setSubtaskDone,
            onDeleteSubtask = viewModel::deleteSubtask,
            onNewNote = viewModel::startNewNote,
            onOpenHistoryPage = { viewModel.openHistoryPage(it.id) },
            onArchive = {
                viewModel.archiveTask()
                onBack()
            }
        )
    }

    val writingPage = inkPages.firstOrNull { it.id == activeWritingPageId }
    if (activeWritingPageId != null) {
        InkPageDialog(
            title = "New note",
            strokes = writingPage?.strokes.orEmpty(),
            onStrokeCompleted = viewModel::onNoteStrokeCompleted,
            onDismiss = viewModel::closeNote
        )
    }

    val historyPage = inkPages.firstOrNull { it.id == viewingHistoryPageId }
    if (historyPage != null) {
        InkPageDialog(
            title = "Note",
            strokes = historyPage.strokes,
            onStrokeCompleted = null,
            onDismiss = viewModel::closeNote
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task,
    availableLists: List<TaskList>,
    inkPages: List<InkPage>,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDueAtChange: (Instant?) -> Unit,
    onPriorityChange: (com.gibsonsg.todo.core.domain.model.Priority) -> Unit,
    onListChange: (String?) -> Unit,
    onRecurrenceChange: (RecurrenceRule?) -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (String, Boolean) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onNewNote: () -> Unit,
    onOpenHistoryPage: (InkPage) -> Unit,
    onArchive: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onArchive) { Text("Archive") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = task.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = task.description.orEmpty(),
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Priority", fontWeight = FontWeight.Medium)
            PrioritySelector(selected = task.priority, onSelect = onPriorityChange)

            DueDatePicker(dueAt = task.dueAt, onDueAtChange = onDueAtChange)

            ListPicker(
                availableLists = availableLists,
                selectedListId = task.listId,
                onListChange = onListChange
            )

            RecurrencePicker(
                current = task.recurrenceRule,
                onChange = onRecurrenceChange
            )

            HorizontalDivider()

            Text("Subtasks", fontWeight = FontWeight.Medium)
            SubtaskChecklist(
                subtasks = task.subtasks,
                onToggle = onToggleSubtask,
                onDelete = onDeleteSubtask,
                onAdd = onAddSubtask
            )

            HorizontalDivider()

            Text("Handwritten notes", fontWeight = FontWeight.Medium)
            InkHistoryList(
                pages = inkPages,
                onNewNote = onNewNote,
                onOpenPage = onOpenHistoryPage
            )
        }
    }
}

@Composable
private fun DueDatePicker(
    dueAt: Instant?,
    onDueAtChange: (Instant?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val label = dueAt?.let {
        DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").format(it.atZone(ZoneId.systemDefault()))
    } ?: "No due date"

    Column {
        Text("Due", fontWeight = FontWeight.Medium)
        Text(label)
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showPicker = true }) { Text("Set due date/time") }
            if (dueAt != null) {
                TextButton(onClick = { onDueAtChange(null) }) { Text("Clear") }
            }
        }
    }

    if (showPicker) {
        DueDateTimeDialog(
            initial = dueAt,
            onDismiss = { showPicker = false },
            onConfirm = {
                onDueAtChange(it)
                showPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDateTimeDialog(
    initial: Instant?,
    onDismiss: () -> Unit,
    onConfirm: (Instant) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val initialZoned = (initial ?: Instant.now()).atZone(zone)

    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialZoned.toInstant().toEpochMilli()
    )
    val timePickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialZoned.hour,
        initialMinute = initialZoned.minute
    )

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedMillis = datePickerState.selectedDateMillis ?: return@TextButton
                val selectedDate = Instant.ofEpochMilli(selectedMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                val combined = selectedDate
                    .atTime(timePickerState.hour, timePickerState.minute)
                    .atZone(zone)
                    .toInstant()
                onConfirm(combined)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        Column {
            androidx.compose.material3.DatePicker(state = datePickerState)
            androidx.compose.material3.TimePicker(state = timePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListPicker(
    availableLists: List<TaskList>,
    selectedListId: String?,
    onListChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = availableLists.firstOrNull { it.id == selectedListId }?.name ?: "Inbox (no list)"

    Column {
        Text("List", fontWeight = FontWeight.Medium)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Inbox (no list)") },
                    onClick = { onListChange(null); expanded = false }
                )
                availableLists.forEach { list ->
                    DropdownMenuItem(
                        text = { Text(list.name) },
                        onClick = { onListChange(list.id); expanded = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrencePicker(
    current: RecurrenceRule?,
    onChange: (RecurrenceRule?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = current?.frequency?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Does not repeat"

    Column {
        Text("Repeat", fontWeight = FontWeight.Medium)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Does not repeat") },
                    onClick = { onChange(null); expanded = false }
                )
                RecurrenceFrequency.entries.forEach { frequency ->
                    DropdownMenuItem(
                        text = { Text(frequency.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = { onChange(RecurrenceRule(frequency)); expanded = false }
                    )
                }
            }
        }
    }
}

