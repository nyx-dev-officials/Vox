package com.vox.music.history

import com.vox.music.db.entities.EventWithSong

interface HistoryLocalDataSource {
    suspend fun loadEvents(limit: Int, offset: Int, hideVideoSongs: Boolean, query: String): List<EventWithSong>
}
