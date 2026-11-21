package com.vox.music.di

import com.vox.music.viewmodels.JamViewModel
import com.vox.music.viewmodels.ListenTogetherViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val integrationPresentationModule = module {
    viewModelOf(::JamViewModel)
    viewModelOf(::ListenTogetherViewModel)
}
