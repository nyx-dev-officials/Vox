/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vox.music.R

/** SF Pro Text font files used for the app's Latin typography. */
val SfProFontFamily = FontFamily(
    Font(R.font.sf_pro_text_regular, FontWeight.Normal),
    Font(R.font.sf_pro_text_medium, FontWeight.Medium),
    Font(R.font.sf_pro_text_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_text_bold, FontWeight.Bold),
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val SansFlexFontFamily = FontFamily(
    Font(
        resId = R.font.sans_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.sans_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val OutfitFontFamily = FontFamily(
    Font(
        resId = R.font.outfit,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400)
        )
    ),
    Font(
        resId = R.font.outfit,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500)
        )
    ),
    Font(
        resId = R.font.outfit,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val PlusJakartaSansFontFamily = FontFamily(
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400)
        )
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500)
        )
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700)
        )
    )
)

/**
 * Vox's two-voice type system.
 *
 * [Display] is Chillax — geometric, warm, slightly narrow — and it is used for
 * everything that names a thing: the Home masthead, section headers, tile
 * titles, the top-bar route title. [Body] is a clean readable grotesque kept
 * for running copy, artist lines and metadata, where a display face would cost
 * legibility at 12–13sp.
 *
 * The split is the point: one face for nouns, another for sentences. That is
 * what stops the app reading as a system-UI clone even before colour.
 */
object VoxType {
    /** Chillax. Headers, masthead, tile titles, wordmark. */
    val Display = ChillaxFontFamily

    /** Plus Jakarta Sans. Body copy, subtitles, metadata. */
    val Body = PlusJakartaSansFontFamily

    /** Route title in the top bar. */
    val TopBarTitle = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.9).sp,
    )

    /** Home masthead greeting — the one display-size line on the page. */
    val Masthead = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 41.sp,
        letterSpacing = (-1.2).sp,
    )

    /** Section header ("Top Picks For You"). */
    val SectionHeader = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppleTokens.SectionHeader,
        lineHeight = AppleTokens.SectionHeaderLineHeight,
        letterSpacing = (-0.4).sp,
    )

    /** Tile and shelf-card title. */
    val TileTitle = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 14.5f.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.1).sp,
    )

    /** Small uppercase eyebrow — "VOX · SATURDAY", "FEATURED". */
    val Eyebrow = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.9.sp,
    )
}

// TODO: Use M3 Expressive font families when the app adopts that type scale.

// Uses the selected app font and applies tighter tracking to large display styles.
// Smaller body and label styles keep Material's tracking values.
fun AppTypography(fontFamily: FontFamily = FontFamily.Default) = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-1.1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.9).sp
    ),
    displaySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.7).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.45).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal, // M3 uses Normal, M2 used Medium
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp // M3 uses 0.5, M2 used 0.15
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
