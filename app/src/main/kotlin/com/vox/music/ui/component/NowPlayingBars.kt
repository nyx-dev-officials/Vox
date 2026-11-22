/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Three-bar "now playing" mark for a track row. It is a single Canvas with no
 * per-row flow and no per-row collector: the row already knows whether it is the
 * active one, so this only animates while that row is playing. Battery saver and
 * paused playback both fall back to a static low profile.
 */

package com.vox.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun NowPlayingBars(
    playing: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val powerSaver = remember {
        context.getSystemService(android.os.PowerManager::class.java)?.isPowerSaveMode ?: false
    }
    val animate = playing && !powerSaver

    val transition = rememberInfiniteTransition(label = "row-bars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(980, easing = LinearEasing), RepeatMode.Restart),
        label = "row-bars-phase",
    )

    Canvas(modifier) {
        val bars = 3
        val gap = size.width / (bars * 2f - 1f)
        for (i in 0 until bars) {
            val factor = if (animate) {
                val wave = sin(((phase + i * 0.28f) % 1f) * PI).toFloat()
                0.28f + 0.72f * wave
            } else {
                0.34f + 0.1f * i
            }
            val height = size.height * factor
            drawRoundRect(
                color = tint,
                topLeft = Offset(i * gap * 2f, size.height - height),
                size = Size(gap, height),
                cornerRadius = CornerRadius(gap / 2f, gap / 2f),
            )
        }
    }
}
