package com.vox.music.di

import com.vox.music.viewmodels.YouTubeBrowseViewModel
import com.vox.music.viewmodels.BrowseViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val browsePresentationModule = module {
    viewModelOf(::YouTubeBrowseViewModel)
    viewModelOf(::BrowseViewModel)
}
