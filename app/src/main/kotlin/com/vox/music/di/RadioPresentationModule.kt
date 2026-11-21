package com.vox.music.di

import com.vox.music.viewmodels.RadioStationViewModel
import com.vox.music.viewmodels.RadioViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val radioPresentationModule = module {
    viewModelOf(::RadioStationViewModel)
    viewModelOf(::RadioViewModel)
}
