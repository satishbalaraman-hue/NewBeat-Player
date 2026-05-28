package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.net.Uri

@Entity(tableName = "scanned_tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val uriString: String,
    val durationMs: Long,
    val size: Long,
    val genre: String,
    val isDemo: Boolean,
    val sampleRateKhz: Float,
    val bitDepth: Int,
    val format: String,
    val albumArtUriString: String?
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        path = path,
        uri = Uri.parse(uriString),
        durationMs = durationMs,
        size = size,
        genre = genre,
        isDemo = isDemo,
        sampleRateKhz = sampleRateKhz,
        bitDepth = bitDepth,
        format = format,
        albumArtUri = albumArtUriString?.let { Uri.parse(it) }
    )

    companion object {
        fun fromTrack(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            path = track.path,
            uriString = track.uri.toString(),
            durationMs = track.durationMs,
            size = track.size,
            genre = track.genre,
            isDemo = track.isDemo,
            sampleRateKhz = track.sampleRateKhz,
            bitDepth = track.bitDepth,
            format = track.format,
            albumArtUriString = track.albumArtUri?.toString()
        )
    }
}
