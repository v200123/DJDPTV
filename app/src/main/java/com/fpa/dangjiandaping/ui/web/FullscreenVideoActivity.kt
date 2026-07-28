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
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager

/** Fullscreen GSYVideo player opened from the WebView JavaScript bridge. */
class FullscreenVideoActivity : Activity() {

    private lateinit var videoPlayer: StandardGSYVideoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL).orEmpty()
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE).orEmpty()
        if (videoUrl.isBlank()) {
            finish()
            return
        }

        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        videoPlayer = StandardGSYVideoPlayer(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setUp(videoUrl, false, videoTitle)
            setOverrideExtension(if (isHlsVideoUrl(videoUrl)) "m3u8" else null)
            isLooping = false
            fullscreenButton.visibility = View.GONE
            titleTextView.visibility = if (videoTitle.isBlank()) View.GONE else View.VISIBLE
            backButton.setOnClickListener { finish() }
            requestFocus()
            startPlayLogic()
        }
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                addView(
                    videoPlayer,
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
            videoPlayer.requestFocus()
        }
    }

    override fun onPause() {
        if (::videoPlayer.isInitialized) videoPlayer.onVideoPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::videoPlayer.isInitialized) videoPlayer.onVideoResume(false)
    }

    override fun onDestroy() {
        if (::videoPlayer.isInitialized) GSYVideoManager.releaseAllVideos()
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

    private fun isHlsVideoUrl(videoUrl: String): Boolean =
        Uri.parse(videoUrl).path?.endsWith(".m3u8", ignoreCase = true) == true

    companion object {
        private const val EXTRA_VIDEO_URL = "video_url"
        private const val EXTRA_VIDEO_TITLE = "video_title"

        fun newIntent(context: Context, videoUrl: String, videoTitle: String): Intent =
            Intent(context, FullscreenVideoActivity::class.java)
                .putExtra(EXTRA_VIDEO_URL, videoUrl)
                .putExtra(EXTRA_VIDEO_TITLE, videoTitle)
    }
}
