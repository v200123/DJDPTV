package com.fpa.dangjiandaping.ui.web

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.ui.focus.focusOnClick
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val NEWS_FILE_DOWNLOAD_BASE_URL =
    "https://www.xyxf.gov.cn/prod-api/download"

internal data class NewsVideo(
    val fileId: String,
    val type: String,
    val typeName: String,
    val filename: String,
) {
    val playbackUrl: String
        get() = "$NEWS_FILE_DOWNLOAD_BASE_URL/${Uri.encode(fileId)}"

    val displayName: String
        get() = typeName.ifBlank {
            filename.ifBlank { "音频播报" }
        }

    val isAudio: Boolean
        get() = type.equals("audio", ignoreCase = true) ||
            filename.endsWith(".mp3", ignoreCase = true) ||
            filename.endsWith(".wav", ignoreCase = true) ||
            filename.endsWith(".m4a", ignoreCase = true)
}

internal data class NewsDetail(
    val id: String,
    val title: String,
    val content: String,
    val source: String,
    val author: String,
    val publishTime: String,
    val videos: List<NewsVideo>,
)

internal fun parseNewsDetail(json: String): NewsDetail {
    val record = unwrapNewsRecord(JSONObject(json))
    return NewsDetail(
        id = record.optString("id"),
        title = record.optString("title").ifBlank { "新闻详情" },
        content = record.optString("content").ifBlank { "<p>暂无正文内容</p>" },
        source = record.optString("source").trim(),
        author = record.optString("author").trim(),
        publishTime = record.optString("pubTime").ifBlank {
            record.optString("publishTime")
        },
        videos = extractNewsVideos(record),
    )
}

private fun unwrapNewsRecord(root: JSONObject): JSONObject {
    root.optJSONObject("record")?.let { return it }
    val data = root.optJSONObject("data") ?: return root
    data.optJSONObject("record")?.let { return it }
    data.optJSONArray("records")?.optJSONObject(0)?.let { return it }
    return data
}

private fun extractNewsVideos(record: JSONObject): List<NewsVideo> {
    val videosValue = record.opt("videos")
    val videos = when (videosValue) {
        is JSONArray -> videosValue
        is JSONObject -> JSONArray().put(videosValue)
        is String -> runCatching { JSONArray(videosValue) }.getOrNull()
        else -> null
    } ?: return emptyList()

    val result = linkedMapOf<String, NewsVideo>()
    for (index in 0 until videos.length()) {
        val item = videos.optJSONObject(index) ?: continue
        val fileId = item.optString("fileId").trim()
        if (fileId.isEmpty()) continue

        result.putIfAbsent(
            fileId,
            NewsVideo(
                fileId = fileId,
                filename = item.optString("filename").trim(),
                type = item.optString("type").trim(),
                typeName = item.optString("typeName").trim(),
            ),
        )
    }
    return result.values.toList()
}

@Composable
internal fun NewsDetailDialog(
    news: NewsDetail,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        NewsDetailDialogContent(
            news = news,
            onDismiss = onDismiss,
            requestInitialFocus = true
        )
    }
}

@Composable
private fun NewsDetailDialogContent(
    news: NewsDetail,
    onDismiss: () -> Unit,
    requestInitialFocus: Boolean
) {
    var activeAudioFileId by remember(news.id, news.title) { mutableStateOf<String?>(null) }
    val audioVideos = remember(news.videos) {
        news.videos.filter(NewsVideo::isAudio)
    }
    val closeFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 42.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFCFCFC))
                .padding(horizontal = 30.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = news.title,
                    color = Color(0xFFD7142B),
                    fontSize = 27.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 72.dp, vertical = 4.dp)
                )
                TvActionButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .focusRequester(closeFocusRequester)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            NewsMetadata(news)

            Spacer(modifier = Modifier.height(18.dp))

            if (audioVideos.isEmpty()) {
                Text(
                    text = "当前新闻数据未提供音频地址",
                    color = Color(0xFF777777),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    audioVideos.take(2).forEach { audio ->
                        NewsAudioPlayer(
                            audio = audio,
                            active = activeAudioFileId == audio.fileId,
                            onToggle = {
                                activeAudioFileId =
                                    if (activeAudioFileId == audio.fileId) null else audio.fileId
                            },
                            onPlaybackFinished = {
                                if (activeAudioFileId == audio.fileId) {
                                    activeAudioFileId = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NewsHtmlContent(
                html = news.content,
                onNavigateUp = closeFocusRequester::requestFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(6.dp))
            )
        }
    }

    LaunchedEffect(news.id, news.title, requestInitialFocus) {
        if (requestInitialFocus) closeFocusRequester.requestFocus()
    }
}

@Composable
private fun NewsMetadata(news: NewsDetail) {
    val metadata = buildList {
        if (news.source.isNotBlank()) add("来源：${news.source}")
        if (news.author.isNotBlank()) add("责编：${news.author}")
        if (news.publishTime.isNotBlank()) add("发布时间：${news.publishTime}")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F1F1))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = metadata.joinToString("       "),
            color = Color(0xFF666666),
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NewsAudioPlayer(
    audio: NewsVideo,
    active: Boolean,
    onToggle: () -> Unit,
    onPlaybackFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = audio.displayName,
                color = Color(0xFF202020),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            TvActionButton(
                text = "▶  0:00 / 6:18",
                onClick = onToggle,
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    val mediaPlayer = remember(audio.fileId) { MediaPlayer() }
    var prepared by remember(audio.fileId) { mutableStateOf(false) }
    var failed by remember(audio.fileId) { mutableStateOf(false) }
    var durationMs by remember(audio.fileId) { mutableIntStateOf(0) }
    var positionMs by remember(audio.fileId) { mutableIntStateOf(0) }

    DisposableEffect(mediaPlayer, audio.fileId) {
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        mediaPlayer.setOnPreparedListener { player ->
            durationMs = player.duration.coerceAtLeast(0)
            prepared = true
        }
        mediaPlayer.setOnCompletionListener {
            positionMs = durationMs
            onPlaybackFinished()
        }
        mediaPlayer.setOnErrorListener { _, _, _ ->
            failed = true
            onPlaybackFinished()
            true
        }
        runCatching {
            mediaPlayer.setDataSource(audio.playbackUrl)
            mediaPlayer.prepareAsync()
        }.onFailure {
            failed = true
        }

        onDispose {
            runCatching { mediaPlayer.stop() }
            mediaPlayer.reset()
            mediaPlayer.release()
        }
    }

    LaunchedEffect(active, prepared, failed, mediaPlayer) {
        if (!prepared || failed) return@LaunchedEffect
        if (active) {
            runCatching { mediaPlayer.start() }.onFailure { failed = true }
        } else {
            runCatching {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            }
        }
    }

    LaunchedEffect(active, prepared, failed, mediaPlayer) {
        while (active && prepared && !failed) {
            positionMs = runCatching { mediaPlayer.currentPosition }.getOrDefault(positionMs)
            delay(400)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = audio.displayName,
            color = Color(0xFF202020),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        TvActionButton(
            text = when {
                failed -> "加载失败"
                !prepared -> "加载中…"
                active -> "❚❚  ${formatDuration(positionMs)} / ${formatDuration(durationMs)}"
                else -> "▶  ${formatDuration(positionMs)} / ${formatDuration(durationMs)}"
            },
            enabled = prepared && !failed,
            onClick = onToggle,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TvActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val clickFocusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(22.dp)
    val backgroundColor = when {
        !enabled -> Color(0xFFE8E8E8)
        focused -> Color(0xFFD7142B)
        else -> Color(0xFFF0F1F2)
    }
    val textColor = when {
        !enabled -> Color(0xFF999999)
        focused -> Color.White
        else -> Color(0xFF202020)
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color(0xFFFFD186) else Color(0xFFD9D9D9),
                shape = shape
            )
            .focusRequester(clickFocusRequester)
            .focusOnClick(clickFocusRequester)
            .clickable(enabled = enabled) {
                clickFocusRequester.requestFocus()
                onClick()
            }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NewsHtmlContent(
    html: String,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Text(
            text = htmlToPlainText(html),
            color = Color(0xFF171717),
            fontSize = 18.sp,
            lineHeight = 32.sp,
            modifier = modifier.padding(horizontal = 24.dp, vertical = 18.dp)
        )
        return
    }

    val textColor = Color(0xFF171717).toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                val scrollStepPx = (72 * resources.displayMetrics.density).toInt()

                setTextColor(textColor)
                textSize = 18f
                setLineSpacing(8f, 1.25f)
                setPadding(24, 18, 24, 24)
                movementMethod = ScrollingMovementMethod.getInstance()
                isFocusable = true
                isFocusableInTouchMode = true
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_NEVER
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK ||
                        keyCode == KeyEvent.KEYCODE_ESCAPE
                    ) {
                        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                            onNavigateUp()
                        }
                        // Consume both DOWN and UP so Dialog does not dismiss after focus moves.
                        true
                    } else if (event.action != KeyEvent.ACTION_DOWN) {
                        false
                    } else {
                        val contentHeight = layout?.height ?: 0
                        val viewportHeight =
                            (height - totalPaddingTop - totalPaddingBottom).coerceAtLeast(0)
                        val maxScrollY = (contentHeight - viewportHeight).coerceAtLeast(0)

                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                if (scrollY <= 0) {
                                    onNavigateUp()
                                } else {
                                    scrollTo(0, (scrollY - scrollStepPx).coerceAtLeast(0))
                                }
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                scrollTo(
                                    0,
                                    (scrollY + scrollStepPx).coerceAtMost(maxScrollY),
                                )
                                true
                            }

                            else -> false
                        }
                    }
                }
            }
        },
        update = { textView ->
            if (textView.tag != html) {
                textView.tag = html
                textView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(html)
                }
                textView.post {
                    val contentHeight = textView.layout?.height ?: 0
                    val viewportHeight =
                        (textView.height - textView.totalPaddingTop - textView.totalPaddingBottom)
                            .coerceAtLeast(0)
                    val maxScrollY = (contentHeight - viewportHeight).coerceAtLeast(0)
                    textView.scrollTo(0, textView.scrollY.coerceIn(0, maxScrollY))
                }
            }
        }
    )
}

private fun htmlToPlainText(html: String): String {
    val text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(html)
    }
    return text.toString()
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

private fun formatDuration(durationMs: Int): String {
    val totalSeconds = (durationMs.coerceAtLeast(0) / 1000)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Preview(
    name = "新闻详情弹窗",
    widthDp = 1280,
    heightDp = 620,
    showBackground = true
)
@Composable
private fun NewsDetailDialogPreview() {
    MaterialTheme {
        NewsDetailDialogContent(
            news = NewsDetail(
                id = "preview-news",
                title = "知规守纪⑮：学习贯彻《中国共产党纪律处分条例》：从事这些营利活动属于违规行为（上）",
                source = "州委组织部党员教育中心",
                author = "柏洁豪",
                publishTime = "2026-07-10",
                videos = listOf(
                    NewsVideo(
                        fileId = "mandarin-preview-file-id",
                        filename = "汉语播报.mp3",
                        type = "audio",
                        typeName = "汉语音频",
                    ),
                    NewsVideo(
                        fileId = "tibetan-preview-file-id",
                        filename = "藏语播报.mp3",
                        type = "audio",
                        typeName = "藏语音频",
                    ),
                ),
                content = """
                    <p style="text-align:center"><b>学习贯彻《中国共产党纪律处分条例》</b></p>
                    <p style="text-align:center">——从事这些营利活动属于违规行为（上）</p>
                    <p>党旗所指，心之所向！这里是《咔哒时间》特别专栏“康巴党旗红”，在这里我们触摸高原跳动的红色基因，聆听雪域儿女对党的深情告白；见证党支部筑起的红色堡垒，定格各族同胞携手奋进的动人瞬间。</p>
                    <p>让党章党规融入灵魂，化为新时代共产党人的精神律动和行动指南。《知规守纪》，现在开讲。</p>
                    <p>听众朋友们，大家好！今天给大家带来的是学习贯彻《中国共产党纪律处分条例》：从事这些营利活动属于违规行为。</p>
                    <p><b>一、违规经营商业、兴办企业</b></p>
                """.trimIndent()
            ),
            onDismiss = {},
            requestInitialFocus = false
        )
    }
}
