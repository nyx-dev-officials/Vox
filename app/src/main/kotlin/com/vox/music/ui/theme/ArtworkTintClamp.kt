/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * The ceiling on any colour taken from album art.
 *
 * Artwork is allowed to tint the UI, never to drive it. At 35% saturation the cover
 * still reads as its own hue, and at 25% value it stays a dark ground that Warm
 * Alabaster text sits on without a scrim. Past either number the player turns into a
 * colour wash and the Champagne accent stops being the brightest thing on screen.
 */
internal object ArtworkTintClamp {
    const val MaxSaturation = 0.35f
    const val MaxValue = 0.25f

    fun of(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = hsv[1].coerceIn(0f, MaxSaturation)
        hsv[2] = hsv[2].coerceIn(0f, MaxValue)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
