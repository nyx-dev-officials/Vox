/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import com.vox.music.constants.AppLanguageKey
import com.vox.music.constants.DisableScreenshotKey
import com.vox.music.constants.SYSTEM_DEFAULT
import com.vox.music.constants.StopMusicOnTaskClearKey
import com.vox.music.db.MusicDatabase
import com.vox.music.di.ValueProvider
import com.vox.music.history.ListenHistoryRepository
import com.vox.music.history.ListenHistorySettingsRepository
import com.vox.music.library.ObservePlaylists
import com.vox.music.playback.DownloadUtil
import com.vox.music.playback.MusicService
import com.vox.music.playback.PlaybackServiceConnector
import com.vox.music.playback.PlayerConnection
import com.vox.music.utils.SyncUtils
import com.vox.music.utils.dataStore
import com.vox.music.utils.get
import com.vox.music.utils.setAppLocale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PLAY_INTRO = "com.vox.music.extra.PLAY_INTRO"
        internal const val ACTION_SEARCH = "com.vox.music.action.SEARCH"
        internal const val ACTION_LIBRARY = "com.vox.music.action.LIBRARY"
    }

    private val database: MusicDatabase by inject()
    private val downloadUtil: DownloadUtil by inject()
    private val syncUtils: SyncUtils by inject()
    private val listenHistoryRepository: ListenHistoryRepository by inject()
    private val listenHistorySettingsRepository: ListenHistorySettingsRepository by inject()
    private val observePlaylists: ObservePlaylists by inject()
    private val listenTogetherManager: com.vox.music.listentogether.ListenTogetherManager by inject()
    private val partyManager: com.vox.music.jam.JamManager by inject()
    private val playbackServiceConnector: PlaybackServiceConnector by inject()

    private var pendingIntent: Intent? = null

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var activityStarted = false
    private var pendingPlaybackBind: Runnable? = null

    override fun onStart() {
        super.onStart()
        activityStarted = true
        if (MusicService.isRunning) {
            bindPlaybackService()
        } else {
            val bind = Runnable {
                pendingPlaybackBind = null
                if (activityStarted) bindPlaybackService()
            }
            pendingPlaybackBind = bind
            window.decorView.postDelayed(bind, 32L)
        }
    }

    override fun onStop() {
        activityStarted = false
        pendingPlaybackBind?.let(window.decorView::removeCallbacks)
        pendingPlaybackBind = null
        playbackServiceConnector.unbind(this)
        super.onStop()
    }

    private fun bindPlaybackService() {
        // The service promotes itself when bound, so bind only while this activity is visible.
        playbackServiceConnector.bind(this, lifecycleScope) { connection ->
            playerConnection = connection
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Handle back navigation here; swiping the task away is handled by the service.
        if (dataStore.get(StopMusicOnTaskClearKey, false) &&
            playerConnection?.isPlaying?.value == true &&
            isFinishing
        ) {
            // onStop already unbound the connection; only stop the service here.
            stopService(Intent(this, MusicService::class.java))
            playerConnection = null
        }
    }

    internal fun takePendingIntent(): Intent? {
        val current = pendingIntent
        pendingIntent = null
        return current
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A Tidal OAuth redirect is consumed here and never routed as a deep link.
        if (consumeTidalRedirect(intent)) return
        // Retain the intent until Compose's listener is ready to handle it.
        pendingIntent = intent
    }

    /** True when [intent] was the Tidal login redirect (handled, not to be routed). */
    private fun consumeTidalRedirect(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        if (data.scheme != "vox" || data.host != "tidal-auth") return false
        intent.data = null
        // Token exchange is a network call; keep it off the main thread and report back.
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val error = com.vox.music.utils.tidal.TidalAuth.completeLogin(this@MainActivity, data)
            runOnUiThread {
                android.widget.Toast.makeText(
                    this@MainActivity,
                    error ?: getString(R.string.tidal_session_active),
                    if (error == null) android.widget.Toast.LENGTH_SHORT else android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
        return true
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
            com.vox.music.ui.component.MenuAnchor.lastDown =
                androidx.compose.ui.geometry.Offset(ev.x, ev.y)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Cold-start Tidal redirect (the process may have been killed mid-login).
        consumeTidalRedirect(intent)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        // Cold launch from the launcher only (WelcomeActivity hands off with this extra);
        // notification/deep-link opens go straight to the app.
        val playIntro = savedInstanceState == null && intent.getBooleanExtra(EXTRA_PLAY_INTRO, false)
        setContent {
            var showIntro by rememberSaveable { mutableStateOf(playIntro) }
            Box(Modifier.fillMaxSize()) {
                AppRootContent(
                    activity = this@MainActivity,
                    intentRouter = deepLinkRouter,
                    playerConnection = playerConnection,
                    database = ValueProvider { database },
                    downloadUtil = ValueProvider { downloadUtil },
                    syncUtils = ValueProvider { syncUtils },
                    listenHistoryRepository = ValueProvider { listenHistoryRepository },
                    listenHistorySettingsRepository = ValueProvider { listenHistorySettingsRepository },
                    observePlaylists = observePlaylists,
                )
                if (showIntro) com.vox.music.ui.component.VoxIntro(onFinished = { showIntro = false })
            }
        }
        window.decorView.doOnPreDraw {
            // A frame-clock callback runs before drawing. Post first, then wait for
            // the queue to become idle so optional startup never competes for that frame.
            window.decorView.post {
                android.os.Looper.myQueue().addIdleHandler {
                    (application as App).initializeAfterFirstDraw()
                    false
                }
            }
        }
    }

    internal val deepLinkRouter by lazy {
        AppIntentRouter(
            this,
            { playerConnection },
            ValueProvider { listenTogetherManager },
            ValueProvider { partyManager },
        )
    }
}
