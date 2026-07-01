package com.gibsonsg.todo.feature.inkoverlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Tracks task-row bounding boxes in the same local coordinate space that an [InkOverlay]
 * sees its stroke points in. Both the overlay's hosting Box and each row report their
 * *root*-relative position (via onGloballyPositioned); subtracting the Box's root offset
 * from a row's root offset converts it into the overlay's local space, matching where
 * stylus stroke points land, so gesture hit-testing compares like with like.
 */
class RowBoundsTracker {
    private var boxRootOffset by mutableStateOf(Offset.Zero)
    private val rowRects = mutableStateMapOf<String, Rect>()

    fun onBoxPositioned(rootOffset: Offset) {
        boxRootOffset = rootOffset
    }

    fun onRowPositioned(taskId: String, rootOffset: Offset, size: Size) {
        rowRects[taskId] = Rect(rootOffset - boxRootOffset, size)
    }

    fun snapshot(): Map<String, Rect> = rowRects.toMap()
}

@Composable
fun rememberRowBoundsTracker(): RowBoundsTracker = remember { RowBoundsTracker() }
