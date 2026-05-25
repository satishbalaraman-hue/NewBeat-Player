package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

object AlbumArtCache {
    private val cache = android.util.LruCache<Long, ByteArray>(100) // cache up to 100 tracks' artwork bytes

    fun get(trackId: Long): ByteArray? {
        return cache.get(trackId)
    }

    fun put(trackId: Long, bytes: ByteArray) {
        cache.put(trackId, bytes)
    }
}

@Composable
fun AlbumArt(
    track: Track?,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val context = LocalContext.current
    val trackId = track?.id
    val trackUri = track?.uri
    val isDemo = track?.isDemo == true
    val albumArtUri = track?.albumArtUri

    val artworkSource = remember(trackId) { mutableStateOf<Any?>(null) }

    LaunchedEffect(trackId, trackUri, albumArtUri) {
        if (trackId != null && trackUri != null && !isDemo && trackUri != android.net.Uri.EMPTY) {
            val cached = AlbumArtCache.get(trackId)
            if (cached != null) {
                artworkSource.value = cached
            } else {
                val resolved: Any? = withContext(Dispatchers.IO) {
                    // Method 1: Direct MediaStore AlbumArt Content URI check
                    if (albumArtUri != null && albumArtUri != android.net.Uri.EMPTY) {
                        try {
                            context.contentResolver.openInputStream(albumArtUri)?.use {
                                // successfully opened, means the media art file exists and is readable
                            }
                            return@withContext albumArtUri
                        } catch (t: Throwable) {
                            // ignore and try next method
                        }
                    }

                    // Method 2: ContentResolver loadThumbnail on API 29+ (loads direct bitmap very efficiently)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        try {
                            val bitmap = context.contentResolver.loadThumbnail(trackUri, android.util.Size(300, 300), null)
                            return@withContext bitmap
                        } catch (t: Throwable) {
                            // ignore
                        }
                    }
                    null
                }

                if (resolved != null) {
                    if (resolved is ByteArray) {
                        AlbumArtCache.put(trackId, resolved)
                    }
                    artworkSource.value = resolved
                } else {
                    artworkSource.value = null
                }
            }
        } else {
            artworkSource.value = null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Always draw gorgeous procedural art as the base layer
        ProceduralArt(track = track)

        artworkSource.value?.let { source ->
            val request = remember(source, trackId) {
                ImageRequest.Builder(context)
                    .data(source)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = "Album Art for ${track?.title}",
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center
            )
        }
    }
}

@Composable
fun ProceduralArt(track: Track?) {
    val title = track?.title ?: "No Track"
    val artist = track?.artist ?: "Unknown Artist"

    // Derive stable abstract seed values based on character sums to guarantee identical colors on re-renders
    val seed1 = remember(title) { title.hashCode().absoluteValue }
    val seed2 = remember(artist) { artist.hashCode().absoluteValue }

    val gradientBaseColor1 = remember(seed1) {
        val hues = listOf(
            Color(0xFFFF006E), // Rose Pink
            Color(0xFF3A86FF), // Neon Blue
            Color(0xFF8338EC), // Purple
            Color(0xFF06D6A0), // Green Teal
            Color(0xFFFFBE0B), // Vibrant Yellow
            Color(0xFFFB5607)  // Flame Orange
        )
        hues[seed1 % hues.size]
    }

    val gradientBaseColor2 = remember(seed2) {
        val hues = listOf(
            Color(0xFF2EC4B6), // Light Teal
            Color(0xFFFF9F1C), // Apricot
            Color(0xFFE0115F), // Ruby Red
            Color(0xFF70E000), // Lime Green
            Color(0xFF00F5D4), // Cyan
            Color(0xFF9B5DE5)  // Amethyst Purple
        )
        hues[seed2 % hues.size]
    }

    val finalBrush = remember(gradientBaseColor1, gradientBaseColor2) {
        Brush.linearGradient(
            colors = listOf(gradientBaseColor1, gradientBaseColor2, Color(0xFF0F0C1B)),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(finalBrush),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.85f)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // Drawing clean geometric layered shapes with continuous visual balance
            val ringCount = 3 + (seed1 % 4) // between 3 and 6 concentric lines
            val separation = (canvasWidth / 2.3f) / ringCount

            for (i in 0 until ringCount) {
                val radius = (i + 1) * separation
                val opacity = 0.15f + (0.15f * (ringCount - i))
                
                // concentric circles alternating filled and stroke style
                if (i % 2 == 0) {
                    drawCircle(
                        color = Color.White.copy(alpha = opacity * 0.4f),
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )
                }
                
                drawCircle(
                    color = Color.White.copy(alpha = opacity),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Draw crosshairs or minimal geometric indicators representing speed/rotations (vinyl micro motif)
            val lineLength = 12.dp.toPx()
            val offsetDist = 6.dp.toPx()

            // Center core
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(centerX, centerY)
            )

            // Minimalist ticks
            // Top tick
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(centerX, centerY - offsetDist - lineLength),
                end = Offset(centerX, centerY - offsetDist),
                strokeWidth = 1.5.dp.toPx()
            )
            // Bottom tick
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(centerX, centerY + offsetDist),
                end = Offset(centerX, centerY + offsetDist + lineLength),
                strokeWidth = 1.5.dp.toPx()
            )
            // Left tick
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(centerX - offsetDist - lineLength, centerY),
                end = Offset(centerX - offsetDist, centerY),
                strokeWidth = 1.5.dp.toPx()
            )
            // Right tick
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(centerX + offsetDist, centerY),
                end = Offset(centerX + offsetDist + lineLength, centerY),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}
