/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vox.music.LocalAmbientBackgroundColor
import com.vox.music.R
import com.vox.music.designsystem.VoxDesignTokens
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.Text
import com.vox.music.ui.theme.AppleTokens
import com.vox.music.ui.theme.LocalAccentTextColor
import com.vox.music.ui.theme.LocalPageMetrics
import com.vox.music.ui.theme.VoxType
import com.vox.music.ui.theme.isHueless
import com.vox.music.ui.utils.bounceClick

/**
 * A section header, set like a page folio.
 *
 * Everything a feed needs to be scannable is in one column: a micro-label, the
 * title on the display face, an action at the far right, and a hairline that
 * closes the block. No floating pill, no champagne tick, no squircle: the rule
 * and the type carry the hierarchy, the way a printed page does it.
 */
@Composable
fun NavigationTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    color: Color? = null,
    onClick: (() -> Unit)? = null,
    onPlayAllClick: (() -> Unit)? = null,
) {
    val ambient = LocalAmbientBackgroundColor.current.value
    val contentColor = color ?: if (ambient.isSpecified && !ambient.isHueless()) {
        LocalAccentTextColor.current
    } else {
        AppTheme.colorScheme.onSurface
    }
    val chevron = onClick != null && LocalPageMetrics.current.chevronHeaders
    val action = onPlayAllClick ?: onClick.takeUnless { chevron }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(
                start = LocalPageMetrics.current.gutter,
                end = LocalPageMetrics.current.gutter,
                top = LocalPageMetrics.current.headerTop,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            thumbnail?.invoke()

            Column(modifier = Modifier.weight(1f)) {
                label?.let {
                    Text(
                        text = it.uppercase(),
                        style = VoxType.Eyebrow,
                        color = contentColor.copy(alpha = 0.45f),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    text = title,
                    style = VoxType.SectionHeader,
                    color = contentColor,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }

            if (chevron) {
                // A drawn arrow, not a glyph: it has to hold its weight next to a
                // semibold 20sp title on both paper and ink.
                HorizontalRuleArrow(
                    color = contentColor.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            if (action != null) {
                Text(
                    text = stringResource(
                        if (onPlayAllClick != null) R.string.play_all else R.string.see_all
                    ).uppercase(),
                    style = VoxType.Eyebrow,
                    color = AppleTokens.AccentGold,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .bounceClick(onClick = action)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // The folio rule. It is the only structural line in a feed, which is why
        // it can stay at 12% and still read as a hard edge.
        Box(
            Modifier
                .fillMaxWidth()
                .height(VoxDesignTokens.Metrics.Hairline)
                .background(contentColor.copy(alpha = 0.12f)),
        )
    }
}

/** Small right-arrow drawn with strokes so its weight matches the header type. */
@Composable
private fun HorizontalRuleArrow(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(width = 16.dp, height = 10.dp)
    ) {
        val stroke = 1.6.dp.toPx()
        val midY = size.height / 2f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, midY),
            end = androidx.compose.ui.geometry.Offset(size.width - stroke, midY),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Square,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width - 5.dp.toPx(), midY - 3.5.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(size.width - stroke, midY),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Square,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width - 5.dp.toPx(), midY + 3.5.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(size.width - stroke, midY),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Square,
        )
    }
}
