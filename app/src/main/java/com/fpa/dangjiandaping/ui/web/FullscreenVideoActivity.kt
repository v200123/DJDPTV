package com.fpa.dangjiandaping.ui.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager

private class PersistentControlsVideoPlayer(context: Context) : StandardGSYVideoPlayer(context) {

    override fun startDismissControlViewTimer() {
        cancelDismissControlViewTimer()
    }

    override fun onClickUiToggle(event: MotionEvent?) {
        showPersistentControls()
    }

    override fun hideAllWidget() {
        showPersistentControls()
    }

    private fun showPersistentControls() {
        mTopContainer?.visibility = View.VISIBLE
        mBottomContainer?.visibility = View.VISIBLE
        mBottomProgressBar?.visibility = View.INVISIBLE
    }
}

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
        videoPlayer = PersistentControlsVideoPlayer(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setUp(videoUrl, false, videoTitle)
            setOverrideExtension(if (isHlsVideoUrl(videoUrl)) "m3u8" else null)
            isLooping = false
            fullscreenButton.visibility = View.GONE
            titleTextView.visibility = View.GONE
            backButton.visibility = View.GONE
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
                addView(createTitleView(videoTitle))
                addView(createExitButton())
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

    private fun createTitleView(videoTitle: String): TextView =
        TextView(this).apply {
            text = videoTitle.ifBlank { "视频播放" }
            setTextColor(Color.WHITE)
            textSize = 24f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(6f, 0f, 2f, Color.BLACK)
            isFocusable = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.TOP,
            ).apply {
                setMargins(dp(32), dp(24), dp(160), 0)
            }
        }

    private fun createExitButton(): Button =
        Button(this).apply {
            text = "退出"
            textSize = 18f
            setTextColor(Color.WHITE)
            isAllCaps = false
            isFocusable = true
            isFocusableInTouchMode = true
            backgroundTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_focused),
                    intArrayOf(android.R.attr.state_pressed),
                    intArrayOf(),
                ),
                intArrayOf(
                    Color.rgb(215, 25, 32),
                    Color.rgb(215, 25, 32),
                    Color.argb(230, 105, 13, 11),
                ),
            )
            setOnClickListener { finish() }
            layoutParams = FrameLayout.LayoutParams(
                dp(112),
                dp(48),
                Gravity.END or Gravity.TOP,
            ).apply {
                setMargins(0, 0, dp(24), dp(12))
            }
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_VIDEO_URL = "video_url"
        private const val EXTRA_VIDEO_TITLE = "video_title"

        fun newIntent(context: Context, videoUrl: String, videoTitle: String): Intent =
            Intent(context, FullscreenVideoActivity::class.java)
                .putExtra(EXTRA_VIDEO_URL, videoUrl)
                .putExtra(EXTRA_VIDEO_TITLE, videoTitle)
    }
}
