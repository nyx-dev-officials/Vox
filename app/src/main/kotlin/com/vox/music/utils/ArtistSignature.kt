package com.vox.music.utils

/** Android entry point for the shared artist wordmark lookup. */
object ArtistSignature {
    suspend fun findUrl(artistName: String): String? = com.vox.music.canvas.ArtistSignature.findUrl(artistName)
}
