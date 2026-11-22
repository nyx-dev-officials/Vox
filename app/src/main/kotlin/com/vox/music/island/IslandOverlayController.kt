/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Mode A of the Live Island: a real window above other apps, pinned under the
 * camera cut-out. It is owned by the playback service because that is the one
 * component that already runs for the whole session, is foreground, and knows
 * what is playing.
 *
 * The controller stays dumb. It is told which line to show and whether to be
 * visible; deciding that (position, parsed lines, foreground state) stays in
 * MusicService so a gap between lyrics never kills the window on its own.
 */

package com.vox.music.island

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.vox.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IslandOverlayController(
    private val context: Context,
    private val onOpenPlayer: () -> Unit,
) {
    private val windowManager: WindowManager? = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density

    private var root: View? = null
    private var art: ImageView? = null
    private var lineView: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    private var attached = false
    private var loadedArtUrl: String? = null
    private var mediaId: String? = null
    private var dismissedFor: String? = null

    /** The app op can be revoked at any time; ask every time instead of caching it. */
    fun permitted(): Boolean = Settings.canDrawOverlays(context)

    suspend fun show(mediaId: String, line: String, artworkUrl: String?, yOffsetDp: Int) {
        if (!permitted()) {
            hide()
            return
        }
        if (mediaId != this.mediaId) {
            this.mediaId = mediaId
            dismissedFor = null
        }
        if (dismissedFor == mediaId) {
            hide()
            return
        }

        val view = ensureView() ?: return
        if (artworkUrl != loadedArtUrl) {
            loadedArtUrl = artworkUrl
            loadArtwork(artworkUrl)
        }
        lineView?.text = line
        moveTo(yOffsetDp)
        attach(view)
    }

    fun hide() {
        val view = root ?: return
        if (!attached) {
            view.visibility = View.GONE
            return
        }
        attached = false
        runCatching { windowManager?.removeView(view) }
    }

    fun release() {
        hide()
        root = null
        art = null
        lineView = null
        params = null
        loadedArtUrl = null
        mediaId = null
        dismissedFor = null
    }

    private fun ensureView(): View? {
        root?.let { return it }
        if (windowManager == null) return null

        val view = LayoutInflater.from(context).inflate(R.layout.view_island_overlay, null)
        val artView = view.findViewById<ImageView>(R.id.island_art)
        val textView = view.findViewById<TextView>(R.id.island_line)

        val gestures = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    onOpenPlayer()
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    val travelledUp = e1 != null && e2.y - e1.y < -dp(24)
                    if (!travelledUp && velocityY > -900f) return false
                    // Swipe up hides the pill for this track only; the next song
                    // brings it back without touching the setting.
                    dismissedFor = mediaId
                    hide()
                    return true
                }
            },
        )
        view.setOnTouchListener { _, event -> gestures.onTouchEvent(event) }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val layoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        root = view
        art = artView
        lineView = textView
        params = layoutParams
        return view
    }

    private fun attach(view: View) {
        view.visibility = View.VISIBLE
        if (attached) return
        val wm = windowManager ?: return
        val layoutParams = params ?: return
        runCatching { wm.addView(view, layoutParams) }
            .onSuccess {
                attached = true
                view.alpha = 0f
                view.animate().alpha(1f).setDuration(160L).start()
            }
            .onFailure {
                loadedArtUrl = null
            }
    }

    private fun moveTo(yOffsetDp: Int) {
        val layoutParams = params ?: return
        val y = topInsetPx() + dp(6) + dp(yOffsetDp)
        if (layoutParams.y == y) return
        layoutParams.y = y
        val view = root ?: return
        if (attached) runCatching { windowManager?.updateViewLayout(view, layoutParams) }
    }

    /** Top of the cut-out (or the status bar on phones without one). */
    private fun topInsetPx(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val insets = windowManager?.currentWindowMetrics?.windowInsets?.getInsetsIgnoringVisibility(
                    WindowInsets.Type.displayCutout() or WindowInsets.Type.statusBars(),
                )
                if (insets != null && insets.top > 0) return insets.top
            }
        }
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else dp(28)
    }

    private suspend fun loadArtwork(url: String?) {
        val target = art ?: return
        if (url.isNullOrBlank()) {
            target.setImageDrawable(null)
            return
        }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connection = java.net.URL(url).openConnection().apply {
                    connectTimeout = 6_000
                    readTimeout = 6_000
                }
                connection.getInputStream().use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }
            }.getOrNull()
        }
        if (loadedArtUrl == url) target.setImageBitmap(bitmap)
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}
