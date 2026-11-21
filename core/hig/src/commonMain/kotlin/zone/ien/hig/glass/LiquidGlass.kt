package zone.ien.hig.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vox.music.designsystem.VoxDesignTokens
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow

/** Tunable liquid-glass material shared by the KMP clients. */
@Immutable
data class LiquidGlassMaterial(
    val tint: Color,
    val blurRadius: Dp = 2.dp,
    val saturation: Float = 1.6f,
    val refractionHeight: Dp = 19.2.dp,
    val refractionAmount: Dp = 28.8.dp,
    val highlightAlpha: Float = 0.38f,
    val shadowAlpha: Float = 0.28f,
    val depthEffect: Boolean = false,
    val chromaticAberration: Boolean = false,
)

object LiquidGlassMaterials {
    val SidePanel = LiquidGlassMaterial(
        tint = Color(0xFF0E0D0C).copy(alpha = VoxDesignTokens.Glass.SidePanelTintOpacity),
        blurRadius = VoxDesignTokens.Glass.SidePanelBlurDp.dp,
        saturation = VoxDesignTokens.Glass.SidePanelSaturation,
        refractionHeight = (VoxDesignTokens.Glass.SidePanelLensHeight * VoxDesignTokens.Glass.MaxLensDp).dp,
        refractionAmount = (VoxDesignTokens.Glass.SidePanelLensAmount * VoxDesignTokens.Glass.MaxLensDp).dp,
        highlightAlpha = VoxDesignTokens.Glass.SidePanelHighlightOpacity,
        shadowAlpha = 0.3f,
    )

    val SelectedTab = LiquidGlassMaterial(
        tint = Color(0xFF262320).copy(alpha = VoxDesignTokens.Glass.DarkSelectedPuckOpacity),
        blurRadius = 2.dp,
        saturation = 1f,
        refractionHeight = 32.dp,
        refractionAmount = 28.dp,
        highlightAlpha = 0f,
        shadowAlpha = 0.35f,
        chromaticAberration = true,
    )

    val MiniPlayer = LiquidGlassMaterial(
        tint = Color(0xFF1B1917).copy(alpha = VoxDesignTokens.Glass.MiniPlayerTintOpacity),
        blurRadius = VoxDesignTokens.Glass.MiniPlayerBlurDp.dp,
        saturation = VoxDesignTokens.Glass.MiniPlayerSaturation,
        refractionHeight = (VoxDesignTokens.Glass.MiniPlayerLensHeight * VoxDesignTokens.Glass.MaxLensDp).dp,
        refractionAmount = (VoxDesignTokens.Glass.MiniPlayerLensAmount * VoxDesignTokens.Glass.MaxLensDp).dp,
        highlightAlpha = VoxDesignTokens.Glass.MiniPlayerHighlightOpacity,
        shadowAlpha = 0.32f,
    )
}

/** Draws a live, refractive glass surface over [backdrop]. */
fun Modifier.liquidGlass(
    backdrop: LayerBackdrop,
    shape: Shape,
    material: LiquidGlassMaterial = LiquidGlassMaterials.SidePanel,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        colorControls(saturation = material.saturation)
        blur(material.blurRadius.toPx())
        lens(
            refractionHeight = material.refractionHeight.toPx(),
            refractionAmount = material.refractionAmount.toPx(),
            depthEffect = material.depthEffect,
            chromaticAberration = material.chromaticAberration,
        )
    },
    highlight = if (material.highlightAlpha <= 0f) null else {
        {
            Highlight(
                width = 0.7.dp,
                blurRadius = 0.45.dp,
                alpha = material.highlightAlpha,
                style = HighlightStyle.Default(angle = 135f),
            )
        }
    },
    shadow = {
        Shadow(radius = 24.dp, offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp), alpha = material.shadowAlpha)
    },
    onDrawSurface = { drawRect(material.tint) },
)
