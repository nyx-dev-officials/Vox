package com.vox.music.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand, layout, type, and glass defaults shared by Vox clients. */
object VoxDesignTokens {
    object Colors {
        /**
         * Vox Ink. A flat, cold near-black — print black rather than a warm
         * brown-black, so the vermilion signal and the hairline grid stay sharp
         * instead of sinking into a tinted fog. Not #000000: OLED voids swallow
         * the entire hairline system.
         */
        val Background = Color(0xFF09090B)

        /** The layer directly above the canvas. */
        val BackgroundElevated = Color(0xFF111114)

        /** Elevated card, one step up again. */
        val Card = Color(0xFF16161A)

        /** A card inside a card. The last step before the surface loses depth. */
        val CardSecondary = Color(0xFF1E1E24)

        /** Warm Stone. Artist names, timestamps, inactive lyrics. */
        val SecondaryLabel = Color(0xFF9C9993)

        /** Bone. Track titles and the active lyric line. */
        val OnSurface = Color(0xFFF4F2EC)

        /**
         * Signal. The single accent in the product: vermilion at full strength.
         * It marks play, progress, the active tab, a section index — nothing else.
         * A print accent, not a gradient, so it survives on top of artwork.
         */
        val AccentSignal = Color(0xFFFF5C38)

        /** Legacy name kept so existing call sites inherit the signal colour. */
        val AccentGold = AccentSignal

        /** The signal at 18%, for the wash behind an active control. */
        val AccentGlow = Color(0x2EFF5C38)

        /** Ink on top of a filled signal surface. Not pure black — it reads warmer. */
        val OnAccentSignal = Color(0xFF120B08)

        /**
         * Paper. Light mode is a printed page: warm stock, ink type, a hairline
         * rule. Never an iOS grey system background.
         */
        val Paper = Color(0xFFF5F1E8)
        val PaperElevated = Color(0xFFFCFAF4)
        val InkOnPaper = Color(0xFF14130F)
        val StoneOnPaper = Color(0xFF6A655C)

        /** 1dp hairline that edges an ink surface. 12% bone. */
        val Hairline = Color(0x1FF4F2EC)
        /** The same rule on paper. */
        val HairlineOnPaper = Color(0x1F14130F)
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
        val ItemGap = 14.dp
        val SectionGap = 32.dp
        val TextGap = 3.dp
        /** Artwork is cut, not squircle-pillowed: a printed plate edge. */
        val ArtworkCorner = 4.dp
        val CardCorner = 8.dp
        val LargeCardCorner = 12.dp
        /** Width of the hairline border that replaces a glass edge on scrolling surfaces. */
        val Hairline = 1.dp
        /** Width and height of the signal tick that opens a Vox section header. */
        val HeaderRuleWidth = 2.dp
        val HeaderRuleHeight = 12.dp
    }

    object Type {
        val ScreenTitle = 38.sp
        val ScreenTitleLineHeight = 42.sp
        /** The Home masthead greeting — the one display-size line on the page. */
        val Masthead = 38.sp
        val MastheadLineHeight = 41.sp
        /** Small uppercase eyebrow above the masthead, and above a featured card. */
        val Eyebrow = 10.sp
        val EyebrowLineHeight = 13.sp
        const val EyebrowTracking = 0.20f
        val SectionHeader = 20.sp
        val SectionHeaderLineHeight = 24.sp
        val ItemTitle = 14.5.sp
        val ItemTitleLineHeight = 19.sp
        val ItemSubtitle = 12.5.sp
        val ItemSubtitleLineHeight = 16.sp
        val Caption = 12.sp
        val CaptionLineHeight = 16.sp
        val SidebarLabel = 17.sp
        val SidebarLabelLineHeight = 22.sp
        val SidebarLabelTracking = (-0.43).sp
        val MiniPlayerTitle = 14.5.sp
        val MiniPlayerTitleLineHeight = 18.sp
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
