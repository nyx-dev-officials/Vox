package com.vox.music.di

import com.vox.music.viewmodels.UpdateViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val updaterPresentationModule = module {
    viewModelOf(::UpdateViewModel)
}
