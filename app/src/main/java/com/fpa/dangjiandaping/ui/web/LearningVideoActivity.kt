package com.fpa.dangjiandaping.ui.web

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.tv.material3.MaterialTheme
import com.fpa.dangjiandaping.BuildConfig
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
    private var cameraStatus by mutableStateOf(CAMERA_STATUS_PENDING_PERMISSION)

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

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL).orEmpty()
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE).orEmpty()
        val classId = intent.getStringExtra(EXTRA_CLASS_ID).orEmpty()
        if (videoUrl.isBlank()) {
            finish()
            return
        }

        // Match the established full-screen player. CameraX only supplies ImageCapture and
        // does not own this video surface or decoder.
        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        snapshotCamera = LearningSnapshotCamera(this, this)
        snapshotRepository = LearningSnapshotRepository.get(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
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
                    onPlaybackStateChanged = studyTracker::onPlaybackStateChanged,
                    onSnapshotDue = {
                        snapshotCamera.takeSnapshot(
                            classId = classId,
                            onSaved = { photoFile ->
                                studyTracker.recordCapture()
                                if (BuildConfig.DEBUG) {
                                    Toast.makeText(
                                        this@LearningVideoActivity,
                                        "学习照片已采集",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                snapshotRepository.recordSnapshot(
                                    classId = classId,
                                    imagePath = photoFile.absolutePath,
                                    capturedAtMillis = System.currentTimeMillis(),
                                )
                            },
                            onError = { cameraStatus = CAMERA_STATUS_CAPTURE_FAILED },
                        )
                    },
                    onExit = { _, _ -> finishWithStudyResult(classId) },
                )
            }
        }
    }

    override fun onDestroy() {
        if (::snapshotCamera.isInitialized) snapshotCamera.close()
        super.onDestroy()
    }

    private fun startSnapshotCamera() {
        cameraStatus = CAMERA_STATUS_STARTING
        snapshotCamera.start(
            onReady = { cameraStatus = CAMERA_STATUS_ENABLED },
            onError = { cameraStatus = CAMERA_STATUS_UNAVAILABLE },
        )
    }

    private fun finishWithStudyResult(classId: String) {
        val result = JSONObject()
            .put("classId", classId)
            .put("playedDurationMs", studyTracker.stop())
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
        private const val EXTRA_STUDY_RESULT_JSON = "learning_video_study_result_json"

        private const val CAMERA_STATUS_PENDING_PERMISSION = "pending_permission"
        private const val CAMERA_STATUS_PERMISSION_DENIED = "permission_denied"
        private const val CAMERA_STATUS_STARTING = "starting"
        private const val CAMERA_STATUS_ENABLED = "enabled"
        private const val CAMERA_STATUS_UNAVAILABLE = "unavailable"
        private const val CAMERA_STATUS_CAPTURE_FAILED = "capture_failed"

        fun newIntent(
            context: Context,
            videoUrl: String,
            videoTitle: String,
            classId: String,
        ): Intent =
            Intent(context, LearningVideoActivity::class.java)
                .putExtra(EXTRA_VIDEO_URL, videoUrl)
                .putExtra(EXTRA_VIDEO_TITLE, videoTitle)
                .putExtra(EXTRA_CLASS_ID, classId)

        internal fun readStudyResultJson(intent: Intent?): String? =
            intent?.getStringExtra(EXTRA_STUDY_RESULT_JSON)
    }
}

@Composable
private fun LearningVideoPlayer(
    videoUrl: String,
    videoTitle: String,
    cameraReady: Boolean,
    onPlaybackStateChanged: (Boolean) -> Unit,
    onSnapshotDue: () -> Unit,
    onExit: (Long, Boolean) -> Unit,
) {
    var playing by remember { mutableStateOf(false) }

    // A pause cancels this effect. Therefore a snapshot is only requested after a full ten
    // seconds of continuous playback and never while the player is paused or in the background.
    LaunchedEffect(playing, cameraReady) {
        if (!playing || !cameraReady) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(SNAPSHOT_INTERVAL_MILLIS)
            onSnapshotDue()
        }
    }

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
    )
}

private class StudyDurationTracker {
    private var playbackStartedAtMs: Long? = null
    private var accumulatedPlaybackMs: Long = 0L
    var captureCount: Int = 0
        private set

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

    fun start(onReady: () -> Unit, onError: () -> Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                runCatching {
                    val provider = providerFuture.get()
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    provider.unbindAll()
                    runCatching {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            capture,
                        )
                    }.getOrElse {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            capture,
                        )
                    }
                    cameraProvider = provider
                    imageCapture = capture
                }.onSuccess { onReady() }
                    .onFailure { onError() }
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
        val directory = File(context.cacheDir, "learning_snapshots/$safeLearningId")
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
private const val SNAPSHOT_INTERVAL_MILLIS = 10_000L
