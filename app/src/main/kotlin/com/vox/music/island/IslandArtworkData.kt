/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Supplies MediaMetadata.artworkData bytes for the current track, so OEM islands
 * (ZTE/Nubia Live Island, Realme Mini Capsule, Xiaomi HyperOS) and dynamicSpot
 * show the cover even when they only read raw bitmap bytes and ignore
 * artworkUri.
 *
 * 256 px JPEG is deliberate: roughly 10-25 KB, so the binder payload of a
 * metadata update stays trivial and the bitmap never sits in memory at album size.
 */

package com.vox.music.island

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IslandArtworkData(context: Context) {

    private val appContext = context.applicationContext
    private val imageLoader by lazy { ImageLoader.Builder(appContext).crossfade(false).build() }

    /** Keyed by URL; a track that comes back gets its bytes from memory. */
    private val cache = object : LruCache<String, ByteArray>(6 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    suspend fun bytesFor(url: String?): ByteArray? {
        if (url.isNullOrBlank()) return null
        cache.get(url)?.let { return it }

        val bytes = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(appContext)
                    .data(url)
                    .size(Edge, Edge)
                    .allowHardware(false)
                    .build()
                val decoded = imageLoader.execute(request).image?.toBitmap() ?: return@runCatching null
                val square = if (decoded.width == Edge && decoded.height == Edge) {
                    decoded
                } else {
                    Bitmap.createScaledBitmap(decoded, Edge, Edge, true)
                }
                ByteArrayOutputStream(24 * 1024).use { out ->
                    square.compress(Bitmap.CompressFormat.JPEG, 82, out)
                    out.toByteArray()
                }
            }.getOrNull()
        }

        if (bytes != null) cache.put(url, bytes)
        return bytes
    }

    private companion object {
        const val Edge = 256
    }
}
