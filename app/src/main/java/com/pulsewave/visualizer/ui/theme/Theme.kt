package com.pulsewave.visualizer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PulseWaveColorScheme = darkColorScheme(
    primary = PulseCyan,
    secondary = PulsePink,
    tertiary = PulsePurple,
    background = PulseBackground,
    surface = PulseSurface,
    onBackground = PulseOnSurface,
    onSurface = PulseOnSurface,
)

@Composable
fun PulseWaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PulseWaveColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
