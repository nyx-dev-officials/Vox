package com.vox.music.di

import com.vox.music.viewmodels.ArtistItemsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val artistItemsPresentationModule = module {
    viewModelOf(::ArtistItemsViewModel)
}
