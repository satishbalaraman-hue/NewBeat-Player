package com.example.model

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val uri: Uri,
    val durationMs: Long,
    val size: Long = 0,
    val genre: String = "Unknown",
    val isDemo: Boolean = false,
    val sampleRateKhz: Float = 44.1f,
    val bitDepth: Int = 16,
    val format: String = "MP3",
    val albumArtUri: Uri? = null
) {
    val isHighRes: Boolean
        get() = sampleRateKhz >= 48.0f || bitDepth > 16
}
