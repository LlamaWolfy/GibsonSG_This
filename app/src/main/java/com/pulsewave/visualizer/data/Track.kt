package com.pulsewave.visualizer.data

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: Uri,
)
