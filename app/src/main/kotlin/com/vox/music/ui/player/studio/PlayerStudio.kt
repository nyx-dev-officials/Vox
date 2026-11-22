/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Player Studio: the alternate expanded Now Playing surface. It is a complete,
 * self-contained stage rather than another garnish on the existing skins — one
 * opaque layer the sheet can show instead, so the vinyl deck, the cassette, the
 * typographic skin and the ambient OLED mode never have to negotiate space with
 * the classic controls.
 *
 * Warm Obsidian #0B0A09, Warm Champagne #E7C8A0. Everything that animates is
 * frame-driven and stops when playback stops; the fluid backdrop is skipped
 * under battery saver.
 */

package com.vox.music.ui.player.studio

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.vox.music.R
import com.vox.music.constants.StudioBackdropBlurKey
import com.vox.music.constants.StudioCoverRadiusKey
import com.vox.music.constants.StudioDaylightScrimKey
import com.vox.music.constants.StudioGrainKey
import com.vox.music.constants.StudioGyroKey
import com.vox.music.constants.StudioHapticsKey
import com.vox.music.constants.StudioRpmKey
import com.vox.music.constants.StudioSkinKey
import com.vox.music.constants.StudioTitleScaleKey
import com.vox.music.constants.StudioTitleWeightKey
import com.vox.music.lyrics.LyricsUtils
import com.vox.music.playback.PlayerConnection
import com.vox.music.utils.rememberPreference
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Obsidian = Color(0xFF0B0A09)
private val Champagne = Color(0xFFE7C8A0)

enum class StudioSkin { COVER, VINYL, CASSETTE, TYPE }

@Composable
fun PlayerStudio(
    playerConnection: PlayerConnection,
    onCollapse: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = playerConnection.player

    val metadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val dj by playerConnection.djState.collectAsStateWithLifecycle()
    val currentFormat by playerConnection.currentFormat.collectAsStateWithLifecycle(initialValue = null)
    val lyricsEntity by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)

    val (skinName) = rememberPreference(StudioSkinKey, defaultValue = "COVER")
    val (radiusDp) = rememberPreference(StudioCoverRadiusKey, defaultValue = 20)
    val (rpm) = rememberPreference(StudioRpmKey, defaultValue = 33)
    val (grain) = rememberPreference(StudioGrainKey, defaultValue = 0.16f)
    val (blurDp) = rememberPreference(StudioBackdropBlurKey, defaultValue = 36f)
    val (haptics) = rememberPreference(StudioHapticsKey, defaultValue = true)
    val (gyro) = rememberPreference(StudioGyroKey, defaultValue = true)
    val (daylight) = rememberPreference(StudioDaylightScrimKey, defaultValue = true)
    val (weightName) = rememberPreference(StudioTitleWeightKey, defaultValue = "BOLD")
    val (titleScale) = rememberPreference(StudioTitleScaleKey, defaultValue = 1f)

    val skin = StudioSkin.values().firstOrNull { it.name == skinName } ?: StudioSkin.COVER
    val titleWeight = when (weightName) {
        "LIGHT" -> FontWeight.Light
        "REGULAR" -> FontWeight.Normal
        else -> FontWeight.Bold
    }

    var ambient by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var inspector by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(flash) {
        if (flash != null) {
            delay(650L)
            flash = null
        }
    }

    val powerSaver = remember {
        context.getSystemService(android.os.PowerManager::class.java)?.isPowerSaveMode ?: false
    }
    val tilt = rememberTilt(gyro)
    val accent by rememberCoverAccent(metadata?.thumbnailUrl)
    val lux = rememberAmbientLux(daylight)
    val scrim = if (daylight && lux > 9_000f) 0.46f else 0.26f

    val position by produceState(0L, isPlaying, metadata?.id) {
        while (isActive) {
            value = player.currentPosition.coerceAtLeast(0L)
            delay(if (isPlaying) 200L else 600L)
        }
    }
    val duration = player.duration.coerceAtLeast(0L)

    val lyrics = remember(lyricsEntity?.lyrics) {
        lyricsEntity?.lyrics?.let { runCatching { LyricsUtils.parseLyrics(it) }.getOrNull() }.orEmpty()
    }
    val lyricLine = remember(lyrics, position) {
        if (lyrics.isEmpty()) null else lyrics
            .getOrNull(LyricsUtils.findCurrentLineIndex(lyrics, position))
            ?.text
            ?.takeIf { it.isNotBlank() }
    }

    val next = remember(metadata?.id) {
        val index = player.currentMediaItemIndex
        if (index >= 0 && index + 1 < player.mediaItemCount) player.getMediaItemAt(index + 1) else null
    }

    Box(modifier.fillMaxSize().background(Obsidian)) {
        if (skin != StudioSkin.TYPE) {
            StudioBackdrop(
                artworkUrl = metadata?.thumbnailUrl,
                blurDp = blurDp,
                isPlaying = isPlaying && !powerSaver,
                accent = accent,
                skipAnimation = powerSaver,
            )
        }
        // Daylight boost: a stronger veil outdoors so type keeps its contrast.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))

        if (ambient) {
            AmbientCanvas(
                artworkUrl = metadata?.thumbnailUrl,
                title = metadata?.title.orEmpty(),
                artist = metadata?.artists?.joinToString { it.name }.orEmpty(),
                lyric = lyricLine,
                accent = accent,
                progress = if (duration > 0) position.toFloat() / duration else 0f,
                onExit = { ambient = false },
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                val wide = maxWidth > 600.dp
                if (wide) {
                    Column(Modifier.fillMaxSize()) {
                    StudioHeader(
                        accent = accent,
                        onCollapse = onCollapse,
                        onLock = { locked = true },
                        onAmbient = { ambient = true },
                    )
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        StudioStage(
                            skin = skin, artworkUrl = metadata?.thumbnailUrl, radiusDp = radiusDp,
                            rpm = rpm, tilt = tilt, isPlaying = isPlaying, accent = accent,
                            progress = if (duration > 0) position.toFloat() / duration else 0f,
                            onDoubleTap = { forward ->
                                player.seekTo((player.currentPosition + if (forward) 10_000L else -10_000L).coerceAtLeast(0L))
                                flash = if (forward) "+10s" else "−10s"
                            },
                            onPinch = { inspector = true },
                            onScratchSeek = { deltaPx ->
                                val window = 2_400f
                                player.seekTo((player.currentPosition + (deltaPx / window * duration)).toLong().coerceIn(0L, duration))
                            },
                            onTapArtwork = { ambient = true },
                            modifier = Modifier.weight(0.95f).padding(end = 24.dp),
                        )
                        StudioControls(
                            metadata = metadata, isPlaying = isPlaying, position = position, duration = duration,
                            accent = accent, haptics = haptics, bpm = dj.currentBpm, key = dj.currentKey,
                            format = currentFormat?.let { f ->
                                val rate = f.sampleRate?.let { " · ${(it / 1000f).roundToInt()} kHz" }.orEmpty()
                                val lossless = f.mimeType.contains("flac", true)
                                (if (lossless) "FLAC" else "AAC") + rate
                            },
                            titleWeight = titleWeight, titleScale = titleScale, lyric = lyricLine,
                            nextTitle = next?.mediaMetadata?.title?.toString(),
                            nextArtwork = next?.mediaMetadata?.artworkUri?.toString(),
                            onSeek = { player.seekTo(it) },
                            onPlayPause = { if (isPlaying) player.pause() else player.play() },
                            onNext = { playerConnection.seekToNext() },
                            onPrevious = { playerConnection.seekToPrevious() },
                            onStop = {
                                player.stop()
                                player.clearMediaItems()
                            },
                            onUpNext = { playerConnection.seekToNext() },
                            onLock = { locked = true },
                            onAmbient = { ambient = true },
                            modifier = Modifier.weight(1.05f),
                        )
                    }
                    }
                } else {
                    Column(Modifier.fillMaxSize().padding(top = 14.dp, bottom = 12.dp)) {
                        StudioHeader(
                            accent = accent,
                            onCollapse = onCollapse,
                            onLock = { locked = true },
                            onAmbient = { ambient = true },
                        )
                        StudioStage(
                            skin = skin, artworkUrl = metadata?.thumbnailUrl, radiusDp = radiusDp,
                            rpm = rpm, tilt = tilt, isPlaying = isPlaying, accent = accent,
                            progress = if (duration > 0) position.toFloat() / duration else 0f,
                            onDoubleTap = { forward ->
                                player.seekTo((player.currentPosition + if (forward) 10_000L else -10_000L).coerceAtLeast(0L))
                                flash = if (forward) "+10s" else "−10s"
                            },
                            onPinch = { inspector = true },
                            onScratchSeek = { deltaPx ->
                                val window = 2_400f
                                player.seekTo((player.currentPosition + (deltaPx / window * duration)).toLong().coerceIn(0L, duration))
                            },
                            onTapArtwork = { ambient = true },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                        StudioControls(
                            metadata = metadata, isPlaying = isPlaying, position = position, duration = duration,
                            accent = accent, haptics = haptics, bpm = dj.currentBpm, key = dj.currentKey,
                            format = currentFormat?.let { f ->
                                val rate = f.sampleRate?.let { " · ${(it / 1000f).roundToInt()} kHz" }.orEmpty()
                                val lossless = f.mimeType.contains("flac", true)
                                (if (lossless) "FLAC" else "AAC") + rate
                            },
                            titleWeight = titleWeight, titleScale = titleScale, lyric = lyricLine,
                            nextTitle = next?.mediaMetadata?.title?.toString(),
                            nextArtwork = next?.mediaMetadata?.artworkUri?.toString(),
                            onSeek = { player.seekTo(it) },
                            onPlayPause = { if (isPlaying) player.pause() else player.play() },
                            onNext = { playerConnection.seekToNext() },
                            onPrevious = { playerConnection.seekToPrevious() },
                            onStop = {
                                player.stop()
                                player.clearMediaItems()
                            },
                            onUpNext = { playerConnection.seekToNext() },
                            onLock = { locked = true },
                            onAmbient = { ambient = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                flash?.let { label ->
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        GrainOverlay(grain)

        if (locked) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable { locked = false },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.lock),
                        contentDescription = null,
                        tint = Champagne,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Touch locked · tap to unlock", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
            }
        }

        if (inspector) {
            CoverInspector(
                artworkUrl = metadata?.thumbnailUrl,
                title = metadata?.title.orEmpty(),
                onDismiss = { inspector = false },
            )
        }
    }
}

@Composable
private fun StudioHeader(
    accent: Color,
    onCollapse: () -> Unit,
    onLock: () -> Unit,
    onAmbient: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "STUDIO",
            color = accent.copy(alpha = 0.75f),
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onCollapse).padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "AMBIENT",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAmbient).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.lock_open),
                contentDescription = "Lock touch controls",
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onLock).padding(5.dp),
            )
        }
    }
}

@Composable
private fun StudioStage(
    skin: StudioSkin,
    artworkUrl: String?,
    radiusDp: Int,
    rpm: Int,
    tilt: Offset,
    isPlaying: Boolean,
    accent: Color,
    progress: Float,
    onDoubleTap: (Boolean) -> Unit,
    onPinch: () -> Unit,
    onScratchSeek: (Float) -> Unit,
    onTapArtwork: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        when (skin) {
            StudioSkin.COVER -> CoverStage(
                artworkUrl = artworkUrl,
                radiusDp = radiusDp,
                tilt = tilt,
                accent = accent,
                onDoubleTap = onDoubleTap,
                onPinch = onPinch,
                onTap = onTapArtwork,
                modifier = Modifier.fillMaxWidth(),
            )
            StudioSkin.VINYL -> VinylStage(
                artworkUrl = artworkUrl,
                rpm = rpm,
                tilt = tilt,
                isPlaying = isPlaying,
                accent = accent,
                onScratchSeek = onScratchSeek,
                onDoubleTap = onDoubleTap,
                modifier = Modifier.fillMaxWidth(),
            )
            StudioSkin.CASSETTE -> CassetteStage(
                artworkUrl = artworkUrl,
                progress = progress,
                isPlaying = isPlaying,
                accent = accent,
                onTap = onTapArtwork,
                modifier = Modifier.fillMaxWidth(),
            )
            StudioSkin.TYPE -> Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CoverStage(
    artworkUrl: String?,
    radiusDp: Int,
    tilt: Offset,
    accent: Color,
    onDoubleTap: (Boolean) -> Unit,
    onPinch: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(0.94f)
                .aspectRatio(1f)
                .graphicsLayer {
                    rotationX = -tilt.y * 8f
                    rotationY = tilt.x * 8f
                    rotationZ = tilt.x * 1.1f
                    cameraDistance = 16f * density
                    scaleX = zoom
                    scaleY = zoom
                    transformOrigin = TransformOrigin.Center
                }
                .clip(RoundedCornerShape(radiusDp.dp))
                .background(Color(0xFF171513))
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, pinch, _ ->
                        zoom = (zoom * pinch).coerceIn(1f, 3f)
                        if (zoom >= 1.9f) {
                            zoom = 1f
                            onPinch()
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { offset -> onDoubleTap(offset.x > size.width / 2f) },
                    )
                },
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Specular sheen that follows the gyroscope, so the card reads as glass.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f + abs(tilt.x) * 0.06f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.16f),
                            ),
                            start = Offset(1_200f * tilt.x, 0f),
                            end = Offset(600f, 1_400f),
                        ),
                    ),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .border(1.dp, accent.copy(alpha = 0.16f), RoundedCornerShape(radiusDp.dp)),
            )
        }
    }
}

@Composable
private fun VinylStage(
    artworkUrl: String?,
    rpm: Int,
    tilt: Offset,
    isPlaying: Boolean,
    accent: Color,
    onScratchSeek: (Float) -> Unit,
    onDoubleTap: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var angle by remember { mutableFloatStateOf(0f) }
    var scratch by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, rpm) {
        if (!isPlaying) return@LaunchedEffect
        val periodMillis = 60_000f / rpm.coerceAtLeast(1)
        var last = 0L
        while (isActive) {
            withFrameMillis { now ->
                if (last != 0L) angle = (angle + (now - last) / periodMillis * 360f) % 360f
                last = now
            }
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { scratch = 0f },
                        onDragEnd = {
                            if (scratch != 0f) onScratchSeek(scratch)
                            scratch = 0f
                        },
                        onDragCancel = { scratch = 0f },
                    ) { change, drag ->
                        change.consume()
                        scratch += drag
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { offset -> onDoubleTap(offset.x > size.width / 2f) })
                }
                .graphicsLayer {
                    rotationZ = angle + scratch * 0.4f
                    rotationX = -tilt.y * 6f
                    rotationY = tilt.x * 6f
                    cameraDistance = 18f * density
                }
                .clip(CircleShape),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Records: body ring over the art, grooves, and the spindle.
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(Color.Black.copy(alpha = 0.86f), r * 0.66f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.64f))
                for (i in 1..9) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = r * (0.42f + i * 0.062f),
                        center = c,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                    )
                }
                drawCircle(accent.copy(alpha = 0.9f), r * 0.045f, c)
            }
        }
        // Tonearm resting at the outer groove.
        Canvas(Modifier.fillMaxSize()) {
            val start = Offset(size.width * 0.93f, size.height * 0.05f)
            val end = Offset(size.width * 0.62f, size.height * 0.30f)
            drawLine(Color.White.copy(alpha = 0.30f), start, end, strokeWidth = 5.dp.toPx())
            drawCircle(accent.copy(alpha = 0.8f), 6.dp.toPx(), end)
        }
    }
}

@Composable
private fun CassetteStage(
    artworkUrl: String?,
    progress: Float,
    isPlaying: Boolean,
    accent: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reelAngle = progress * 720f
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1.55f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF141210))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 26.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Reel(angle = reelAngle, accent = accent, spinning = isPlaying)
                Reel(angle = reelAngle, accent = accent, spinning = isPlaying)
            }
            Box(
                Modifier
                    .fillMaxWidth(0.42f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Bottom tape window.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .fillMaxWidth(0.28f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
            ) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxSize().background(accent.copy(alpha = 0.8f)))
            }
        }
    }
}

@Composable
private fun Reel(angle: Float, accent: Color, spinning: Boolean) {
    Canvas(Modifier.size(74.dp).graphicsLayer { rotationZ = angle }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawCircle(Color(0xFF26221E), r, c)
        drawCircle(accent.copy(alpha = 0.55f), r * 0.78f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        for (i in 0 until 6) {
            val a = Math.toRadians((i * 60).toDouble())
            drawLine(
                Color.White.copy(alpha = if (spinning) 0.30f else 0.18f),
                c,
                Offset(c.x + (Math.cos(a) * r * 0.72f).toFloat(), c.y + (Math.sin(a) * r * 0.72f).toFloat()),
                strokeWidth = 2.dp.toPx(),
            )
        }
        drawCircle(Color.Black, r * 0.18f, c)
    }
}

@Composable
private fun StudioControls(
    metadata: com.vox.music.models.MediaMetadata?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    accent: Color,
    haptics: Boolean,
    bpm: Float,
    key: String?,
    format: String?,
    titleWeight: FontWeight,
    titleScale: Float,
    lyric: String?,
    nextTitle: String?,
    nextArtwork: String?,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onUpNext: () -> Unit,
    onLock: () -> Unit,
    onAmbient: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = metadata?.title.orEmpty(),
            color = Color.White,
            fontSize = (26 * titleScale).sp,
            fontWeight = titleWeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = metadata?.artists?.joinToString { it.name }.orEmpty(),
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (!lyric.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = lyric,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = Shadow(color = accent.copy(alpha = 0.45f), blurRadius = 26f)),
            )
        }

        Spacer(Modifier.height(16.dp))
        TrackChips(bpm = bpm, key = key, format = format, accent = accent)

        Spacer(Modifier.height(18.dp))
        StudioScrubber(position = position, duration = duration, accent = accent, haptics = haptics, onSeek = onSeek)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
            Text(formatTime(duration), color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = "Previous",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onPrevious).padding(6.dp),
            )
            BreathingPlay(
                isPlaying = isPlaying,
                bpm = bpm,
                accent = accent,
                onPlayPause = onPlayPause,
                onStop = onStop,
            )
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = "Next",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onNext).padding(6.dp),
            )
        }

        if (!nextTitle.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            UpNextPill(title = nextTitle, artworkUrl = nextArtwork, accent = accent, onClick = onUpNext)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                "AMBIENT",
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAmbient).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.lock_open),
                contentDescription = "Lock touch controls",
                tint = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onLock).padding(5.dp),
            )
        }
    }
}

@Composable
private fun TrackChips(bpm: Float, key: String?, format: String?, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (bpm > 0f) Chip("${bpm.roundToInt()} BPM", accent)
        if (!key.isNullOrBlank()) Chip(key, accent)
        if (!format.isNullOrBlank()) Chip(format, accent)
        if (bpm <= 0f && key.isNullOrBlank() && format.isNullOrBlank()) {
            Chip("—", accent)
        }
    }
}

@Composable
private fun Chip(text: String, accent: Color) {
    Text(
        text = text,
        color = accent.copy(alpha = 0.9f),
        fontSize = 10.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BreathingPlay(
    isPlaying: Boolean,
    bpm: Float,
    accent: Color,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
) {
    val beatMillis = if (bpm > 0f) (60_000f / bpm).roundToInt().coerceIn(300, 1_200) else 900
    val pulse by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(beatMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(86.dp)
                .graphicsLayer { alpha = if (isPlaying) pulse * 0.55f else 0.18f }
                .background(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent)),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(accent)
                .combinedClickable(
                    onClick = onPlayPause,
                    onLongClick = onStop,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Obsidian,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun StudioScrubber(
    position: Long,
    duration: Long,
    accent: Color,
    haptics: Boolean,
    onSeek: (Long) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    var lastDetent by remember { mutableStateOf(-1) }

    val fraction = if (dragFraction >= 0f) dragFraction else if (duration > 0) position.toFloat() / duration else 0f

    Box(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        lastDetent = -1
                    },
                    onDragEnd = {
                        if (dragFraction >= 0f && duration > 0) onSeek((dragFraction * duration).toLong())
                        dragFraction = -1f
                    },
                    onDragCancel = { dragFraction = -1f },
                ) { change, _ ->
                    change.consume()
                    val f = (change.position.x / size.width).coerceIn(0f, 1f)
                    dragFraction = f
                    val step = (f * 10).toInt()
                    if (step != lastDetent) {
                        lastDetent = step
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(6.dp)) {
            val y = size.height / 2f
            drawLine(Color.White.copy(alpha = 0.16f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
            drawLine(accent, Offset(0f, y), Offset(size.width * fraction.coerceIn(0f, 1f), y), strokeWidth = 2.dp.toPx())
            drawCircle(accent, 4.dp.toPx(), Offset(size.width * fraction.coerceIn(0f, 1f), y))
        }
    }
}

@Composable
private fun UpNextPill(title: String, artworkUrl: String?, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)),
        )
        Spacer(Modifier.width(9.dp))
        Text("UP NEXT", color = accent.copy(alpha = 0.75f), fontSize = 9.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.width(7.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(170.dp),
        )
    }
}

@Composable
private fun StudioBackdrop(
    artworkUrl: String?,
    blurDp: Float,
    isPlaying: Boolean,
    accent: Color,
    skipAnimation: Boolean,
) {
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(blurDp.dp),
        )
        Box(Modifier.fillMaxSize().background(Obsidian.copy(alpha = 0.62f)))
        if (!skipAnimation) {
            FluidBackdrop(accent = accent, animate = isPlaying)
        }
    }
}

/** Three slow, out-of-phase colour fields: cheap, no AGSL, no per-frame allocations. */
@Composable
private fun FluidBackdrop(accent: Color, animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "fluid")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val opacity = if (animate) 0.30f else 0.14f

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = opacity), Color.Transparent),
                center = Offset(w * (0.24f + drift * 0.18f), h * 0.22f),
                radius = w * 0.85f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0xFF2A1F1A).copy(alpha = opacity), Color.Transparent),
                center = Offset(w * (0.78f - drift * 0.14f), h * 0.66f),
                radius = w * 0.75f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = opacity * 0.5f), Color.Transparent),
                center = Offset(w * (0.5f + drift * 0.22f), h * 0.92f),
                radius = w * 0.6f,
            ),
        )
    }
}

/** Film grain over the whole stage: 420 deterministic dots, drawn once per size change. */
@Composable
private fun GrainOverlay(amount: Float) {
    if (amount <= 0.01f) return
    val dots = remember {
        val random = java.util.Random(7L)
        List(420) { Offset(random.nextFloat(), random.nextFloat()) }
    }
    Canvas(Modifier.fillMaxSize()) {
        dots.forEach { point ->
            drawCircle(
                color = Color.White.copy(alpha = amount * 0.10f),
                radius = 0.8.dp.toPx(),
                center = Offset(point.x * size.width, point.y * size.height),
            )
        }
    }
}

@Composable
private fun AmbientCanvas(
    artworkUrl: String?,
    title: String,
    artist: String,
    lyric: String?,
    accent: Color,
    progress: Float,
    onExit: () -> Unit,
) {
    val bob by rememberInfiniteTransition(label = "ambient").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(7_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob",
    )

    Box(
        Modifier
            .fillMaxSize()
            .clickable(onClick = onExit),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(190.dp)
                    .graphicsLayer { translationY = bob }
                    .clip(RoundedCornerShape(28.dp)),
            )
            Spacer(Modifier.height(26.dp))
            Text(title, color = Color.White.copy(alpha = 0.92f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(artist, color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
            Spacer(Modifier.height(22.dp))
            if (!lyric.isNullOrBlank()) {
                Text(
                    text = lyric,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = Shadow(color = accent.copy(alpha = 0.55f), blurRadius = 30f)),
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
        }
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.64f)
                .padding(bottom = 42.dp)
                .height(2.dp),
        ) {
            drawLine(Color.White.copy(alpha = 0.18f), Offset.Zero, Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
            drawLine(accent, Offset.Zero, Offset(size.width * progress.coerceIn(0f, 1f), 0f), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
private fun CoverInspector(artworkUrl: String?, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var saving by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f))) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset += pan
                    }
                }
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = if (scale > 2f) 1f else 3f }) },
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "CLOSE",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
            Text(
                if (saving) "SAVING…" else "SAVE COVER",
                color = Obsidian,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Champagne)
                    .clickable(enabled = !saving && artworkUrl != null) {
                        saving = true
                        scope.launch {
                            val bitmap = withContext(Dispatchers.IO) {
                                runCatching {
                                    val request = ImageRequest.Builder(context)
                                        .data(artworkUrl)
                                        .size(1_500, 1_500)
                                        .allowHardware(false)
                                        .build()
                                    ImageLoader.Builder(context).build().execute(request).image?.toBitmap()
                                }.getOrNull()
                            }
                            if (bitmap != null) {
                                runCatching {
                                    com.vox.music.utils.ComposeToImage.saveBitmapAsFile(
                                        context,
                                        bitmap,
                                        "vox-cover-${System.currentTimeMillis()}",
                                    )
                                }
                                android.widget.Toast.makeText(context, "Saved to Pictures/Vox", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            saving = false
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun rememberCoverAccent(url: String?): androidx.compose.runtime.State<Color> {
    val context = LocalContext.current
    return produceState(Champagne, url) {
        val target = url ?: return@produceState
        val sampled = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(target)
                    .size(96, 96)
                    .allowHardware(false)
                    .build()
                val bitmap = ImageLoader.Builder(context).build().execute(request).image?.toBitmap()
                bitmap?.let { Palette.from(it).generate().dominantSwatch?.rgb }
            }.getOrNull()
        }
        if (sampled != null) {
            // Keep the champagne family: brighten dark artwork until the accent can read.
            val color = Color(sampled)
            value = if (color.luminanceCompat() < 0.35f) Champagne else color
        }
    }
}

private fun Color.luminanceCompat(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

@Composable
private fun rememberTilt(enabled: Boolean): Offset {
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) return@DisposableEffect onDispose {}
        val listener = object : SensorEventListener {
            private val rotation = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                tilt = Offset(
                    (orientation[2] / 0.6f).coerceIn(-1f, 1f),
                    (orientation[1] / 0.6f).coerceIn(-1f, 1f),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
    return tilt
}

@Composable
private fun rememberAmbientLux(enabled: Boolean): Float {
    val context = LocalContext.current
    var lux by remember { mutableFloatStateOf(0f) }
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (manager == null || sensor == null) return@DisposableEffect onDispose {}
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                lux = event.values.firstOrNull() ?: 0f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { manager.unregisterListener(listener) }
    }
    return lux
}

private fun formatTime(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
