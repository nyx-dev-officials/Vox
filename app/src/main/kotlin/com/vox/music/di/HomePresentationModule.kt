package com.vox.music.di

import com.vox.music.viewmodels.NewReleaseViewModel
import com.vox.music.viewmodels.HomeViewModel
import com.vox.music.history.ListenHistoryRepository
import com.vox.music.history.ListenHistorySettingsRepository
import com.vox.music.utils.SyncUtils
import com.vox.music.ui.screens.wrapped.WrappedAudioService
import com.vox.music.ui.screens.wrapped.WrappedManager
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homePresentationModule = module {
    viewModelOf(::NewReleaseViewModel)
    viewModel {
        val koin = getKoin()
        HomeViewModel(
            context = get(),
            database = get(),
            listenHistoryRepository = ValueProvider { koin.get<ListenHistoryRepository>() },
            listenHistorySettingsRepository = ValueProvider { koin.get<ListenHistorySettingsRepository>() },
            syncUtils = ValueProvider { koin.get<SyncUtils>() },
            wrappedManager = get<WrappedManager>(),
            wrappedAudioService = get<WrappedAudioService>(),
        )
    }
}
