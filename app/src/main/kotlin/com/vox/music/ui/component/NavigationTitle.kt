/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import com.vox.music.designsystem.VoxDesignTokens
import com.vox.music.ui.theme.VoxType
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.vox.music.ui.utils.bounceClick
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vox.music.R

import com.vox.music.LocalAmbientBackgroundColor
import com.vox.music.ui.ios.compat.LocalContentColor
import com.vox.music.ui.theme.AppleTokens
import com.vox.music.ui.theme.LocalPageMetrics
import com.vox.music.ui.theme.LocalAccentTextColor
import com.vox.music.ui.theme.isHueless
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

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
    // Headings stay neutral per theme; only hero screens keep the
    // artwork-derived tint (provided into this local), so a section title
    // matches the screen it is on.
    val ambient = LocalAmbientBackgroundColor.current.value
    val contentColor = color ?: if (ambient.isSpecified && !ambient.isHueless()) {
        LocalAccentTextColor.current
    } else {
        AppTheme.colorScheme.onSurface
    }
    val chevron = onClick != null && LocalPageMetrics.current.chevronHeaders

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(
                start = LocalPageMetrics.current.gutter,
                end = LocalPageMetrics.current.gutter,
                top = LocalPageMetrics.current.headerTop,
                bottom = LocalPageMetrics.current.headerBottom,
            )
    ) {
        thumbnail?.invoke()

        // The Vox section mark: a short champagne rule that opens every header.
        // It is the one place the accent appears in the scrolling feed, so the
        // page has a repeatable signature instead of a plain text label.
        Box(
            Modifier
                .padding(end = 10.dp)
                .size(VoxDesignTokens.Metrics.HeaderRuleWidth, VoxDesignTokens.Metrics.HeaderRuleHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(AppleTokens.AccentGold.copy(alpha = 0.9f)),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                label?.let { label ->
                    Text(
                        text = label.uppercase(),
                        style = VoxType.Eyebrow,
                        color = contentColor.copy(alpha = 0.55f),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }

                Text(
                    text = title,
                    style = VoxType.SectionHeader,
                    color = contentColor,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
            // The whole header is clickable; the chevron marks the linked section.
            if (chevron) {
                // SF chevron.right at Title2 bold: a short heavy stroke about the
                // title's cap height. The monochrome SF vector is the regular weight
                // and reads thin and oversized beside a bold header, so draw it.
                val chevronColor = AppleTokens.AccentGold.copy(alpha = 0.75f)
                Canvas(Modifier.padding(start = 6.dp, top = 1.dp).size(width = 9.dp, height = 15.dp)) {
                    val stroke = 2.6.dp.toPx()
                    val inset = stroke / 2
                    drawPath(
                        Path().apply {
                            moveTo(inset, inset)
                            lineTo(size.width - inset, size.height / 2)
                            lineTo(inset, size.height - inset)
                        },
                        color = chevronColor,
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }

        val action = onPlayAllClick ?: onClick.takeUnless { chevron }
        if (action != null) {
            Text(
                text = stringResource(
                    if (onPlayAllClick != null) R.string.play_all else R.string.see_all
                ).uppercase(),
                style = VoxType.Eyebrow,
                color = AppleTokens.AccentGold.copy(alpha = 0.85f),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .bounceClick(onClick = action)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}
