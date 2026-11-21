package com.vox.music.di

import com.vox.music.ui.screens.equalizer.EQViewModel
import com.vox.music.ui.screens.equalizer.axion.AxionEqViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val equalizerPresentationModule = module {
    viewModelOf(::EQViewModel)
    viewModelOf(::AxionEqViewModel)
}
