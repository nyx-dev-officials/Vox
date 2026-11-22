/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.vox.music.R
import com.vox.music.ui.component.LyricsBackgroundStyle
import com.vox.music.ui.component.ShareAspectRatio
import com.vox.music.ui.component.ShareCardTemplate
import com.vox.music.ui.component.ShareFontStyle
import com.vox.music.ui.component.ShareWatermarkStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Draws the shareable lyrics card.
 *
 * One renderer feeds both the sheet's live preview and the exported PNG, so what
 * the user arranges on screen is exactly what leaves the app. Everything is laid
 * out in "design dp" from a 340dp-wide reference and scaled to the output, which
 * keeps the composition identical at preview size (720px) and export size (2160px).
 */
object ComposeToImage {

    /** Export width. Preview renders pass a smaller [renderWidth]. */
    private const val EXPORT_WIDTH = 2160

    private const val INK = 0xFF0B0A09.toInt()
    private const val OBSIDIAN = 0xFF0B0A09.toInt()
    private const val WARM_PAPER = 0xFFF3ECE2.toInt()
    private const val CHAMPAGNE = 0xFFE7C8A0.toInt()

    suspend fun createLyricsImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        backgroundColor: Int? = null,
        backgroundStyle: LyricsBackgroundStyle = LyricsBackgroundStyle.BLUR,
        textColor: Int? = null,
        secondaryTextColor: Int? = null,
        lyricsAlignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER,
        template: ShareCardTemplate = ShareCardTemplate.CLASSIC_BLUR,
        aspectRatio: ShareAspectRatio = ShareAspectRatio.AUTO,
        fontStyle: ShareFontStyle = ShareFontStyle.CHILLAX,
        watermark: ShareWatermarkStyle = ShareWatermarkStyle.MINIMAL,
        backdropDim: Float = 0.42f,
        backdropBlur: Float = 0.55f,
        artwork: Bitmap? = null,
        renderWidth: Int = 0,
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = if (renderWidth > 0) renderWidth else EXPORT_WIDTH
        val blankLines = lyrics.lines().count { it.isNotBlank() }.coerceAtLeast(1)
        val height = when (aspectRatio) {
            ShareAspectRatio.SQUARE -> width
            ShareAspectRatio.STORY -> (width * 16f / 9f).roundToInt()
            // Compact auto: grows with the quote until it has room, then stops.
            ShareAspectRatio.AUTO -> (width * (0.98f + 0.055f * blankLines))
                .roundToInt()
                .coerceIn(width, (width * 1.7f).roundToInt())
        }

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val scale = width / 340f
        val padding = 28f * scale

        val cover = artwork ?: loadArtwork(context, coverArtUrl)

        val templateBase = when (template) {
            ShareCardTemplate.EDITORIAL_POSTER -> WARM_PAPER
            else -> OBSIDIAN
        }
        val baseColor = backgroundColor ?: templateBase
        val darkBase = ColorUtils.calculateLuminance(baseColor) < 0.45
        val ink = textColor ?: if (darkBase) 0xFFFFFFFF.toInt() else INK
        val secondaryInk = secondaryTextColor
            ?: withAlpha(ink, if (darkBase) 0.62f else 0.55f)

        drawBackdrop(
            canvas = canvas,
            width = width,
            height = height,
            baseColor = baseColor,
            cover = cover,
            style = backgroundStyle,
            backdropDim = backdropDim,
            backdropBlur = backdropBlur,
        )

        // A hairline inset frame gives the card an edge on any feed background.
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * scale
            color = withAlpha(ink, 0.10f)
        }
        canvas.drawRoundRect(
            RectF(padding * 0.2f, padding * 0.2f, width - padding * 0.2f, height - padding * 0.2f),
            22f * scale,
            22f * scale,
            frame,
        )

        val displayTypeface = typefaceFor(context, fontStyle, bold = true)
        val bodyTypeface = typefaceFor(context, fontStyle, bold = false)

        val titlePaint = TextPaint().apply {
            color = ink
            textSize = 21f * scale
            typeface = displayTypeface
            isAntiAlias = true
        }
        val artistPaint = TextPaint().apply {
            color = secondaryInk
            textSize = 14f * scale
            typeface = bodyTypeface
            isAntiAlias = true
            letterSpacing = 0.01f
        }

        var contentTop = padding
        when (template) {
            ShareCardTemplate.EDITORIAL_POSTER -> {
                // Editorial: artist as an eyebrow, title as the display line, no thumbnail.
                val eyebrowPaint = TextPaint(artistPaint).apply {
                    textSize = 11f * scale
                    letterSpacing = 0.18f
                    color = secondaryInk
                }
                val eyebrow = StaticLayout.Builder
                    .obtain(artistName.uppercase(), 0, artistName.uppercase().length, eyebrowPaint, (width - padding * 2).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                canvas.save()
                canvas.translate(padding, contentTop)
                eyebrow.draw(canvas)
                canvas.restore()
                contentTop += eyebrow.height + 10f * scale

                val bigTitle = TextPaint(titlePaint).apply { textSize = 34f * scale }
                val title = StaticLayout.Builder
                    .obtain(songTitle, 0, songTitle.length, bigTitle, (width - padding * 2).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .setLineSpacing(0f, 1.05f)
                    .build()
                canvas.save()
                canvas.translate(padding, contentTop)
                title.draw(canvas)
                canvas.restore()
                contentTop += title.height
            }

            ShareCardTemplate.VINYL_FRAME -> {
                // Centred record: grooves behind the sleeve, type underneath.
                val coverSize = 96f * scale
                val centreX = width / 2f
                val centreY = contentTop + coverSize / 2f
                drawVinylGrooves(canvas, centreX, centreY, coverSize * 0.86f, ink)
                drawCover(
                    canvas = canvas,
                    cover = cover,
                    left = centreX - coverSize / 2f,
                    top = contentTop,
                    size = coverSize,
                    radius = 3f * scale,
                    ink = ink,
                    scale = scale,
                )
                contentTop += coverSize + 16f * scale

                val centredTitle = TextPaint(titlePaint).apply {
                    textSize = 20f * scale
                }
                val centredArtist = TextPaint(artistPaint).apply {
                    textSize = 13f * scale
                }
                val textWidth = (width - padding * 2).toInt()
                val title = StaticLayout.Builder
                    .obtain(songTitle, 0, songTitle.length, centredTitle, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                val artist = StaticLayout.Builder
                    .obtain(artistName, 0, artistName.length, centredArtist, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                canvas.save()
                canvas.translate(padding, contentTop)
                title.draw(canvas)
                canvas.translate(0f, title.height.toFloat() + 2f * scale)
                artist.draw(canvas)
                canvas.restore()
                contentTop += title.height + artist.height + 4f * scale
            }

            else -> {
                // Row: sleeve on the left, title and artist stacked beside it.
                val coverSize = 64f * scale
                drawCover(
                    canvas = canvas,
                    cover = cover,
                    left = padding,
                    top = contentTop,
                    size = coverSize,
                    radius = 3f * scale,
                    ink = ink,
                    scale = scale,
                )
                val textStartX = padding + coverSize + 16f * scale
                val textWidth = (width - textStartX - padding).toInt().coerceAtLeast(1)
                val title = StaticLayout.Builder
                    .obtain(songTitle, 0, songTitle.length, titlePaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                val artist = StaticLayout.Builder
                    .obtain(artistName, 0, artistName.length, artistPaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(1)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                val stackHeight = title.height + artist.height + 2f * scale
                canvas.save()
                canvas.translate(textStartX, contentTop + (coverSize - stackHeight) / 2f)
                title.draw(canvas)
                canvas.translate(0f, title.height.toFloat() + 2f * scale)
                artist.draw(canvas)
                canvas.restore()
                contentTop += coverSize
            }
        }

        // Lyrics block, sized to fill the space rather than to a fixed point size.
        val footerHeight = when (watermark) {
            ShareWatermarkStyle.HIDDEN -> 0f
            else -> 34f * scale
        }
        val lyricsTop = contentTop + 24f * scale
        val lyricsBottom = height - padding - footerHeight - 10f * scale
        val lyricsWidth = (width - padding * 2).toInt().coerceAtLeast(1)
        val lyricsHeight = max(1f, lyricsBottom - lyricsTop)

        val lyricsPaint = TextPaint().apply {
            color = ink
            typeface = displayTypeface
            isAntiAlias = true
            letterSpacing = -0.01f
        }
        val initialSize = when (template) {
            ShareCardTemplate.EDITORIAL_POSTER -> 34f * scale
            ShareCardTemplate.MINIMAL_OBSIDIAN -> 26f * scale
            else -> 30f * scale
        }
        val minSize = 11f * scale
        var size = initialSize
        var layout: StaticLayout? = null
        while (size >= minSize) {
            lyricsPaint.textSize = size
            val candidate = lyricsLayout(lyrics, lyricsPaint, lyricsWidth, lyricsAlignment)
            if (candidate.height <= lyricsHeight) {
                layout = candidate
                break
            }
            size -= 1f * scale
        }
        lyricsPaint.textSize = size
        val finalLayout = layout ?: lyricsLayout(lyrics, lyricsPaint, lyricsWidth, lyricsAlignment)

        val lyricsY = if (finalLayout.height < lyricsHeight) {
            lyricsTop + (lyricsHeight - finalLayout.height) / 2f
        } else {
            lyricsTop
        }
        canvas.save()
        canvas.translate(padding, lyricsY)
        finalLayout.draw(canvas)
        canvas.restore()

        if (watermark != ShareWatermarkStyle.HIDDEN) {
            drawWatermark(
                context = context,
                canvas = canvas,
                x = padding,
                top = height - padding - 26f * scale,
                scale = scale,
                ink = ink,
                secondaryInk = secondaryInk,
                baseColor = baseColor,
                style = watermark,
            )
        }

        return@withContext bitmap
    }

    /** Background treatments. Image-backed styles dim to whichever level the user set. */
    private fun drawBackdrop(
        canvas: Canvas,
        width: Int,
        height: Int,
        baseColor: Int,
        cover: Bitmap?,
        style: LyricsBackgroundStyle,
        backdropDim: Float,
        backdropBlur: Float,
    ) {
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dim = backdropDim.coerceIn(0f, 0.9f)
        val needsArt = style != LyricsBackgroundStyle.SOLID

        paint.color = baseColor
        canvas.drawRect(rect, paint)

        if (cover != null && needsArt) {
            when (style) {
                LyricsBackgroundStyle.BLUR -> {
                    val radius = (4 + 28 * backdropBlur.coerceIn(0f, 1f)).toInt()
                    val small = Bitmap.createScaledBitmap(
                        cover,
                        (width / 9).coerceAtLeast(1),
                        (height / 9).coerceAtLeast(1),
                        true,
                    )
                    val blurred = fastBlur(small, 1f, radius)
                    if (blurred != null) {
                        canvas.drawBitmap(blurred, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
                    }
                }

                LyricsBackgroundStyle.GRADIENT -> {
                    val palette = Palette.from(cover).generate()
                    val from = palette.getVibrantColor(baseColor)
                    val to = palette.getDarkMutedColor(baseColor)
                    paint.shader = LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        intArrayOf(from, to), null, Shader.TileMode.CLAMP,
                    )
                    canvas.drawRect(rect, paint)
                }

                LyricsBackgroundStyle.MESH -> {
                    val palette = Palette.from(cover).generate()
                    val swatches = listOfNotNull(
                        palette.vibrantSwatch,
                        palette.dominantSwatch,
                        palette.mutedSwatch,
                    ).map { it.rgb }.ifEmpty { listOf(baseColor) }
                    val radius = max(width, height) * 0.75f
                    val anchors = listOf(
                        0.18f to 0.22f,
                        0.82f to 0.38f,
                        0.35f to 0.85f,
                    )
                    anchors.forEachIndexed { index, (fx, fy) ->
                        val color = swatches[index % swatches.size]
                        paint.shader = RadialGradient(
                            width * fx,
                            height * fy,
                            radius,
                            withAlpha(color, 0.85f),
                            withAlpha(color, 0f),
                            Shader.TileMode.CLAMP,
                        )
                        canvas.drawRect(rect, paint)
                    }
                    paint.shader = null
                }

                LyricsBackgroundStyle.FULL_BLEED -> {
                    val scale = max(width.toFloat() / cover.width, height.toFloat() / cover.height)
                    val matrix = android.graphics.Matrix().apply {
                        setScale(scale, scale)
                        postTranslate(
                            (width - cover.width * scale) / 2f,
                            (height - cover.height * scale) / 2f,
                        )
                    }
                    paint.shader = android.graphics.BitmapShader(
                        cover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP,
                    ).apply { setLocalMatrix(matrix) }
                    canvas.drawRect(rect, paint)
                    paint.shader = null
                }

                LyricsBackgroundStyle.SOLID -> Unit
            }
        }

        // Scrim: strongest at the bottom, where the transport-less card still needs
        // its watermark and final lines to stay crisp.
        if (needsArt) {
            paint.shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    withAlpha(0xFF000000.toInt(), dim * 0.7f),
                    withAlpha(0xFF000000.toInt(), dim),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(rect, paint)
            paint.shader = null
        }
    }

    private fun lyricsLayout(
        lyrics: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment,
    ): StaticLayout =
        StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, paint, width)
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.3f)
            .setIncludePad(false)
            .build()

    private fun drawCover(
        canvas: Canvas,
        cover: Bitmap?,
        left: Float,
        top: Float,
        size: Float,
        radius: Float,
        ink: Int,
        scale: Float,
    ) {
        val rect = RectF(left, top, left + size, top + size)
        if (cover != null) {
            val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(cover, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val placeholder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(ink, 0.10f)
            }
            canvas.drawRoundRect(rect, radius, radius, placeholder)
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * scale
            color = withAlpha(ink, 0.16f)
        }
        canvas.drawRoundRect(rect, radius, radius, border)
    }

    /** Concentric grooves behind a centred sleeve; the vinyl template's signature. */
    private fun drawVinylGrooves(
        canvas: Canvas,
        centreX: Float,
        centreY: Float,
        radius: Float,
        ink: Int,
    ) {
        val groove = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = withAlpha(ink, 0.10f)
        }
        var r = radius
        var ring = 0
        while (r > radius * 0.35f && ring < 9) {
            groove.strokeWidth = if (ring == 0) 2.2f else 1.1f
            canvas.drawCircle(centreX, centreY, r, groove)
            r -= radius * 0.075f
            ring++
        }
    }

    private fun drawWatermark(
        context: Context,
        canvas: Canvas,
        x: Float,
        top: Float,
        scale: Float,
        ink: Int,
        secondaryInk: Int,
        baseColor: Int,
        style: ShareWatermarkStyle,
    ) {
        val wordmark = context.getString(R.string.app_name)
        val wordPaint = TextPaint().apply {
            color = secondaryInk
            textSize = 13f * scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            letterSpacing = 0.02f
        }
        val wordWidth = wordPaint.measureText(wordmark)
        val logoSize = 18f * scale

        when (style) {
            ShareWatermarkStyle.MINIMAL -> {
                val boxSize = logoSize + 8f * scale
                val box = RectF(x, top, x + boxSize, top + boxSize)
                canvas.drawOval(
                    box,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = secondaryInk },
                )
                drawLogo(context, canvas, box, baseColor)
                val baseline = top + boxSize / 2f -
                    (wordPaint.descent() + wordPaint.ascent()) / 2f
                canvas.drawText(wordmark, x + boxSize + 8f * scale, baseline, wordPaint)
            }

            ShareWatermarkStyle.PILL -> {
                val pillHeight = 30f * scale
                val pillWidth = logoSize + wordWidth + 30f * scale
                val pill = RectF(x, top - 4f * scale, x + pillWidth, top - 4f * scale + pillHeight)
                canvas.drawRoundRect(
                    pill,
                    pillHeight / 2f,
                    pillHeight / 2f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(ink, 0.16f) },
                )
                val logoBox = RectF(
                    pill.left + 7f * scale,
                    pill.centerY() - logoSize / 2f,
                    pill.left + 7f * scale + logoSize,
                    pill.centerY() + logoSize / 2f,
                )
                drawLogo(context, canvas, logoBox, ink)
                val baseline = pill.centerY() -
                    (wordPaint.descent() + wordPaint.ascent()) / 2f
                canvas.drawText(wordmark, logoBox.right + 6f * scale, baseline, wordPaint)
            }

            ShareWatermarkStyle.HIDDEN -> Unit
        }
    }

    private fun drawLogo(
        context: Context,
        canvas: Canvas,
        box: RectF,
        tint: Int,
    ) {
        val logo = context.getDrawable(R.drawable.vox_logo)?.toBitmap() ?: return
        val inset = box.width() * 0.18f
        val target = RectF(
            box.left + inset,
            box.top + inset,
            box.right - inset,
            box.bottom - inset,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
            isFilterBitmap = true
        }
        canvas.drawBitmap(logo, null, target, paint)
    }

    private fun typefaceFor(
        context: Context,
        style: ShareFontStyle,
        bold: Boolean,
    ): Typeface = when (style) {
        ShareFontStyle.CHILLAX -> ResourcesCompat.getFont(
            context,
            if (bold) R.font.chillax_bold else R.font.chillax_regular,
        ) ?: Typeface.DEFAULT_BOLD
        ShareFontStyle.JAKARTA -> ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
            ?: Typeface.DEFAULT
        ShareFontStyle.SERIF -> Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        ShareFontStyle.MONO -> Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private suspend fun loadArtwork(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(1024)
                .allowHardware(false)
                .build()
            ImageLoader(context).execute(request).image?.toBitmap()
        }.getOrNull()
    }

    private fun withAlpha(color: Int, fraction: Float): Int {
        val alpha = (fraction.coerceIn(0f, 1f) * 255).roundToInt()
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    /** The accent the sheet offers alongside the cover's own swatches. */
    val ChampagneAccent: Int = CHAMPAGNE

    // Stack Blur v1.0 from http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
    // Java Author: Mario Klingemann <mario at quasimondo.com>
    // http://incubator.quasimondo.com
    // created Feburary 29, 2004
    // Android port : Yahel Bouaziz <yahel at kayenko.com>
    // http://www.kayenko.com
    // ported to Kotlin and adapted
    private fun fastBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        val width = (sentBitmap.width * scale).roundToInt()
        val height = (sentBitmap.height * scale).roundToInt()

        if (width <= 0 || height <= 0) return null

        val bitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yw = 0
        yi = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        var r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = -0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    fun saveBitmapAsFile(context: Context, bitmap: Bitmap, fileName: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Vox")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Failed to create new MediaStore record")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            uri
        } else {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "$fileName.png")
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                imageFile
            )
        }
    }
}
