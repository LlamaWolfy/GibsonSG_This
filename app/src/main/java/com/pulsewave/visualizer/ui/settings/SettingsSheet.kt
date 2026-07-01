package com.pulsewave.visualizer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: VisualizerSettings,
    presets: List<VisualizerPreset>,
    onSettingsChange: ((VisualizerSettings) -> VisualizerSettings) -> Unit,
    onSavePreset: (String) -> Unit,
    onLoadPreset: (VisualizerPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Presets", style = MaterialTheme.typography.titleSmall)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showSaveDialog = true },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        label = { Text("Save current") },
                    )
                }
                items(presets) { preset ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = false,
                            onClick = { onLoadPreset(preset) },
                            label = { Text(preset.name) },
                        )
                        IconButton(
                            onClick = { onDeletePreset(preset.name) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete ${preset.name}")
                        }
                    }
                }
            }

            Text("Visualizer style", style = MaterialTheme.typography.titleSmall)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                items(VisualizerStyle.entries) { style ->
                    FilterChip(
                        selected = settings.style == style,
                        onClick = { onSettingsChange { it.copy(style = style) } },
                        label = { Text(style.label) },
                    )
                }
            }

            Text("Color theme", style = MaterialTheme.typography.titleSmall)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                items(ColorTheme.entries) { theme ->
                    FilterChip(
                        selected = settings.colorTheme == theme,
                        onClick = { onSettingsChange { it.copy(colorTheme = theme) } },
                        label = { Text(theme.label) },
                    )
                }
            }

            LabeledSlider(
                label = "Sensitivity",
                value = settings.sensitivity,
                range = 0.3f..3f,
                onValueChange = { v -> onSettingsChange { it.copy(sensitivity = v) } },
            )
            LabeledSlider(
                label = "Smoothing",
                value = settings.smoothing,
                range = 0f..0.95f,
                onValueChange = { v -> onSettingsChange { it.copy(smoothing = v) } },
            )
            LabeledSlider(
                label = "Density",
                value = settings.density,
                range = 0.3f..2f,
                onValueChange = { v -> onSettingsChange { it.copy(density = v) } },
            )

            ToggleRow(
                label = "Mirror / symmetry",
                checked = settings.mirror,
                onCheckedChange = { v -> onSettingsChange { it.copy(mirror = v) } },
            )
            ToggleRow(
                label = "Beat glow",
                checked = settings.beatFlash,
                onCheckedChange = { v -> onSettingsChange { it.copy(beatFlash = v) } },
            )
            ToggleRow(
                label = "Album art backdrop",
                checked = settings.showAlbumArt,
                onCheckedChange = { v -> onSettingsChange { it.copy(showAlbumArt = v) } },
            )

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("Done")
            }
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onConfirm = { name ->
                onSavePreset(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }
}

@Composable
private fun SavePresetDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save preset") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: ${"%.2f".format(value)}")
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
