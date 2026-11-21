package com.vox.music.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand, layout, type, and glass defaults shared by Vox clients. */
object VoxDesignTokens {
    object Colors {
        /**
         * Vox Ink. A deeper, browner obsidian than a neutral near-black, so the
         * champagne rule and hairline edges have something warm to sit on.
         * Deliberately not #000000: an OLED void swallows the hairline system.
         */
        val Background = Color(0xFF0B0A09)

        /** Burnt Umber. The layer directly above the canvas. */
        val BackgroundElevated = Color(0xFF151210)

        /** Elevated card, one step up again. */
        val Card = Color(0xFF1D1916)

        /** A card inside a card. The last step before the surface loses depth. */
        val CardSecondary = Color(0xFF262119)

        /** Warm Stone. Artist names, timestamps, inactive lyrics. */
        val SecondaryLabel = Color(0xFFA29A90)

        /** Warm Alabaster. Track titles and the active lyric line. */
        val OnSurface = Color(0xFFF7F4EE)

        /**
         * Champagne Gold. The only accent in the product, so it stays one: play,
         * scrubber progress, active tab, the section-header rule.
         */
        val AccentGold = Color(0xFFE8C79B)

        /** The same gold at 20%, for the bloom behind an active control. */
        val AccentGlow = Color(0x33E8C79B)

        /**
         * Warm Paper. Light mode is not iOS grey — it is a printed page, so type
         * sits on paper stock rather than on a cool system background.
         */
        val Paper = Color(0xFFF4EFE7)
        val PaperElevated = Color(0xFFFBF8F3)
        val InkOnPaper = Color(0xFF191512)
        val StoneOnPaper = Color(0xFF6E635A)

        /** 1dp white hairline that edges a warm-obsidian surface. */
        val Hairline = Color(0x1FFFFFFF)
        /** The same rule on paper. */
        val HairlineOnPaper = Color(0x14000000)
    }

    object Metrics {
        val SidebarWidth = 320.dp
        val SidebarCollapsedWidth = 72.dp
        val SidebarInset = 30.dp
        val SidebarContentGap = 20.dp
        val SidebarRowHeight = 44.dp
        val SidebarLeadingSlot = 24.dp
        val SidebarIconSize = 22.dp
        val SidebarCorner = 20.dp
        val TabletWidthThreshold = 840.dp
        val MiniPlayerHeight = 64.dp
        val MiniPlayerArtworkSize = 38.dp
        val MiniPlayerControlSize = 36.dp
        val MiniPlayerIconSize = 18.dp
        val MiniPlayerPlayIconSize = 22.dp
        const val MiniPlayerWidthFraction = 0.8f
        val ContentGutter = 20.dp
        val ListRowInset = 16.dp
        val ItemGap = 16.dp
        val SectionGap = 24.dp
        val TextGap = 2.dp
        val ArtworkCorner = 10.dp
        val CardCorner = 16.dp
        val LargeCardCorner = 20.dp
        /** Width of the hairline border that replaces a glass edge on scrolling surfaces. */
        val Hairline = 1.dp
        /** Width and height of the champagne rule that opens a Vox section header. */
        val HeaderRuleWidth = 2.5.dp
        val HeaderRuleHeight = 16.dp
    }

    object Type {
        val ScreenTitle = 34.sp
        val ScreenTitleLineHeight = 41.sp
        /** The Home masthead greeting — the one display-size line on the page. */
        val Masthead = 32.sp
        val MastheadLineHeight = 36.sp
        /** Small uppercase eyebrow above the masthead, and above a featured card. */
        val Eyebrow = 10.sp
        val EyebrowLineHeight = 14.sp
        const val EyebrowTracking = 0.16f
        val SectionHeader = 22.sp
        val SectionHeaderLineHeight = 28.sp
        val ItemTitle = 15.sp
        val ItemTitleLineHeight = 20.sp
        val ItemSubtitle = 13.sp
        val ItemSubtitleLineHeight = 18.sp
        val Caption = 12.sp
        val CaptionLineHeight = 16.sp
        val SidebarLabel = 17.sp
        val SidebarLabelLineHeight = 22.sp
        val SidebarLabelTracking = (-0.43).sp
        val MiniPlayerTitle = 15.sp
        val MiniPlayerTitleLineHeight = 20.sp
        val MiniPlayerArtist = 11.sp
        val MiniPlayerArtistLineHeight = 13.sp
    }

    object Glass {
        const val MaxLensDp = 48f
        const val SidePanelBlurDp = 2f
        /**
         * No saturation boost anywhere in the glass stack. Artwork tints are already
         * clamped to 35% saturation; multiplying them here would hand the extra
         * chroma straight back and the ceiling would only exist on paper.
         */
        const val SidePanelSaturation = 1f
        const val SidePanelLensHeight = 0.4f
        const val SidePanelLensAmount = 0.6f
        const val SidePanelTintOpacity = 0.75f
        const val MiniPlayerBlurDp = 3f
        const val MiniPlayerSaturation = 1f
        const val MiniPlayerLensHeight = 0.6f
        const val MiniPlayerLensAmount = 0.69f
        /** Smoked Espresso at the spec's 68%, not a white lift. */
        const val MiniPlayerTintOpacity = 0.68f
        const val DarkSelectedPuckOpacity = 0.55f
        const val SidePanelHighlightOpacity = 0.3f
        const val MiniPlayerHighlightOpacity = 0.3f
    }
}
