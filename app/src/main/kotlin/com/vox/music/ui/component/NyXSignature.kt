/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vox.music.designsystem.VoxDesignTokens

/**
 * The author credit: `MADE BY NYX`, set small and wide-tracked so it reads as a
 * maker's mark rather than a sentence.
 *
 * The split colour is the whole point of the mark. "Ny" takes Warm Alabaster and the
 * final "X" takes Champagne, which puts the only saturated pixel in the signature on
 * the letter that ends the name. The lead-in stays Warm Stone so it never competes.
 *
 * @param lead what precedes the name, already in the case it should render in.
 */
@Composable
fun NyXSignature(
    modifier: Modifier = Modifier,
    lead: String = "MADE BY",
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = VoxDesignTokens.Colors.SecondaryLabel)) {
                append(lead)
            }
            append(" ")
            withStyle(SpanStyle(color = VoxDesignTokens.Colors.OnSurface)) {
                append("NY")
            }
            withStyle(SpanStyle(color = VoxDesignTokens.Colors.AccentGold)) {
                append("X")
            }
        },
        modifier = modifier.padding(vertical = 8.dp),
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.em,
    )
}
