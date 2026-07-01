package com.pulsewave.visualizer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: VisualizerSettings,
    onSettingsChange: ((VisualizerSettings) -> VisualizerSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Visualizer style", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
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

            Text("Color theme", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
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

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("Done")
            }
        }
    }
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
