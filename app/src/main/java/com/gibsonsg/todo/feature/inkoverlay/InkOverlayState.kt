package com.gibsonsg.todo.feature.inkoverlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.gibsonsg.todo.core.domain.model.StrokePoint

/** Transient (non-persisted) drawing state for a single [InkOverlay] instance. */
class InkOverlayState {
    var isInkModeActive by mutableStateOf(false)
        private set

    val currentStrokePoints: SnapshotStateList<StrokePoint> = mutableStateListOf()

    val isDrawing: Boolean get() = currentStrokePoints.isNotEmpty()

    fun toggleInkMode() {
        isInkModeActive = !isInkModeActive
    }

    fun setInkMode(active: Boolean) {
        isInkModeActive = active
    }

    fun beginStroke(point: StrokePoint) {
        currentStrokePoints.clear()
        currentStrokePoints.add(point)
    }

    fun appendPoint(point: StrokePoint) {
        currentStrokePoints.add(point)
    }

    fun clearCurrentStroke() {
        currentStrokePoints.clear()
    }
}

@Composable
fun rememberInkOverlayState(): InkOverlayState = remember { InkOverlayState() }
