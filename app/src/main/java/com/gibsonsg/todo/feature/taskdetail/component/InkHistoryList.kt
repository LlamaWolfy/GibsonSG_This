package com.gibsonsg.todo.feature.taskdetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gibsonsg.todo.core.domain.model.InkPage
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InkHistoryList(
    pages: List<InkPage>,
    onNewNote: () -> Unit,
    onOpenPage: (InkPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember(Unit) { DateTimeFormatter.ofPattern("MMM d, HH:mm") }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onNewNote) {
            Icon(Icons.Default.Draw, contentDescription = null)
            Text(" New note")
        }
    }

    if (pages.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pages, key = { it.id }) { page ->
                InkPageThumbnail(
                    label = formatter.format(page.createdAt.atZone(ZoneId.systemDefault())),
                    strokeCount = page.strokes.size,
                    onClick = { onOpenPage(page) }
                )
            }
        }
    }
}

@Composable
private fun InkPageThumbnail(
    label: String,
    strokeCount: Int,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .size(width = 96.dp, height = 72.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(Icons.Default.Draw, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text("$strokeCount strokes", style = MaterialTheme.typography.labelSmall)
    }
}
