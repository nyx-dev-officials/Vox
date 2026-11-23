/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The Live Island: a black capsule at the top of the screen that carries the
 * lyric line playing right now. It only exists while a track is playing and the
 * current line is non-blank, so a track without synced lyrics costs nothing.
 */

package com.vox.music.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vox.music.R
import com.vox.music.constants.IslandYOffsetKey
import com.vox.music.constants.LiveIslandPillKey
import com.vox.music.constants.ReduceDynamicBlurKey
import com.vox.music.lyrics.LyricsEntry
import com.vox.music.lyrics.LyricsUtils
import com.vox.music.playback.PlayerConnection
import com.vox.music.ui.ios.compat.Icon
import com.vox.music.ui.ios.compat.Text
import com.vox.music.ui.utils.Motion
import com.vox.music.utils.rememberPreference

@Composable
fun LiveIslandPill(
    playerConnection: PlayerConnection,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (enabled) = rememberPreference(LiveIslandPillKey, defaultValue = false)
    if (!enabled) return

    IslandContent(playerConnection, visible, onClick, modifier)
}

@Composable
private fun IslandContent(
    playerConnection: PlayerConnection,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val (yOffset) = rememberPreference(IslandYOffsetKey, defaultValue = 0)
    val (reduceMotion) = rememberPreference(ReduceDynamicBlurKey, defaultValue = false)

    val snapshot by playerConnection.playbackSnapshot.collectAsStateWithLifecycle()
    val lyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)

    // Parsing is keyed on the lyrics blob, not on the play position, so the
    // 250 ms position tick only re-runs the binary search below.
    val lines = remember(lyrics?.lyrics) {
        lyrics?.lyrics?.let { LyricsUtils.parseLyrics(it) }.orEmpty()
    }
    val currentLine = remember(lines, snapshot.positionMs) {
        if (lines.isEmpty()) {
            null
        } else {
            val index = LyricsUtils.findCurrentLineIndex(lines, snapshot.positionMs)
            lines.getOrNull(index)?.takeIf { it.text.isNotBlank() }
        }
    }

    val show = visible && snapshot.isPlaying && currentLine != null

    AnimatedVisibility(
        visible = show,
        enter = fadeIn(Motion.gate(reduceMotion, tween(220))) +
            slideInVertically(Motion.gate(reduceMotion, tween(260))) { -it / 2 },
        exit = fadeOut(Motion.gate(reduceMotion, tween(160))) +
            slideOutVertically(Motion.gate(reduceMotion, tween(200))) { -it / 2 },
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 8.dp)
            // The offset slider allows -40..40 dp. Feeding that into padding would throw
            // "Padding must be non-negative" below -8 dp, so shift the positioned pill instead.
            .offset(y = yOffset.dp),
    ) {
        // AnimatedVisibility's content scope is not a BoxScope, so the pill owns
        // its own row and the caller only positions the wrapper.
        IslandCapsule(
            line = currentLine ?: LyricsEntry.HEAD_LYRICS_ENTRY,
            reduceMotion = reduceMotion,
            onClick = onClick,
        )
    }
}

@Composable
private fun IslandCapsule(
    line: LyricsEntry,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .widthIn(max = 320.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.graphic_eq),
            contentDescription = null,
            tint = Color(0xFF1ED760),
            modifier = Modifier.size(16.dp),
        )
        AnimatedContent(
            targetState = line.text,
            transitionSpec = {
                val fadeSpec = Motion.gate(reduceMotion, tween<Float>(220))
                val slideSpec = Motion.gate(reduceMotion, tween<IntOffset>(220))
                (fadeIn(fadeSpec) + slideInVertically(slideSpec) { it / 3 })
                    .togetherWith(fadeOut(fadeSpec) + slideOutVertically(slideSpec) { -it / 3 })
            },
            label = "island-line",
        ) { text ->
            Text(
                text = text,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
