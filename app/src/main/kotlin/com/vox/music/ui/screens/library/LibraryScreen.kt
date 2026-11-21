/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.vox.music.constants.ChipSortTypeKey
import com.vox.music.constants.LibraryFilter
import com.vox.music.ui.screens.library.apple.AppleAlbumsPage
import com.vox.music.ui.screens.library.apple.AppleArtistsPage
import com.vox.music.ui.screens.library.apple.AppleLibraryDestination
import com.vox.music.ui.screens.library.apple.AppleLibraryScreen
import com.vox.music.ui.screens.library.apple.ApplePlaylistsPage
import com.vox.music.ui.screens.library.apple.AppleSongsPage
import com.vox.music.ui.screens.library.apple.AppleLocalSongsPage
import com.vox.music.utils.rememberEnumPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val back = { filterType = LibraryFilter.LIBRARY }
    Box(modifier = Modifier.fillMaxSize()) {
        // Apple Music's Library and its lists: new screens on the plain system background.
        when (filterType) {
            LibraryFilter.LIBRARY -> AppleLibraryScreen(
                navController = navController,
                onOpen = { destination ->
                    when (destination) {
                        AppleLibraryDestination.Playlists -> filterType = LibraryFilter.PLAYLISTS
                        AppleLibraryDestination.Artists -> filterType = LibraryFilter.ARTISTS
                        AppleLibraryDestination.Albums -> filterType = LibraryFilter.ALBUMS
                        AppleLibraryDestination.Songs -> filterType = LibraryFilter.SONGS
                        AppleLibraryDestination.Local -> filterType = LibraryFilter.LOCAL
                        AppleLibraryDestination.Downloaded -> navController.navigate("auto_playlist/downloaded")
                        AppleLibraryDestination.Liked -> navController.navigate("auto_playlist/liked")
                        AppleLibraryDestination.Podcasts -> navController.navigate("auto_playlist/podcasts")
                        AppleLibraryDestination.Cached -> navController.navigate("cache_playlist/cached")
                        AppleLibraryDestination.Top -> navController.navigate("top_playlist/50")
                        AppleLibraryDestination.Uploaded -> navController.navigate("auto_playlist/uploaded")
                        AppleLibraryDestination.History -> navController.navigate("history")
                    }
                },
            )
            LibraryFilter.PLAYLISTS -> ApplePlaylistsPage(navController, back)
            LibraryFilter.SONGS -> AppleSongsPage(navController, back)
            LibraryFilter.ALBUMS -> AppleAlbumsPage(navController, back)
            LibraryFilter.ARTISTS -> AppleArtistsPage(navController, back)
            LibraryFilter.LOCAL -> AppleLocalSongsPage(navController, back)
        }
    }
}
