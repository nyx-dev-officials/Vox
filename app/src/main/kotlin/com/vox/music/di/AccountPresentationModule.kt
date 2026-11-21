package com.vox.music.di

import com.vox.music.viewmodels.AccountViewModel
import com.vox.music.viewmodels.AccountSettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountPresentationModule = module {
    viewModelOf(::AccountViewModel)
    viewModelOf(::AccountSettingsViewModel)
}
