package com.fpa.dangjiandaping.ui.web

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.ui.focus.focusOnClick
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayState
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayerSurface
import com.shuyu.gsyvideoplayer.compose.native_.rememberGSYPlayerController
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager

/** Fullscreen video player opened from the WebView JavaScript bridge. */
class FullscreenVideoActivity : ComponentActivity() {

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
        setContent {
            MaterialTheme {
                FullscreenVideoPlayer(
                    videoUrl = videoUrl,
                    videoTitle = videoTitle,
                    onExit = ::finish,
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val EXTRA_VIDEO_URL = "video_url"
        private const val EXTRA_VIDEO_TITLE = "video_title"

        fun newIntent(context: Context, videoUrl: String, videoTitle: String): Intent =
            Intent(context, FullscreenVideoActivity::class.java)
                .putExtra(EXTRA_VIDEO_URL, videoUrl)
                .putExtra(EXTRA_VIDEO_TITLE, videoTitle)
    }
}

@Composable
private fun FullscreenVideoPlayer(
    videoUrl: String,
    videoTitle: String,
    onExit: () -> Unit,
) {
    val controller = rememberGSYPlayerController(
        url = videoUrl,
        title = videoTitle,
        autoPlay = true,
    )
    val snapshot by controller.snapshot
    val lifecycleOwner = LocalLifecycleOwner.current
    val playFocusRequester = remember { FocusRequester() }
    val seekFocusRequester = remember { FocusRequester() }
    val exitFocusRequester = remember { FocusRequester() }
    var exiting by remember { mutableStateOf(false) }

    val exitPlayer = {
        if (!exiting) {
            exiting = true
            // Release while GSYPlayerSurface is still attached. Once AndroidView.onRelease
            // detaches the host, controller disposal can no longer reach this player.
            controller.setStartAfterPrepared(false)
            controller.release()
            onExit()
        }
    }

    LaunchedEffect(controller, videoUrl) {
        controller.setOverrideExtension(
            if (isHlsVideoUrl(videoUrl)) "m3u8" else null,
        )
        controller.setLooping(false)
    }

    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> controller.pause()
                Lifecycle.Event.ON_RESUME -> controller.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.release()
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        playFocusRequester.requestFocus()
    }

    BackHandler(onBack = exitPlayer)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        GSYPlayerSurface(
            controller = controller,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xD9000000), Color.Transparent),
                    ),
                )
                .padding(start = 32.dp, top = 20.dp, end = 24.dp, bottom = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = videoTitle.ifBlank { "视频播放" },
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(24.dp))
            PlayerControlButton(
                text = "退出",
                focusRequester = exitFocusRequester,
                downFocusRequester = playFocusRequester,
                onClick = exitPlayer,
            )
        }

        if (snapshot.state == GSYPlayState.Preparing) {
            Text(
                text = "正在加载…",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xE6000000)),
                    ),
                )
                .padding(start = 32.dp, top = 48.dp, end = 32.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PlayerControlButton(
                text = if (snapshot.isPlaying) "暂停" else "播放",
                focusRequester = playFocusRequester,
                rightFocusRequester = seekFocusRequester,
                upFocusRequester = exitFocusRequester,
                onClick = controller::togglePlayPause,
            )
            TvSeekBar(
                currentPosition = snapshot.currentPosition,
                duration = snapshot.duration,
                bufferPercent = snapshot.bufferPercent,
                focusRequester = seekFocusRequester,
                leftFocusRequester = playFocusRequester,
                upFocusRequester = exitFocusRequester,
                onSeekTo = controller::seekTo,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${formatDuration(snapshot.currentPosition)} / " +
                    formatDuration(snapshot.duration),
                color = Color.White,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun PlayerControlButton(
    text: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    rightFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusOnClick(focusRequester)
            .focusProperties {
                rightFocusRequester?.let { right = it }
                downFocusRequester?.let { down = it }
                upFocusRequester?.let { up = it }
            }
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color(0xFFD71920) else Color(0xB3000000))
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) Color(0xFFFFD889) else Color(0x80FFFFFF),
                shape = shape,
            )
            .clickable {
                focusRequester.requestFocus()
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TvSeekBar(
    currentPosition: Long,
    duration: Long,
    bufferPercent: Int,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val playedFraction =
        if (duration > 0L) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val bufferFraction = bufferPercent.coerceIn(0, 100) / 100f

    Column(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusOnClick(focusRequester)
            .focusProperties {
                left = leftFocusRequester
                up = upFocusRequester
            }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.DirectionLeft -> {
                        onSeekTo((currentPosition - SEEK_STEP_MILLIS).coerceAtLeast(0L))
                        true
                    }

                    Key.DirectionRight -> {
                        onSeekTo(
                            if (duration > 0L) {
                                (currentPosition + SEEK_STEP_MILLIS).coerceAtMost(duration)
                            } else {
                                currentPosition + SEEK_STEP_MILLIS
                            },
                        )
                        true
                    }

                    else -> false
                }
            }
            .focusable()
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    focusRequester.requestFocus()
                    if (duration > 0L) {
                        onSeekTo((duration * (offset.x / size.width).coerceIn(0f, 1f)).toLong())
                    }
                }
            }
            .padding(vertical = 12.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (focused) 18.dp else 14.dp),
        ) {
            val centerY = size.height / 2f
            val trackHeight = if (focused) 6.dp.toPx() else 4.dp.toPx()
            drawRoundRect(
                color = Color(0x66FFFFFF),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(size.width, trackHeight),
            )
            drawRoundRect(
                color = Color(0x99FFFFFF),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(size.width * bufferFraction, trackHeight),
            )
            drawRoundRect(
                color = Color(0xFFD71920),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(size.width * playedFraction, trackHeight),
            )
            drawCircle(
                color = if (focused) Color(0xFFFFD889) else Color.White,
                radius = if (focused) 8.dp.toPx() else 6.dp.toPx(),
                center = Offset(size.width * playedFraction, centerY),
            )
        }
    }
}

private fun isHlsVideoUrl(videoUrl: String): Boolean =
    Uri.parse(videoUrl).path?.endsWith(".m3u8", ignoreCase = true) == true

private fun formatDuration(valueMillis: Long): String {
    val totalSeconds = (valueMillis.coerceAtLeast(0L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Preview(
    name = "全屏视频播放器",
    widthDp = 960,
    heightDp = 540,
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun FullscreenVideoPlayerPreview() {
    val playFocusRequester = remember { FocusRequester() }
    val seekFocusRequester = remember { FocusRequester() }
    val exitFocusRequester = remember { FocusRequester() }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF333333), Color.Black),
                    ),
                ),
        ) {
            Text(
                text = "视频画面预览",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 32.sp,
                modifier = Modifier.align(Alignment.Center),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xD9000000), Color.Transparent),
                        ),
                    )
                    .padding(start = 32.dp, top = 20.dp, end = 24.dp, bottom = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "丹巴县：依托教育人才“组团式”帮扶推动高中教育提质增效",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(24.dp))
                PlayerControlButton(
                    text = "退出",
                    focusRequester = exitFocusRequester,
                    downFocusRequester = playFocusRequester,
                    onClick = {},
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xE6000000)),
                        ),
                    )
                    .padding(start = 32.dp, top = 48.dp, end = 32.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                PlayerControlButton(
                    text = "暂停",
                    focusRequester = playFocusRequester,
                    rightFocusRequester = seekFocusRequester,
                    upFocusRequester = exitFocusRequester,
                    onClick = {},
                )
                TvSeekBar(
                    currentPosition = 72_000L,
                    duration = 240_000L,
                    bufferPercent = 68,
                    focusRequester = seekFocusRequester,
                    leftFocusRequester = playFocusRequester,
                    upFocusRequester = exitFocusRequester,
                    onSeekTo = {},
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "01:12 / 04:00",
                    color = Color.White,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

private const val SEEK_STEP_MILLIS = 10_000L
