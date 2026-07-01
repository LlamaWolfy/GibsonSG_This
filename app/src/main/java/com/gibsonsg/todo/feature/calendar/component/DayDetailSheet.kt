package com.gibsonsg.todo.feature.calendar.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gibsonsg.todo.core.domain.model.Stroke
import com.gibsonsg.todo.core.domain.model.Task
import com.gibsonsg.todo.feature.inkoverlay.GestureClassifier
import com.gibsonsg.todo.feature.inkoverlay.InkGestureResult
import com.gibsonsg.todo.feature.inkoverlay.InkOverlay
import com.gibsonsg.todo.feature.inkoverlay.rememberInkOverlayState
import com.gibsonsg.todo.feature.inkoverlay.rememberRowBoundsTracker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * Full-screen writing surface for a single day - the calendar's expanded, tap-and-hold
 * destination where actual S-Pen strokes are captured at legible scale. Month-view cells
 * only ever show a small read-only ink preview; this is where writing happens. A stylus
 * strike-through over a task row here completes that task instead of being saved as ink.
 */
@Composable
fun DayDetailSheet(
    date: LocalDate,
    tasks: List<Task>,
    committedStrokes: List<Stroke>,
    onStrokeCompleted: (Stroke) -> Unit,
    onTaskCompleted: (String) -> Unit,
    onTaskUncompleted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val inkState = rememberInkOverlayState()
    val rowBoundsTracker = rememberRowBoundsTracker()
    val gestureClassifier = remember { GestureClassifier() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(DateTimeFormatter.ofPattern("EEEE, MMM d").format(date)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(onClick = { inkState.toggleInkMode() }) {
                    Icon(
                        imageVector = if (inkState.isInkModeActive) Icons.Default.PanTool else Icons.Default.Draw,
                        contentDescription = if (inkState.isInkModeActive) "Exit ink mode" else "Enter ink mode"
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .onGloballyPositioned { rowBoundsTracker.onBoxPositioned(it.positionInRoot()) }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Tasks",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(tasks, key = { it.id }) { task ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = task.title,
                                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                                    )
                                },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    rowBoundsTracker.onRowPositioned(
                                        task.id,
                                        coordinates.positionInRoot(),
                                        coordinates.size.toSize()
                                    )
                                }
                            )
                        }
                    }
                }

                // Ink layer sits above the task list content; only intercepts stylus
                // input while ink mode is active, otherwise finger scroll/tap on the
                // list above still works normally. A recognized strike-through over a
                // task row completes that task and is never saved as ink; everything
                // else persists to this day's permanent ink page.
                InkOverlay(
                    committedStrokes = committedStrokes,
                    state = inkState,
                    onStrokeCompleted = { stroke ->
                        when (val result = gestureClassifier.classify(stroke, rowBoundsTracker.snapshot())) {
                            is InkGestureResult.CompletesTask -> {
                                onTaskCompleted(result.taskId)
                                scope.launch {
                                    val outcome = snackbarHostState.showSnackbar(
                                        message = "Task marked complete",
                                        actionLabel = "Undo"
                                    )
                                    if (outcome == SnackbarResult.ActionPerformed) {
                                        onTaskUncompleted(result.taskId)
                                    }
                                }
                            }
                            InkGestureResult.Annotation -> onStrokeCompleted(stroke)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
