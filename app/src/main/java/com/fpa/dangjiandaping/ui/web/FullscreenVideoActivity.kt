package com.fpa.dangjiandaping.ui.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView

/** Fullscreen native video player opened from the WebView JavaScript bridge. */
class FullscreenVideoActivity : Activity() {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL).orEmpty()
        if (videoUrl.isBlank()) {
            finish()
            return
        }

        videoView = VideoView(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMediaController(MediaController(this@FullscreenVideoActivity).also { controller ->
                controller.setAnchorView(this)
            })
            setVideoURI(Uri.parse(videoUrl))
            setOnPreparedListener { player ->
                player.isLooping = false
                requestFocus()
                start()
            }
            requestFocus()
        }
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                addView(
                    videoView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            },
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterImmersiveMode()
            videoView.requestFocus()
        }
    }

    override fun onDestroy() {
        if (::videoView.isInitialized) videoView.stopPlayback()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun enterImmersiveMode() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    companion object {
        private const val EXTRA_VIDEO_URL = "video_url"

        fun newIntent(context: Context, videoUrl: String): Intent =
            Intent(context, FullscreenVideoActivity::class.java)
                .putExtra(EXTRA_VIDEO_URL, videoUrl)
    }
}
