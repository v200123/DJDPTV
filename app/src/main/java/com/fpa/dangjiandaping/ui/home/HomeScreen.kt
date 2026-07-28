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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.R
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayState
import com.shuyu.gsyvideoplayer.compose.native_.GSYPlayerSurface
import com.shuyu.gsyvideoplayer.compose.native_.rememberGSYPlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val DEFAULT_HOME_VIDEO_URL = "https://imgcdn.scdjw.com.cn/video/d28b65ed-ec69-490d-b339-9380572419f6.mp4"

private val Gold = Color(0xFFFFD889)
private val BrightGold = Color(0xF8EAEA)
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
    PartyStat(R.drawable.ic_home_jiguandangjian, "机关党建", 105, 10905),
    PartyStat(R.drawable.ic_home_shiyedanwei, "事业单位", 126, 10909),
    PartyStat(R.drawable.ic_home_qiyedangjian, "企业党建", 26, 10903),
    PartyStat(R.drawable.ic_home_xinxinglingyu, "新兴领域", 154, 10901),
    PartyStat(R.drawable.ic_home_dangyuanjiaoyu, "党员教育动态", 105, 8565),
)

private data class CadreTask(val name: String, val duty: String, val date: String)

private val cadreTasks = listOf(
    CadreTask("李尚谦", "拟任正县级领导职务", "2026-07-13"),
    CadreTask("祁光清", "拟任县（市）党委副书记", "2026-07-13"),
    CadreTask("曲几扎波", "拟任副县级领导职务", "2026-06-12"),
)

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    videoUrl: String = DEFAULT_HOME_VIDEO_URL,
    partyStats: List<PartyStat> = defaultPartyStats,
    contentFocusRequester: FocusRequester? = null,
    onRequestTabFocus: () -> Unit = {},
) {
    val lastTopicFocusRequester = remember { FocusRequester() }
    val fullscreenFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
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
                .padding(horizontal = 51.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NewsTicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(218.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeVideoPlayer(
                    videoUrl = videoUrl,
                    playFocusRequester = contentFocusRequester,
                    fullscreenFocusRequester = fullscreenFocusRequester,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                PartyWorkPanel(
                    partyStats = partyStats,
                    fullscreenFocusRequester = fullscreenFocusRequester,
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
                    fullscreenFocusRequester = fullscreenFocusRequester,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                CoursewarePanel(
                    leftTopicFocusRequester = lastTopicFocusRequester,
                    modifier = Modifier
                        .weight(1.08f)
                        .fillMaxHeight(),
                )
            }
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
private fun NewsTicker(modifier: Modifier = Modifier) {
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
            TickerItem("关于组织开展2024年度党员教育培训工作", Modifier.weight(1f))
            TickerDivider()
            TickerItem("雅江县：“三维赋能”让党员教育在高原落地生根", Modifier.weight(1.2f))
            TickerDivider()
            TickerItem("康定市：建强农业实用人才队伍……", Modifier.weight(0.82f))
        }
    }
}

@Composable
private fun TickerItem(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color(0xFF650D0B),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = 12.dp),
    )
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
    videoUrl: String,
    playFocusRequester: FocusRequester? = null,
    fullscreenFocusRequester: FocusRequester? = null,
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
                videoUrl = videoUrl,
                playFocusRequester = playFocusRequester,
                fullscreenFocusRequester = fullscreenFocusRequester,
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
    videoUrl: String,
    playFocusRequester: FocusRequester?,
    fullscreenFocusRequester: FocusRequester?,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val controller = rememberGSYPlayerController(
        url = videoUrl,
        title = "康巴党旗红",
        autoPlay = false,
    )
    // GSY 已在应用入口切换到 Exo2/Media3 内核。对 HLS 地址再显式声明格式，
    // 可避免带 query 参数的 m3u8 链接被错误按普通媒体源解析。
    LaunchedEffect(controller, videoUrl) {
        controller.setOverrideExtension(
            if (isHlsVideoUrl(videoUrl)) "m3u8" else null,
        )
    }
    val snapshot by controller.snapshot
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

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        key(surfaceGeneration) {
            GSYPlayerSurface(controller, Modifier.fillMaxSize())
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
            primaryControlFocusRequester =
                if (snapshot.isPlaying) playFocusRequester else null,
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
    Canvas(
        modifier = modifier
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onDragChanged(fraction)
                        tryAwaitRelease()
                        onDragFinished(fraction)
                    },
                )
            },
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
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, label = "homeActionScale")
    val background by animateColorAsState(if (focused) focusedColor else normalColor, label = "homeActionColor")

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .then(if (focused) Modifier.border(2.dp, Gold, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        content(focused)
    }
}

@Composable
private fun PartyWorkPanel(
    partyStats: List<PartyStat>,
    fullscreenFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    HomePanel(modifier.focusGroup()) {
        SectionTitle(R.drawable.ic_home_jiceng)
        Spacer(Modifier.height(3.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                ,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Row(Modifier.fillMaxWidth()) {
                partyStats.take(4).forEach { stat ->
                    PartyStatItem(
                        stat,
                        Modifier
                            .weight(1f)
                            .then(
                                if (stat.title == "农村党建" || stat.title == "企业党建") {
                                    Modifier.focusProperties { left = fullscreenFocusRequester }
                                } else {
                                    Modifier
                                }
                            ),
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                partyStats.drop(4).forEach { stat ->
                    PartyStatItem(
                        stat = stat,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {  SectionTitle(R.drawable.ic_home_ganburenmian) }
            PartyPanelFocusableItem(
                modifier = Modifier.focusProperties { left = fullscreenFocusRequester },
            ) {
//                Text(
//                    "更多 >>",
//                    color = Color.White,
//                    fontSize = 11.sp,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
//                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Column(
            modifier = Modifier.weight(0.86f),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            cadreTasks.forEach { task ->
                PartyPanelFocusableItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { left = fullscreenFocusRequester },
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
                        Text(task.date, color = Color.White, fontSize = 10.sp)
                    }
                }
            }
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
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(stat.icon), contentDescription = "", contentScale = ContentScale.Fit, modifier = Modifier.size(42.dp,40.dp))
            Spacer(Modifier.width(7.dp))
            Column {
                Text(
                    stat.title,
                    color = Color(0xFFF8EAEA),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(buildAnnotatedString {
                            append("${stat.count}")
                            withStyle(SpanStyle(fontSize = 10.sp)){
                                append("篇")
                            }
                        }
                        , color = Color(0xFFF6CD8B), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun PartyPanelFocusableItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "partyPanelItemScale")
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(if (focused) Color(0x33FFFFFF) else Color.Transparent)
            .then(if (focused) Modifier.border(2.dp, Gold, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
    ) {
        content()
    }
}

@Composable
private fun TopicPanel(
    lastTopicFocusRequester: FocusRequester,
    fullscreenFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    HomePanel(modifier) {
        SectionTitle(R.drawable.ic_home_zhuantizhuanlan)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxSize().padding(0.dp,0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusProperties { up = fullscreenFocusRequester },
                R.drawable.ic_home_zhuanlan_01
            )
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusProperties { up = fullscreenFocusRequester },
                R.drawable.ic_home_zhuanlan_02

            )
            FeatureCard(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(lastTopicFocusRequester)
                    .focusProperties { up = fullscreenFocusRequester },
                R.drawable.ic_home_zhuanlan_03

            )
        }
    }
}

@Composable
private fun FeatureCard(modifier: Modifier, @DrawableRes image: Int) {
    FocusableTile(modifier) {
        Image(
            painter = painterResource(image),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize().height(65.dp),
            contentDescription = null,
        )
    }
}

@Composable
private fun CoursewarePanel(
    leftTopicFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    HomePanel(modifier) {
        SectionTitle(R.drawable.ic_home_zuixinkejian)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CourseCard(
                "中组部",
                "最新课件28个",
                Color(0xFFA31712),
                R.drawable.ic_home_kejian_01,
                Modifier.focusProperties { left = leftTopicFocusRequester },
            )
            CourseCard("省委组织部", "最新课件28个", Color(0xFF294581), R.drawable.ic_home_kejian_02,  Modifier)
            CourseCard("州委组织部", "最新课件28个", Color(0xFF185E2F), R.drawable.ic_home_kejian_03,  Modifier)
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
        , modifier = Modifier.height(29.dp))
}

@Composable
private fun CourseCard(
    title: String,
    subtitle: String,
    titleTextColor: Color,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
) {
    FocusableTile(modifier) {
        Box(
            Modifier
                .size(135.dp, 65.dp)
        ) {
            Image(painterResource(icon), contentDescription = "", contentScale = ContentScale.FillBounds, modifier = Modifier.size(135.dp, 65.dp))
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
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "homeTileScale")

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp))
            .then(if (focused) Modifier.border(3.dp, BrightGold, RoundedCornerShape(8.dp)) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable { }
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
