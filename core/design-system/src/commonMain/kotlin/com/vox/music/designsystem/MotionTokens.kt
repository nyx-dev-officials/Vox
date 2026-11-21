package com.vox.music.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import kotlin.math.exp

/** Shared timing and curves for Vox navigation, artwork morphs, and selection motion. */
object MotionTokens {
    const val ArtworkMillis = 350
    const val SharedArtworkMillis = 500
    const val PushMillis = 260
    const val PageEnterMillis = 300
    const val PageExitMillis = 220
    const val PlayerMorphEnterMillis = 340
    const val PlayerMorphExitMillis = 260
    const val PlayerMorphStiffness = 950f
    const val PlayerSheetStiffness = 360f
    const val PlayerSheetLandingDamping = 0.72f
    const val PlayerLandingDamping = 0.5f
    const val PlayerLandingStiffness = 140f
    const val PlayerLandingMaxDipDp = 22f
    const val PlayerLandingSquash = 0.14f
    const val PlayerLandingSpread = 0.04f
    const val PushParallax = 0.30f
    const val PushDimAlpha = 0.85f

    private const val ArtworkEndpoint = 9f
    private val ArtworkNormalization = 1f / (1f - (1f + ArtworkEndpoint) * exp(-ArtworkEndpoint))
    private val ArtworkEasing: Easing = Easing(::artworkProgress)
    private val PushEasing: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val PlayerMorphEasing: Easing = CubicBezierEasing(0.3f, 0.5f, 0.4f, 1f)

    fun <T> artwork(): FiniteAnimationSpec<T> = tween(ArtworkMillis, easing = ArtworkEasing)

    fun <T> sharedArtwork(): FiniteAnimationSpec<T> = tween(SharedArtworkMillis, easing = ArtworkEasing)

    /**
     * A pushed page. Spring rather than a fixed tween: two screens of different
     * heights then settle together instead of ending on a visible seam, and a
     * gesture that interrupts mid-flight keeps its velocity.
     */
    fun <T> push(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.96f, stiffness = 420f)

    fun <T> playerMorph(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = PlayerMorphStiffness,
    )

    fun <T> playerMorphEnter(): FiniteAnimationSpec<T> =
        tween(PlayerMorphEnterMillis, easing = PlayerMorphEasing)

    fun <T> playerMorphExit(): FiniteAnimationSpec<T> =
        tween(PlayerMorphExitMillis, easing = PlayerMorphEasing)

    /**
     * Mini player -> full player. One spring, the same one the tab morph uses: the
     * sheet's container bounds fly on 380/0.85 so the pill's plate and the player's
     * plate move as a single bounded object instead of two surfaces overlapping.
     */
    fun <T> playerSheetOpen(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.85f,
        stiffness = 380f,
    )

    fun <T> playerSheetClose(landing: Boolean = false): FiniteAnimationSpec<T> = spring(
        dampingRatio = if (landing) PlayerSheetLandingDamping else Spring.DampingRatioNoBouncy,
        stiffness = PlayerSheetStiffness,
    )

    fun collectionPush(): FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 1f,
        stiffness = 380f,
        visibilityThreshold = IntOffset(1, 1),
    )

    fun <T> tabMorph(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.85f, stiffness = 380f)

    fun <T> tabPuck(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 380f)

    // ---- Vox motion family -------------------------------------------------
    // Everything that moves a surface in this app picks one of the four specs
    // below. Ad-hoc tweens with default easings are what make a Compose app feel
    // stitched together; one physical family is what makes it feel machined.

    /** A surface changing size or place. No bounce: a page has mass, not spring toys. */
    fun <T> settle(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.92f, stiffness = 340f)

    /** Immediate feedback on touch — press, toggle, selection. Fast enough to feel direct. */
    fun <T> snap(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.82f, stiffness = 640f)

    /** Content arriving: expo-out, one long clean deceleration. */
    fun <T> reveal(): FiniteAnimationSpec<T> = tween(
        durationMillis = 520,
        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
    )

    /** Content leaving. Always ~30% faster than [reveal]. */
    fun <T> dismiss(): FiniteAnimationSpec<T> = tween(
        durationMillis = 180,
        easing = CubicBezierEasing(0.4f, 0f, 1f, 1f),
    )

    /** Delay between siblings in a staggered entrance. */
    const val StaggerMillis = 28

    /** Travel for a reveal: 12dp of lift, no scale warping of text. */
    val RevealTravel = androidx.compose.ui.unit.Dp(12f)

    fun parallaxOffset(fullWidth: Int): Int = -(fullWidth * PushParallax).toInt()

    fun artworkProgress(fraction: Float): Float {
        if (!fraction.isFinite()) return 1f
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        val position = ArtworkEndpoint * fraction
        return (1f - (1f + position) * exp(-position)) * ArtworkNormalization
    }
}
