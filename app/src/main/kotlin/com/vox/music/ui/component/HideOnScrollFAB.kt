/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vox.music.LocalPlayerAwareWindowInsets
import com.vox.music.R
import com.vox.music.designsystem.VoxDesignTokens
import androidx.compose.foundation.shape.RoundedCornerShape
import com.vox.music.ui.ios.compat.Icon
import com.vox.music.ui.theme.AppleTokens
import com.vox.music.ui.utils.bounceClick
import com.vox.music.ui.utils.isScrollingUp
import dev.chrisbanes.haze.HazeState

/**
 * The Vox action dock.
 *
 * These used to be two stacked translucent discs — a mic floating over a shuffle
 * button — which sat on top of the video rails as grey blobs and never looked
 * related to each other. They are now one capsule: a quiet mic, a hairline, and
 * the accent action on champagne. One object, one edge, docked at the end of the
 * list instead of hovering over its content.
 */
@Composable
private fun FabDock(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    onRecognitionClick: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .height(52.dp)
            .clip(shape)
            .background(AppleTokens.BgElevated.copy(alpha = 0.94f))
            .border(1.dp, VoxDesignTokens.Colors.Hairline, shape)
            .padding(horizontal = 6.dp),
    ) {
        if (onRecognitionClick != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .bounceClick(onClick = onRecognitionClick),
            ) {
                SymbolIcon(
                    symbol = sf("mic.fill"),
                    contentDescription = stringResource(R.string.recognize_music),
                    size = 18.dp,
                    tint = AppleTokens.OnSurface.copy(alpha = 0.8f),
                )
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(VoxDesignTokens.Colors.Hairline),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppleTokens.AccentGold)
                .bounceClick(onClick = onClick),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = VoxDesignTokens.Colors.Background,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.dockModifier() = Modifier
    .align(Alignment.BottomEnd)
    .windowInsetsPadding(
        LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
    )
    .padding(16.dp)

/** Pass the screen's scrolling [HazeState] for a frosted blur; null keeps the dock flat. */
@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    lazyListState: LazyListState,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    onRecognitionClick: (() -> Unit)? = null,
    hazeState: HazeState? = null,
) {
    AnimatedVisibility(
        visible = visible && lazyListState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = dockModifier(),
    ) {
        FabDock(icon = icon, onClick = onClick, onRecognitionClick = onRecognitionClick)
    }
}

@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    lazyListState: LazyGridState,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    onRecognitionClick: (() -> Unit)? = null,
    hazeState: HazeState? = null,
) {
    AnimatedVisibility(
        visible = visible && lazyListState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = dockModifier(),
    ) {
        FabDock(icon = icon, onClick = onClick, onRecognitionClick = onRecognitionClick)
    }
}

@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    scrollState: ScrollState,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    onRecognitionClick: (() -> Unit)? = null,
    hazeState: HazeState? = null,
) {
    AnimatedVisibility(
        visible = visible && scrollState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = dockModifier(),
    ) {
        FabDock(icon = icon, onClick = onClick, onRecognitionClick = onRecognitionClick)
    }
}
