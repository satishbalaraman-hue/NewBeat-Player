package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.components.AlbumArt
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LibraryTab
import com.example.util.neumorphic
import com.example.viewmodel.MusicPlayerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val filteredTracks by viewModel.tracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var selectedAlbumName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistAlbumName by rememberSaveable { mutableStateOf<String?>(null) }

    // Determine target permission by OS version
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(permission = audioPermission) { granted ->
        if (granted) {
            viewModel.scanMedia()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF44474A)
        val secondaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7E858D)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
        // App title & Subtitle
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
            }

            // Sync Media Files from Storage
            Box(
                modifier = Modifier
                    .neumorphic(cornerRadius = 20.dp, elevation = 3.dp)
                    .clickable {
                        if (permissionState.status.isGranted) {
                            viewModel.scanMedia()
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("scan_storage_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan Media",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter tabs (Songs, Albums, Artists, Genres)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LibraryTabItem(
                text = "Tracks",
                selected = selectedTab == LibraryTab.TRACKS,
                onClick = {
                    selectedAlbumName = null
                    selectedArtistName = null
                    selectedArtistAlbumName = null
                    viewModel.setSelectedTab(LibraryTab.TRACKS)
                },
                icon = Icons.Default.MusicNote
            )
            LibraryTabItem(
                text = "Albums",
                selected = selectedTab == LibraryTab.ALBUMS,
                onClick = {
                    selectedAlbumName = null
                    selectedArtistName = null
                    selectedArtistAlbumName = null
                    viewModel.setSelectedTab(LibraryTab.ALBUMS)
                },
                icon = Icons.Default.Album
            )
            LibraryTabItem(
                text = "Artists",
                selected = selectedTab == LibraryTab.ARTISTS,
                onClick = {
                    selectedAlbumName = null
                    selectedArtistName = null
                    selectedArtistAlbumName = null
                    viewModel.setSelectedTab(LibraryTab.ARTISTS)
                },
                icon = Icons.Default.Person
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (filteredTracks.isEmpty()) {
                EmptyLibraryState(
                    hasQuery = false,
                    onRegenDemo = { viewModel.loadDemoLibrary() }
                )
            } else {
                when (selectedTab) {
                    LibraryTab.TRACKS -> {
                        TrackListView(
                            tracks = filteredTracks,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackSelect = { clickedTrack ->
                                viewModel.selectTrack(clickedTrack, filteredTracks)
                            }
                        )
                    }
                    LibraryTab.ALBUMS -> {
                        if (selectedAlbumName == null) {
                            AlbumGridScreen(
                                tracks = filteredTracks,
                                onAlbumSelect = { albumName ->
                                    selectedAlbumName = albumName
                                }
                            )
                        } else {
                            val albumTracks = filteredTracks.filter { it.album == selectedAlbumName }
                            LaunchedEffect(albumTracks) {
                                if (albumTracks.isEmpty()) {
                                    selectedAlbumName = null
                                }
                            }
                            if (albumTracks.isNotEmpty()) {
                                AlbumDetailScreen(
                                    albumName = selectedAlbumName!!,
                                    albumTracks = albumTracks,
                                    onBack = { selectedAlbumName = null },
                                    onSongSelect = { clickedTrack ->
                                        viewModel.selectTrack(clickedTrack, albumTracks)
                                    }
                                )
                            }
                        }
                    }
                    LibraryTab.ARTISTS -> {
                        if (selectedArtistName == null) {
                            ArtistGridScreen(
                                tracks = filteredTracks,
                                onArtistSelect = { artistName ->
                                    selectedArtistName = artistName
                                }
                            )
                        } else if (selectedArtistAlbumName == null) {
                            val artistTracks = filteredTracks.filter { it.artist == selectedArtistName }
                            LaunchedEffect(artistTracks) {
                                if (artistTracks.isEmpty()) {
                                    selectedArtistName = null
                                }
                            }
                            if (artistTracks.isNotEmpty()) {
                                ArtistAlbumsScreen(
                                    artistName = selectedArtistName!!,
                                    artistTracks = artistTracks,
                                    onBack = { selectedArtistName = null },
                                    onAlbumSelect = { albumName ->
                                        selectedArtistAlbumName = albumName
                                    }
                                )
                            }
                        } else {
                            val artistAlbumTracks = filteredTracks.filter {
                                it.artist == selectedArtistName && it.album == selectedArtistAlbumName
                            }
                            LaunchedEffect(artistAlbumTracks) {
                                if (artistAlbumTracks.isEmpty()) {
                                    selectedArtistAlbumName = null
                                }
                            }
                            if (artistAlbumTracks.isNotEmpty()) {
                                AlbumDetailScreen(
                                    albumName = selectedArtistAlbumName!!,
                                    albumTracks = artistAlbumTracks,
                                    onBack = { selectedArtistAlbumName = null },
                                    onSongSelect = { clickedTrack ->
                                        viewModel.selectTrack(clickedTrack, artistAlbumTracks)
                                    }
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        }
    }
}

@Composable
fun LibraryTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Box(
        modifier = Modifier
            .height(36.dp)
            .neumorphic(
                cornerRadius = 20.dp,
                elevation = if (selected) 4.dp else 2.dp,
                isPressed = !selected,
                accentColor = if (selected) MaterialTheme.colorScheme.primary else null
            )
            .clickable(onClick = onClick)
            .testTag("tab_chip_${text.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun TrackListView(
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackSelect: (Track) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            val isActive = currentTrack?.id == track.id
            TrackRowItem(
                track = track,
                isActive = isActive,
                isPlaying = isPlaying,
                onClick = { onTrackSelect(track) }
            )
        }
    }
}

@Composable
fun TrackRowItem(
    track: Track,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF44474A)
    val secondaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7E858D)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(
                cornerRadius = 12.dp,
                elevation = if (isActive) 2.dp else 4.dp,
                isPressed = isActive,
                accentColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else null
            )
            .clickable(onClick = onClick)
            .testTag("track_row_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art with sunken picture frame effect
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .neumorphic(cornerRadius = 12.dp, elevation = 2.dp, isPressed = true)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AlbumArt(track = track, size = 52.dp, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, // Bold weight for primary title
                    color = if (isActive) MaterialTheme.colorScheme.primary else primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        fontSize = 12.sp, // Inter scale body-xs
                        fontWeight = FontWeight.Normal,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = secondaryTextColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = track.album,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = secondaryTextColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Spec / Details Badges
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (track.isHighRes) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "HI-RES",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = formatMs(track.durationMs),
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
            }
        }
    }
}

@Composable
fun AlbumGridScreen(
    tracks: List<Track>,
    onAlbumSelect: (String) -> Unit
) {
    val albums = remember(tracks) {
        tracks.groupBy { it.album }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums.keys.toList(), key = { it }) { albumTitle ->
            val albumTracks = albums[albumTitle] ?: emptyList()
            AlbumGridItem(
                albumTitle = albumTitle,
                albumTracks = albumTracks,
                onClick = { onAlbumSelect(albumTitle) }
            )
        }
    }
}

@Composable
fun AlbumGridItem(
    albumTitle: String,
    albumTracks: List<Track>,
    onClick: () -> Unit
) {
    val sampleTrack = albumTracks.firstOrNull()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF44474A)
    val secondaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7E858D)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(cornerRadius = 20.dp, elevation = 4.dp) // Standard standard radius
            .clickable(onClick = onClick)
            .testTag("album_grid_item_$albumTitle")
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Recessed picture frame album art container (24.dp corner radius per specs)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .neumorphic(cornerRadius = 24.dp, elevation = 3.dp, isPressed = true)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                AlbumArt(
                    track = sampleTrack,
                    size = 200.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = albumTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sampleTrack?.artist ?: "Unknown Artist",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasHiRes = albumTracks.any { it.isHighRes }
                if (hasHiRes) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "HI-RES",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = "${albumTracks.size} ${if (albumTracks.size == 1) "Track" else "Tracks"}",
                    fontSize = 11.sp,
                    color = secondaryTextColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    albumName: String,
    albumTracks: List<Track>,
    onBack: () -> Unit,
    onSongSelect: (Track) -> Unit
) {
    val sampleTrack = albumTracks.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .testTag("album_detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Albums Grid",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Back to Albums",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF44474A)
        val secondaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7E858D)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(cornerRadius = 20.dp, elevation = 4.dp) // Standard radius
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Recessed picture frame album art (24.dp corner radius per specs)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .neumorphic(cornerRadius = 24.dp, elevation = 3.dp, isPressed = true)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                AlbumArt(
                    track = sampleTrack,
                    size = 100.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = albumName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sampleTrack?.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${albumTracks.size} High-Fidelity Tracks",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor.copy(alpha = 0.8f)
                )
            }
        }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tracks",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = secondaryTextColor,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), // Spacing is 12px/12.dp per spec
            modifier = Modifier.fillMaxWidth()
        ) {
            items(albumTracks, key = { it.id }) { track ->
                AlbumDetailTrackRow(
                    track = track,
                    onClick = { onSongSelect(track) }
                )
            }
        }
    }
}

@Composable
fun AlbumDetailTrackRow(
    track: Track,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF44474A)
    val secondaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7E858D)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(cornerRadius = 12.dp, elevation = 3.dp) // Playlist song item radius 12.dp per spec
            .clickable(onClick = onClick)
            .testTag("album_detail_track_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (track.isHighRes) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "HI-RES",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatMs(track.durationMs),
                fontSize = 13.sp,
                color = secondaryTextColor
            )
        }
    }
}

@Composable
fun ArtistGridScreen(
    tracks: List<Track>,
    onArtistSelect: (String) -> Unit
) {
    val artists = remember(tracks) {
        tracks.groupBy { it.artist }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), // spacedBy 12px/12.dp per spec
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists.keys.toList(), key = { it }) { artistName ->
            val artistTracks = artists[artistName] ?: emptyList()
            ArtistListItem(
                artistName = artistName,
                artistTracks = artistTracks,
                onClick = { onArtistSelect(artistName) }
            )
        }
    }
}

@Composable
fun ArtistListItem(
    artistName: String,
    artistTracks: List<Track>,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF44474A)
    val secondaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7E858D)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(cornerRadius = 12.dp, elevation = 3.dp) // List item 12.dp radius
            .clickable(onClick = onClick)
            .testTag("artist_list_item_$artistName")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artistName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val uniqueAlbumsCount = artistTracks.map { it.album }.distinct().size
                Text(
                    text = "$uniqueAlbumsCount ${if (uniqueAlbumsCount == 1) "Album" else "Albums"} • ${artistTracks.size} ${if (artistTracks.size == 1) "Track" else "Tracks"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryTextColor
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Albums",
                tint = secondaryTextColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ArtistAlbumsScreen(
    artistName: String,
    artistTracks: List<Track>,
    onBack: () -> Unit,
    onAlbumSelect: (String) -> Unit
) {
    val albumsOfArtist = remember(artistTracks) {
        artistTracks.groupBy { it.album }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("artist_albums_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Artists",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Back to Artists",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = artistName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Text(
            text = "Albums",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF44474A)
        val secondaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7E858D)

        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), // spacing 12px/12.dp per spec
            modifier = Modifier.fillMaxWidth()
        ) {
            items(albumsOfArtist.keys.toList(), key = { it }) { albumTitle ->
                val albumTracks = albumsOfArtist[albumTitle] ?: emptyList()
                val sampleTrack = albumTracks.firstOrNull()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(cornerRadius = 12.dp, elevation = 4.dp) // List items 12.dp radius
                        .clickable { onAlbumSelect(albumTitle) }
                        .testTag("artist_album_item_$albumTitle")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Recessed picture frame album art
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .neumorphic(cornerRadius = 16.dp, elevation = 2.dp, isPressed = true)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AlbumArt(
                                track = sampleTrack,
                                size = 64.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = albumTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${albumTracks.size} ${if (albumTracks.size == 1) "Track" else "Tracks"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = secondaryTextColor
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View Tracks",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryState(
    hasQuery: Boolean,
    onRegenDemo: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryTextColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF44474A)
    val secondaryTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7E858D)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .neumorphic(cornerRadius = 48.dp, elevation = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (hasQuery) "No matching tracks found" else "Your library is empty",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = primaryTextColor
        )
        Text(
            text = if (hasQuery) "Try adjusting your search criteria" else "Scan your active Android storage structure or load high-resolution synthesized tracks below.",
            fontSize = 13.sp,
            color = secondaryTextColor,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (!hasQuery) {
            Box(
                modifier = Modifier
                    .neumorphic(cornerRadius = 20.dp, elevation = 4.dp)
                    .clickable(onClick = onRegenDemo)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("load_demo_library_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Generate Demo High-Res Music",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun formatMs(ms: Long): String {
    val secTotal = ms / 1000
    val min = secTotal / 60
    val sec = secTotal % 60
    return String.format("%02d:%02d", min, sec)
}
