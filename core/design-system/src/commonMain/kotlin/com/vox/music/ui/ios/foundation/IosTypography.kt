/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.ios.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The eleven iOS text styles, at their default (Large) Dynamic Type size.
 *
 * Hosts supply the selected font family; the shared client uses the same bundled
 * family and weight mapping as the production Android client.
 */
@Immutable
data class IosTypography(
    val largeTitle: TextStyle,
    val title: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val subheadline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val footnote: TextStyle,
    val caption: TextStyle,
    val caption2: TextStyle,
)

/**
 * @param fontFamily the body voice — whatever the reader picked in settings.
 * @param displayFamily the display voice used for titles, headers and numerals.
 *        Vox pairs a geometric display face with a readable grotesque, so the
 *        two never fight over the same line of text.
 */
fun iosTypography(
    fontFamily: FontFamily = FontFamily.Default,
    displayFamily: FontFamily = fontFamily,
): IosTypography {
    fun style(size: Float, lineHeight: Float, weight: FontWeight, tracking: Float) = TextStyle(
        fontFamily = fontFamily,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
    )

    // Display sizes run on the display face, tight tracking, no extrabold. A
    // newspaper sets its headline in one weight and lets scale do the shouting.
    fun display(size: Float, weight: FontWeight, tracking: Float) = TextStyle(
        fontFamily = displayFamily,
        fontSize = size.sp,
        lineHeight = (size * 1.12f).sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
    )

    return IosTypography(
        largeTitle = display(38f, FontWeight.SemiBold, -1.1f),
        title = display(29f, FontWeight.SemiBold, -0.8f),
        title2 = display(22f, FontWeight.SemiBold, -0.5f),
        title3 = display(19f, FontWeight.Medium, -0.3f),
        headline = display(16f, FontWeight.SemiBold, -0.2f),
        subheadline = style(14f, 19f, FontWeight.Medium, 0f),
        body = style(15f, 21f, FontWeight.Normal, 0.1f),
        callout = style(14f, 19f, FontWeight.Medium, 0.1f),
        footnote = style(12.5f, 17f, FontWeight.Normal, 0.2f),
        caption = style(11.5f, 15f, FontWeight.Normal, 0.3f),
        // Micro-label: uppercase eyebrows and index numbers live here.
        caption2 = style(10f, 13f, FontWeight.SemiBold, 1.8f),
    )
}
