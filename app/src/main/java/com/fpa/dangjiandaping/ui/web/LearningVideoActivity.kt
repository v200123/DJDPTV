package com.fpa.dangjiandaping.ui.web

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.BuildConfig
import com.fpa.dangjiandaping.data.learning.LEARNING_SNAPSHOT_CACHE_DIRECTORY
import com.fpa.dangjiandaping.data.learning.LearningSnapshotRepository
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executor
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager

/**
 * Native learning player opened by H5. The video and camera have separate lifecycles: a camera
 * failure never stops playback, but is returned in the study result so H5 can decide how to
 * handle an incomplete verification record.
 */
class LearningVideoActivity : ComponentActivity() {

    private lateinit var snapshotCamera: LearningSnapshotCamera
    private lateinit var snapshotRepository: LearningSnapshotRepository
    private val studyTracker = StudyDurationTracker()
    private var persistedStudyDurationMs = 0L
    private var cameraStatus by mutableStateOf(CAMERA_STATUS_PENDING_PERMISSION)
    private var capturedSnapshotCount by mutableIntStateOf(0)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startSnapshotCamera()
        } else {
            cameraStatus = CAMERA_STATUS_PERMISSION_DENIED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL).orEmpty()
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE).orEmpty()
        val classId = intent.getStringExtra(EXTRA_CLASS_ID).orEmpty()
        val resourceId = intent.getStringExtra(EXTRA_RESOURCE_ID).orEmpty()
        val resourceType = intent.getStringExtra(EXTRA_RESOURCE_TYPE).orEmpty()
        val resourceTypeName = intent.getStringExtra(EXTRA_RESOURCE_TYPE_NAME).orEmpty()
        if (videoUrl.isBlank()) {
            finish()
            return
        }

        // Match the established full-screen player. CameraX only supplies ImageCapture and
        // does not own this video surface or decoder.
        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        snapshotCamera = LearningSnapshotCamera(this, this)
        snapshotRepository = LearningSnapshotRepository.get(this)
        snapshotRepository.recordCourseProgress(
            classId = classId,
            resourceId = resourceId,
            resourceName = videoTitle,
            resourceType = resourceType,
            resourceTypeName = resourceTypeName,
            durationDeltaMs = 0L,
        )
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            cameraStatus = CAMERA_STATUS_UNAVAILABLE
            showDebugCameraToast("未检测到可用摄像头")
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startSnapshotCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MaterialTheme {
                LearningVideoPlayer(
                    videoUrl = videoUrl,
                    videoTitle = videoTitle,
                    cameraReady = cameraStatus == CAMERA_STATUS_ENABLED,
                    cameraStatusText = cameraStatus.toDisplayText(),
                    captureCount = capturedSnapshotCount,
                    onPlaybackStateChanged = { isPlaying ->
                        studyTracker.onPlaybackStateChanged(isPlaying)
                        if (!isPlaying) {
                            persistNewStudyDuration(
                                classId = classId,
                                resourceId = resourceId,
                                resourceName = videoTitle,
                                resourceType = resourceType,
                                resourceTypeName = resourceTypeName,
                            )
                        }
                    },
                    onSnapshotDue = {
                        // The Compose timer is cancelled on pause. Check the tracker again here
                        // to also cover a pause racing with the timer callback.
                        if (studyTracker.isPlaying) {
                            snapshotCamera.takeSnapshot(
                                classId = classId,
                                onSaved = { photoFile ->
                                    studyTracker.recordCapture()
                                    capturedSnapshotCount = studyTracker.captureCount
                                    if (BuildConfig.DEBUG) {
                                        Toast.makeText(
                                            this@LearningVideoActivity,
                                            "学习照片已采集",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    snapshotRepository.recordSnapshot(
                                        classId = classId,
                                        resourceId = resourceId,
                                        resourceName = videoTitle,
                                        imagePath = photoFile.absolutePath,
                                        capturedAtMillis = System.currentTimeMillis(),
                                    )
                                },
                                onError = { cameraStatus = CAMERA_STATUS_CAPTURE_FAILED },
                            )
                        }
                    },
                    onExit = { _, _ ->
                        finishWithStudyResult(
                            classId = classId,
                            resourceId = resourceId,
                            resourceName = videoTitle,
                            resourceType = resourceType,
                            resourceTypeName = resourceTypeName,
                        )
                    },
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
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        if (::snapshotCamera.isInitialized) snapshotCamera.close()
        super.onDestroy()
    }

    private fun startSnapshotCamera() {
        cameraStatus = CAMERA_STATUS_STARTING
        snapshotCamera.start(
            onReady = { cameraStatus = CAMERA_STATUS_ENABLED },
            onError = { noCameraAvailable ->
                cameraStatus = CAMERA_STATUS_UNAVAILABLE
                if (noCameraAvailable) {
                    showDebugCameraToast("未检测到可用摄像头")
                }
            },
        )
    }

    private fun showDebugCameraToast(message: String) {
        if (BuildConfig.DEBUG) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun persistNewStudyDuration(
        classId: String,
        resourceId: String,
        resourceName: String,
        resourceType: String,
        resourceTypeName: String,
    ) {
        val currentDurationMs = studyTracker.currentDurationMs()
        val durationDeltaMs = currentDurationMs - persistedStudyDurationMs
        if (durationDeltaMs <= 0L) return
        persistedStudyDurationMs = currentDurationMs
        snapshotRepository.recordCourseProgress(
            classId = classId,
            resourceId = resourceId,
            resourceName = resourceName,
            resourceType = resourceType,
            resourceTypeName = resourceTypeName,
            durationDeltaMs = durationDeltaMs,
        )
    }

    private fun finishWithStudyResult(
        classId: String,
        resourceId: String,
        resourceName: String,
        resourceType: String,
        resourceTypeName: String,
    ) {
        studyTracker.stop()
        persistNewStudyDuration(
            classId = classId,
            resourceId = resourceId,
            resourceName = resourceName,
            resourceType = resourceType,
            resourceTypeName = resourceTypeName,
        )
        val result = JSONObject()
            .put("classId", classId)
            .put("id", resourceId)
            .put("resourceType", resourceType)
            .put("resourceTypeName", resourceTypeName)
            .put("playedDurationMs", studyTracker.currentDurationMs())
            .put("captureCount", studyTracker.captureCount)
            .put("cameraStatus", cameraStatus)
            .toString()
        setResult(RESULT_OK, Intent().putExtra(EXTRA_STUDY_RESULT_JSON, result))
        finish()
    }

    companion object {
        private const val EXTRA_VIDEO_URL = "learning_video_url"
        private const val EXTRA_VIDEO_TITLE = "learning_video_title"
        private const val EXTRA_CLASS_ID = "learning_video_class_id"
        private const val EXTRA_RESOURCE_ID = "learning_video_resource_id"
        private const val EXTRA_RESOURCE_TYPE = "learning_video_resource_type"
        private const val EXTRA_RESOURCE_TYPE_NAME = "learning_video_resource_type_name"
        private const val EXTRA_SPEAKER = "learning_video_speaker"
        private const val EXTRA_START_TIME = "learning_video_start_time"
        private const val EXTRA_END_TIME = "learning_video_end_time"
        private const val EXTRA_STUDY_RESULT_JSON = "learning_video_study_result_json"

        private const val CAMERA_STATUS_PENDING_PERMISSION = "pending_permission"
        private const val CAMERA_STATUS_PERMISSION_DENIED = "permission_denied"
        private const val CAMERA_STATUS_STARTING = "starting"
        private const val CAMERA_STATUS_ENABLED = "enabled"
        private const val CAMERA_STATUS_UNAVAILABLE = "unavailable"
        private const val CAMERA_STATUS_CAPTURE_FAILED = "capture_failed"

        private fun String.toDisplayText(): String = when (this) {
            CAMERA_STATUS_ENABLED -> "抓拍服务已开启"
            CAMERA_STATUS_PERMISSION_DENIED -> "未获得摄像头权限"
            CAMERA_STATUS_UNAVAILABLE -> "未检测到可用摄像头"
            CAMERA_STATUS_CAPTURE_FAILED -> "本次抓拍失败，将继续尝试"
            else -> "正在连接摄像头"
        }

        fun newIntent(
            context: Context,
            videoUrl: String,
            videoTitle: String,
            classId: String,
            resourceId: String,
            resourceType: String,
            resourceTypeName: String,
            speaker: String,
            startTime: String,
            endTime: String,
        ): Intent =
            Intent(context, LearningVideoActivity::class.java)
                .putExtra(EXTRA_VIDEO_URL, videoUrl)
                .putExtra(EXTRA_VIDEO_TITLE, videoTitle)
                .putExtra(EXTRA_CLASS_ID, classId)
                .putExtra(EXTRA_RESOURCE_ID, resourceId)
                .putExtra(EXTRA_RESOURCE_TYPE, resourceType)
                .putExtra(EXTRA_RESOURCE_TYPE_NAME, resourceTypeName)
                .putExtra(EXTRA_SPEAKER, speaker)
                .putExtra(EXTRA_START_TIME, startTime)
                .putExtra(EXTRA_END_TIME, endTime)

        internal fun readStudyResultJson(intent: Intent?): String? =
            intent?.getStringExtra(EXTRA_STUDY_RESULT_JSON)
    }
}

@Composable
private fun LearningVideoPlayer(
    videoUrl: String,
    videoTitle: String,
    cameraReady: Boolean,
    cameraStatusText: String,
    captureCount: Int,
    onPlaybackStateChanged: (Boolean) -> Unit,
    onSnapshotDue: () -> Unit,
    onExit: (Long, Boolean) -> Unit,
) {
    var playing by remember { mutableStateOf(false) }
    var displayedStudyDurationMs by remember { mutableLongStateOf(0L) }
    var showCaptureNotice by remember { mutableStateOf(true) }
    var videoFullscreen by remember { mutableStateOf(false) }
    var nextSnapshotDueMs by remember { mutableLongStateOf(SNAPSHOT_INTERVAL_MILLIS) }
    var snapshotPending by remember { mutableStateOf(false) }

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var lastTick = SystemClock.elapsedRealtime()
        while (true) {
            kotlinx.coroutines.delay(STUDY_DURATION_TICK_MILLIS)
            val now = SystemClock.elapsedRealtime()
            displayedStudyDurationMs += (now - lastTick).coerceAtLeast(0L)
            lastTick = now
        }
    }

    // Keep capture deadlines on the playback clock, even while the camera is warming up.
    // If a deadline passes before the camera is ready, take one catch-up snapshot as soon as
    // it becomes available; later deadlines remain aligned to the same playback timeline.
    LaunchedEffect(playing, cameraReady) {
        if (!playing) return@LaunchedEffect
        if (cameraReady && snapshotPending) {
            snapshotPending = false
            onSnapshotDue()
        }
        while (true) {
            val remainingMs = (nextSnapshotDueMs - displayedStudyDurationMs)
                .coerceAtLeast(100L)
            kotlinx.coroutines.delay(remainingMs)
            if (displayedStudyDurationMs < nextSnapshotDueMs) continue

            if (cameraReady) {
                onSnapshotDue()
            } else {
                snapshotPending = true
            }
            nextSnapshotDueMs += SNAPSHOT_INTERVAL_MILLIS
        }
    }

    val exitPlayer = { onExit(0L, playing) }
    val embeddedPlayerShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PARTY_RED_BACKGROUND_DARK,
                        PARTY_RED_DARK,
                        PARTY_RED,
                    ),
                ),
            ),
    ) {
        FullscreenVideoPlayer(
            videoUrl = videoUrl,
            videoTitle = videoTitle.ifBlank { "学习视频" },
            startPositionMs = 0L,
            autoPlay = true,
            onExit = onExit,
            onPlaybackStateChanged = { isPlaying ->
                playing = isPlaying
                onPlaybackStateChanged(isPlaying)
            },
            modifier = if (videoFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(start = 26.dp, top = 88.dp, end = 26.dp, bottom = 132.dp)
                    .shadow(12.dp, embeddedPlayerShape)
                    .clip(embeddedPlayerShape)
                    .border(1.dp, PARTY_RED_BORDER, embeddedPlayerShape)
            },
            showTopBar = false,
            isFullscreen = videoFullscreen,
            onFullscreenToggle = { videoFullscreen = !videoFullscreen },
            onBackRequest = if (videoFullscreen) {
                { videoFullscreen = false }
            } else {
                null
            },
        )

        if (!videoFullscreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 26.dp, top = 14.dp, end = 26.dp),
            ) {
                LearningVideoHeader(
                    title = videoTitle.ifBlank { "学习视频" },
                    onBack = exitPlayer,
                )
            }
            LearningStatusPanel(
                studyDurationMs = displayedStudyDurationMs,
                playing = playing,
                captureCount = captureCount,
                cameraReady = cameraReady,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 26.dp, end = 26.dp, bottom = 16.dp),
            )
        }

        if (showCaptureNotice) {
            LearningCaptureNotice(
                cameraReady = cameraReady,
                playing = playing,
                statusText = cameraStatusText,
                onDismiss = { showCaptureNotice = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = if (videoFullscreen) 18.dp else 112.dp,
                        end = if (videoFullscreen) 18.dp else 38.dp,
                    ),
            )
        }
    }
}

@Composable
private fun LearningVideoHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LearningBackButton(onClick = onBack)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PARTY_HEADER_TITLE,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "党员教育  ·  学习时长同步记录",
                color = PARTY_GOLD_LIGHT.copy(alpha = 0.86f),
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LearningBackButton(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(if (focused) 8.dp else 2.dp, shape)
            .clip(shape)
            .background(if (focused) PARTY_RED else Color.White.copy(alpha = 0.94f))
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) PARTY_GOLD else PARTY_RED_BORDER,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        val arrowColor = if (focused) Color.White else PARTY_RED
        Canvas(modifier = Modifier.size(22.dp)) {
            val stroke = 2.6.dp.toPx()
            val left = size.width * 0.25f
            val centerY = size.height * 0.5f
            val shoulderX = size.width * 0.62f
            drawLine(
                color = arrowColor,
                start = Offset(shoulderX, size.height * 0.22f),
                end = Offset(left, centerY),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawLine(
                color = arrowColor,
                start = Offset(left, centerY),
                end = Offset(shoulderX, size.height * 0.78f),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawLine(
                color = arrowColor,
                start = Offset(left + size.width * 0.04f, centerY),
                end = Offset(size.width * 0.78f, centerY),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun LearningCaptureNotice(
    cameraReady: Boolean,
    playing: Boolean,
    statusText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .width(340.dp)
            .shadow(10.dp, shape)
            .clip(shape)
            .background(Color(0xF7FFF9F5))
            .border(1.dp, PARTY_RED_BORDER, shape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(PARTY_RED, PARTY_RED_LIGHT),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            CameraGlyph(tint = Color.White, modifier = Modifier.size(31.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    !cameraReady -> "学习过程抓拍"
                    playing -> "学习过程抓拍已开启"
                    else -> "学习过程抓拍已暂停"
                },
                color = PARTY_TEXT_DARK,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    !cameraReady -> statusText
                    playing -> "请注意，10秒后将进行抓拍"
                    else -> "继续播放后重新计时，满 10 秒后自动抓拍"
                },
                color = if (cameraReady && playing) PARTY_TEXT_MUTED else PARTY_RED,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        Text(
            text = "×",
            color = PARTY_TEXT_MUTED,
            fontSize = 24.sp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun LearningStatusPanel(
    studyDurationMs: Long,
    playing: Boolean,
    captureCount: Int,
    cameraReady: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(106.dp)
            .shadow(7.dp, shape)
            .clip(shape)
            .background(Color(0xF9FFFDFC))
            .border(1.dp, PARTY_RED_BORDER, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(PARTY_RED_SOFT),
            contentAlignment = Alignment.Center,
        ) {
            ClockGlyph(modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.width(206.dp)) {
            Text(
                text = "累计学习时长",
                color = PARTY_TEXT_MUTED,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatStudyDuration(studyDurationMs),
                color = PARTY_RED,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp)
                .width(1.dp)
                .background(PARTY_RED_BORDER),
        )
        Spacer(Modifier.width(18.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (playing) "正在累计有效学习时间" else "视频已暂停，学习计时同步暂停",
                color = PARTY_TEXT_DARK,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PARTY_RED_SOFT),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (playing) 1f else 0.22f)
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(listOf(PARTY_RED, PARTY_RED_LIGHT)),
                        ),
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = "仅统计视频实际播放时长",
                color = PARTY_TEXT_MUTED,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(16.dp))
        Row(
            modifier = Modifier
                .width(252.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(PARTY_RED_DARK, PARTY_RED, PARTY_RED_LIGHT),
                    ),
                )
                .border(1.dp, PARTY_GOLD.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, PARTY_GOLD.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CameraGlyph(tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    text = "本次学习凭证",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (cameraReady) "已抓拍 $captureCount 次" else "抓拍服务暂未就绪",
                    color = PARTY_GOLD_LIGHT,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ClockGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.6.dp.toPx()
        drawCircle(
            color = PARTY_RED,
            style = Stroke(width = strokeWidth),
        )
        val center = Offset(size.width / 2f, size.height / 2f)
        drawLine(
            color = PARTY_RED,
            start = center,
            end = Offset(center.x, center.y - size.height * 0.24f),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = PARTY_RED,
            start = center,
            end = Offset(center.x + size.width * 0.2f, center.y + size.height * 0.12f),
            strokeWidth = strokeWidth,
        )
    }
}

@Composable
private fun CameraGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.4.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.08f, size.height * 0.27f),
            size = Size(size.width * 0.84f, size.height * 0.61f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            style = Stroke(width = strokeWidth),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.28f, size.height * 0.12f),
            size = Size(size.width * 0.32f, size.height * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.17f,
            center = Offset(size.width * 0.5f, size.height * 0.58f),
            style = Stroke(width = strokeWidth),
        )
    }
}

private fun formatStudyDuration(valueMillis: Long): String {
    val totalSeconds = valueMillis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private class StudyDurationTracker {
    private var playbackStartedAtMs: Long? = null
    private var accumulatedPlaybackMs: Long = 0L
    var captureCount: Int = 0
        private set
    val isPlaying: Boolean
        get() = playbackStartedAtMs != null

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (isPlaying && playbackStartedAtMs == null) {
            playbackStartedAtMs = now
        } else if (!isPlaying) {
            stopAt(now)
        }
    }

    fun recordCapture() {
        captureCount += 1
    }

    fun stop(): Long {
        stopAt(SystemClock.elapsedRealtime())
        return accumulatedPlaybackMs
    }

    fun currentDurationMs(): Long {
        val activeDurationMs = playbackStartedAtMs
            ?.let { (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L) }
            ?: 0L
        return accumulatedPlaybackMs + activeDurationMs
    }

    private fun stopAt(nowMs: Long) {
        val startedAt = playbackStartedAtMs ?: return
        accumulatedPlaybackMs += (nowMs - startedAt).coerceAtLeast(0L)
        playbackStartedAtMs = null
    }
}

private class LearningSnapshotCamera(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var captureInFlight = false

    fun start(onReady: () -> Unit, onError: (noCameraAvailable: Boolean) -> Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                runCatching {
                    val provider = providerFuture.get()
                    val firstCameraInfo = provider.availableCameraInfos.firstOrNull()
                        ?: throw NoAvailableCameraException()
                    val cameraSelector = CameraSelector.Builder()
                        .addCameraFilter { cameraInfos ->
                            cameraInfos.filter { it == firstCameraInfo }
                        }
                        .build()
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, cameraSelector, capture)
                    cameraProvider = provider
                    imageCapture = capture
                }.onSuccess { onReady() }
                    .onFailure { onError(it is NoAvailableCameraException) }
            },
            mainExecutor,
        )
    }

    fun takeSnapshot(
        classId: String,
        onSaved: (File) -> Unit,
        onError: () -> Unit,
    ) {
        val capture = imageCapture ?: return onError()
        if (captureInFlight) return
        captureInFlight = true
        val safeLearningId = classId.replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(80)
            .ifBlank { "anonymous" }
        val directory = File(
            context.cacheDir,
            "$LEARNING_SNAPSHOT_CACHE_DIRECTORY/$safeLearningId",
        )
        if (!directory.exists() && !directory.mkdirs()) {
            captureInFlight = false
            return onError()
        }
        val photoFile = File(directory, "${System.currentTimeMillis()}.jpg")
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    captureInFlight = false
                    onSaved(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    captureInFlight = false
                    onError()
                }
            },
        )
    }

    fun close() {
        imageCapture = null
        cameraProvider?.unbindAll()
        cameraProvider = null
    }
}
private class NoAvailableCameraException : IllegalStateException()

private const val SNAPSHOT_INTERVAL_MILLIS = 10_000L
private const val STUDY_DURATION_TICK_MILLIS = 500L

private val PARTY_RED = Color(0xFFC91522)
private val PARTY_RED_DARK = Color(0xFF8F0712)
private val PARTY_RED_LIGHT = Color(0xFFE8463F)
private val PARTY_RED_SOFT = Color(0xFFFFECE8)
private val PARTY_RED_BORDER = Color(0xFFF2C9C2)
private val PARTY_RED_BACKGROUND_DARK = Color(0xFF5F050C)
private val PARTY_GOLD = Color(0xFFE8B94D)
private val PARTY_GOLD_LIGHT = Color(0xFFFFE5A0)
private val PARTY_HEADER_TITLE = Color(0xFFFFF3D0)
private val PARTY_TEXT_DARK = Color(0xFF3A1518)
private val PARTY_TEXT_MUTED = Color(0xFF7B5A5D)
