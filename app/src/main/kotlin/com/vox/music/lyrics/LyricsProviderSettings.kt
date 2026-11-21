package com.vox.music.lyrics

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.vox.music.constants.EnableBetterLyricsKey
import com.vox.music.constants.EnableKugouKey
import com.vox.music.constants.EnableLrcLibKey
import com.vox.music.constants.EnableMusixmatchKey
import com.vox.music.constants.EnablePaxsenixKey
import com.vox.music.constants.EnableSimpMusicKey
import com.vox.music.constants.EnableUnisonKey
import com.vox.music.constants.EnableYouLyPlusKey
import com.vox.music.constants.PreferredLyricsProvider
import com.vox.music.lyrics.bitchord.PaxSenix
import com.vox.music.utils.dataStore
import com.vox.music.utils.get

object LyricsProviderSettings {
    fun idForLegacyPreference(provider: PreferredLyricsProvider): String = when (provider) {
        PreferredLyricsProvider.LRCLIB -> "LrcLib"
        PreferredLyricsProvider.KUGOU -> "Kugou"
        PreferredLyricsProvider.BETTER_LYRICS -> "BetterLyrics"
        PreferredLyricsProvider.SIMPMUSIC -> "SimpMusic"
        PreferredLyricsProvider.YOULYPLUS -> "YouLyPlus"
        PreferredLyricsProvider.PAXSENIX -> "Paxsenix"
    }

    fun enabledKey(id: String): Preferences.Key<Boolean> = when (id) {
        "BetterLyrics" -> EnableBetterLyricsKey
        "Kugou" -> EnableKugouKey
        "LrcLib" -> EnableLrcLibKey
        "Musixmatch" -> EnableMusixmatchKey
        "Paxsenix" -> EnablePaxsenixKey
        "SimpMusic" -> EnableSimpMusicKey
        "Unison" -> EnableUnisonKey
        "YouLyPlus" -> EnableYouLyPlusKey
        else -> booleanPreferencesKey("enableLyrics$id")
    }

    fun isEnabled(context: Context, provider: LyricsProvider): Boolean =
        context.dataStore[enabledKey(provider.id)] ?: true

    fun initialize(context: Context, provider: LyricsProvider) {
        if (provider !is BitChordLyricsProvider) return
        val credentials = context.applicationContext.getSharedPreferences("lyrics_credentials", Context.MODE_PRIVATE)
        PaxSenix.setApiKey(credentials.getString("paxsenix_api_key", "").orEmpty())
    }
}
