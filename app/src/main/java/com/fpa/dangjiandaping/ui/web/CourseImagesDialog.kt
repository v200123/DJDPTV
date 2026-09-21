package com.fpa.dangjiandaping.ui.web

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.data.learning.LearningSnapshotEntity
import com.fpa.dangjiandaping.ui.focus.logFocusTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CourseImagesDialog(
    resourceId: String,
    snapshots: List<LearningSnapshotEntity>?,
    onDismiss: () -> Unit,
) {
    val closeFocusRequester = remember { FocusRequester() }
    val previewCloseFocusRequester = remember { FocusRequester() }
    val snapshotIds = snapshots?.map { it.id }.orEmpty()
    val photoFocusRequesters = remember(snapshotIds) {
        List(snapshotIds.size) { FocusRequester() }
    }
    var selectedSnapshot by remember(resourceId) {
        mutableStateOf<LearningSnapshotEntity?>(null)
    }
    var restorePhotoIndex by remember(resourceId) { mutableStateOf<Int?>(null) }
    val courseName = snapshots
        ?.firstOrNull { it.resourceName.isNotBlank() }
        ?.resourceName
        .orEmpty()

    Dialog(
        onDismissRequest = {
            if (selectedSnapshot != null) {
                selectedSnapshot = null
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        // Compose Dialog has its own platform dim layer. Remove it so the content scrim below
        // is the only background treatment and does not stack into an opaque black backdrop.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialogWindow?.setDimAmount(0f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(COURSE_EVIDENCE_SCRIM),
            contentAlignment = Alignment.Center,
        ) {
            val previewSnapshot = selectedSnapshot
            if (previewSnapshot != null) {
                LearningEvidenceImagePreview(
                    snapshot = previewSnapshot,
                    closeFocusRequester = previewCloseFocusRequester,
                    onDismiss = { selectedSnapshot = null },
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .fillMaxHeight(0.84f)
                        .shadow(18.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(COURSE_EVIDENCE_BACKGROUND)
                        .border(1.dp, COURSE_EVIDENCE_BORDER, RoundedCornerShape(18.dp))
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(COURSE_EVIDENCE_RED),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "学习凭证",
                            color = COURSE_EVIDENCE_TEXT,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        TvDialogCloseButton(
                            onClick = onDismiss,
                            focusRequester = closeFocusRequester,
                            downFocusRequester = photoFocusRequesters.firstOrNull(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = courseName.ifBlank { "课程学习记录" },
                        color = COURSE_EVIDENCE_TEXT,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = when {
                            snapshots == null -> "正在读取学习凭证…"
                            snapshots.isEmpty() -> "该课程暂未采集学习凭证"
                            else -> "共抓拍 ${snapshots.size} 次"
                        },
                        color = COURSE_EVIDENCE_MUTED,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(14.dp))

                    when {
                        snapshots == null -> CourseEvidenceEmptyState("正在加载…")
                        snapshots.isEmpty() -> CourseEvidenceEmptyState(
                            "暂未找到该课程的照片\n课程 ID：$resourceId",
                        )
                        else -> LazyVerticalGrid(
                            columns = GridCells.Fixed(PHOTO_GRID_COLUMN_COUNT),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(snapshots, key = { _, snapshot -> snapshot.id }) {
                                    index, snapshot ->
                                LearningEvidencePhotoCard(
                                    snapshot = snapshot,
                                    focusRequester = photoFocusRequesters[index],
                                    upFocusRequester = closeFocusRequester.takeIf {
                                        index < PHOTO_GRID_COLUMN_COUNT
                                    },
                                    onClick = {
                                        restorePhotoIndex = index
                                        selectedSnapshot = snapshot
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(resourceId) { closeFocusRequester.requestFocus() }
    LaunchedEffect(selectedSnapshot) {
        if (selectedSnapshot != null) {
            previewCloseFocusRequester.requestFocus()
        } else {
            val index = restorePhotoIndex ?: return@LaunchedEffect
            withFrameNanos { }
            photoFocusRequesters.getOrNull(index)?.requestFocus()
            restorePhotoIndex = null
        }
    }
}

@Composable
private fun CourseEvidenceEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(COURSE_EVIDENCE_SOFT_RED)
            .border(1.dp, COURSE_EVIDENCE_BORDER, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = COURSE_EVIDENCE_MUTED,
            fontSize = 17.sp,
            lineHeight = 26.sp,
        )
    }
}

@Composable
private fun LearningEvidencePhotoCard(
    snapshot: LearningSnapshotEntity,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(
                width = if (focused) 4.dp else 1.dp,
                color = if (focused) COURSE_EVIDENCE_GOLD else COURSE_EVIDENCE_BORDER,
                shape = shape,
            )
            .focusRequester(focusRequester)
            .focusProperties {
                upFocusRequester?.let { up = it }
            }
            .onFocusChanged { focused = it.isFocused }
            .logFocusTarget("CourseImages.Photo.${snapshot.id}")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LocalEvidenceImage(
            path = snapshot.imagePath,
            modifier = Modifier
                .fillMaxWidth()
                .height(102.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(6.dp))
        EvidenceTimestamp(timestampMillis = snapshot.capturedAtMillis)
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun LearningEvidenceImagePreview(
    snapshot: LearningSnapshotEntity,
    closeFocusRequester: FocusRequester,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.9f)
            .shadow(18.dp, shape)
            .clip(shape)
            .background(COURSE_EVIDENCE_BACKGROUND)
            .border(1.dp, COURSE_EVIDENCE_BORDER, shape)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = snapshot.resourceName.ifBlank { "学习凭证大图" },
                color = COURSE_EVIDENCE_TEXT,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TvDialogCloseButton(
                onClick = onDismiss,
                focusRequester = closeFocusRequester,
            )
        }
        Spacer(Modifier.height(12.dp))
        LocalEvidenceImage(
            path = snapshot.imagePath,
            requestedWidth = 1_600,
            requestedHeight = 900,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(COURSE_EVIDENCE_SOFT_RED),
        )
        Spacer(Modifier.height(10.dp))
        EvidenceTimestamp(
            timestampMillis = snapshot.capturedAtMillis,
            enlarged = true,
        )
    }
}

@Composable
private fun EvidenceTimestamp(
    timestampMillis: Long,
    enlarged: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatEvidenceDate(timestampMillis),
            color = COURSE_EVIDENCE_TEXT,
            fontSize = if (enlarged) 16.sp else 13.sp,
            lineHeight = if (enlarged) 19.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = formatEvidenceClockTime(timestampMillis),
            color = COURSE_EVIDENCE_MUTED,
            fontSize = if (enlarged) 13.sp else 11.sp,
            lineHeight = if (enlarged) 16.sp else 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LocalEvidenceImage(
    path: String,
    requestedWidth: Int = 360,
    requestedHeight: Int = 204,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path, requestedWidth, requestedHeight) {
        value = withContext(Dispatchers.IO) {
            decodeSampledBitmap(path, requestedWidth, requestedHeight)
        }
    }
    val loadedBitmap = bitmap
    if (loadedBitmap == null) {
        Box(
            modifier = modifier.background(COURSE_EVIDENCE_SOFT_RED),
            contentAlignment = Alignment.Center,
        ) {
            Text("照片读取中…", color = COURSE_EVIDENCE_MUTED, fontSize = 14.sp)
        }
    } else {
        Image(
            bitmap = loadedBitmap.asImageBitmap(),
            contentDescription = "学习抓拍照片",
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}

private fun decodeSampledBitmap(path: String, requestedWidth: Int, requestedHeight: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= requestedWidth &&
        bounds.outHeight / (sampleSize * 2) >= requestedHeight
    ) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )
}

private fun formatEvidenceDate(timestampMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestampMillis))

private fun formatEvidenceClockTime(timestampMillis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMillis))

private val COURSE_EVIDENCE_BACKGROUND = Color(0xFFFFFAF6)
private val COURSE_EVIDENCE_SCRIM = Color(0x33C91522)
private val COURSE_EVIDENCE_SOFT_RED = Color(0xFFFFECE8)
private val COURSE_EVIDENCE_RED = Color(0xFFC91522)
private val COURSE_EVIDENCE_GOLD = Color(0xFFE4B44A)
private val COURSE_EVIDENCE_BORDER = Color(0xFFE8C9C4)
private val COURSE_EVIDENCE_TEXT = Color(0xFF351619)
private val COURSE_EVIDENCE_MUTED = Color(0xFF775B5E)
private const val PHOTO_GRID_COLUMN_COUNT = 5
