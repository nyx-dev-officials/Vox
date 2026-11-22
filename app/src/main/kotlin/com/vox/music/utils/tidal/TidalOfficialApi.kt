/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Official Tidal Web API surface used by Vox: catalog search and the
 * post-paywall playback manifest.
 *
 * `playbackinfopostpaywall` answers with a base64 manifest:
 *   - `application/vnd.tidal.bts`  → JSON with direct FLAC URLs (CD quality)
 *   - `application/dash+xml`       → MPD for hi-res (24-bit) audio
 * Both are decoded here; the caller decides whether it can play DASH.
 */

package com.vox.music.utils.tidal

import android.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class TidalPlaybackInfo(
    @SerialName("trackId") val trackId: Long? = null,
    @SerialName("audioQuality") val audioQuality: String? = null,
    @SerialName("audioMode") val audioMode: String? = null,
    @SerialName("manifestMimeType") val manifestMimeType: String? = null,
    @SerialName("manifest") val manifest: String? = null,
)

@Serializable
private data class TidalBtsManifest(
    @SerialName("mimeType") val mimeType: String? = null,
    @SerialName("codecs") val codecs: String? = null,
    @SerialName("urls") val urls: List<String> = emptyList(),
)

@Serializable
private data class TidalSearchPage(
    @SerialName("items") val items: List<TidalTrack> = emptyList(),
)

/** A resolved Tidal stream, ready for ExoPlayer. */
data class TidalManifest(
    /** Direct progressive URL, or null when the manifest is DASH. */
    val directUrl: String?,
    /** Raw MPD XML, or null for a progressive manifest. */
    val dashXml: String?,
    val mimeType: String,
    val codecs: String,
    val audioQuality: String,
    val sampleRate: Int,
    val bitDepth: Int,
) {
    val isDash: Boolean get() = dashXml != null
    val qualityLabel: String
        get() = when (audioQuality.uppercase(Locale.US)) {
            "HI_RES_LOSSLESS", "HI_RES" -> "MAX"
            "LOSSLESS" -> "CD"
            "HIGH" -> "HIGH"
            else -> audioQuality.uppercase(Locale.US)
        }
}

object TidalOfficialApi {

    private const val TAG = "TidalOfficial"
    private const val API_BASE = "https://api.tidal.com/v1"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .callTimeout(9, TimeUnit.SECONDS)
            .build()
    }

    /** ISO-3166 alpha-2 country the account is billed in; the API rejects "ZZ". */
    fun countryCode(): String =
        Locale.getDefault().country.uppercase(Locale.US).takeIf { it.length == 2 && it != "ZZ" } ?: "US"

    /**
     * Catalog search with a user token. Returns an empty list on any failure —
     * the caller treats that as "Tidal cannot help here" and moves on.
     */
    fun search(accessToken: String, query: String, limit: Int = 25): List<TidalTrack> {
        if (query.isBlank()) return emptyList()
        val url = "$API_BASE/search/tracks?query=${encodeUrl(query)}&limit=$limit&offset=0&countryCode=${countryCode()}"
        val body = get(accessToken, url) ?: return emptyList()
        return runCatching {
            json.decodeFromString<TidalSearchPage>(body).items
        }.getOrElse {
            Timber.tag(TAG).d("search parse failed: ${it.message}")
            emptyList()
        }.filter { it.id != 0L && it.title.isNotBlank() }
    }

    /**
     * Post-paywall playback info for [trackId]. Tries each quality in [qualities]
     * in order and returns the first manifest that decodes into something playable.
     */
    fun playbackInfo(
        accessToken: String,
        trackId: Long,
        qualities: List<String>,
        allowDash: Boolean = true,
    ): TidalManifest? {
        for (quality in qualities) {
            val url = buildString {
                append("$API_BASE/tracks/$trackId/playbackinfopostpaywall")
                append("?audioquality=$quality")
                append("&playbackmode=STREAM")
                append("&assetpresentation=FULL")
                append("&countryCode=${countryCode()}")
            }
            val body = get(accessToken, url) ?: continue
            val info = runCatching { json.decodeFromString<TidalPlaybackInfo>(body) }.getOrNull() ?: continue
            val manifest = decodeManifest(info, quality) ?: continue
            if (manifest.isDash && !allowDash) {
                Timber.tag(TAG).d("track=$trackId quality=$quality is DASH — skipped (progressive only)")
                continue
            }
            Timber.tag(TAG).d("track=$trackId quality=$quality -> ${if (manifest.isDash) "DASH" else "progressive"}")
            return manifest
        }
        return null
    }

    /** Decodes either manifest flavour. Null when it is not usable as-is. */
    internal fun decodeManifest(info: TidalPlaybackInfo, requestedQuality: String): TidalManifest? {
        val encoded = info.manifest ?: return null
        val mime = info.manifestMimeType.orEmpty()
        val quality = info.audioQuality ?: requestedQuality
        val decoded = runCatching {
            String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull() ?: return null

        val hiRes = quality.uppercase(Locale.US).startsWith("HI_RES")
        if (mime.contains("dash", ignoreCase = true) || decoded.trimStart().startsWith("<")) {
            // Absolute segment URLs are mandatory once the MPD lives on disk.
            if (!decoded.contains("https://")) {
                Timber.tag(TAG).d("DASH manifest has no absolute URLs — not usable")
                return null
            }
            return TidalManifest(
                directUrl = null,
                dashXml = decoded,
                mimeType = "audio/mp4",
                codecs = "flac",
                audioQuality = quality,
                sampleRate = dashSampleRate(decoded),
                bitDepth = if (hiRes) 24 else 16,
            )
        }

        val bts = runCatching { json.decodeFromString<TidalBtsManifest>(decoded) }.getOrNull() ?: return null
        val url = bts.urls.firstOrNull { it.startsWith("http") } ?: return null
        val codecs = bts.codecs?.takeIf { it.isNotBlank() } ?: "flac"
        return TidalManifest(
            directUrl = url,
            dashXml = null,
            mimeType = bts.mimeType?.takeIf { it.isNotBlank() } ?: "audio/flac",
            codecs = codecs,
            audioQuality = quality,
            sampleRate = if (hiRes) 96_000 else 44_100,
            bitDepth = if (hiRes) 24 else 16,
        )
    }

    /** Best-effort sample rate from the MPD's audio Representation. */
    private fun dashSampleRate(mpd: String): Int {
        val match = Regex("audioSamplingRate\\s*=\\s*\"(\\d+)\"").find(mpd)
            ?: Regex("sampleRate\\s*=\\s*\"(\\d+)\"").find(mpd)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 44_100
    }

    private fun get(accessToken: String, url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.tag(TAG).d("HTTP ${response.code} for ${url.substringBefore('?')}")
                return@use null
            }
            response.body?.string()
        }
    }.getOrElse {
        Timber.tag(TAG).d("request failed: ${it.message}")
        null
    }

    private fun encodeUrl(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
