package com.vox.music.di

import com.vox.music.library.libraryCoreModule
import com.vox.music.search.searchHistoryModule

val appModules = listOf(
    chartsPresentationModule,
    catalogPresentationModule,
    backupRestorePresentationModule,
    accountPresentationModule,
    homePresentationModule,
    androidAppDependenciesModule,
    equalizerPresentationModule,
    networkStoragePresentationModule,
    playlistDetailPresentationModule,
    integrationPresentationModule,
    artistItemsPresentationModule,
    browsePresentationModule,
    lyricsPresentationModule,
    historyPresentationModule,
    libraryCoreModule,
    searchHistoryModule,
    localLibraryPresentationModule,
    librarySectionsPresentationModule,
    playlistPresentationModule,
    localPlaylistPresentationModule,
    searchPresentationModule,
    statsPresentationModule,
    radioPresentationModule,
    updaterDataModule,
    updaterPresentationModule,
)
