package com.gibsonsg.todo.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gibsonsg.todo.core.domain.model.Task
import java.time.LocalDate

@Composable
fun DayCell(
    date: LocalDate,
    isInCurrentMonth: Boolean,
    isToday: Boolean,
    tasks: List<Task>,
    hasInk: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .aspectRatio(0.85f)
            .padding(2.dp)
            .background(
                color = if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress
            )
            .padding(4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isInCurrentMonth) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
        )

        tasks.take(MAX_VISIBLE_TASK_DOTS).forEach { task ->
            Text(
                text = "• ${task.title}",
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (tasks.size > MAX_VISIBLE_TASK_DOTS) {
            Text(text = "+${tasks.size - MAX_VISIBLE_TASK_DOTS} more", fontSize = 10.sp)
        }

        // Small read-only marker shown when a day has handwritten ink; the real
        // stroke-thumbnail preview is wired in once the ink feature lands.
        if (hasInk) {
            Text(text = "✎", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

private const val MAX_VISIBLE_TASK_DOTS = 2
