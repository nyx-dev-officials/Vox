/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Mode B of the Live Island: the current lyric line goes into the media
 * notification title, which is what OEM islands (Realme Mini Capsule, ZTE Live
 * Island, Xiaomi HyperOS) and apps like dynamicSpot mirror. Android 15/16 also
 * read the short critical text for the live-update chip, so that is set too.
 *
 * The playing MediaItem is never touched: rewriting the notification avoids the
 * re-prepare that replacing the current item can trigger, so a line change can
 * never cause a buffer gap. The cost is the documented one — the notification,
 * Bluetooth and car displays now show the lyric instead of the song title.
 */

package com.vox.music.island

import android.app.Notification
import android.content.Context
import android.os.Build

object IslandNotificationLyrics {

    /** Where the real song title is kept so the rewrite can be undone. */
    const val ORIGINAL_TITLE = "vox.island.originalTitle"

    @Volatile
    var enabled: Boolean = false

    @Volatile
    var line: String? = null

    fun rewrite(context: Context, source: Notification, enabled: Boolean): Notification {
        val lyric = line?.takeIf { enabled && it.isNotBlank() } ?: return source
        val original = source.extras?.getCharSequence(ORIGINAL_TITLE)
            ?: source.extras?.getCharSequence(Notification.EXTRA_TITLE)

        return rebuild(context, source) { builder ->
            builder.setContentTitle(lyric)
            if (original != null) builder.extras.putCharSequence(ORIGINAL_TITLE, original)
            setShortCriticalText(builder, lyric)
        }
    }

    /** Puts the song title back after Mode B is switched off or lyrics run out. */
    fun restore(context: Context, source: Notification): Notification {
        val original = source.extras?.getCharSequence(ORIGINAL_TITLE) ?: return source
        return rebuild(context, source) { builder ->
            builder.setContentTitle(original)
            builder.extras.remove(ORIGINAL_TITLE)
        }
    }

    private inline fun rebuild(
        context: Context,
        source: Notification,
        edit: (Notification.Builder) -> Unit,
    ): Notification = runCatching {
        val builder = Notification.Builder.recoverBuilder(context, source)
        // recoverBuilder only restores what it understands, so carry our marker
        // (and the platform's own keys) across explicitly.
        builder.extras.putAll(source.extras)
        edit(builder)
        builder.build().also { rebuilt -> copySessionToken(source, rebuilt) }
    }.getOrDefault(source)

    /**
     * Notification.Builder.build() drops the media session token MediaStyle
     * needs to stay linked to the session; put it back on the rebuilt copy.
     */
    private fun copySessionToken(source: Notification, target: Notification) {
        val token = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            source.extras.getParcelable(
                Notification.EXTRA_MEDIA_SESSION,
                android.media.session.MediaSession.Token::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            source.extras.getParcelable<android.media.session.MediaSession.Token>(
                Notification.EXTRA_MEDIA_SESSION,
            )
        }
        if (token != null) {
            target.extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, token)
        }
    }

    private fun setShortCriticalText(builder: Notification.Builder, text: CharSequence) {
        try {
            Notification.Builder::class.java
                .getMethod("setShortCriticalText", CharSequence::class.java)
                .invoke(builder, text)
        } catch (_: Exception) {
            builder.extras.putCharSequence("android.shortCriticalText", text)
        }
    }
}
