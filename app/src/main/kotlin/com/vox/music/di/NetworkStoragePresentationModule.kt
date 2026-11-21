package com.vox.music.di

import com.vox.music.viewmodels.NetworkBrowserViewModel
import com.vox.music.viewmodels.NetworkStorageViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val networkStoragePresentationModule = module {
    viewModelOf(::NetworkStorageViewModel)
    viewModelOf(::NetworkBrowserViewModel)
}
