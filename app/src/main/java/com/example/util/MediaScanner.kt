package com.example.util

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.model.Track
import java.io.File

object MediaScanner {
    private const val TAG = "MediaScanner"

    fun scanLocalMedia(context: Context): List<Track> {
        val attributedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.createAttributionContext("attribution")
            } catch (t: Throwable) {
                context
            }
        } else {
            context
        }
        val tracks = mutableListOf<Track>()

        // Check permission before querying
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val hasPermission = attributedContext.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w(TAG, "Read audio storage permission is not granted. Cannot scan MediaStore.")
            return emptyList()
        }

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Select only music files (is_music != 0) or any audio mime type to catch m4a files
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            attributedContext.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val path = cursor.getString(pathCol) ?: ""
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val albumId = cursor.getLong(albumIdCol)

                    // Determine format from file path
                    val file = File(path)
                    val format = file.extension.uppercase()

                    // Estimate technical qualities based on file properties
                    // Since MediaStore doesn't give sampleRate/bitDepth directly, defaults or metadata inspection is used.
                    val sampleRate = if (format == "WAV" || format == "FLAC") 48.0f else 44.1f
                    val bitDepth = if (format == "FLAC" || format == "WAV") 24 else 16

                    val trackUri = android.content.ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val sArtworkUri = android.net.Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = android.content.ContentUris.withAppendedId(sArtworkUri, albumId)

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            path = path,
                            uri = trackUri,
                            durationMs = duration,
                            size = size,
                            genre = "Local Audio",
                            isDemo = false,
                            sampleRateKhz = sampleRate,
                            bitDepth = bitDepth,
                            format = format,
                            albumArtUri = albumArtUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore", e)
        }

        return tracks
    }
}
