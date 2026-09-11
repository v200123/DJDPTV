package com.fpa.dangjiandaping.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.R
import com.fpa.dangjiandaping.network.XyxfNewsApiClient
import com.fpa.dangjiandaping.network.model.XyxfArticle
import com.fpa.dangjiandaping.ui.focus.focusOnClick
import com.fpa.dangjiandaping.ui.focus.logFocusTarget
import com.fpa.dangjiandaping.ui.web.FullscreenVideoActivity
import com.fpa.dangjiandaping.ui.web.WebViewDialog
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayState
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayerSurface
import com.shuyu.gsyvideoplayer.compose.native_.rememberGSYPlayerController
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView
import kotlinx.coroutines.CancellationException

private const val DEFAULT_HOME_VIDEO_URL = "https://vod.scycjy.gov.cn/20260729/eE9NUYRQ/2000kb/hls/index.m3u8"
private const val PARTY_PIONEER_MOBILE_URL = "https://12371.people.com.cn/"
private const val PARTY_MEMBER_LEARNING_URL = "https://www.scycjy.gov.cn/dyxx_mys.html"
private const val KANGBA_PARTY_FLAG_URL = "https://www.scycjy.gov.cn/scdjw/2026wsdy.html"//专题专栏第三个选择
private const val WORK_DYNAMICS_DETAIL_URL_PREFIX = "https://www.xyxf.gov.cn/#/index/details?id="
private const val WORK_DYNAMICS_DETAIL_URL_SUFFIX = "&details=true&name=%E7%BB%84%E5%B7%A5%E5%8A%A8%E6%80%81"
private const val PIONEER_COMMENTARY_DETAIL_URL = "https://www.xyxf.gov.cn/#/index/details?id=2097918339568496642&name=%E9%9B%AA%E5%9F%9F%E5%85%88%E9%94%8B%E6%97%B6%E8%AF%84"
private val PARTY_WORK_TAB_WIDTH = 96.dp
private val Gold = Color(0xFFFFD889)
private val BrightGold = Color(0xFFFFD186)
private val PanelRed = Color(0xB078101B)
private val PanelStroke = Color(0x90E56E59)
private val PrimaryRed = Color(0xFFD71920)

private data class HomeFullscreenRequest(
    val positionMs: Long,
    val shouldResume: Boolean,
)

private enum class PartyWorkCategory(
    val label: String,
    val columnId: String,
    val onlyImage: Boolean,
    @DrawableRes val coverImage: Int,
) {
    WorkDynamics(
        label = "工作动态",
        columnId = "1978746246661910530",
        onlyImage = true,
        coverImage = R.drawable.ic_home_gzdt_head,
    ),
    PioneerCommentary(
        label = "雪域先锋时评",
        columnId = "2009152698225565697",
        onlyImage = false,
        coverImage = R.drawable.ic_home_xyxf_head,
    );

    fun detailUrl(articleId: String): String = when (this) {
        WorkDynamics ->
            "https://www.xyxf.gov.cn/#/index/details?id=$articleId&details=true&name=%E7%BB%84%E5%B7%A5%E5%8A%A8%E6%80%81"

        PioneerCommentary ->
            "https://www.xyxf.gov.cn/#/index/details?id=$articleId&name=%E9%9B%AA%E5%9F%9F%E5%85%88%E9%94%8B%E6%97%B6%E8%AF%84"
    }
}

private sealed interface PartyWorkFeedState {
    data object Loading : PartyWorkFeedState

    data class Loaded(val articles: List<XyxfArticle>) : PartyWorkFeedState

    data class Failed(val message: String) : PartyWorkFeedState
}

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    videoUrl: String = DEFAULT_HOME_VIDEO_URL,
    contentFocusRequester: FocusRequester? = null,
    onRequestTabFocus: () -> Unit = {},
    onCoursewareClick: (Int) -> Unit = {},
    onPartyBuildingTabClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val lastTopicFocusRequester = remember { FocusRequester() }
    val fullscreenFocusRequester = remember { FocusRequester() }
    val videoControlFocusRequester = remember { FocusRequester() }
    val firstPartyBuildingFocusRequester = remember { FocusRequester() }
    val lowerPartyBuildingFocusRequester = remember { FocusRequester() }
    val partyWorkTabFocusRequester = remember { FocusRequester() }
    val partyWorkReviewFocusRequester = remember { FocusRequester() }
    val partyWorkMoreFocusRequester = remember { FocusRequester() }
    var webViewDialogUrl by remember { mutableStateOf<String?>(null) }
    var partyWorkCategory by remember { mutableStateOf(PartyWorkCategory.WorkDynamics) }
    var partyWorkFeedStates by remember {
        mutableStateOf<Map<PartyWorkCategory, PartyWorkFeedState>>(emptyMap())
    }
    var partyWorkRetryCategory by remember { mutableStateOf<PartyWorkCategory?>(null) }
    val partyWorkFeedState = partyWorkFeedStates[partyWorkCategory] ?: PartyWorkFeedState.Loading
    var homeVideoVisible by remember { mutableStateOf(true) }
    var homeVideoSession by remember { mutableIntStateOf(0) }
    var homeVideoStartPositionMs by remember { mutableLongStateOf(0L) }
    var homeVideoAutoPlay by remember { mutableStateOf(true) }
    var pendingFullscreenRequest by remember { mutableStateOf<HomeFullscreenRequest?>(null) }
    val fullscreenLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val request = pendingFullscreenRequest
        val playback = FullscreenVideoActivity.readPlaybackResult(result.data)
        homeVideoStartPositionMs = playback?.positionMs ?: request?.positionMs ?: 0L
        homeVideoAutoPlay = playback?.shouldResume ?: request?.shouldResume ?: true
        homeVideoSession += 1
        homeVideoVisible = true
        pendingFullscreenRequest = null
    }

    LaunchedEffect(pendingFullscreenRequest) {
        val request = pendingFullscreenRequest ?: return@LaunchedEffect
        // 先让首页 AndroidView 释放，再启动独立全屏播放器；两者绝不共用或克隆 Surface。
        withFrameNanos { }
        fullscreenLauncher.launch(
            FullscreenVideoActivity.newIntent(
                context = context,
                videoUrl = videoUrl,
                videoTitle = "康巴党旗红",
                startPositionMs = request.positionMs,
                autoPlay = request.shouldResume,
            ),
        )
    }

    LaunchedEffect(active, partyWorkCategory, partyWorkRetryCategory) {
        if (!active) return@LaunchedEffect
        val requestCategory = partyWorkCategory
        val shouldRetry = partyWorkRetryCategory == requestCategory
        if (!shouldRetry && partyWorkFeedStates.containsKey(requestCategory)) {
            return@LaunchedEffect
        }
        partyWorkFeedStates = partyWorkFeedStates + (
            requestCategory to PartyWorkFeedState.Loading
        )
        val loadedState = runCatching {
            XyxfNewsApiClient.getArticles(
                columnId = requestCategory.columnId,
                onlyImage = requestCategory.onlyImage,
            ).articles
        }.fold(
            onSuccess = { articles -> PartyWorkFeedState.Loaded(articles) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                PartyWorkFeedState.Failed(
                    error.message?.ifBlank { null } ?: "资讯加载失败，请稍后重试。",
                )
            },
        )
        partyWorkFeedStates = partyWorkFeedStates + (requestCategory to loadedState)
        if (shouldRetry && partyWorkRetryCategory == requestCategory) {
            partyWorkRetryCategory = null
        }
    }

    Box(
        modifier = modifier
            .alpha(if (active) 1f else 0f)
            .fillMaxSize()
            .focusProperties {
                onExit = {
                    if (requestedFocusDirection == FocusDirection.Up) {
                        onRequestTabFocus()
                    } else {
                        cancelFocusChange()
                    }
                }
            }
            .focusGroup()
    ) {
//        HomeBackdrop(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 8.dp,
                    bottom = 10.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NewsTicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                firstItemFocusRequester = contentFocusRequester,
                firstItemDownFocusRequester = videoControlFocusRequester,
                onOpenUrl = { url -> webViewDialogUrl = url },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeVideoPlayer(
                    active = active && webViewDialogUrl == null,
                    videoUrl = videoUrl,
                    showRuntime = homeVideoVisible,
                    sessionKey = homeVideoSession,
                    startPositionMs = homeVideoStartPositionMs,
                    autoPlay = homeVideoAutoPlay,
                    playFocusRequester = videoControlFocusRequester,
                    fullscreenFocusRequester = fullscreenFocusRequester,
                    rightFocusRequester = firstPartyBuildingFocusRequester,
                    onFullscreen = { positionMs, shouldResume ->
                        homeVideoVisible = false
                        pendingFullscreenRequest = HomeFullscreenRequest(
                            positionMs = positionMs,
                            shouldResume = shouldResume,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                PartyWorkPanel(
                    videoControlFocusRequester = videoControlFocusRequester,
                    topFocusRequester = contentFocusRequester,
                    firstItemFocusRequester = firstPartyBuildingFocusRequester,
                    lowerItemFocusRequester = lowerPartyBuildingFocusRequester,
                    tabFocusRequester = partyWorkTabFocusRequester,
                    reviewTabFocusRequester = partyWorkReviewFocusRequester,
                    moreFocusRequester = partyWorkMoreFocusRequester,
                    selectedCategory = partyWorkCategory,
                    feedState = partyWorkFeedState,
                    onCategorySelected = { partyWorkCategory = it },
                    onRetry = { partyWorkRetryCategory = partyWorkCategory },
                    onOpenArticle = { articleId ->
                        webViewDialogUrl = partyWorkCategory.detailUrl(articleId)
                    },
                    onMoreClick = onPartyBuildingTabClick,
                    modifier = Modifier
                        .weight(1.08f)
                        .fillMaxHeight(),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TopicPanel(
                    lastTopicFocusRequester = lastTopicFocusRequester,
                    videoControlFocusRequester = videoControlFocusRequester,
                    onOpenUrl = { url -> webViewDialogUrl = url },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                CoursewarePanel(
                    partyBuildingFocusRequester = lowerPartyBuildingFocusRequester,
                    onCoursewareClick = onCoursewareClick,
                    modifier = Modifier
                        .weight(1.08f)
                        .fillMaxHeight(),
                )
            }
        }

        webViewDialogUrl?.let { url ->
            WebViewDialog(
                url = url,
                onDismiss = { webViewDialogUrl = null },
            )
        }
    }
}

@Composable
private fun HomeBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x52FF8B4C), Color.Transparent),
                center = Offset(w * 0.12f, h * 0.72f),
                radius = w * 0.55f,
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.12f, h * 0.72f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x40FFB33E), Color.Transparent),
                center = Offset(w * 0.88f, h * 0.48f),
                radius = w * 0.42f,
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.88f, h * 0.48f),
        )

        val ribbon = Path().apply {
            moveTo(0f, h * 0.66f)
            cubicTo(w * 0.23f, h * 0.98f, w * 0.43f, h * 0.45f, w, h * 0.81f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = ribbon,
            brush = Brush.verticalGradient(listOf(Color(0x26FF8A38), Color(0x8A84020B))),
        )

        val mountain = Path().apply {
            moveTo(w * 0.68f, h * 0.72f)
            lineTo(w * 0.78f, h * 0.45f)
            lineTo(w * 0.85f, h * 0.67f)
            lineTo(w * 0.92f, h * 0.36f)
            lineTo(w, h * 0.61f)
            lineTo(w, h)
            lineTo(w * 0.68f, h)
            close()
        }
        drawPath(mountain, Color(0x2580000A))
    }
}

@Composable
private fun NewsTicker(
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    firstItemDownFocusRequester: FocusRequester? = null,
    onOpenUrl: (String) -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Color(0x66FFF4E8), RoundedCornerShape(6.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_home_tips),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpeakerIcon(Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            TickerItem(
                text = "关于组织开展2024年度党员教育培训工作",
                onClick = {
                    onOpenUrl(
                        "https://www.xyxf.gov.cn/#/index/details?id=2080461005451776002&name=%E5%B7%A5%E4%BD%9C%E5%8A%A8%E6%80%81",
                    )
                },
                modifier = Modifier.weight(1f),
                focusRequester = firstItemFocusRequester,
                downFocusRequester = firstItemDownFocusRequester,
            )
            TickerDivider()
            TickerItem(
                text = "雅江县：“三维赋能”让党员教育在高原落地生根",
                modifier = Modifier.weight(1.2f),
                onClick = {
                    onOpenUrl(
                        "https://www.xyxf.gov.cn/#/index/details?id=2080460616023232513&name=%E5%B7%A5%E4%BD%9C%E5%8A%A8%E6%80%81",
                    )
                },
            )
            TickerDivider()
            TickerItem(
                text = "康定市：建强农业实用人才队伍……",
                modifier = Modifier.weight(0.82f),
                onClick = {
                    onOpenUrl(
                        "https://www.xyxf.gov.cn/#/index/details?id=2080459428695461890&name=%E5%B7%A5%E4%BD%9C%E5%8A%A8%E6%80%81",
                    )
                },
            )
        }
    }
}

@Composable
private fun TickerItem(
    text: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onClick: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val defaultFocusRequester = remember { FocusRequester() }
    val clickFocusRequester = focusRequester ?: defaultFocusRequester
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (focused) Modifier.border(1.dp, PrimaryRed, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusRequester(clickFocusRequester)
            .logFocusTarget("Home.Ticker[$text]")
            .focusOnClick(clickFocusRequester)
            .then(
                if (downFocusRequester != null) {
                    Modifier.focusProperties { down = downFocusRequester }
                } else {
                    Modifier
                },
            )
            .clickable {
                clickFocusRequester.requestFocus()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = Color(0xFF650D0B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TickerDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(22.dp)
            .background(Color(0x8068424A)),
    )
}

@Composable
private fun SpeakerIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val red = Color(0xFFC51B2B)
        drawRect(red, topLeft = Offset(size.width * 0.08f, size.height * 0.36f), size = Size(size.width * 0.22f, size.height * 0.28f))
        val horn = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.36f)
            lineTo(size.width * 0.58f, size.height * 0.15f)
            lineTo(size.width * 0.58f, size.height * 0.85f)
            lineTo(size.width * 0.3f, size.height * 0.64f)
            close()
        }
        drawPath(horn, red)
        drawArc(red, -50f, 100f, false, Offset(size.width * 0.48f, size.height * 0.25f), Size(size.width * 0.34f, size.height * 0.5f), style = Stroke(2.dp.toPx()))
        drawArc(red, -48f, 96f, false, Offset(size.width * 0.42f, size.height * 0.10f), Size(size.width * 0.54f, size.height * 0.8f), style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun HomeVideoPlayer(
    active: Boolean,
    videoUrl: String,
    showRuntime: Boolean,
    sessionKey: Int,
    startPositionMs: Long,
    autoPlay: Boolean,
    playFocusRequester: FocusRequester? = null,
    fullscreenFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    onFullscreen: (positionMs: Long, shouldResume: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inPreview = LocalInspectionMode.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .border(3.dp, Color(0xFFE8B56F), RoundedCornerShape(14.dp)),
    ) {
        if (inPreview || !showRuntime) {
            VideoPoster(Modifier.fillMaxSize())
            if (inPreview) {
                StaticVideoControls(Modifier.align(Alignment.BottomCenter))
            }
        } else {
            key(sessionKey) {
                RuntimeVideoPlayer(
                    active = active,
                    videoUrl = videoUrl,
                    startPositionMs = startPositionMs,
                    autoPlay = autoPlay,
                    playFocusRequester = playFocusRequester,
                    fullscreenFocusRequester = fullscreenFocusRequester,
                    rightFocusRequester = rightFocusRequester,
                    onFullscreen = onFullscreen,
                )
            }
        }

//        Box(
//            modifier = Modifier
//                .align(Alignment.TopEnd)
//                .padding(top = 3.dp, end = 16.dp)
//                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
//                .background(Color(0xFFD0212E))
//                .padding(horizontal = 12.dp, vertical = 4.dp),
//        ) {
//            Text("● 直播中", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
//        }
    }
}

@Composable
private fun RuntimeVideoPlayer(
    active: Boolean,
    videoUrl: String,
    startPositionMs: Long,
    autoPlay: Boolean,
    playFocusRequester: FocusRequester?,
    fullscreenFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onFullscreen: (positionMs: Long, shouldResume: Boolean) -> Unit,
) {
    val controller = rememberGSYPlayerController(
        url = videoUrl,
        title = "康巴党旗红",
        // 首播在下方配置 seek 后由首页的 Lifecycle 逻辑启动，避免先从 0 播放一帧。
        autoPlay = false,
        // 首页自行按照 active 与 Lifecycle 控制暂停/恢复；不要再由库的全局
        // GSYVideoManager 同时干预，避免全屏克隆与首页 Surface 争夺播放状态。
        autoPauseResume = false,
    )
    val snapshot by controller.snapshot
    val lifecycleOwner = LocalLifecycleOwner.current
    var appInForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var previousPlaybackAllowed by remember(controller) { mutableStateOf<Boolean?>(null) }
    var resumeAfterInterruption by remember(controller) { mutableStateOf(autoPlay) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> appInForeground = true
                Lifecycle.Event.ON_PAUSE -> appInForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val playbackAllowed = active && appInForeground
    // 在准备播放前完成 HLS 类型与续播进度配置。全屏返回后的会话是一个全新
    // controller，因此不会保留已被全屏窗口克隆过的 Exo 输出 Surface。
    LaunchedEffect(controller, videoUrl, startPositionMs) {
        controller.setOverrideExtension(
            if (isHlsVideoUrl(videoUrl)) "m3u8" else null,
        )
        controller.setSeekOnStart(startPositionMs.coerceAtLeast(0L))
    }
    LaunchedEffect(controller, playbackAllowed) {
        val wasAllowed = previousPlaybackAllowed
        if (!playbackAllowed) {
            if (wasAllowed == true) {
                resumeAfterInterruption = when (controller.withHost { it.currentState }) {
                    GSYVideoView.CURRENT_STATE_PREPAREING,
                    GSYVideoView.CURRENT_STATE_PLAYING,
                    GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START,
                    -> true
                    else -> false
                }
            }
            controller.setStartAfterPrepared(false)
            controller.pause()
        } else if (wasAllowed != true) {
            controller.setStartAfterPrepared(resumeAfterInterruption)
            if (resumeAfterInterruption) {
                when (controller.withHost { it.currentState }) {
                    GSYVideoView.CURRENT_STATE_PAUSE -> controller.resume()
                    GSYVideoView.CURRENT_STATE_PREPAREING,
                    GSYVideoView.CURRENT_STATE_PLAYING,
                    GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START,
                    -> Unit
                    else -> controller.play()
                }
            }
        }
        previousPlaybackAllowed = playbackAllowed
    }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    Box(Modifier.fillMaxSize()) {
        GSYPlayerSurface(controller, Modifier.matchParentSize())

        if (snapshot.state == GSYPlayState.Idle ||
            snapshot.state == GSYPlayState.Preparing ||
            snapshot.state == GSYPlayState.Error ||
            snapshot.state == GSYPlayState.Completed
        ) {
            VideoPoster(Modifier.fillMaxSize())
        }

//        if (!snapshot.isPlaying) {
//            PlayerRoundButton(
//                label = if (snapshot.state == GSYPlayState.Preparing) "…" else "▶",
//                onClick = controller::togglePlayPause,
//                modifier = Modifier
//                    .align(Alignment.Center)
//                    .size(64.dp)
//                    .then(
//                        if (playFocusRequester != null) {
//                            Modifier.focusRequester(playFocusRequester)
//                        } else {
//                            Modifier
//                        }
//                    ),
//            )
//        }

        VideoControlBar(
            isPlaying = snapshot.isPlaying,
            currentPosition = snapshot.currentPosition,
            duration = snapshot.duration,
            bufferPercent = snapshot.bufferPercent,
            dragging = dragging,
            dragFraction = dragFraction,
            onTogglePlay = controller::togglePlayPause,
            onDragChanged = { fraction ->
                dragging = true
                dragFraction = fraction
            },
            onDragFinished = { fraction ->
                controller.seekTo((snapshot.duration * fraction).toLong())
                dragging = false
            },
            onFullscreen = {
                onFullscreen(
                    snapshot.currentPosition,
                    snapshot.isPlaying,
                )
            },
            primaryControlFocusRequester = playFocusRequester,
            primaryControlRightFocusRequester = fullscreenFocusRequester,
            fullscreenFocusRequester = fullscreenFocusRequester,
            fullscreenRightFocusRequester = rightFocusRequester,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun isHlsVideoUrl(videoUrl: String): Boolean =
    Uri.parse(videoUrl).path?.endsWith(".m3u8", ignoreCase = true) == true

@Composable
private fun VideoPoster(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.video_image)
        , contentDescription = ""
        , modifier = modifier, contentScale = ContentScale.FillBounds)
}

@Composable
private fun StaticVideoControls(modifier: Modifier = Modifier) {
    Box(Modifier.fillMaxSize()) {
        VideoControlBar(
            isPlaying = false,
            currentPosition = 74_000L,
            duration = 240_000L,
            bufferPercent = 64,
            dragging = false,
            dragFraction = 0f,
            onTogglePlay = {},
            onDragChanged = {},
            onDragFinished = {},
            onFullscreen = {},
            modifier = modifier,
        )
        PlayerRoundButton(
            label = "▶",
            onClick = {},
            modifier = Modifier.align(Alignment.Center).size(64.dp),
        )
    }
}

@Composable
private fun VideoControlBar(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    bufferPercent: Int,
    dragging: Boolean,
    dragFraction: Float,
    onTogglePlay: () -> Unit,
    onDragChanged: (Float) -> Unit,
    onDragFinished: (Float) -> Unit,
    onFullscreen: () -> Unit,
    primaryControlFocusRequester: FocusRequester? = null,
    primaryControlRightFocusRequester: FocusRequester? = null,
    fullscreenFocusRequester: FocusRequester? = null,
    fullscreenRightFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val playedFraction = when {
        dragging -> dragFraction
        duration > 0 -> (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
        else -> 0f
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(
                Brush.verticalGradient(listOf(Color(0x22000000), Color(0xE5101017))),
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VideoBarButton(
            label = if (isPlaying) "Ⅱ" else "▶",
            onClick = onTogglePlay,
            modifier = Modifier
                .size(30.dp)
                .then(
                    if (primaryControlFocusRequester != null) {
                        Modifier.focusRequester(primaryControlFocusRequester)
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (fullscreenFocusRequester != null || primaryControlRightFocusRequester != null) {
                        Modifier.focusProperties {
                            right = fullscreenFocusRequester ?: primaryControlRightFocusRequester!!
                        }
                    } else {
                        Modifier
                    }
                ),
        )
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Box(Modifier.size(7.dp).background(PrimaryRed, CircleShape))
//            Spacer(Modifier.width(5.dp))
//            Text("直播", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
//        }
        SeekBar(
            playedFraction = playedFraction,
            bufferFraction = bufferPercent.coerceIn(0, 100) / 100f,
            onDragChanged = onDragChanged,
            onDragFinished = onDragFinished,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${formatDuration(currentPosition)} / ${formatDuration(duration)}",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 9.sp,
        )
//        VideoBarButton(")))", onClick = {})
        VideoBarButton(
            label = "⛶",
            onClick = onFullscreen,
            modifier = Modifier
                .size(30.dp)
                .then(
                    if (fullscreenFocusRequester != null) {
                        Modifier.focusRequester(fullscreenFocusRequester)
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (primaryControlFocusRequester != null || fullscreenRightFocusRequester != null) {
                        Modifier.focusProperties {
                            primaryControlFocusRequester?.let { left = it }
                            fullscreenRightFocusRequester?.let { right = it }
                        }
                    } else {
                        Modifier
                    }
                ),
        )
    }
}

@Composable
private fun SeekBar(
    playedFraction: Float,
    bufferFraction: Float,
    onDragChanged: (Float) -> Unit,
    onDragFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clickFocusRequester = remember { FocusRequester() }
    Canvas(
        modifier = modifier
            .height(24.dp)
            .focusRequester(clickFocusRequester)
            .logFocusTarget("Home.VideoSeekBar")
            .focusOnClick(clickFocusRequester)
            .pointerInput(clickFocusRequester) {
                detectTapGestures(
                    onPress = { offset ->
                        clickFocusRequester.requestFocus()
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onDragChanged(fraction)
                        tryAwaitRelease()
                        onDragFinished(fraction)
                    },
                )
            }
            .focusable(),
    ) {
        val centerY = size.height / 2f
        val trackHeight = 3.dp.toPx()
        drawRoundRect(
            color = Color(0x55FFFFFF),
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width, trackHeight),
        )
        drawRoundRect(
            color = Color(0x99FFFFFF),
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width * bufferFraction, trackHeight),
        )
        drawRoundRect(
            color = PrimaryRed,
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width * playedFraction, trackHeight),
        )
        drawCircle(PrimaryRed, 5.dp.toPx(), Offset(size.width * playedFraction, centerY))
    }
}

@Composable
private fun PlayerRoundButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableAction(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        normalColor = Color(0xE6FFFFFF),
        focusedColor = BrightGold,
    ) { focused ->
        Text(
            text = label,
            color = Color(0xFF263A71),
            fontSize = if (focused) 27.sp else 24.sp,
            fontWeight = FontWeight.Black,
            modifier = if (label == "▶") Modifier.padding(start = 3.dp) else Modifier,
        )
    }
}

@Composable
private fun VideoBarButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(30.dp),
) {
    FocusableAction(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        normalColor = Color.Transparent,
        focusedColor = Color(0x66FFFFFF),
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FocusableAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    normalColor: Color = Color.Transparent,
    focusedColor: Color = Color(0x33FFFFFF),
    content: @Composable (focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val clickFocusRequester = remember { FocusRequester() }
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, label = "homeActionScale")
    val background by animateColorAsState(if (focused) focusedColor else normalColor, label = "homeActionColor")

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .then(if (focused) Modifier.border(2.dp, Gold, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusRequester(clickFocusRequester)
            .logFocusTarget("Home.Action")
            .focusOnClick(clickFocusRequester)
            .clickable {
                clickFocusRequester.requestFocus()
                onClick()
            }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        content(focused)
    }
}

@Composable
private fun PartyWorkPanel(
    videoControlFocusRequester: FocusRequester,
    topFocusRequester: FocusRequester?,
    firstItemFocusRequester: FocusRequester,
    lowerItemFocusRequester: FocusRequester,
    tabFocusRequester: FocusRequester,
    reviewTabFocusRequester: FocusRequester,
    moreFocusRequester: FocusRequester,
    selectedCategory: PartyWorkCategory,
    feedState: PartyWorkFeedState,
    onCategorySelected: (PartyWorkCategory) -> Unit,
    onRetry: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayItems = when (feedState) {
        PartyWorkFeedState.Loading -> listOf(
            PartyWorkDisplayItem(title = "正在加载${selectedCategory.label}…", publishedAt = ""),
        )

        is PartyWorkFeedState.Loaded -> feedState.articles
            .take(PARTY_WORK_VISIBLE_COUNT)
            .map { article ->
                PartyWorkDisplayItem(
                    id = article.id,
                    title = article.title,
                    publishedAt = article.publishedAt,
                    detailUrl = selectedCategory.detailUrl(article.id),
                )
            }
            .ifEmpty {
                listOf(PartyWorkDisplayItem(title = "暂无${selectedCategory.label}", publishedAt = ""))
            }

        is PartyWorkFeedState.Failed -> listOf(
            PartyWorkDisplayItem(title = feedState.message, publishedAt = "按确认键重试"),
        )
    }

    HomePanel(modifier.focusGroup()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        ) {
            SectionTitle(R.drawable.ic_home_zugongdongtai)
            PartyWorkMoreButton(
                focusRequester = moreFocusRequester,
                leftFocusRequester = reviewTabFocusRequester,
                downFocusRequester = firstItemFocusRequester,
                onClick = onMoreClick,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .padding(horizontal = 4.dp),
        ) {
            PartyWorkTab(
                category = PartyWorkCategory.WorkDynamics,
                selected = selectedCategory == PartyWorkCategory.WorkDynamics,
                focusRequester = tabFocusRequester,
                upFocusRequester = topFocusRequester,
                downFocusRequester = firstItemFocusRequester,
                rightFocusRequester = reviewTabFocusRequester,
                leftFocusRequester = videoControlFocusRequester,
                onClick = { onCategorySelected(PartyWorkCategory.WorkDynamics) },
                modifier = Modifier.width(104.dp),
            )
            PartyWorkTab(
                category = PartyWorkCategory.PioneerCommentary,
                selected = selectedCategory == PartyWorkCategory.PioneerCommentary,
                focusRequester = reviewTabFocusRequester,
                upFocusRequester = topFocusRequester,
                downFocusRequester = firstItemFocusRequester,
                rightFocusRequester = moreFocusRequester,
                leftFocusRequester = tabFocusRequester,
                onClick = { onCategorySelected(PartyWorkCategory.PioneerCommentary) },
                modifier = Modifier.width(104.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            displayItems.forEachIndexed { index, item ->
                PartyWorkCard(
                    item = item,
                    coverImage = selectedCategory.coverImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(
                            if (index == 0) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else if (index == displayItems.lastIndex) {
                                Modifier.focusRequester(lowerItemFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .focusProperties {
                            if (index == 0) {
                                left = videoControlFocusRequester
                                up = tabFocusRequester
                            }
                        },
                    onClick = {
                        if (feedState is PartyWorkFeedState.Failed) {
                            onRetry()
                        } else {
                            item.id?.let(onOpenArticle)
                        }
                    },
                )
                if (index != displayItems.lastIndex) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x30FFE4C0)),
                    )
                }
            }
        }
    }
}

@Composable
private fun PartyWorkTab(
    category: PartyWorkCategory,
    selected: Boolean,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .height(26.dp)
            .background(if (selected) Color(0xFFFFD58B) else Color(0x88EF503B))
            .then(if (focused) Modifier.border(2.dp, Gold, shape) else Modifier)
            .clip(shape)
            .onFocusChanged { focused = it.isFocused }
            .focusRequester(focusRequester)
            .focusProperties {
                upFocusRequester?.let { up = it }
                down = downFocusRequester
                left = leftFocusRequester
                right = rightFocusRequester
            }
            .logFocusTarget("Home.PartyWorkTab[${category.label}]")
            .focusOnClick(focusRequester)
            .clickable {
                focusRequester.requestFocus()
                onClick()
            }
            .focusable()
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = category.label,
            color = if (selected) Color(0xFF8C251D) else Color(0xFFF8D7C8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PartyWorkCard(
    item: PartyWorkDisplayItem,
    @DrawableRes coverImage: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    PartyPanelFocusableItem(
        modifier = modifier,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 3.dp, end = 2.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.publishedAt,
                        color = Color(0xFFFEE5E2),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                PartyWorkBadge(
                    coverImage = coverImage,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun PartyWorkBadge(
    @DrawableRes coverImage: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(coverImage),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .width(58.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, Color(0x55FA6A54), RoundedCornerShape(3.dp)),
    )
}

@Composable
private fun PartyWorkMoreButton(
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (focused) Color(0x44FFFFFF) else Color.Transparent)
            .then(if (focused) Modifier.border(2.dp, Gold, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusRequester(focusRequester)
            .focusProperties {
                left = leftFocusRequester
                down = downFocusRequester
            }
            .logFocusTarget("Home.PartyWorkMore")
            .focusOnClick(focusRequester)
            .clickable {
                focusRequester.requestFocus()
                onClick()
            }
            .focusable()
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            text = "更多+",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private data class PartyWorkDisplayItem(
    val id: String? = null,
    val title: String,
    val publishedAt: String,
    val detailUrl: String? = null,
)

private const val PARTY_WORK_VISIBLE_COUNT = 3

private fun PartyWorkCategory.detailUrl(articleId: String): String = when (this) {
    PartyWorkCategory.WorkDynamics ->
        WORK_DYNAMICS_DETAIL_URL_PREFIX + Uri.encode(articleId) + WORK_DYNAMICS_DETAIL_URL_SUFFIX
    PartyWorkCategory.PioneerCommentary -> PIONEER_COMMENTARY_DETAIL_URL
}

@Composable
private fun PartyPanelFocusableItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val clickFocusRequester = remember { FocusRequester() }
    val scale by animateFloatAsState(
        if (focused) 1.04f else 1f,
        label = "partyPanelItemScale",
    )
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (focused) Color(0x33FFFFFF) else Color.Transparent)
            .then(if (focused) Modifier.border(2.dp, Gold, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusRequester(clickFocusRequester)
            .logFocusTarget("Home.PartyPanelItem")
            .focusOnClick(clickFocusRequester)
            .clickable {
                clickFocusRequester.requestFocus()
                onClick()
            }
            .focusable(),
    ) {
        content()
    }
}

@Composable
private fun TopicPanel(
    lastTopicFocusRequester: FocusRequester,
    videoControlFocusRequester: FocusRequester,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    HomePanel(modifier) {
        SectionTitle(R.drawable.ic_home_zhuantizhuanlan)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxSize().padding(0.dp,0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusProperties { up = videoControlFocusRequester },
                image = R.drawable.ic_home_zhuanlan_01,
                onClick = { onOpenUrl(PARTY_PIONEER_MOBILE_URL) },
            )
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusProperties { up = videoControlFocusRequester },
                image = R.drawable.ic_home_zhuanlan_02,
                onClick = { onOpenUrl(PARTY_MEMBER_LEARNING_URL) },
            )
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(lastTopicFocusRequester)
                    .focusProperties { up = videoControlFocusRequester },
                image = R.drawable.ic_home_zhuanlan_03,
                onClick = { onOpenUrl(KANGBA_PARTY_FLAG_URL) },
            )
        }
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier,
    @DrawableRes image: Int,
    onClick: () -> Unit,
) {
    FocusableTile(modifier, onClick) {
        Image(
            painter = painterResource(image),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.height(65.dp).fillMaxWidth(),
            contentDescription = null,
        )
    }
}

@Composable
private fun CoursewarePanel(
    partyBuildingFocusRequester: FocusRequester,
    onCoursewareClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    HomePanel(modifier) {
        SectionTitle(R.drawable.ic_home_shipinkejian)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusProperties { up = partyBuildingFocusRequester },
                image = R.drawable.ic_home_kejian_01,
                onClick = { onCoursewareClick(1) },
            )
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusProperties { up = partyBuildingFocusRequester },
                image = R.drawable.ic_home_kejian_02,
                onClick = { onCoursewareClick(2) },
            )
            FeatureCard(
                    modifier = Modifier
                        .weight(1f)
                        .focusProperties { up = partyBuildingFocusRequester },
            image = R.drawable.ic_home_kejian_03,
            onClick = { onCoursewareClick(3) },
            )

//            CourseCard(
//                "",
//                "",
//                Color(0xFFA31712),
//                R.drawable.ic_home_kejian_01,
//                Modifier.focusProperties { left = leftTopicFocusRequester },
//                onClick = { onCoursewareClick(1) },
//            )
//            CourseCard("",
//                "",
//                Color(0xFF294581),
//                R.drawable.ic_home_kejian_02,
//                Modifier,
//                onClick = { onCoursewareClick(2) },
//            )
//            CourseCard("",
//                "",
//                Color(0xFF185E2F),
//                R.drawable.ic_home_kejian_03,
//                Modifier,
//                onClick = { onCoursewareClick(3) },
//            )
        }
    }
}

@Composable
private fun HomePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PanelRed)
            .border(1.dp, PanelStroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = content,
    )
}

@Composable
private fun SectionTitle(@DrawableRes image: Int) {
    Image(painterResource(image), contentDescription = ""
        , contentScale = ContentScale.FillBounds
        , modifier = Modifier.width(299.dp).height(28.dp))
}

@Composable
private fun CourseCard(
    title: String,
    subtitle: String,
    titleTextColor: Color,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    FocusableTile(modifier, onClick) {
        Box(
            Modifier
                .size( 65.dp)
        ) {
            Image(painterResource(icon), contentDescription = "", contentScale = ContentScale.Fit, modifier = Modifier.height(65.dp))
            Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = titleTextColor, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(subtitle, color = Color(0xFFEC9649), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FocusableTile(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val clickFocusRequester = remember { FocusRequester() }
    val scale by animateFloatAsState(if (focused) 1.10f else 1f, label = "homeTileScale")

    Box(
        modifier = modifier
            .fillMaxHeight()
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp))
            .then(if (focused) Modifier.border(3.dp, BrightGold, RoundedCornerShape(8.dp)) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .focusRequester(clickFocusRequester)
            .logFocusTarget("Home.Tile")
            .focusOnClick(clickFocusRequester)
            .clickable {
                clickFocusRequester.requestFocus()
                onClick()
            }
            .focusable(),
    ) {
        content()
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1_000L).coerceAtLeast(0L)
    return "%02d:%02d".format(seconds / 60L, seconds % 60L)
}

@Preview(
    name = "原生首页",
    widthDp = 1280,
    heightDp = 600,
    showBackground = true,
)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(Modifier.fillMaxSize())
    }
}
