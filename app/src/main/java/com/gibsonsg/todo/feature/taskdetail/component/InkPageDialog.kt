package com.gibsonsg.todo.feature.taskdetail.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gibsonsg.todo.core.domain.model.Stroke
import com.gibsonsg.todo.feature.inkoverlay.InkOverlay
import com.gibsonsg.todo.feature.inkoverlay.rememberInkOverlayState

/**
 * Full-screen ink writing/viewing surface for a single task-anchored ink page. In writing
 * mode (a fresh session, [onStrokeCompleted] non-null) strokes append to that page as they're
 * drawn. Prior history pages are opened read-only: no pen toggle, no persistence callback,
 * so old notes can never be silently edited.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkPageDialog(
    title: String,
    strokes: List<Stroke>,
    onStrokeCompleted: ((Stroke) -> Unit)?,
    onDismiss: () -> Unit
) {
    val inkState = rememberInkOverlayState()
    val isWritable = onStrokeCompleted != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (isWritable) {
                    FloatingActionButton(onClick = { inkState.toggleInkMode() }) {
                        Icon(
                            imageVector = if (inkState.isInkModeActive) Icons.Default.PanTool else Icons.Default.Draw,
                            contentDescription = if (inkState.isInkModeActive) "Exit ink mode" else "Enter ink mode"
                        )
                    }
                }
            }
        ) { padding ->
            InkOverlay(
                committedStrokes = strokes,
                state = inkState,
                onStrokeCompleted = { stroke -> onStrokeCompleted?.invoke(stroke) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}
