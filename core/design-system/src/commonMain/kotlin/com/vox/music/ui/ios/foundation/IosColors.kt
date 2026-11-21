/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.ios.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.vox.music.designsystem.VoxDesignTokens

/**
 * The iOS system palette, split into the light and dark variants Apple ships.
 *
 * Mirrors the "Color" section of exploreswiftui.com: [standard colors][systemBlue],
 * [semantic colors][label], and the UIKit label / fill / separator / background sets.
 */
@Immutable
data class IosColors(
    val isDark: Boolean,

    // Standard colors
    val systemRed: Color,
    val systemOrange: Color,
    val systemYellow: Color,
    val systemGreen: Color,
    val systemMint: Color,
    val systemTeal: Color,
    val systemCyan: Color,
    val systemBlue: Color,
    val systemIndigo: Color,
    val systemPurple: Color,
    val systemPink: Color,
    val systemBrown: Color,

    // Gray variants
    val systemGray: Color,
    val systemGray2: Color,
    val systemGray3: Color,
    val systemGray4: Color,
    val systemGray5: Color,
    val systemGray6: Color,

    // Label colors
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val quaternaryLabel: Color,
    val placeholderText: Color,

    // Fill colors
    val systemFill: Color,
    val secondarySystemFill: Color,
    val tertiarySystemFill: Color,
    val quaternarySystemFill: Color,

    // Separator colors
    val separator: Color,
    val opaqueSeparator: Color,

    // Background colors
    val systemBackground: Color,
    val secondarySystemBackground: Color,
    val tertiarySystemBackground: Color,

    // Grouped background colors
    val systemGroupedBackground: Color,
    val secondarySystemGroupedBackground: Color,
    val tertiarySystemGroupedBackground: Color,

    /** The default accent applied to controls that do not carry an explicit tint. */
    val accent: Color,
) {
    /** The colour a filled control should draw its label in, over [accent]. */
    val onAccent: Color get() = VoxDesignTokens.Colors.OnAccentSignal
}

fun lightIosColors(accent: Color = VoxDesignTokens.Colors.AccentGold): IosColors = IosColors(
    isDark = false,
    systemRed = Color(0xFFFF3B30),
    systemOrange = Color(0xFFFF9500),
    systemYellow = Color(0xFFFFCC00),
    systemGreen = Color(0xFF34C759),
    systemMint = Color(0xFF00C7BE),
    systemTeal = Color(0xFF30B0C7),
    systemCyan = Color(0xFF32ADE6),
    systemBlue = Color(0xFF007AFF),
    systemIndigo = Color(0xFF5856D6),
    systemPurple = Color(0xFFAF52DE),
    systemPink = Color(0xFFFF2D55),
    systemBrown = Color(0xFFA2845E),
    systemGray = Color(0xFF8E8E93),
    systemGray2 = Color(0xFFAEAEB2),
    systemGray3 = Color(0xFFC7C7CC),
    systemGray4 = Color(0xFFD1D1D6),
    systemGray5 = Color(0xFFE5E5EA),
    systemGray6 = Color(0xFFF2F2F7),
    label = VoxDesignTokens.Colors.InkOnPaper,
    secondaryLabel = VoxDesignTokens.Colors.StoneOnPaper,
    tertiaryLabel = Color(0x9914130F),
    quaternaryLabel = Color(0x4D14130F),
    placeholderText = Color(0x6614130F),
    systemFill = Color(0x1F14130F),
    secondarySystemFill = Color(0x1414130F),
    tertiarySystemFill = Color(0x0F14130F),
    quaternarySystemFill = Color(0x0A14130F),
    separator = VoxDesignTokens.Colors.HairlineOnPaper,
    opaqueSeparator = Color(0xFFD9D2C3),
    systemBackground = VoxDesignTokens.Colors.Paper,
    secondarySystemBackground = Color(0xFFEDE7DB),
    tertiarySystemBackground = VoxDesignTokens.Colors.PaperElevated,
    systemGroupedBackground = VoxDesignTokens.Colors.Paper,
    secondarySystemGroupedBackground = VoxDesignTokens.Colors.PaperElevated,
    tertiarySystemGroupedBackground = Color(0xFFEDE7DB),
    accent = accent,
)

fun darkIosColors(accent: Color = VoxDesignTokens.Colors.AccentGold): IosColors = IosColors(
    isDark = true,
    systemRed = Color(0xFFFF453A),
    systemOrange = Color(0xFFFF9F0A),
    systemYellow = Color(0xFFFFD60A),
    systemGreen = Color(0xFF30D158),
    systemMint = Color(0xFF63E6E2),
    systemTeal = Color(0xFF40C8E0),
    systemCyan = Color(0xFF64D2FF),
    systemBlue = Color(0xFF0A84FF),
    systemIndigo = Color(0xFF5E5CE6),
    systemPurple = Color(0xFFBF5AF2),
    systemPink = Color(0xFFFF375F),
    systemBrown = Color(0xFFAC8E68),
    systemGray = Color(0xFF8E8E93),
    systemGray2 = Color(0xFF636366),
    systemGray3 = Color(0xFF48484A),
    systemGray4 = Color(0xFF3A3A3C),
    systemGray5 = Color(0xFF2C2C2E),
    systemGray6 = Color(0xFF1C1C1E),
    label = VoxDesignTokens.Colors.OnSurface,
    secondaryLabel = VoxDesignTokens.Colors.SecondaryLabel,
    tertiaryLabel = Color(0x66F4F2EC),
    quaternaryLabel = Color(0x33F4F2EC),
    placeholderText = Color(0x66F4F2EC),
    systemFill = Color(0x1AF4F2EC),
    secondarySystemFill = Color(0x12F4F2EC),
    tertiarySystemFill = Color(0x0DF4F2EC),
    quaternarySystemFill = Color(0x08F4F2EC),
    separator = VoxDesignTokens.Colors.Hairline,
    opaqueSeparator = Color(0xFF2A2A31),
    // Ink, not an OLED void: the hairline grid and the signal need a floor.
    systemBackground = VoxDesignTokens.Colors.Background,
    secondarySystemBackground = VoxDesignTokens.Colors.BackgroundElevated,
    tertiarySystemBackground = VoxDesignTokens.Colors.Card,
    systemGroupedBackground = VoxDesignTokens.Colors.Background,
    secondarySystemGroupedBackground = VoxDesignTokens.Colors.BackgroundElevated,
    tertiarySystemGroupedBackground = VoxDesignTokens.Colors.Card,
    accent = accent,
)
