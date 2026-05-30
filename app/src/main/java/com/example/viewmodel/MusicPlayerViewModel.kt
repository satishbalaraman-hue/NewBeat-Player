package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.Track
import com.example.model.TrackEntity
import com.example.db.AppDatabase
import com.example.util.AudioSynthesizer
import com.example.util.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@OptIn(androidx.media3.common.util.UnstableApi::class)
class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MusicPlayerViewModel"

    // Master Track Library
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    // Playlist Search & Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(LibraryTab.TRACKS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    val filteredTracks: StateFlow<List<Track>> = combine(
        _tracks,
        _searchQuery
    ) { trackList, query ->
        if (query.isBlank()) {
            trackList
        } else {
            trackList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ExoPlayer instances
    private var player: ExoPlayer? = null

    // Playback state flows
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _trackDuration = MutableStateFlow(0L)
    val trackDuration: StateFlow<Long> = _trackDuration.asStateFlow()

    private val _repeatMode = MutableStateFlow(PlaybackRepeatMode.OFF)
    val repeatMode: StateFlow<PlaybackRepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<List<Track>>(emptyList())
    val currentPlaylist: StateFlow<List<Track>> = _currentPlaylist.asStateFlow()



    // Themes & Custom Accent colors
    private val _accentColor = MutableStateFlow(
        Color(
            value = getApplication<Application>()
                .getSharedPreferences("music_player_settings", android.content.Context.MODE_PRIVATE)
                .getLong("accent_color_value", ColorPresets.RoyalAmethyst.value.toLong()).toULong()
        )
    )
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT) // default beautiful light mode
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Polling job for track seek position
    private var positionPoller: Job? = null

    init {
        // Automatically load demo tracks and persisted local tracks from database
        loadLibrary()
        initializePlayer()

        // Auto-refresh library if storage read permission is already granted
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val hasPermission = getApplication<Application>().checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            scanMedia()
        }

        // Dynamic Accent Color Auto-Extraction on track changes
        viewModelScope.launch {
            _currentTrack.collect { track ->
                if (track != null) {
                    val color = withContext(Dispatchers.IO) {
                        extractMajorColor(track)
                    }
                    _accentColor.value = color
                    getApplication<Application>()
                        .getSharedPreferences("music_player_settings", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putLong("accent_color_value", color.value.toLong())
                        .apply()
                }
            }
        }
    }

    private fun initializePlayer() {
        if (player != null) return

        try {
            val context = getApplication<Application>()
            player = ExoPlayer.Builder(context).build().apply {
                // Media3 handles gapless audio transition perfectly by default in its playlist engine!
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        _isPlaying.value = playWhenReady && state == Player.STATE_READY
                        if (state == Player.STATE_READY) {
                            _trackDuration.value = duration.coerceAtLeast(0L)
                            startPositionPolling()
                        } else if (state == Player.STATE_ENDED) {
                            stopPositionPolling()
                            _isPlaying.value = false
                            _playbackPosition.value = 0
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            startPositionPolling()
                        } else {
                            stopPositionPolling()
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // Locate the active playing track in our list
                        mediaItem?.mediaId?.toLongOrNull()?.let { trackId ->
                            val activeTrack = _tracks.value.find { it.id == trackId }
                            _currentTrack.value = activeTrack
                            _playbackPosition.value = 0L
                            _trackDuration.value = duration.coerceAtLeast(0L)
                        }
                    }


                })
            }
            Log.d(TAG, "ExoPlayer Engine initialized successfully.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize ExoPlayer", e)
        }
    }

    private fun startPositionPolling() {
        positionPoller?.cancel()
        positionPoller = viewModelScope.launch {
            while (true) {
                player?.let {
                    if (it.isPlaying) {
                        _playbackPosition.value = it.currentPosition
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPoller?.cancel()
        positionPoller = null
    }

    // Media Actions
    private fun loadLibrary() {
        viewModelScope.launch {
            val combined = withContext(Dispatchers.IO) {
                val demoTracks = AudioSynthesizer.generateDemoTracks(getApplication())
                val db = AppDatabase.getDatabase(getApplication())
                val savedEntities = try {
                    db.trackDao().getAllTracks()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load saved tracks", e)
                    emptyList()
                }
                val savedTracks = savedEntities.map { it.toTrack() }
                demoTracks + savedTracks
            }
            _tracks.value = combined
            Log.d(TAG, "Library loaded on startup. Total tracks: ${combined.size}")
        }
    }

    fun scanMedia() {
        viewModelScope.launch {
            val combinedTracks = withContext(Dispatchers.IO) {
                val localTracks = MediaScanner.scanLocalMedia(getApplication())
                val demoTracks = AudioSynthesizer.generateDemoTracks(getApplication())
                
                // Save newly scanned local tracks to Room
                val db = AppDatabase.getDatabase(getApplication())
                try {
                    db.trackDao().clearAll()
                    db.trackDao().insertAll(localTracks.map { TrackEntity.fromTrack(it) })
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save scanned tracks to database", e)
                }
                
                localTracks + demoTracks
            }
            // Merge actual storage files with high-res synthesizer demo loop tracks
            _tracks.value = combinedTracks
            Log.d(TAG, "Media scanning completed. Total: ${_tracks.value.size}")
        }
    }

    fun loadDemoLibrary() {
        viewModelScope.launch {
            val demoTracks = withContext(Dispatchers.IO) {
                AudioSynthesizer.generateDemoTracks(getApplication())
            }
            _tracks.value = demoTracks
        }
    }

    private fun getMimeTypeForTrack(context: android.content.Context, track: Track): String? {
        val uriStr = track.uri.toString()
        if (uriStr.startsWith("content://")) {
            try {
                val type = context.contentResolver.getType(track.uri)
                if (type != null) {
                    if (type.equals("audio/m4a", ignoreCase = true) || type.equals("audio/x-m4a", ignoreCase = true)) {
                        return "audio/mp4"
                    }
                    return type
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return when (track.format.uppercase()) {
            "MP3" -> "audio/mpeg"
            "M4A", "MP4" -> "audio/mp4"
            "WAV", "WAVE" -> "audio/wav"
            "FLAC" -> "audio/flac"
            "OGG", "OGA" -> "audio/ogg"
            "AAC" -> "audio/aac"
            else -> {
                // Guess from path extension
                val ext = track.path.substringAfterLast('.', "").uppercase()
                when (ext) {
                    "MP3" -> "audio/mpeg"
                    "M4A", "MP4" -> "audio/mp4"
                    "WAV", "WAVE" -> "audio/wav"
                    "FLAC" -> "audio/flac"
                    "OGG", "OGA" -> "audio/ogg"
                    "AAC" -> "audio/aac"
                    else -> null
                }
            }
        }
    }

    fun selectTrack(track: Track, fromList: List<Track>) {
        initializePlayer()
        val p = player ?: return

        _currentPlaylist.value = fromList
        _currentTrack.value = track

        val context = getApplication<Application>()
        // Clear existing playlist and inject selected playlist for seamless gapless playback
        p.clearMediaItems()
        val mediaItems = fromList.map { t ->
            val builder = MediaItem.Builder()
                .setUri(t.uri)
                .setMediaId(t.id.toString())
            getMimeTypeForTrack(context, t)?.let { mimeType ->
                builder.setMimeType(mimeType)
            }
            builder.build()
        }
        p.addMediaItems(mediaItems)

        val startIndex = fromList.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        p.seekTo(startIndex, 0L)
        p.playWhenReady = true
        p.prepare()
        p.play()

        // Sync extra player attributes
        syncExoPlayerShuffleState()
        syncExoPlayerRepeatState()
    }

    private fun extractMajorColor(track: Track): Color {
        if (track.isDemo || track.uri == android.net.Uri.EMPTY) {
            return getProceduralColor(track)
        }

        var bitmap: Bitmap? = null
        val context = getApplication<Application>()

        try {
            // Strategy 1: Load from albumArtUri if present
            val artUri = track.albumArtUri
            if (artUri != null && artUri != android.net.Uri.EMPTY) {
                try {
                    context.contentResolver.openInputStream(artUri)?.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4 // Safe downsampling
                        }
                        bitmap = BitmapFactory.decodeStream(stream, null, options)
                    }
                } catch (t: Throwable) {
                    // Ignore and try next
                }
            }

            // Strategy 2: Use loadThumbnail on Q+ (if Strategy 1 failed or wasn't available)
            if (bitmap == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    bitmap = context.contentResolver.loadThumbnail(track.uri, android.util.Size(120, 120), null)
                } catch (t: Throwable) {
                    // Ignore
                }
            }

            // Analyze decoded bitmap to extract major color
            val activeBitmap = bitmap
            if (activeBitmap != null) {
                val scaled = Bitmap.createScaledBitmap(activeBitmap, 12, 12, true)
                val width = scaled.width
                val height = scaled.height
                val pixels = IntArray(width * height)
                scaled.getPixels(pixels, 0, width, 0, 0, width, height)

                var bestColor = -1
                var maxScore = -1f
                var sumR = 0L
                var sumG = 0L
                var sumB = 0L
                var validCount = 0

                val hsv = FloatArray(3)
                for (pixel in pixels) {
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    sumR += r
                    sumG += g
                    sumB += b
                    validCount++

                    android.graphics.Color.colorToHSV(pixel, hsv)
                    val s = hsv[1]
                    val v = hsv[2]

                    // Prioritize distinct, highly saturated colors suited for active UI elements
                    if (s > 0.25f && v > 0.25f && v < 0.9f) {
                        val score = s * v
                        if (score > maxScore) {
                            maxScore = score
                            bestColor = pixel
                        }
                    }
                }

                if (bestColor != -1) {
                    val r = (bestColor shr 16) and 0xFF
                    val g = (bestColor shr 8) and 0xFF
                    val b = bestColor and 0xFF
                    return Color(r, g, b)
                } else if (validCount > 0) {
                    val avgR = (sumR / validCount).toInt()
                    val avgG = (sumG / validCount).toInt()
                    val avgB = (sumB / validCount).toInt()
                    return Color(avgR, avgG, avgB)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error extracting dynamic color: ${e.message}", e)
        }

        return getProceduralColor(track)
    }

    private fun getProceduralColor(track: Track): Color {
        val seed = track.title.hashCode().absoluteValue
        val hues = listOf(
            Color(0xFFFF006E), // Rose Pink
            Color(0xFF3A86FF), // Neon Blue
            Color(0xFF8338EC), // Purple
            Color(0xFF06D6A0), // Green Teal
            Color(0xFFFFBE0B), // Vibrant Yellow
            Color(0xFFFB5607)  // Flame Orange
        )
        return hues[seed % hues.size]
    }

    fun togglePlayPause() {
        player?.let { p ->
            if (p.isPlaying) {
                p.pause()
            } else {
                if (p.playbackState == Player.STATE_ENDED) {
                    p.seekTo(0, 0L)
                }
                p.play()
            }
        }
    }

    fun skipToNext() {
        player?.let { p ->
            if (p.hasNextMediaItem()) {
                p.seekToNextMediaItem()
            } else {
                // Wrap around gapless queue or stop
                p.seekTo(0, 0L)
            }
        }
    }

    fun skipToPrevious() {
        player?.let { p ->
            if (p.hasPreviousMediaItem()) {
                p.seekToPreviousMediaItem()
            } else {
                p.seekTo(0, 0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player?.let { p ->
            p.seekTo(positionMs)
            _playbackPosition.value = positionMs
        }
    }

    fun toggleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            PlaybackRepeatMode.OFF -> PlaybackRepeatMode.ALL
            PlaybackRepeatMode.ALL -> PlaybackRepeatMode.ONE
            PlaybackRepeatMode.ONE -> PlaybackRepeatMode.OFF
        }
        _repeatMode.value = nextMode
        syncExoPlayerRepeatState()
    }

    private fun syncExoPlayerRepeatState() {
        player?.let { p ->
            p.repeatMode = when (_repeatMode.value) {
                PlaybackRepeatMode.OFF -> Player.REPEAT_MODE_OFF
                PlaybackRepeatMode.ALL -> Player.REPEAT_MODE_ALL
                PlaybackRepeatMode.ONE -> Player.REPEAT_MODE_ONE
            }
        }
    }

    fun toggleShuffleMode() {
        val nextShuffle = !_shuffleMode.value
        _shuffleMode.value = nextShuffle
        syncExoPlayerShuffleState()
    }

    private fun syncExoPlayerShuffleState() {
        player?.let { p ->
            p.shuffleModeEnabled = _shuffleMode.value
        }
    }

    // Cosmetics
    fun setAccentColor(color: Color) {
        _accentColor.value = color
        getApplication<Application>()
            .getSharedPreferences("music_player_settings", android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong("accent_color_value", color.value.toLong())
            .apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setSelectedTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionPolling()
        try {
            player?.release()
        } catch (t: Throwable) {
            Log.e(TAG, "Error releasing ExoPlayer in onCleared", t)
        }
        player = null
        Log.d(TAG, "Released ExoPlayer engine inside cleared ViewModel.")
    }
}

enum class LibraryTab {
    TRACKS,
    ALBUMS,
    ARTISTS,
    GENRES
}

enum class PlaybackRepeatMode {
    OFF,
    ALL,
    ONE
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

object ColorPresets {
    val RoyalAmethyst = Color(0xFF8338EC)
    val MidnightBlue = Color(0xFF3A86FF)
    val EmeraldGreen = Color(0xFF06D6A0)
    val GoldenAmber = Color(0xFFF77F00)
    val ParadisePink = Color(0xFFFF006E)
    val CrimsonRed = Color(0xFFD90429)

    val AllPresets = listOf(
        RoyalAmethyst,
        MidnightBlue,
        EmeraldGreen,
        GoldenAmber,
        ParadisePink,
        CrimsonRed
    )

    fun getName(color: Color): String {
        return when (color) {
            RoyalAmethyst -> "Royal Amethyst"
            MidnightBlue -> "Midnight Blue"
            EmeraldGreen -> "Emerald Green"
            GoldenAmber -> "Golden Amber"
            ParadisePink -> "Paradise Pink"
            CrimsonRed -> "Crimson Red"
            else -> "Custom Accent"
        }
    }
}
