/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.vox.music.playback.MusicService
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class MusicWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Only trigger update through MusicService if it's already running
        // This prevents BackgroundServiceStartNotAllowedException on Android 14+
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Service might be restricted in background
            }
        }
        if (!MusicService.isRunning) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    GlobalContext.get().get<VoxWidgetManager>().updateWidgets(
                        title = context.getString(com.vox.music.R.string.app_name),
                        artist = context.getString(com.vox.music.R.string.widget_open_app),
                        artworkUri = null, isPlaying = false, isLiked = false,
                    )
                } catch (error: Exception) {
                    timber.log.Timber.w(error, "Could not initialize widgets")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (!MusicService.isRunning) {
            onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
            return
        }
        // Trigger widget update when size changes
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Service might be restricted in background
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TOGGLE_LYRICS_MODE -> toggleLyricsMode(context, intent)
            ACTION_PLAY_PAUSE, ACTION_LIKE, ACTION_NEXT, ACTION_PREVIOUS -> {
                // User interactions from widget buttons can start the service.
                // Use startForegroundService so the system grants FGS privilege
                // on Android 12+ (API 31+) where background FGS starts are banned.
                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    // Service might be restricted in background
                }
            }
        }
    }

    /**
     * Flips the artwork lyrics widget between its cover and lyric states.
     *
     * The choice is per widget instance and lives in [WidgetConfig], so the next
     * repaint from the service already renders the new state. When the service
     * isn't running there is nothing to repaint from, so the card is refreshed
     * through the receiver's own idle update instead of a silent no-op.
     */
    private fun toggleLyricsMode(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val config = WidgetConfig.load(context, widgetId)
        val next = if (config.lyricsMode == LyricsWidgetMode.COVER) {
            LyricsWidgetMode.LYRICS
        } else {
            LyricsWidgetMode.COVER
        }
        WidgetConfig.save(context, widgetId, config.copy(lyricsMode = next))

        if (MusicService.isRunning) {
            runCatching {
                context.startService(
                    Intent(context, MusicService::class.java).apply {
                        action = ACTION_UPDATE_WIDGET
                    }
                )
            }
            return
        }

        context.sendBroadcast(
            Intent(context, LyricsWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
            }
        )
    }

    companion object {
        const val ACTION_TOGGLE_LYRICS_MODE = "com.vox.music.widget.TOGGLE_LYRICS_MODE"
        const val ACTION_PLAY_PAUSE = "com.vox.music.widget.PLAY_PAUSE"
        const val ACTION_LIKE = "com.vox.music.widget.LIKE"
        const val ACTION_NEXT = "com.vox.music.widget.NEXT"
        const val ACTION_PREVIOUS = "com.vox.music.widget.PREVIOUS"
        const val ACTION_UPDATE_WIDGET = "com.vox.music.widget.UPDATE_WIDGET"
    }
}
