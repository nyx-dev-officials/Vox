package com.vox.music.di

import com.vox.music.viewmodels.LocalPlaylistViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val localPlaylistPresentationModule = module {
    viewModelOf(::LocalPlaylistViewModel)
}
