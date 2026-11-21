package com.vox.music.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.vox.music.constants.MaxSongCacheSizeKey
import com.vox.music.db.DatabaseDao
import com.vox.music.db.InternalDatabase
import com.vox.music.db.MusicDatabase
import com.vox.music.eq.EqualizerService
import com.vox.music.eq.data.EQProfileRepository
import com.vox.music.history.HistoryLocalDataSource
import com.vox.music.history.DataStoreListenHistorySettingsRepository
import com.vox.music.history.ListenHistoryRepository
import com.vox.music.history.ListenHistorySettingsRepository
import com.vox.music.history.RoomHistoryLocalDataSource
import com.vox.music.library.LocalPlaylistSongDataSource
import com.vox.music.library.MusicLibraryRemoteDataSource
import com.vox.music.library.MusicPlaylistRemoteDataSource
import com.music.innertube.InnertubeRepository
import com.music.innertube.YouTube
import com.music.innertube.library.InnertubeMusicLibraryDataSource
import com.music.innertube.library.InnertubeMusicPlaylistDataSource
import com.vox.music.library.DataStorePlaylistDisplayPreferencesRepository
import com.vox.music.library.DataStorePendingPlaylistDeletionRepository
import com.vox.music.library.PendingPlaylistDeletionRepository
import com.vox.music.library.PlaylistDisplayPreferencesRepository
import com.vox.music.library.PlaylistRepository
import com.vox.music.library.RoomPlaylistRepository
import com.vox.music.search.RoomSearchHistoryRepository
import com.vox.music.search.SearchHistoryRepository
import com.vox.music.search.DataStoreSearchHistorySettingsRepository
import com.vox.music.search.SearchHistorySettingsRepository
import com.vox.music.jam.JamManager
import com.vox.music.listentogether.ListenTogetherClient
import com.vox.music.listentogether.ListenTogetherManager
import com.vox.music.listentogether.ListenTogetherPlayerConnectionBridge
import com.vox.music.lyrics.LyricsHelper
import com.vox.music.lyrics.LyricsProviderRepository
import com.vox.music.playback.DownloadUtil
import com.vox.music.playback.MediaLibrarySessionCallback
import com.vox.music.playback.PlaybackServiceConnector
import com.vox.music.utils.NetworkConnectivityObserver
import com.vox.music.utils.SyncUtils
import com.vox.music.utils.dataStore
import com.vox.music.utils.get
import com.vox.music.widget.VoxWidgetManager
import com.vox.storage.data.EncryptedNetworkServerStore
import com.vox.storage.data.NetworkMusicRepository
import com.vox.storage.domain.NetworkServerStore
import com.vox.music.ui.screens.wrapped.WrappedAudioService
import com.vox.music.ui.screens.wrapped.WrappedManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val androidAppDependenciesModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<MusicDatabase> { InternalDatabase.newInstance(androidContext()) }
    single<InternalDatabase> { get<MusicDatabase>().delegate }
    single<DatabaseDao> { get<InternalDatabase>().dao }
    // Must be the signed-in global client; a new instance has no session.
    single<InnertubeRepository> { YouTube }
    single<MusicLibraryRemoteDataSource> { InnertubeMusicLibraryDataSource(get()) }
    single<MusicPlaylistRemoteDataSource> { InnertubeMusicPlaylistDataSource(get()) }
    single<DatabaseProvider> { StandaloneDatabaseProvider(androidContext()) }
    single<SimpleCache>(named("playerCache")) {
        val context: Context = androidContext()
        val cacheSize = context.dataStore[MaxSongCacheSizeKey] ?: 1024
        SimpleCache(
            context.filesDir.resolve("exoplayer"),
            if (cacheSize == -1) NoOpCacheEvictor() else LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L),
            get(),
        )
    }
    single<SimpleCache>(named("downloadCache")) {
        val context: Context = androidContext()
        SimpleCache(context.filesDir.resolve("download"), NoOpCacheEvictor(), get())
    }
    single<NetworkServerStore> { EncryptedNetworkServerStore(androidContext()) }
    single { NetworkMusicRepository(get()) }
    single { NetworkConnectivityObserver(androidContext()) }
    single { RoomHistoryLocalDataSource(get()) }
    single<HistoryLocalDataSource> { get<RoomHistoryLocalDataSource>() }
    single<ListenHistoryRepository> { get<RoomHistoryLocalDataSource>() }
    single<ListenHistorySettingsRepository> {
        DataStoreListenHistorySettingsRepository(androidContext())
    }
    single<RoomPlaylistRepository> { RoomPlaylistRepository(get()) }
    single<PlaylistRepository> { get<RoomPlaylistRepository>() }
    single<LocalPlaylistSongDataSource> { get<RoomPlaylistRepository>() }
    single<PlaylistDisplayPreferencesRepository> {
        DataStorePlaylistDisplayPreferencesRepository(androidContext())
    }
    single<PendingPlaylistDeletionRepository> {
        DataStorePendingPlaylistDeletionRepository(androidContext())
    }
    single<SearchHistoryRepository> { RoomSearchHistoryRepository(get()) }
    single<SearchHistorySettingsRepository> {
        DataStoreSearchHistorySettingsRepository(androidContext())
    }
    single { ListenTogetherPlayerConnectionBridge() }
    single { ListenTogetherClient(androidContext()) }
    single { ListenTogetherManager(get(), androidContext(), get()) }
    single { JamManager(androidContext(), get(), ValueProvider { getKoin().get() }) }
    single { LyricsProviderRepository(androidContext()) }
    single { LyricsHelper(androidContext(), get(), get()) }
    single { SyncUtils(androidContext(), get(), get(), get()) }
    single { EqualizerService() }
    single { EQProfileRepository(androidContext()) }
    single { WrappedManager(get(), androidContext()) }
    single { WrappedAudioService(androidContext()) }
    single {
        DownloadUtil(
            context = androidContext(),
            database = get(),
            databaseProvider = get(),
            downloadCache = get(named("downloadCache")),
            playerCache = get(named("playerCache")),
            lyricsHelper = get(),
        )
    }
    single { MediaLibrarySessionCallback(androidContext(), get(), get()) }
    single { VoxWidgetManager(androidContext(), get()) }
    factory { PlaybackServiceConnector(ValueProvider { getKoin().get() }, get()) }
}
