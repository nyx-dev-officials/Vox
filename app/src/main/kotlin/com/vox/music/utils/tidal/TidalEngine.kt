/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * One door for lossless playback:
 *
 *   official Tidal (user OAuth)  →  public hifi-api proxy  →  null
 *
 * A null result is not an error. The caller falls through to Innertube and the
 * listener never notices; that is the whole point of the hybrid engine.
 *
 * Candidate matching is title + artist + duration, with duration promoted to a
 * hard requirement whenever both sides report one — a 3:41 studio take and a
 * 7:12 live version share a title, and swapping them mid-queue is worse than
 * playing Opus.
 */

package com.vox.music.utils.tidal

import android.content.Context
import com.vox.music.constants.TidalQualityCap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Locale

/** A stream Vox can hand to ExoPlayer, with the facts the badge needs. */
data class TidalStream(
    val trackId: Long,
    val url: String,
    val isDash: Boolean,
    val mimeType: String,
    val codecs: String,
    /** MAX / CD / HIGH. */
    val quality: String,
    val sampleRate: Int,
    val bitDepth: Int,
    val durationSec: Int?,
    /** True when this came from the official API rather than a proxy. */
    val official: Boolean,
)

object TidalEngine {

    private const val TAG = "TidalEngine"

    /** Higher first, so the best master is attempted before a fallback rendition. */
    internal fun qualityRank(audioQuality: String?): Int =
        when (audioQuality?.uppercase(Locale.US)) {
            "HI_RES_LOSSLESS", "HI_RES" -> 3
            "LOSSLESS" -> 2
            "HIGH" -> 1
            else -> 0
        }

    internal fun isMatch(
        candidate: TidalTrack,
        wantedTitle: String,
        wantedArtists: List<String>,
        wantedDuration: Int?,
    ): Boolean {
        val candidateTitle = candidate.title.lowercase(Locale.US)
        val candidateArtists = candidate.artistNames.map { it.lowercase(Locale.US) }
        val titleMatches = candidateTitle.contains(wantedTitle) || wantedTitle.contains(candidateTitle)
        val artistMatches = wantedArtists.isEmpty() || wantedArtists.any { wanted ->
            candidateArtists.any { it.contains(wanted) || wanted.contains(it) }
        }
        val durationMatches = if (wantedDuration != null && candidate.duration != null) {
            kotlin.math.abs(candidate.duration - wantedDuration) <= 10
        } else {
            candidateTitle == wantedTitle
        }
        return titleMatches && artistMatches && durationMatches
    }

    /**
     * Resolves [title]/[artists] to a Tidal stream.
     *
     * @param allowDash when false (downloads), hi-res DASH manifests are skipped
     *   so a plain progressive file is downloaded instead.
     */
    suspend fun resolve(
        context: Context,
        title: String,
        artists: List<String>,
        durationSec: Int?,
        cap: TidalQualityCap,
        allowDash: Boolean = true,
        /** User's own hifi-api instance, when they set one. */
        proxyUrl: String? = null,
    ): TidalStream? {
        if (title.isBlank()) return null
        val wantedTitle = title.lowercase(Locale.US)
        val wantedArtists = artists.map { it.lowercase(Locale.US) }.filter { it.isNotBlank() }
        val query = listOf(title, artists.joinToString(" ")).joinToString(" ").trim()

        officialStream(context, query, wantedTitle, wantedArtists, durationSec, cap, allowDash)
            ?.let { return it }

        return proxyStream(query, wantedTitle, wantedArtists, durationSec, cap, proxyUrl)
    }

    private suspend fun officialStream(
        context: Context,
        query: String,
        wantedTitle: String,
        wantedArtists: List<String>,
        durationSec: Int?,
        cap: TidalQualityCap,
        allowDash: Boolean,
    ): TidalStream? {
        if (!TidalAuth.isLoggedIn(context)) return null
        val token = withContext(Dispatchers.IO) { TidalAuth.accessToken(context) } ?: return null
        val candidates = withContext(Dispatchers.IO) { TidalOfficialApi.search(token, query) }
        if (candidates.isEmpty()) {
            Timber.tag(TAG).d("official: no results for \"$query\"")
            return null
        }
        val matches = candidates
            .filter { isMatch(it, wantedTitle, wantedArtists, durationSec) }
            .sortedByDescending { qualityRank(it.audioQuality) }

        for (candidate in matches) {
            val manifest = withContext(Dispatchers.IO) {
                TidalOfficialApi.playbackInfo(token, candidate.id, cap.apiValues(), allowDash)
            } ?: continue

            val url = if (manifest.isDash) {
                val file = withContext(Dispatchers.IO) {
                    writeDashManifest(context, candidate.id, manifest.dashXml.orEmpty())
                } ?: continue
                file.toURI().toString()
            } else {
                manifest.directUrl ?: continue
            }

            Timber.tag(TAG).i(
                "official: \"${candidate.title}\" ${manifest.qualityLabel} " +
                    "${manifest.bitDepth}-bit/${manifest.sampleRate / 1000} kHz (id=${candidate.id})",
            )
            return TidalStream(
                trackId = candidate.id,
                url = url,
                isDash = manifest.isDash,
                mimeType = manifest.mimeType,
                codecs = manifest.codecs,
                quality = manifest.qualityLabel,
                sampleRate = manifest.sampleRate,
                bitDepth = manifest.bitDepth,
                durationSec = candidate.duration,
                official = true,
            )
        }
        return null
    }

    private suspend fun proxyStream(
        query: String,
        wantedTitle: String,
        wantedArtists: List<String>,
        durationSec: Int?,
        cap: TidalQualityCap,
        customUrl: String?,
    ): TidalStream? {
        if (cap == TidalQualityCap.HIGH) return null // HIGH only exists on the official API.
        val candidates = TidalService.search(query, customUrl)
        if (candidates.isEmpty()) return null
        val matches = candidates
            .filter { isMatch(it, wantedTitle, wantedArtists, durationSec) }
            .sortedByDescending { qualityRank(it.audioQuality) }

        for (candidate in matches) {
            // The proxy never serves hi-res; LOSSLESS is its ceiling.
            val url = TidalService.streamUrl(candidate.id, "LOSSLESS", customUrl) ?: continue
            return TidalStream(
                trackId = candidate.id,
                url = url,
                isDash = false,
                mimeType = "audio/flac",
                codecs = "flac",
                quality = "CD",
                sampleRate = 44_100,
                bitDepth = 16,
                durationSec = candidate.duration,
                official = false,
            )
        }
        return null
    }

    private fun writeDashManifest(context: Context, trackId: Long, xml: String): File? = runCatching {
        val dir = File(context.cacheDir, "tidal-dash").apply { mkdirs() }
        File(dir, "track-$trackId.mpd").apply { writeText(xml) }
    }.getOrElse {
        Timber.tag(TAG).d("could not stage DASH manifest: ${it.message}")
        null
    }
}
