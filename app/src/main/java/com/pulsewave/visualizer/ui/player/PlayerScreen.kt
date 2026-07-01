package com.pulsewave.visualizer.ui.player

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pulsewave.visualizer.audio.AudioSourceType
import com.pulsewave.visualizer.ui.settings.SettingsSheet
import com.pulsewave.visualizer.ui.settings.VisualizerStyle
import com.pulsewave.visualizer.ui.visualizer.AudioVisualization

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onOpenLibrary: () -> Unit,
) {
    val context = LocalContext.current
    val frame by viewModel.displayFrame.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    var immersive by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.selectSource(AudioSourceType.MICROPHONE) }

    val projectionManager = remember {
        context.getSystemService(MediaProjectionManager::class.java)
    }
    val systemCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewModel.onSystemCaptureGranted(result.resultCode, data)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                viewModel.updateSettings { it.copy(style = nextStyle(it.style)) }
            },
    ) {
        val albumArt = viewModel.albumArt
        if (settings.showAlbumArt && selectedSource == AudioSourceType.LIBRARY && albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp)
                    .alpha(0.45f),
            )
        }

        AudioVisualization(frame = frame, settings = settings, modifier = Modifier.fillMaxSize())

        if (!immersive) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    onOpenLibrary = onOpenLibrary,
                    onOpenSettings = { showSettings = true },
                    immersive = immersive,
                    onToggleImmersive = { immersive = !immersive },
                )

                SourceSwitcher(
                    selectedSource = selectedSource,
                    onSelect = { source ->
                        when (source) {
                            AudioSourceType.LIBRARY -> viewModel.selectSource(AudioSourceType.LIBRARY)
                            AudioSourceType.MICROPHONE -> {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                    PackageManager.PERMISSION_GRANTED
                                ) {
                                    viewModel.selectSource(AudioSourceType.MICROPHONE)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            AudioSourceType.SYSTEM_CAPTURE -> {
                                projectionManager?.let {
                                    systemCaptureLauncher.launch(it.createScreenCaptureIntent())
                                }
                            }
                        }
                    },
                )

                Box(modifier = Modifier.fillMaxSize().weight(1f, fill = false)) {}

                TransportControls(viewModel = viewModel, modifier = Modifier.fillMaxWidth())
            }
        } else {
            IconButton(
                onClick = { immersive = false },
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit fullscreen", tint = Color.White)
            }
        }

        if (showSettings) {
            val presets by viewModel.presets.collectAsState()
            SettingsSheet(
                settings = settings,
                presets = presets,
                onSettingsChange = viewModel::updateSettings,
                onSavePreset = viewModel::saveCurrentAsPreset,
                onLoadPreset = viewModel::loadPreset,
                onDeletePreset = viewModel::deletePreset,
                onDismiss = { showSettings = false },
            )
        }
    }
}

private fun nextStyle(current: VisualizerStyle): VisualizerStyle {
    val values = VisualizerStyle.entries
    val next = (values.indexOf(current) + 1) % values.size
    return values[next]
}

@Composable
private fun TopBar(
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    immersive: Boolean,
    onToggleImmersive: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "PulseWave",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row {
            IconButton(onClick = onOpenLibrary) {
                Icon(Icons.Filled.LibraryMusic, contentDescription = "Library", tint = Color.White)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
            }
            IconButton(onClick = onToggleImmersive) {
                Icon(
                    if (immersive) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = "Toggle fullscreen",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SourceSwitcher(selectedSource: AudioSourceType, onSelect: (AudioSourceType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SourceChip(
            label = "Library",
            icon = Icons.Filled.LibraryMusic,
            selected = selectedSource == AudioSourceType.LIBRARY,
            onClick = { onSelect(AudioSourceType.LIBRARY) },
        )
        SourceChip(
            label = "Mic",
            icon = Icons.Filled.Mic,
            selected = selectedSource == AudioSourceType.MICROPHONE,
            onClick = { onSelect(AudioSourceType.MICROPHONE) },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SourceChip(
                label = "System audio",
                icon = Icons.Filled.RssFeed,
                selected = selectedSource == AudioSourceType.SYSTEM_CAPTURE,
                onClick = { onSelect(AudioSourceType.SYSTEM_CAPTURE) },
            )
        }
    }
}

@Composable
private fun SourceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

@Composable
private fun TransportControls(viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val track = viewModel.currentTrack()

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(12.dp),
    ) {
        Text(
            text = track?.title ?: "Nothing playing",
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = track?.artist ?: "Pick a track from your library",
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )

        Slider(
            value = viewModel.positionMs.toFloat().coerceAtMost(viewModel.durationMs.toFloat().coerceAtLeast(1f)),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..viewModel.durationMs.toFloat().coerceAtLeast(1f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::toggleShuffle) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (viewModel.shuffleEnabled) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
            IconButton(onClick = viewModel::skipPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
            }
            IconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.size(56.dp)) {
                Icon(
                    if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = viewModel::skipNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
            }
            IconButton(onClick = viewModel::toggleRepeat) {
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (viewModel.repeatAll) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
        }
    }
}
