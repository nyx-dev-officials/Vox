package com.vox.music.di

import com.vox.music.ui.screens.library.LocalMusicViewModel
import com.vox.music.ui.screens.library.apple.AppleLibraryViewModel
import com.vox.music.ui.screens.library.apple.AppleLocalSongsViewModel
import com.vox.music.viewmodels.LocalHomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val localLibraryPresentationModule = module {
    viewModelOf(::LocalMusicViewModel)
    viewModelOf(::LocalHomeViewModel)
    viewModelOf(::AppleLibraryViewModel)
    viewModelOf(::AppleLocalSongsViewModel)
}
