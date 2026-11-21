package com.vox.music.di

import com.vox.music.viewmodels.AppleMusicImportViewModel
import com.vox.music.viewmodels.AutoPlaylistViewModel
import com.vox.music.viewmodels.PlaylistsViewModel
import com.vox.music.viewmodels.SpotifyImportViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playlistPresentationModule = module {
    viewModelOf(::PlaylistsViewModel)
    viewModelOf(::AutoPlaylistViewModel)
    viewModelOf(::SpotifyImportViewModel)
    viewModelOf(::AppleMusicImportViewModel)
}
