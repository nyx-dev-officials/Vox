/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.screens

import com.vox.music.ui.utils.plusStart
import com.vox.music.ui.utils.bleedStart
import com.vox.music.ui.utils.bounceClick
import com.vox.music.ui.component.sf
import com.vox.music.ui.component.SymbolIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.SFMusicNote
import com.composables.sfsymbols.monochrome.SFXmark
import com.vox.music.R
import com.vox.music.models.MediaMetadata
import com.vox.music.ui.component.LocalItemPrefs
import com.vox.music.ui.component.NavigationTitle
import com.vox.music.ui.component.YouTubeGridItem
import com.vox.music.ui.component.flipAndMultiply
import com.vox.music.ui.component.shapes.ContinuousRoundedRectangle
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.Icon
import com.vox.music.ui.ios.compat.LocalContentColor
import com.vox.music.ui.ios.compat.Text
import com.vox.music.ui.ios.foundation.IosTheme
import com.vox.music.ui.screens.library.apple.Cover
import com.vox.music.ui.theme.AppleTokens
import com.vox.music.ui.theme.LocalPageMetrics
import com.vox.music.ui.theme.VoxType
import com.vox.music.ui.utils.rememberGridSpacing
import com.vox.music.ui.utils.sharedArtworkSource
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem

/** Builds the home feed with its header and music recommendation rails. */
internal fun LazyListScope.figmaHomeContent(
    deps: HomeSectionDeps,
    mediaMetadata: State<MediaMetadata?>,
    isPlaying: State<Boolean>,
    topPicks: List<YTItem>,
    topPicksTitle: String,
    newReleaseAlbums: List<AlbumItem>,
    newReleaseTitle: String,
    trending: List<YTItem>,
    trendingTitle: String,
) {
    figmaRail(
        deps = deps,
        mediaMetadata = mediaMetadata,
        isPlaying = isPlaying,
        key = "top_picks",
        title = topPicksTitle,
        route = "charts_screen",
        items = topPicks,
        hero = true,
    )
    figmaRail(
        deps = deps,
        mediaMetadata = mediaMetadata,
        isPlaying = isPlaying,
        key = "new_albums",
        title = newReleaseTitle,
        route = "new_release",
        items = newReleaseAlbums,
    )
    figmaRail(
        deps = deps,
        mediaMetadata = mediaMetadata,
        isPlaying = isPlaying,
        key = "trending",
        title = trendingTitle,
        route = "charts_screen",
        items = trending,
    )
}

/** Adds a titled horizontal rail; Top Picks can use the larger hero layout. */
internal fun LazyListScope.figmaRail(
    deps: HomeSectionDeps,
    mediaMetadata: State<MediaMetadata?>,
    isPlaying: State<Boolean>,
    key: String,
    title: String,
    route: String,
    items: List<YTItem>,
    hero: Boolean = false,
) {
    if (items.isEmpty()) return
    if (!hero && items.isSongShelf()) {
        item(key = "${key}_title", contentType = "section_title") {
            NavigationTitle(
                title = title,
                modifier = Modifier.animateItem(),
                onClick = { deps.navController.navigate(route) },
            )
        }
        shelfList(key = "${key}_list", deps = deps, entries = items.distinctBy { it.id }.map { it.toShelfEntry(deps) })
        return
    }
    if (!hero) {
        item(key = "${key}_title", contentType = "section_title") {
            NavigationTitle(
                title = title,
                modifier = Modifier.animateItem(),
                onClick = { deps.navController.navigate(route) },
            )
        }
    }
    item(key = "${key}_list", contentType = if (hero) "hero_rail" else "rail") {
        // Compensate for tile padding so covers keep their intended size, gap, and edge inset.
        val gs = rememberGridSpacing()
        val prefs = LocalItemPrefs.current
        val railState = rememberLazyListState()
        Column(Modifier.animateItem()) {
        if (hero) {
            // Keep the hero title close to the cards without regular section spacing.
            CompositionLocalProvider(
                LocalPageMetrics provides LocalPageMetrics.current.copy(gutter = 16.dp, headerTop = 0.dp, headerBottom = 4.dp),
            ) {
                NavigationTitle(
                    title = title,
                    onClick = { deps.navController.navigate(route) },
                )
            }
        }
        // Add the tile's horizontal padding to keep the cover itself 154.dp wide.
        CompositionLocalProvider(LocalItemPrefs provides prefs.copy(gridCardHeightOverrideDp = (154.dp + gs).value.toInt())) {
            LazyRow(
                state = railState,
                // One gutter for the whole page: a rail starts on the same 20dp
                // line as the headers above it instead of its own 16dp inset.
                contentPadding = PaddingValues(
                    horizontal = if (hero) 20.dp else (20.dp - gs / 2).coerceAtLeast(0.dp),
                ).plusStart(deps.sideInset),
                horizontalArrangement = Arrangement.spacedBy(if (hero) 12.dp else (14.dp - gs).coerceAtLeast(0.dp)),
                modifier = Modifier.fillMaxWidth().bleedStart(deps.sideInset),
            ) {
                items(items, key = { it.id }, contentType = { if (hero) "figma_hero" else "figma_card" }) { item ->
                    HomePopMenu(
                        pop = HomePop.Yt(item),
                        deps = deps,
                        thumbnailUrl = item.thumbnail,
                        title = item.title,
                        subtitle = item.shelfSubtitle(),
                        onClick = { deps.openYtItem(item) },
                        modifier = Modifier.animateItem(),
                    ) {
                        if (hero) {
                            FigmaTopPickCard(item = item)
                        } else {
                            YouTubeGridItem(
                                item = item,
                                isActive = item.id == mediaMetadata.value?.id,
                                isPlaying = isPlaying.value && item.id == mediaMetadata.value?.id,
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * The Home masthead.
 *
 * One display-size line that names the moment ("Good evening") over a small
 * champagne eyebrow carrying the brand and the day. It replaces the generic
 * "here is a feed" opening with an editorial one, and it is the only place on
 * Home that uses the display face at masthead size.
 */
@Composable
internal fun HomeMasthead(modifier: Modifier = Modifier) {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> R.string.home_masthead_morning
        in 12..17 -> R.string.home_masthead_afternoon
        else -> R.string.home_masthead_evening
    }
    val weekday = remember {
        java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
            .format(java.util.Date())
            .uppercase(java.util.Locale.getDefault())
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = LocalPageMetrics.current.gutter,
                end = LocalPageMetrics.current.gutter,
                top = 4.dp,
                bottom = 2.dp,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(AppleTokens.AccentGold),
            )
            Text(
                text = "VOX · $weekday",
                style = VoxType.Eyebrow,
                color = AppleTokens.AccentGold.copy(alpha = 0.8f),
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(greeting),
            style = VoxType.Masthead,
            color = AppTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * Apple Music Home's "Transfer music" card: 16dp from the edges, 20dp continuous
 * corners on secondarySystemBackground, red glyph + 15sp body, a 24dp grey close
 * disc top-right and a full-width tinted "Get Started" capsule underneath.
 */
@Composable
internal fun HomeSignInCard(onGetStarted: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = IosTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ContinuousRoundedRectangle(20.dp))
            .background(colors.secondarySystemBackground)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SymbolIcon(symbol = sf("SFMusicNote"), contentDescription = null, size = 44.dp, modifier = Modifier.padding(top = 8.dp).padding(6.dp), tint = colors.systemRed)
            Text(
                text = stringResource(R.string.home_sign_in_card_body),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.23).sp,
                fontWeight = FontWeight.Medium,
                color = colors.label,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 8.dp, end = 8.dp),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.tertiarySystemFill)
                    .clickable(
                        onClick = onDismiss,
                        onClickLabel = stringResource(R.string.dismiss),
                    ),
            ) {
                SymbolIcon(symbol = sf("SFXmark"), contentDescription = stringResource(R.string.dismiss), size = 12.dp, tint = colors.secondaryLabel)
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(end = 4.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(ContinuousRoundedRectangle(12.dp))
                .background(colors.accent.copy(alpha = 0.12f))
                .bounceClick(onClick = onGetStarted),
        ) {
            Text(
                text = stringResource(R.string.get_started),
                fontSize = 15.sp,
                letterSpacing = (-0.23).sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
            )
        }
    }
}

/**
 * Top Picks card with artwork reflected in a frosted footer and centered overlay text.
 */
@Composable
internal fun FigmaTopPickCard(
    item: YTItem,
    modifier: Modifier = Modifier,
) {
    FigmaTopPickCard(
        thumbnail = item.thumbnail,
        title = item.title,
        label = when (item) {
            is SongItem -> item.artists.firstOrNull()?.name
            is AlbumItem -> item.artists?.firstOrNull()?.name
            is PlaylistItem -> item.author?.name
            is ArtistItem -> null
        },
        detail = when (item) {
            is AlbumItem -> item.year?.toString()
            is SongItem -> item.album?.name
            else -> null
        },
        // A song opens its album, so it claims the album's id for the cover flight.
        artworkKey = if (item is SongItem) item.album?.id else item.id,
        modifier = modifier,
    )
}

@Composable
internal fun FigmaTopPickCard(
    thumbnail: String?,
    title: String,
    label: String?,
    detail: String?,
    artworkKey: String?,
    modifier: Modifier = Modifier,
) {
    val shape = ContinuousRoundedRectangle(20.dp)
    val context = LocalContext.current
    val request = remember(thumbnail) {
        ImageRequest.Builder(context).data(thumbnail).crossfade(false).build()
    }
    // Vox featured card: the cover runs edge to edge inside its corner and the
    // type lives outside it, on the page. Type over artwork was the old card's
    // weak point — it needed a frosted footer and still washed out on a bright
    // cover. A scrim now only settles the artwork into the page.
    Column(modifier = modifier.width(242.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .height(300.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = Color.Black.copy(alpha = 0.22f),
                )
                .clip(shape),
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .sharedArtworkSource(artworkKey, thumbnail)
                        .matchParentSize(),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .matchParentSize()
                        .background(AppTheme.colorScheme.surfaceContainerHigh),
                ) {
                    SymbolIcon(
                        symbol = sf("SFMusicNote"),
                        contentDescription = null,
                        size = 56.dp,
                        tint = AppTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Bottom scrim: keeps the card from ending on a hard bright edge
            // against the obsidian page, and gives the corner a place to sit.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = VoxType.TileTitle.copy(fontSize = 16.sp, lineHeight = 21.sp),
                color = LocalContentColor.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(label, detail).joinToString("  ·  ")
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    style = AppTheme.typography.bodySmall.copy(fontFamily = VoxType.Body),
                    fontSize = 12.5f.sp,
                    lineHeight = 17.sp,
                    color = AppleTokens.Metadata.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
