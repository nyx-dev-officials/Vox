package com.vox.music.di

import com.vox.music.viewmodels.LyricsAppearanceViewModel
import com.vox.music.viewmodels.LyricsMenuViewModel
import com.vox.music.viewmodels.LyricsProviderViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val lyricsPresentationModule = module {
    viewModelOf(::LyricsAppearanceViewModel)
    viewModelOf(::LyricsMenuViewModel)
    viewModelOf(::LyricsProviderViewModel)
}
