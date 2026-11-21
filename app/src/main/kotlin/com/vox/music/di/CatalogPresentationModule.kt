package com.vox.music.di

import com.vox.music.viewmodels.AlbumViewModel
import com.vox.music.viewmodels.AiDjSetViewModel
import com.vox.music.viewmodels.ArtistAlbumsViewModel
import com.vox.music.viewmodels.ArtistViewModel
import com.vox.music.viewmodels.ArtistSongsViewModel
import com.vox.music.viewmodels.SongInsightViewModel
import com.vox.music.viewmodels.TopPlaylistViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val catalogPresentationModule = module {
    viewModelOf(::AlbumViewModel)
    viewModelOf(::ArtistViewModel)
    viewModelOf(::ArtistSongsViewModel)
    viewModelOf(::ArtistAlbumsViewModel)
    viewModelOf(::TopPlaylistViewModel)
    viewModelOf(::AiDjSetViewModel)
    viewModelOf(::SongInsightViewModel)
}
