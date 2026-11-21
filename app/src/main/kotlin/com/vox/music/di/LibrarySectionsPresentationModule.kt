package com.vox.music.di

import com.vox.music.viewmodels.LibraryAlbumsViewModel
import com.vox.music.viewmodels.LibraryArtistsViewModel
import com.vox.music.viewmodels.LibraryMixViewModel
import com.vox.music.viewmodels.LibraryPlaylistsViewModel
import com.vox.music.viewmodels.LibrarySongsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val librarySectionsPresentationModule = module {
    viewModelOf(::LibrarySongsViewModel)
    viewModelOf(::LibraryArtistsViewModel)
    viewModelOf(::LibraryAlbumsViewModel)
    viewModelOf(::LibraryPlaylistsViewModel)
    viewModelOf(::LibraryMixViewModel)
}
