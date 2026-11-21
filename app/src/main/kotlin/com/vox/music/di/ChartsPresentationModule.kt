package com.vox.music.di

import com.vox.music.viewmodels.ChartsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chartsPresentationModule = module {
    viewModelOf(::ChartsViewModel)
}
