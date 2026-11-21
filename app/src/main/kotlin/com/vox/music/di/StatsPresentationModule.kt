package com.vox.music.di

import com.vox.music.viewmodels.StatsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val statsPresentationModule = module {
    viewModelOf(::StatsViewModel)
}
