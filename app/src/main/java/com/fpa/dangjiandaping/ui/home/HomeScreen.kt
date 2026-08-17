package com.fpa.dangjiandaping.ui.home

import android.R.attr.foreground
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.text.style.TextAlign
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
import com.fpa.dangjiandaping.ui.focus.focusOnClick
import com.fpa.dangjiandaping.ui.web.WebViewDialog
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayState
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayerSurface
import com.shuyu.gsyvideoplayer.compose.native_.rememberGSYPlayerController
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val DEFAULT_HOME_VIDEO_URL = "https://www.cdlhyj.com/web/html/video/wsp/Example_10_Leng_Xiaokun_Making_Chinese_Robots_Serve_Production_and_Life.mp4"
private const val PARTY_PIONEER_MOBILE_URL = "https://12371.people.com.cn/"
private const val PARTY_MEMBER_LEARNING_URL = "https://www.scycjy.gov.cn/dyxx_mys.html"
private const val KANGBA_PARTY_FLAG_URL = "http://dangjian-app.people.cn/"
private const val CADRE_APPOINTMENT_URL =
    "http://renshi.people.com.cn/n1/2026/0811/c139617-40777396.html"

private val Gold = Color(0xFFFFD889)
private val BrightGold = Color(0xFFFFD186)
private val PanelRed = Color(0xB078101B)
private val PanelStroke = Color(0x90E56E59)
private val PrimaryRed = Color(0xFFD71920)

internal data class PartyStat(
    @DrawableRes val icon: Int,
    val title: String,
    val count: Int,
    val channelId: Int,
)

internal data class PartyStatUpdate(val channelId: Int, val newsCount: Int)

private const val PARTY_STATISTICS_URL =
    "https://www.scycjy.gov.cn/api/services/app/NewsService/GetChildChannelList?Id=8545"

internal val defaultPartyStats = listOf(
    PartyStat(R.drawable.ic_home_dangjian_nongcun, "农村党建", 126, 10899),
    PartyStat(R.drawable.ic_home_chengshishequ, "城市社区", 54, 10897),
    PartyStat(R.drawable.ic_home_jiguandangjian, "\u3000机关党建\u3000", 105, 10905),
    PartyStat(R.drawable.ic_home_shiyedanwei, "事业单位", 126, 10909),
    PartyStat(R.drawable.ic_home_qiyedangjian, "企业党建", 26, 10903),
    PartyStat(R.drawable.ic_home_xinxinglingyu, "新兴领域", 154, 10901),
    PartyStat(R.drawable.ic_home_dangyuanjiaoyu, "党员教育动态", 105, 8565),
    PartyStat(R.drawable.ic_home_dangjian_qita, "其\u3000\u3000他", 105, 10913),
)

private data class CadreTask(val name: String, val duty: String, val date: String)

private val cadreTasks = listOf(
    CadreTask("蒋玮", "拟任中国老龄协会会长", "2026-08-11"),
    CadreTask("罗文", "免去国家市场监督管理总局局长", "2026-08-11"),
    CadreTask("曲几扎波", "拟任副县级领导职务", "2026-06-12"),
)

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    videoUrl: String = DEFAULT_HOME_VIDEO_URL,
    partyStats: List<PartyStat> = defaultPartyStats,
    contentFocusRequester: FocusRequester? = null,
    onRequestTabFocus: () -> Unit = {},
    onCoursewareClick: (Int) -> Unit = {},
    onPartyBuildingClick: (Int) -> Unit = {},
) {
    val lastTopicFocusRequester = remember { FocusRequester() }
    val fullscreenFocusRequester = remember { FocusRequester() }
    val videoControlFocusRequester = remember { FocusRequester() }
    val firstPartyBuildingFocusRequester = remember { FocusRequester() }
    val cadreAppointmentFocusRequester = remember { FocusRequester() }
    var webViewDialogUrl by remember { mutableStateOf<String?>(null) }

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
                    playFocusRequester = videoControlFocusRequester,
                    fullscreenFocusRequester = fullscreenFocusRequester,
                    rightFocusRequester = firstPartyBuildingFocusRequester,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                PartyWorkPanel(
                    partyStats = partyStats,
                    videoControlFocusRequester = videoControlFocusRequester,
                    topFocusRequester = contentFocusRequester,
                    firstItemFocusRequester = firstPartyBuildingFocusRequester,
                    cadreAppointmentFocusRequester = cadreAppointmentFocusRequester,
                    onPartyBuildingClick = onPartyBuildingClick,
                    onCadreClick = { webViewDialogUrl = CADRE_APPOINTMENT_URL },
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
                    cadreAppointmentFocusRequester = cadreAppointmentFocusRequester,
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

internal suspend fun fetchPartyStats(): Map<String, PartyStatUpdate>? = withContext(Dispatchers.IO) {
    runCatching {
        val response = (URL(PARTY_STATISTICS_URL).openConnection() as HttpURLConnection).run {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            inputStream.bufferedReader().use { it.readText() }.also { disconnect() }
        }
        val children = JSONObject(response)
            .optJSONObject("result")
            ?.optJSONArray("children")
            ?: return@runCatching emptyMap()
        val titleByChannel = mapOf(
            "农村" to "农村党建",
            "城市社区" to "城市社区",
            "机关党员" to "机关党建",
            "事业单位" to "事业单位",
            "企业" to "企业党建",
            "新兴领域" to "新兴领域",
            "党员教育动态" to "党员教育动态",
        )

        buildMap {
            for (index in 0 until children.length()) {
                val child = children.optJSONObject(index) ?: continue
                val title = titleByChannel[child.optString("channelName")] ?: continue
                put(
                    title,
                    PartyStatUpdate(
                        channelId = child.optInt("id"),
                        newsCount = child.optInt("newsCount"),
                    ),
                )
            }
        }
    }.getOrNull()
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
                text = "《国家综合气象观测系统建设“十五五”规划》印发",
                onClick = {
                    onOpenUrl(
                        "https://www.sc.gov.cn/10462/13241/2026/8/13/cf303f8b63be4eac97bf197d8cee3c5f.shtml",
                    )
                },
                modifier = Modifier.weight(1f),
                focusRequester = firstItemFocusRequester,
                downFocusRequester = firstItemDownFocusRequester,
            )
            TickerDivider()
            TickerItem(
                text = "张道平在基层一线调研慰问时强调：扎实抓好防汛抗旱度夏保供工作 确保人民群众生命财产安全生产生活有序",
                modifier = Modifier.weight(1.2f),
                onClick = {
                    onOpenUrl(
                        "https://www.scmstv.cn/#/detail2?id=2087112195331764226&source=%E4%B9%A6%E8%AE%B0%E5%8A%A8%E6%80%81&sPath=%2Fmingsheng&isAutoRoute=true&isFromLanmu=false&type=1",
                    )
                },
            )
            TickerDivider()
            TickerItem(
                text = "张道平前往部分市级部门走访调研时强调：锚定目标锻长补短加力攻坚 奋力推动经济持续向新向优向好发展",
                modifier = Modifier.weight(0.82f),
                onClick = {
                    onOpenUrl(
                        "https://www.scmstv.cn/#/detail2?id=2086773237041926145&source=%E4%B9%A6%E8%AE%B0%E5%8A%A8%E6%80%81&sPath=%2Fmingsheng&isAutoRoute=true&isFromLanmu=false&type=1",
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
    playFocusRequester: FocusRequester? = null,
    fullscreenFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val inPreview = LocalInspectionMode.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .border(3.dp, Color(0xFFE8B56F), RoundedCornerShape(14.dp)),
    ) {
        if (inPreview) {
            VideoPoster(Modifier.fillMaxSize())
            StaticVideoControls(Modifier.align(Alignment.BottomCenter))
        } else {
            RuntimeVideoPlayer(
                active = active,
                videoUrl = videoUrl,
                playFocusRequester = playFocusRequester,
                fullscreenFocusRequester = fullscreenFocusRequester,
                rightFocusRequester = rightFocusRequester,
            )
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
    playFocusRequester: FocusRequester?,
    fullscreenFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val controller = rememberGSYPlayerController(
        url = videoUrl,
        title = "康巴党旗红",
        autoPlay = true,
    )
    val snapshot by controller.snapshot
    val lifecycleOwner = LocalLifecycleOwner.current
    var appInForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var previousPlaybackAllowed by remember(controller) { mutableStateOf<Boolean?>(null) }
    var resumeAfterInterruption by remember(controller) { mutableStateOf(true) }

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
    // GSY 已在应用入口切换到 Exo2/Media3 内核。对 HLS 地址再显式声明格式，
    // 可避免带 query 参数的 m3u8 链接被错误按普通媒体源解析。
    LaunchedEffect(controller, videoUrl) {
        controller.setOverrideExtension(
            if (isHlsVideoUrl(videoUrl)) "m3u8" else null,
        )
    }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var surfaceGeneration by remember { mutableStateOf(0) }
    var rebindSurfaceAfterFullscreen by remember { mutableStateOf(false) }

    BackHandler(enabled = controller.isFullscreen && activity != null) {
        activity?.let {
            controller.exitFullscreen(it)
            rebindSurfaceAfterFullscreen = true
        }
    }

    LaunchedEffect(rebindSurfaceAfterFullscreen) {
        if (rebindSurfaceAfterFullscreen) {
            // Let the fullscreen surface finish detaching before attaching a fresh inline one.
            withFrameNanos { }
            withFrameNanos { }
            surfaceGeneration += 1
            rebindSurfaceAfterFullscreen = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        key(surfaceGeneration) {
            GSYPlayerSurface(controller, Modifier.matchParentSize())
        }

        if (snapshot.state == GSYPlayState.Idle ||
            snapshot.state == GSYPlayState.Preparing ||
            snapshot.state == GSYPlayState.Error
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
            onFullscreen = { activity?.let { controller.enterFullscreen(it) } },
            primaryControlFocusRequester = playFocusRequester,
            primaryControlRightFocusRequester = rightFocusRequester,
            fullscreenFocusRequester = fullscreenFocusRequester,
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
                    if (primaryControlRightFocusRequester != null) {
                        Modifier.focusProperties { right = primaryControlRightFocusRequester }
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
//        VideoBarButton(
//            label = "⛶",
//            onClick = onFullscreen,
//            modifier = Modifier
//                .size(30.dp)
//                .then(
//                    if (fullscreenFocusRequester != null) {
//                        Modifier.focusRequester(fullscreenFocusRequester)
//                    } else {
//                        Modifier
//                    }
//                ),
//        )
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
    partyStats: List<PartyStat>,
    videoControlFocusRequester: FocusRequester,
    topFocusRequester: FocusRequester?,
    firstItemFocusRequester: FocusRequester,
    cadreAppointmentFocusRequester: FocusRequester,
    onPartyBuildingClick: (Int) -> Unit,
    onCadreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sectionHorizontalPadding = 4.dp

    HomePanel(modifier.focusGroup()) {
        SectionTitle(R.drawable.ic_home_jiceng)
        Spacer(Modifier.height(3.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sectionHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PartyStatRow(
                stats = partyStats.take(4),
                leftFocusRequester = videoControlFocusRequester,
                initialFocusRequester = firstItemFocusRequester,
                upFocusRequester = topFocusRequester,
                onItemClick = onPartyBuildingClick,
            )
            PartyStatRow(
                stats = partyStats.drop(4).take(4),
                leftFocusRequester = videoControlFocusRequester,
                onItemClick = onPartyBuildingClick,
            )
        }

        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionTitle(R.drawable.ic_home_ganburenmian) }
        }
        Spacer(Modifier.height(2.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.86f)
                .padding(horizontal = sectionHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(
                space = 5.dp,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            cadreTasks.take(2).forEachIndexed { index, task ->
                PartyPanelFocusableItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (index == 1) {
                                Modifier.focusRequester(cadreAppointmentFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                        .focusProperties { left = videoControlFocusRequester },
                    onClick = onCadreClick,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(5.dp).background(Color(0xFFF6CD8B)))
                        Text(
                            task.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp).width(76.dp),
                        )
                        Text(
                            task.duty,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            task.date,
                            color = Color.White,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyStatRow(
    stats: List<PartyStat>,
    leftFocusRequester: FocusRequester,
    initialFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    onItemClick: (Int) -> Unit,
) {
    if (stats.isEmpty()) return

    fun itemModifier(index: Int, base: Modifier): Modifier {
        var result = base
        if (index == 0 && initialFocusRequester != null) {
            result = result.focusRequester(initialFocusRequester)
        }
        return result.focusProperties {
            if (index == 0) {
                left = leftFocusRequester
            }
            if (upFocusRequester != null) {
                up = upFocusRequester
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        stats.forEachIndexed { index, stat ->
            PartyStatItem(
                stat = stat,
                modifier = itemModifier(index, Modifier),
                onClick = { onItemClick(stat.channelId) },
            )
        }
    }
}

@Composable
private fun PartyStatItem(
    stat: PartyStat,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    PartyPanelFocusableItem(
        modifier = modifier,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(stat.icon),
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 32.dp, height = 28.dp),
                )
                Text(
                    text = partyStatDisplayTitle(stat.title),
                    color = Color(0xFFF8EAEA),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun partyStatDisplayTitle(title: String): String = when (title) {
    "农村党建" -> "农　村"
    "机关党建" -> "机关党员"
    "企业党建" -> "企　业"
    "其他" -> "其　他"
    else -> title
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
    cadreAppointmentFocusRequester: FocusRequester,
    onCoursewareClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    HomePanel(modifier) {
        SectionTitle(R.drawable.ic_home_zuixinkejian)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusProperties { up = cadreAppointmentFocusRequester },
                image = R.drawable.ic_home_kejian_01,
                onClick = { onCoursewareClick(1) },
            )
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusProperties { up = cadreAppointmentFocusRequester },
                image = R.drawable.ic_home_kejian_02,
                onClick = { onCoursewareClick(2) },
            )
            FeatureCard(
                    modifier = Modifier
                        .weight(1f)
                        .focusProperties { up = cadreAppointmentFocusRequester },
            image = R.drawable.ic_home_kejian_04,
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
