package com.vox.music.playback

import com.vox.music.playback.smart.TrackAnalysis

/** Persistent cache for provider-qualified, offline playback analysis. */
interface PlaybackTrackAnalysisRepository {
    suspend fun get(trackIdentity: String): TrackAnalysis?

    suspend fun save(trackIdentity: String, analysis: TrackAnalysis)

    suspend fun remove(trackIdentity: String)
}
