/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.component

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.vox.music.R
import com.vox.music.models.MediaMetadata
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.BasicAlertDialog
import com.vox.music.utils.ComposeToImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The signature accent, mirrored from the renderer so the sheet and the card agree. */
private val Champagne = Color(0xFFE7C8A0)
private val ObsidianInk = Color(0xFF0B0A09)
private val WarmPaper = Color(0xFFF3ECE2)

/**
 * Share-card customiser.
 *
 * Every control writes into one state block, the preview renders through the same
 * code path as the export, and the two CTAs differ only in whether the result is
 * handed to the share sheet or saved to the gallery.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShareLyricsCustomizer(
    lyricsText: String,
    songTitle: String,
    artists: String,
    coverUrl: String?,
    mediaMetadata: MediaMetadata?,
    defaultCentered: Boolean,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var template by remember { mutableStateOf(ShareCardTemplate.CLASSIC_BLUR) }
    var ratio by remember { mutableStateOf(ShareAspectRatio.AUTO) }
    var backgroundStyle by remember { mutableStateOf(LyricsBackgroundStyle.BLUR) }
    var backdropDim by remember { mutableFloatStateOf(0.42f) }
    var backdropBlur by remember { mutableFloatStateOf(0.55f) }
    var fontStyle by remember { mutableStateOf(ShareFontStyle.CHILLAX) }
    var watermark by remember { mutableStateOf(ShareWatermarkStyle.MINIMAL) }
    var centered by remember { mutableStateOf(defaultCentered) }
    var background by remember { mutableStateOf(Color(0xFF121212)) }
    var primaryInk by remember { mutableStateOf(Color.White) }
    var secondaryInk by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }
    var generating by remember { mutableStateOf(false) }

    val paletteColors = remember { mutableStateListOf<Color>() }

    LaunchedEffect(coverUrl) {
        if (coverUrl == null) return@LaunchedEffect
        // Extract off the main thread, then hand the swatches back to snapshot state.
        val colors = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .allowHardware(false)
                    .build()
                val bitmap = ImageLoader(context).execute(request).image?.toBitmap()
                    ?: return@runCatching emptyList<Color>()
                Palette.from(bitmap).generate().swatches
                    .sortedByDescending { it.population }
                    .map { Color(it.rgb) }
                    .filter { color ->
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                        hsv[1] > 0.2f
                    }
                    .take(4)
            }.getOrDefault(emptyList())
        }
        paletteColors.clear()
        paletteColors.addAll(colors)
    }

    fun applyTemplate(next: ShareCardTemplate) {
        template = next
        when (next) {
            ShareCardTemplate.CLASSIC_BLUR -> {
                backgroundStyle = LyricsBackgroundStyle.BLUR
                background = Color(0xFF121212)
                primaryInk = Color.White
                secondaryInk = Color.White.copy(alpha = 0.7f)
            }
            ShareCardTemplate.EDITORIAL_POSTER -> {
                backgroundStyle = LyricsBackgroundStyle.SOLID
                background = WarmPaper
                primaryInk = ObsidianInk
                secondaryInk = ObsidianInk.copy(alpha = 0.55f)
            }
            ShareCardTemplate.FULL_BLEED -> {
                backgroundStyle = LyricsBackgroundStyle.FULL_BLEED
                background = ObsidianInk
                primaryInk = Color.White
                secondaryInk = Color.White.copy(alpha = 0.72f)
            }
            ShareCardTemplate.MINIMAL_OBSIDIAN -> {
                backgroundStyle = LyricsBackgroundStyle.SOLID
                background = ObsidianInk
                primaryInk = Color.White
                secondaryInk = Color.White.copy(alpha = 0.62f)
            }
            ShareCardTemplate.VINYL_FRAME -> {
                backgroundStyle = LyricsBackgroundStyle.BLUR
                background = ObsidianInk
                primaryInk = Color.White
                secondaryInk = Color.White.copy(alpha = 0.62f)
            }
        }
    }

    suspend fun renderCard(): android.graphics.Bitmap = ComposeToImage.createLyricsImage(
        context = context,
        coverArtUrl = coverUrl,
        songTitle = songTitle,
        artistName = artists,
        lyrics = lyricsText,
        backgroundColor = background.toArgb(),
        backgroundStyle = backgroundStyle,
        textColor = primaryInk.toArgb(),
        secondaryTextColor = secondaryInk.toArgb(),
        lyricsAlignment = if (centered) {
            android.text.Layout.Alignment.ALIGN_CENTER
        } else {
            android.text.Layout.Alignment.ALIGN_NORMAL
        },
        template = template,
        aspectRatio = ratio,
        fontStyle = fontStyle,
        watermark = watermark,
        backdropDim = backdropDim,
        backdropBlur = backdropBlur,
    )

    fun export(share: Boolean) {
        if (generating) return
        scope.launch {
            generating = true
            try {
                val bitmap = renderCard()
                val uri = ComposeToImage.saveBitmapAsFile(
                    context,
                    bitmap,
                    "lyrics_${System.currentTimeMillis()}",
                )
                if (share) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, context.getString(R.string.share_lyrics))
                    )
                    onDismiss()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.share_saved),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.failed_to_create_image, e.message),
                    Toast.LENGTH_SHORT,
                ).show()
            } finally {
                generating = false
            }
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(28.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = AppTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth().padding(10.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.share_lyrics),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black),
                ) {
                    LyricsShareCardPreview(
                        lyricText = lyricsText,
                        mediaMetadata = mediaMetadata,
                        template = template,
                        aspectRatio = ratio,
                        backgroundStyle = backgroundStyle,
                        backgroundColor = background,
                        textColor = primaryInk,
                        secondaryTextColor = secondaryInk,
                        fontStyle = fontStyle,
                        watermark = watermark,
                        textAlign = if (centered) TextAlign.Center else TextAlign.Left,
                        backdropDim = backdropDim,
                        backdropBlur = backdropBlur,
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.share_card_layout))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShareCardTemplate.entries.forEach { candidate ->
                        ShareChip(
                            label = stringResource(candidate.labelKey),
                            selected = template == candidate,
                            onClick = { applyTemplate(candidate) },
                        )
                    }
                }

                SectionLabel(stringResource(R.string.share_aspect_ratio))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShareAspectRatio.entries.forEach { candidate ->
                        ShareChip(
                            label = stringResource(
                                when (candidate) {
                                    ShareAspectRatio.SQUARE -> R.string.share_ratio_square
                                    ShareAspectRatio.STORY -> R.string.share_ratio_story
                                    ShareAspectRatio.AUTO -> R.string.share_ratio_auto
                                }
                            ),
                            selected = ratio == candidate,
                            onClick = { ratio = candidate },
                        )
                    }
                }

                SectionLabel(stringResource(R.string.share_background))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LyricsBackgroundStyle.entries.forEach { candidate ->
                        ShareChip(
                            label = stringResource(
                                when (candidate) {
                                    LyricsBackgroundStyle.SOLID -> R.string.share_bg_solid
                                    LyricsBackgroundStyle.BLUR -> R.string.share_bg_blur
                                    LyricsBackgroundStyle.GRADIENT -> R.string.share_bg_gradient
                                    LyricsBackgroundStyle.MESH -> R.string.share_bg_mesh
                                    LyricsBackgroundStyle.FULL_BLEED -> R.string.share_bg_full_bleed
                                }
                            ),
                            selected = backgroundStyle == candidate,
                            onClick = { backgroundStyle = candidate },
                        )
                    }
                }

                val imageBacked = backgroundStyle != LyricsBackgroundStyle.SOLID
                if (imageBacked) {
                    SliderRow(
                        label = stringResource(R.string.share_backdrop_dim),
                        value = backdropDim,
                        onChange = { backdropDim = it },
                    )
                    SliderRow(
                        label = stringResource(R.string.share_backdrop_blur),
                        value = backdropBlur,
                        onChange = { backdropBlur = it },
                    )
                }

                SectionLabel(stringResource(R.string.share_typography))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShareFontStyle.entries.forEach { candidate ->
                        ShareChip(
                            label = stringResource(
                                when (candidate) {
                                    ShareFontStyle.CHILLAX -> R.string.share_font_chillax
                                    ShareFontStyle.JAKARTA -> R.string.share_font_jakarta
                                    ShareFontStyle.SERIF -> R.string.share_font_serif
                                    ShareFontStyle.MONO -> R.string.share_font_mono
                                }
                            ),
                            selected = fontStyle == candidate,
                            onClick = { fontStyle = candidate },
                        )
                    }
                }

                SectionLabel(stringResource(R.string.share_alignment))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareChip(
                        label = stringResource(R.string.share_align_left),
                        selected = !centered,
                        onClick = { centered = false },
                    )
                    ShareChip(
                        label = stringResource(R.string.share_align_center),
                        selected = centered,
                        onClick = { centered = true },
                    )
                }

                SectionLabel(stringResource(R.string.share_watermark))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShareWatermarkStyle.entries.forEach { candidate ->
                        ShareChip(
                            label = stringResource(
                                when (candidate) {
                                    ShareWatermarkStyle.MINIMAL -> R.string.share_watermark_minimal
                                    ShareWatermarkStyle.PILL -> R.string.share_watermark_pill
                                    ShareWatermarkStyle.HIDDEN -> R.string.share_watermark_hidden
                                }
                            ),
                            selected = watermark == candidate,
                            onClick = { watermark = candidate },
                        )
                    }
                }

                SectionLabel(stringResource(R.string.background_color))
                ColorSwatchRow(
                    colors = paletteColors + listOf(
                        Color(0xFF121212), ObsidianInk, WarmPaper, Color.White, Champagne,
                    ),
                    selected = background,
                    onSelect = { background = it },
                )

                SectionLabel(stringResource(R.string.text_color))
                ColorSwatchRow(
                    colors = paletteColors + listOf(
                        Color.White, ObsidianInk, Champagne, WarmPaper,
                    ),
                    selected = primaryInk,
                    onSelect = { primaryInk = it },
                )

                SectionLabel(stringResource(R.string.secondary_text_color))
                ColorSwatchRow(
                    colors = paletteColors.map { it.copy(alpha = 0.7f) } + listOf(
                        Color.White.copy(alpha = 0.7f),
                        ObsidianInk.copy(alpha = 0.7f),
                        Champagne.copy(alpha = 0.8f),
                    ),
                    selected = secondaryInk,
                    onSelect = { secondaryInk = it },
                )

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Primary action: the signature champagne pill, ink text.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Champagne)
                            .clickable { export(share = true) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.share),
                            color = ObsidianInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .border(1.dp, Champagne.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                            .clickable { export(share = false) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.share_save_image),
                            color = AppTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    if (generating) {
        BasicAlertDialog(onDismissRequest = { }) {
            androidx.compose.material3.Card(shape = RoundedCornerShape(20.dp)) {
                Box(modifier = Modifier.padding(28.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image),
                        color = AppTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun ShareChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, fontSize = 12.sp) },
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            modifier = Modifier.width(104.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatchRow(
    colors: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        colors.distinct().take(9).forEach { color ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(color)
                    .border(
                        width = if (color == selected) 2.dp else 1.dp,
                        color = if (color == selected) Champagne
                        else AppTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(9.dp),
                    )
                    .clickable { onSelect(color) },
            )
        }
    }
}
