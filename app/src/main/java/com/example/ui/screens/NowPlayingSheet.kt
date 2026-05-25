package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.components.AlbumArt
import com.example.viewmodel.MusicPlayerViewModel
import com.example.viewmodel.PlaybackRepeatMode

@Composable
fun NowPlayingMiniAndFullPlayer(
    viewModel: MusicPlayerViewModel,
    expanded: Boolean,
    onExpandChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.trackDuration.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()

    val track = currentTrack ?: return

    Box(modifier = modifier.fillMaxWidth()) {
        // 1. Collapsed Mini Launcher Bar (Persistent Bottom Anchor)
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clickable { onExpandChanged(true) }
                    .testTag("now_playing_mini_bar"),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArt(track = track, size = 48.dp)
                    
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Tech Qualities Badge on mini bar
                    if (track.isHighRes) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            contentColor = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                "HR",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Controls
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "play or pause music",
                            tint = accentColor
                        )
                    }

                    IconButton(
                        onClick = { viewModel.skipToNext() },
                        modifier = Modifier.testTag("mini_player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "skip to next track",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Full-Screen Slide-Up Immersive Console Playing Dashboard
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
            ) + fadeOut()
        ) {
            FullPlayerConsole(
                track = track,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                accentColor = accentColor,
                viewModel = viewModel,
                onCollapse = { onExpandChanged(false) }
            )
        }
    }
}

@Composable
fun FullPlayerConsole(
    track: Track,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    accentColor: Color,
    viewModel: MusicPlayerViewModel,
    onCollapse: () -> Unit
) {
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background
        )
    )

    // Pulse animation based of active audio playing state
    val scalePulse by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 0.98f,
        animationSpec = spring(stiffness = 8f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.testTag("full_player_dismiss")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse full screen player",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "NOW PLAYING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.width(48.dp)) // horizontal alignment solver
        }

        Spacer(modifier = Modifier.weight(0.6f))

        // Big detailed generated artwork
        Card(
            modifier = Modifier
                .size(270.dp)
                .scale(scalePulse)
                .clip(RoundedCornerShape(24.dp))
                .testTag("full_player_artwork_container"),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            AlbumArt(track = track, size = 270.dp)
        }

        Spacer(modifier = Modifier.weight(1.4f))

        // Tracks text info (Asymmetric dynamic spacing)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = track.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.artist,
                fontSize = 16.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.album,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress bar slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            val safeDuration = if (duration > 0) duration else 1L
            val sliderPos = position.toFloat().coerceIn(0f, safeDuration.toFloat())

            WaveformSeekBar(
                track = track,
                position = position,
                duration = duration,
                accentColor = accentColor,
                onSeek = { viewModel.seekTo(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("full_player_seek_slider")
            )

            // Duration text readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMs(position),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = formatMs(safeDuration),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main controls row (Skip prev, Shuffle, play pause, repeat, next)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = { viewModel.toggleShuffleMode() },
                modifier = Modifier.testTag("full_player_shuffle")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "toggle shuffle mode",
                    tint = if (shuffleMode) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Skip Prev
            IconButton(
                onClick = { viewModel.skipToPrevious() },
                modifier = Modifier.testTag("full_player_prev")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Large Circular elevated Play/Pause button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { viewModel.togglePlayPause() }
                    .testTag("full_player_play_pause"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "play pause",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Skip Next
            IconButton(
                onClick = { viewModel.skipToNext() },
                modifier = Modifier.testTag("full_player_next")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat Mode
            IconButton(
                onClick = { viewModel.toggleRepeatMode() },
                modifier = Modifier.testTag("full_player_repeat")
            ) {
                val repIcon = if (repeatMode == PlaybackRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                Icon(
                    imageVector = repIcon,
                    contentDescription = "cycle repeat mode",
                    tint = if (repeatMode != PlaybackRepeatMode.OFF) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1.2f))

        // Custom Audiophile specifications quality readout panel
        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .testTag("full_player_tech_specs_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AudioFile,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${track.format} • ${track.sampleRateKhz} kHz • ${track.bitDepth}-bit High-Res PCM • Gapless",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WaveformSeekBar(
    track: Track,
    position: Long,
    duration: Long,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = if (duration > 0) duration else 1L
    val progressFraction = (position.toFloat() / safeDuration).coerceIn(0f, 1f)
    
    // Track localized visual drag progress without flooding continuous async player updates on every single pixel movement
    var dragProgressFraction by remember(track.id) { mutableStateOf<Float?>(null) }
    val activeProgressFraction = dragProgressFraction ?: progressFraction

    val barCount = 45
    // Memoize the wave pattern for each specific track to avoid jumping/recalculating elements
    val waveHeights = remember(track.id, track.title) {
        val seed = (track.title.hashCode() xor track.artist.hashCode()).toLong()
        val random = java.util.Random(seed)
        FloatArray(barCount) { index ->
            val x = index.toFloat() / (barCount - 1)
            // Parabolic envelope to taper end ranges smoothly and create professional sound package look
            val envelope = (4f * x * (1f - x)).coerceIn(0.15f, 1.0f)
            val noise = 0.2f + random.nextFloat() * 0.8f
            (noise * envelope).coerceIn(0.08f, 1.0f)
        }
    }

    val unplayedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)

    BoxWithConstraints(
        modifier = modifier
    ) {
        val width = constraints.maxWidth.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(safeDuration, track.id) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / width).coerceIn(0f, 1f)
                        onSeek((fraction * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration, track.id) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragProgressFraction = (offset.x / width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            dragProgressFraction?.let { fraction ->
                                onSeek((fraction * safeDuration).toLong())
                            }
                            dragProgressFraction = null
                        },
                        onDragCancel = {
                            dragProgressFraction = null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / width).coerceIn(0f, 1f)
                            dragProgressFraction = fraction
                        }
                    )
                }
        ) {
            val totalBars = waveHeights.size
            val barSpacing = 3.dp.toPx()
            val totalSpacing = barSpacing * (totalBars - 1)
            val barWidth = (size.width - totalSpacing) / totalBars

            for (i in 0 until totalBars) {
                val barHeightFraction = waveHeights[i]
                val barHeight = size.height * barHeightFraction
                
                // Position vertically symmetric
                val top = (size.height - barHeight) / 2f
                val left = i * (barWidth + barSpacing)

                val barFraction = i.toFloat() / totalBars
                val isPlayed = barFraction <= activeProgressFraction

                val color = if (isPlayed) {
                    accentColor
                } else {
                    unplayedColor
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
