package com.fpa.dangjiandaping.ui.update

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.ui.focus.focusOnClick
import com.fpa.dangjiandaping.ui.web.HelpDialogGold
import com.fpa.dangjiandaping.ui.web.HelpDialogGoldBorder
import com.fpa.dangjiandaping.ui.web.HelpDialogLightGold
import com.fpa.dangjiandaping.ui.web.HelpDialogMutedText
import com.fpa.dangjiandaping.ui.web.HelpDialogWarmWhite
import com.fpa.dangjiandaping.ui.web.tvDialogPanel

@Composable
internal fun AppUpdateDialog(
    update: AppUpdate,
    downloading: Boolean,
    downloadProgress: DownloadProgress?,
    errorMessage: String?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val downloadFocusRequester = remember { FocusRequester() }
    val laterFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !downloading,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.30f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(650.dp)
                    .tvDialogPanel(RoundedCornerShape(12.dp))
                    .padding(horizontal = 34.dp, vertical = 28.dp),
            ) {
                Text(
                    text = "发现新版本",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "最新版本  ${update.version.ifBlank { update.build }}",
                    color = HelpDialogGold,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "更新内容",
                    color = HelpDialogLightGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = update.changelog.ifBlank { "本次版本优化了系统体验。" },
                    color = HelpDialogWarmWhite,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xB37A0D16))
                        .border(1.dp, HelpDialogGoldBorder, RoundedCornerShape(8.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                )
                errorMessage?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = Color(0xFFFFA7A7), fontSize = 14.sp)
                }
                if (downloading) {
                    Spacer(Modifier.height(16.dp))
                    DownloadProgressContent(downloadProgress)
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    UpdateActionButton(
                        text = if (downloading) "正在下载…" else "立即下载并安装",
                        enabled = !downloading,
                        focusedColor = Color(0xFFE33A3F),
                        normalColor = Color(0xFFB51F2B),
                        focusRequester = downloadFocusRequester,
                        onClick = onDownload,
                        modifier = Modifier
                            .weight(1f)
                            .focusProperties { right = laterFocusRequester },
                    )
                    UpdateActionButton(
                        text = "稍后再说",
                        enabled = !downloading,
                        focusedColor = Color(0xFFB92B32),
                        normalColor = Color(0xFF741019),
                        focusRequester = laterFocusRequester,
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .focusProperties { left = downloadFocusRequester },
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "按遥控器左右键选择，按确认键执行。",
                    color = HelpDialogMutedText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    LaunchedEffect(Unit) { downloadFocusRequester.requestFocus() }
}

@Composable
private fun DownloadProgressContent(progress: DownloadProgress?) {
    val downloaded = progress?.downloadedBytes ?: 0L
    val total = progress?.totalBytes ?: -1L
    val fraction = if (total > 0L) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0.12f
    val status = if (total > 0L) {
        "正在下载  ${(fraction * 100).toInt()}%  (${formatFileSize(downloaded)} / ${formatFileSize(total)})"
    } else {
        "正在下载  ${formatFileSize(downloaded)}"
    }
    Text(status, color = HelpDialogLightGold, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0x667A0D16)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(10.dp)
                .background(HelpDialogGold),
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}

@Composable
private fun UpdateActionButton(
    text: String,
    enabled: Boolean,
    focusedColor: Color,
    normalColor: Color,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused && enabled) 1.035f else 1f, label = "updateScale")
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .height(54.dp)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(if (focused && enabled) focusedColor else normalColor.copy(alpha = if (enabled) 1f else 0.65f))
            .border(if (focused && enabled) 2.dp else 1.dp, if (focused && enabled) HelpDialogGold else HelpDialogGoldBorder, shape)
            .focusRequester(focusRequester)
            .focusOnClick(focusRequester)
            .clickable(enabled = enabled, role = Role.Button) {
                focusRequester.requestFocus()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
