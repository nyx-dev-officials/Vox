package com.vox.music.di

import com.vox.music.viewmodels.HistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val historyPresentationModule = module {
    viewModelOf(::HistoryViewModel)
}
