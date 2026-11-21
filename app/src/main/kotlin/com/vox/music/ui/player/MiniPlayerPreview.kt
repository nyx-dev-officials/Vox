/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composables.sfsymbols.monochrome.SFBackwardFill
import com.composables.sfsymbols.monochrome.SFForwardFill
import com.composables.sfsymbols.monochrome.SFPerson2
import com.composables.sfsymbols.monochrome.SFSquareAndArrowUp
import com.composables.sfsymbols.monochrome.SFStar
import com.composables.sfsymbols.monochrome.SFStarFill
import com.vox.music.LocalDatabase
import com.vox.music.LocalPlayerConnection
import com.vox.music.R
import com.vox.music.designsystem.VoxDesignTokens
import com.vox.music.lyrics.LyricsUtils
import com.vox.music.models.MediaMetadata
import com.vox.music.ui.component.AnimatedPlayPauseIcon
import com.vox.music.ui.ios.compat.Icon
import com.vox.music.ui.component.backdrop.drawBackdrop
import com.vox.music.ui.component.backdrop.effects.blur
import com.vox.music.ui.component.rememberPlaybackFraction
import com.vox.music.ui.component.sf
import com.vox.music.ui.component.shapes.ContinuousRoundedRectangle
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.Text
import com.vox.music.ui.ios.component.IosGlyph
import com.vox.music.ui.ios.component.IosMenu
import com.vox.music.ui.ios.component.IosMenuItem
import com.vox.music.ui.ios.component.IosMenuSection
import com.vox.music.ui.menu.AddToPlaylistDialog
import com.vox.music.ui.theme.AppleTokens
import com.vox.music.ui.theme.VoxType
import com.vox.music.utils.makeTimeString

private val CardShape = ContinuousRoundedRectangle(24.dp)
private val ArtShape = ContinuousRoundedRectangle(18.dp)

/**
 * The mini player, lifted.
 *
 * Long-pressing the mini player puts a frozen, 20dp-blurred copy of the page behind a
 * single rounded card: big artwork, title, a scrubber you can drag, the lyric line that
 * is playing right now, transport, and the same stacked menu a held row opens. It is a
 * modal — the blur is drawn once from the app's cached backdrop and never runs while
 * anything scrolls.
 */
@Composable
fun MiniPlayerPreview(
    metadata: MediaMetadata,
    onDismiss: () -> Unit,
    onOpenPlayer: () -> Unit,
    onGoToAlbum: (String, String?) -> Unit,
    onGoToArtist: (String, String?) -> Unit,
    onListenTogether: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current.get()
    val context = androidx.compose.ui.platform.LocalContext.current

    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val song by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val lyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val fraction = rememberPlaybackFraction(playerConnection.player, isPlaying)

    val durationMs = playerConnection.player.duration.coerceAtLeast(0L)
    val positionMs = (fraction.value * durationMs).toLong()

    // The one line that matters right now; parsed once per lyrics document, not per frame.
    val lines = remember(lyrics?.lyrics) {
        lyrics?.lyrics?.takeIf { it.isNotBlank() && it != "LYRICS_NOT_FOUND" }
            ?.let { runCatching { LyricsUtils.parseLyrics(it) }.getOrNull() }
            .orEmpty()
    }
    val activeLine = remember(lines, positionMs) {
        lines.getOrNull(LyricsUtils.findCurrentLineIndex(lines, positionMs))?.text?.trim()
    }

    var shown by remember { mutableStateOf(false) }
    var addToPlaylist by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        // A touch of overshoot on the way up, critically damped on the way down.
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "miniPreviewAppear",
    )

    val backdrop = com.vox.music.ui.component.LocalAppBackdrop.current
    val shape = CardShape
    val ink = AppleTokens.OnSurface

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            // The frozen page: one cached backdrop draw with a 20dp blur, plus a dim.
            if (backdrop != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RectangleShape },
                            effects = { blur(20.dp.toPx()) },
                            highlight = { null },
                            shadow = { null },
                            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.45f)) },
                            frozen = { true },
                        )
                        .clickable(onClick = onDismiss),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.62f))
                        .clickable(onClick = onDismiss),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 18.dp)
                    .graphicsLayer {
                        alpha = appear
                        val s = 0.94f + 0.06f * appear
                        scaleX = s
                        scaleY = s
                    }
                    .widthIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Card.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 28.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(shape)
                        .background(AppleTokens.Card.copy(alpha = 0.97f))
                        .border(1.dp, VoxDesignTokens.Colors.Hairline, shape)
                        .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
                ) {
                    AsyncImage(
                        model = metadata.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(ArtShape)
                            .clickable(onClick = onOpenPlayer),
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = metadata.title,
                        style = VoxType.TileTitle.copy(fontSize = 21.sp, lineHeight = 26.sp),
                        color = ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = metadata.artists.joinToString { it.name },
                        style = AppTheme.typography.bodySmall.copy(fontFamily = VoxType.Body),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = AppleTokens.Metadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(14.dp))

                    PreviewScrubber(
                        fraction = fraction.value,
                        playedColor = AppleTokens.AccentGold,
                        trackColor = ink.copy(alpha = 0.16f),
                        onSeek = { f ->
                            if (durationMs > 0L) playerConnection.player.seekTo((f * durationMs).toLong())
                        },
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = makeTimeString(positionMs),
                            fontSize = 11.sp,
                            color = AppleTokens.Metadata.copy(alpha = 0.8f),
                        )
                        Text(
                            text = makeTimeString(durationMs),
                            fontSize = 11.sp,
                            color = AppleTokens.Metadata.copy(alpha = 0.8f),
                        )
                    }

                    if (!activeLine.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = activeLine,
                            style = AppTheme.typography.bodyMedium.copy(fontFamily = VoxType.Body),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = ink.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(26.dp),
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_media_previous),
                            contentDescription = stringResource(R.string.player_icon_previous),
                            tint = if (canSkipPrevious) ink else ink.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(26.dp)
                                .clickable(enabled = canSkipPrevious) { playerConnection.seekToPrevious() },
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AppleTokens.AccentGold)
                                .clickable { playerConnection.togglePlayPause() },
                        ) {
                            AnimatedPlayPauseIcon(
                                isPlaying = isPlaying,
                                tint = VoxDesignTokens.Colors.Background,
                                size = 26.dp,
                            )
                        }
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_media_next),
                            contentDescription = stringResource(R.string.player_icon_next),
                            tint = if (canSkipNext) ink else ink.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(26.dp)
                                .clickable(enabled = canSkipNext) { playerConnection.seekToNext() },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // The stacked menu, directly under the card, as on iOS.
                Box(Modifier.widthIn(max = 340.dp)) {
                    IosMenu(
                        sections = listOf(
                            IosMenuSection(
                                listOfNotNull(
                                    IosMenuItem(
                                        title = stringResource(R.string.add_to_playlist_ellipsis),
                                        icon = { tint -> IosGlyph(sf("SFTextBadgePlus"), tint, 20.dp) },
                                    ) { addToPlaylist = true },
                                    metadata.album?.id?.let { albumId ->
                                        IosMenuItem(
                                            title = stringResource(R.string.apple_go_to_album),
                                            icon = { tint -> IosGlyph(sf("SFSquareStack"), tint, 20.dp) },
                                        ) {
                                            onDismiss()
                                            onGoToAlbum(albumId, metadata.thumbnailUrl)
                                        }
                                    },
                                    metadata.artists.firstOrNull()?.id?.let { artistId ->
                                        IosMenuItem(
                                            title = stringResource(R.string.apple_go_to_artist),
                                            icon = { tint -> IosGlyph(sf("microphone.dynamic.on.stand"), tint, 20.dp) },
                                        ) {
                                            onDismiss()
                                            onGoToArtist(artistId, metadata.thumbnailUrl)
                                        }
                                    },
                                ),
                            ),
                        ),
                        quickActions = listOf(
                            IosMenuItem(
                                title = stringResource(R.string.favorite),
                                icon = { tint ->
                                    IosGlyph(
                                        if (song?.song?.liked == true) sf("SFStarFill") else sf("SFStar"),
                                        tint,
                                        20.dp,
                                    )
                                },
                            ) {
                                song?.let { entry -> database.query { update(entry.song.toggleLike()) } }
                            },
                            IosMenuItem(
                                title = stringResource(R.string.share),
                                icon = { tint -> IosGlyph(sf("SFSquareAndArrowUp"), tint, 20.dp) },
                            ) {
                                context.startActivity(
                                    android.content.Intent.createChooser(
                                        android.content.Intent(android.content.Intent.ACTION_SEND)
                                            .setType("text/plain")
                                            .putExtra(
                                                android.content.Intent.EXTRA_TEXT,
                                                com.vox.music.utils.ShareUtils.songUrl(metadata.id),
                                            ),
                                        null,
                                    ),
                                )
                            },
                            IosMenuItem(
                                title = stringResource(com.vox.music.ui.screens.Screens.ListenTogether.titleId),
                                icon = { tint -> IosGlyph(sf("SFPerson2"), tint, 20.dp) },
                            ) {
                                onDismiss()
                                onListenTogether()
                            },
                        ),
                        onDismissRequest = onDismiss,
                    )
                }
            }
        }

        if (addToPlaylist) {
            AddToPlaylistDialog(
                isVisible = true,
                onGetSong = { listOf(metadata.id) },
                onDismiss = { addToPlaylist = false },
            )
        }
    }
}

/** Thin champagne scrubber with a real thumb; drag anywhere on the track to seek. */
@Composable
private fun PreviewScrubber(
    fraction: Float,
    playedColor: Color,
    trackColor: Color,
    onSeek: (Float) -> Unit,
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek((offset.x / widthPx).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onSeek((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumb = 12.dp
        val travel = (maxWidth - thumb).coerceAtLeast(0.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(trackColor),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(3.dp)
                .clip(CircleShape)
                .background(playedColor),
        )
        Box(
            Modifier
                .offset(x = travel * fraction.coerceIn(0f, 1f))
                .size(thumb)
                .clip(CircleShape)
                .background(playedColor),
        )
    }
}
