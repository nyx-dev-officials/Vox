package com.vox.music.di

import com.vox.music.core.updater.ApkVerifier
import com.vox.music.core.updater.GitHubReleaseSource
import com.vox.music.core.updater.UpdateInstaller
import com.vox.music.core.updater.UpdateRepository
import com.vox.music.core.updater.UpdateStore
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val updaterDataModule = module {
    factoryOf(::ApkVerifier)
    singleOf(::GitHubReleaseSource)
    singleOf(::UpdateStore)
    singleOf(::UpdateInstaller)
    singleOf(::UpdateRepository)
}
