/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.vox.music.models.MediaMetadata
import com.vox.music.utils.ComposeToImage
import kotlinx.coroutines.delay

/** Background treatment behind the quote. */
enum class LyricsBackgroundStyle {
    SOLID,
    BLUR,
    GRADIENT,
    /** Palette blobs extracted from the cover. */
    MESH,
    /** The cover itself, edge to edge, under the scrim. */
    FULL_BLEED,
}

/** The card compositions offered in the share sheet. */
enum class ShareCardTemplate {
    /** Sleeve-left header, centred quote — the classic look. */
    CLASSIC_BLUR,
    /** No thumbnail: artist eyebrow over a display-size title, warm paper. */
    EDITORIAL_POSTER,
    /** Artwork edge to edge, type over the scrim. */
    FULL_BLEED,
    /** Quiet obsidian with small type and generous air. */
    MINIMAL_OBSIDIAN,
    /** Centred sleeve with concentric grooves behind it. */
    VINYL_FRAME,
}

enum class ShareAspectRatio {
    SQUARE,
    STORY,
    /** Grows with the quote instead of forcing a fixed frame. */
    AUTO,
}

enum class ShareFontStyle {
    CHILLAX,
    JAKARTA,
    SERIF,
    MONO,
}

enum class ShareWatermarkStyle {
    MINIMAL,
    PILL,
    HIDDEN,
}

/**
 * Live preview of the card the user is about to export.
 *
 * It renders through [ComposeToImage] rather than re-implementing the layout in
 * Compose, so the preview cannot drift from the PNG. Dragging a slider re-renders
 * a small bitmap and supersedes the previous one.
 */
@Composable
fun LyricsShareCardPreview(
    lyricText: String,
    mediaMetadata: MediaMetadata?,
    template: ShareCardTemplate,
    aspectRatio: ShareAspectRatio,
    backgroundStyle: LyricsBackgroundStyle,
    backgroundColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    fontStyle: ShareFontStyle,
    watermark: ShareWatermarkStyle,
    textAlign: TextAlign,
    backdropDim: Float,
    backdropBlur: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val thumbnailUrl = mediaMetadata?.thumbnailUrl
    val songTitle = mediaMetadata?.title ?: ""
    val artistName = mediaMetadata?.artists?.joinToString { it.name } ?: ""

    var artwork by remember { mutableStateOf<Bitmap?>(null) }
    var rendered by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(thumbnailUrl) {
        artwork = null
        if (!thumbnailUrl.isNullOrBlank()) {
            artwork = runCatching {
                val request = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(512)
                    .allowHardware(false)
                    .build()
                ImageLoader(context).execute(request).image?.toBitmap()
            }.getOrNull()
        }
    }

    LaunchedEffect(
        lyricText, songTitle, artistName, artwork, template, aspectRatio, backgroundStyle,
        backgroundColor, textColor, secondaryTextColor, fontStyle, watermark, textAlign,
        backdropDim, backdropBlur,
    ) {
        // Coalesce slider drags into one render instead of queueing every frame.
        delay(90)
        rendered = runCatching {
            ComposeToImage.createLyricsImage(
                context = context,
                coverArtUrl = thumbnailUrl,
                songTitle = songTitle,
                artistName = artistName,
                lyrics = lyricText,
                backgroundColor = backgroundColor.toArgb(),
                backgroundStyle = backgroundStyle,
                textColor = textColor.toArgb(),
                secondaryTextColor = secondaryTextColor.toArgb(),
                lyricsAlignment = if (textAlign == TextAlign.Center) {
                    android.text.Layout.Alignment.ALIGN_CENTER
                } else {
                    android.text.Layout.Alignment.ALIGN_NORMAL
                },
                template = template,
                aspectRatio = aspectRatio,
                fontStyle = fontStyle,
                watermark = watermark,
                backdropDim = backdropDim,
                backdropBlur = backdropBlur,
                artwork = artwork,
                renderWidth = 640,
            )
        }.getOrNull()
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        rendered?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Kept for callers that only need the label of a template. */
val ShareCardTemplate.labelKey: Int
    get() = when (this) {
        ShareCardTemplate.CLASSIC_BLUR -> com.vox.music.R.string.share_template_classic
        ShareCardTemplate.EDITORIAL_POSTER -> com.vox.music.R.string.share_template_editorial
        ShareCardTemplate.FULL_BLEED -> com.vox.music.R.string.share_template_full_bleed
        ShareCardTemplate.MINIMAL_OBSIDIAN -> com.vox.music.R.string.share_template_obsidian
        ShareCardTemplate.VINYL_FRAME -> com.vox.music.R.string.share_template_vinyl
    }
