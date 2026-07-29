package com.fpa.dangjiandaping.ui.web

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

internal data class PublicHelpRequest(
    val id: String,
    val title: String,
    val urgency: String,
    val location: String,
    val content: String,
    val attachments: List<String>,
    val serviceType: String,
    val serviceTeam: String,
    val requesterName: String,
    val phone: String,
    val otherContact: String,
    val submittedAt: String,
)

/**
 * H5 调用示例：
 * AndroidFocusBridge.showPublicHelpRequest(JSON.stringify({
 *   id: "help-001",
 *   urgency: "紧急求助",
 *   location: "四川省甘孜州康定市炉城街道榆林社区",
 *   content: "我母亲突发心脏病，现昏倒在家中，急需送医救治。",
 *   attachments: ["https://example.com/help-1.jpg"],
 *   serviceType: "紧急救治",
 *   serviceTeam: "炉城街道党员服务队",
 *   requesterName: "张某某",
 *   phone: "138 1234 5678",
 *   otherContact: "zhangmoumou88",
 *   submittedAt: "2024-04-24 09:53:22"
 * }))
 */
internal fun parsePublicHelpRequest(json: String): PublicHelpRequest {
    val root = JSONObject(json)
    val record = root.optJSONObject("data")
        ?: root.optJSONObject("result")
        ?: root.optJSONObject("record")
        ?: root.optJSONObject("helpRequest")
        ?: root
    return PublicHelpRequest(
        id = record.helpStringOf("id", "requestId", "applyId"),
        title = record.helpStringOf("title", "name").ifBlank { "群众求助申请" },
        urgency = record.helpStringOf("urgency", "emergencyLevel", "level", "tag")
            .ifBlank { "紧急求助" },
        location = record.helpStringOf(
            "location",
            "address",
            "position",
            "peopleLocation",
            "requestLocation",
        ),
        content = record.helpStringOf(
            "content",
            "helpContent",
            "requestContent",
            "description",
            "message",
        ),
        attachments = record.helpStringListOf(
            "attachments",
            "images",
            "photos",
            "imageUrls",
            "pictureList",
        ),
        serviceType = record.helpStringOf("serviceType", "helpType", "type", "category"),
        serviceTeam = record.helpStringOf(
            "serviceTeam",
            "partyServiceTeam",
            "teamName",
            "serviceTeamName",
        ),
        requesterName = record.helpStringOf(
            "requesterName",
            "applicantName",
            "peopleName",
            "name",
        ),
        phone = record.helpStringOf("phone", "mobile", "telephone", "contactPhone"),
        otherContact = record.helpStringOf(
            "otherContact",
            "contact",
            "wechat",
            "wechatId",
            "contactWay",
        ),
        submittedAt = record.helpStringOf(
            "submittedAt",
            "submitTime",
            "createTime",
            "createdAt",
            "applicationTime",
        ),
    )
}

private fun JSONObject.helpStringOf(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key ->
        optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
    }.orEmpty()

private fun JSONObject.helpStringListOf(vararg keys: String): List<String> {
    for (key in keys) {
        val result = when (val raw = opt(key)) {
            is JSONArray -> buildList {
                for (index in 0 until raw.length()) {
                    val item = raw.opt(index)
                    val value = when (item) {
                        is JSONObject -> item.helpStringOf(
                            "url",
                            "imageUrl",
                            "fileUrl",
                            "path",
                            "src",
                        )
                        else -> item?.toString()?.trim().orEmpty()
                    }
                    if (value.isNotBlank() && value != "null") add(value)
                }
            }
            is String -> raw.split(',', '，')
                .map(String::trim)
                .filter(String::isNotBlank)
            else -> emptyList()
        }
        if (result.isNotEmpty()) return result
    }
    return emptyList()
}

@Composable
internal fun PublicHelpRequestDialog(
    request: PublicHelpRequest,
    onDismiss: () -> Unit,
    onHandled: () -> Unit,
    onContactLater: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        PublicHelpRequestDialogContent(
            request = request,
            onDismiss = onDismiss,
            onHandled = onHandled,
            onContactLater = onContactLater,
        )
    }
}

@Composable
private fun PublicHelpRequestDialogContent(
    request: PublicHelpRequest,
    onDismiss: () -> Unit,
    onHandled: () -> Unit,
    onContactLater: () -> Unit,
    requestInitialFocus: Boolean = true,
) {
    val closeFocusRequester = remember { FocusRequester() }
    val leftContentFocusRequester = remember { FocusRequester() }
    val handledFocusRequester = remember { FocusRequester() }
    val laterFocusRequester = remember { FocusRequester() }
    val panelShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 30.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(panelShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF35131D), Color(0xFF081426)),
                        radius = 900f,
                    ),
                )
                .border(1.dp, Color(0xFF35425A), panelShape)
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            HelpDialogHeader(
                request = request,
                onDismiss = onDismiss,
                closeFocusRequester = closeFocusRequester,
                leftContentFocusRequester = leftContentFocusRequester,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "有群众向您所辖党员服务队提交了帮助申请，请及时联系处置。",
                color = Color(0xFFF1F3FA),
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                HelpRequestLeftColumn(
                    request = request,
                    focusRequester = leftContentFocusRequester,
                    closeFocusRequester = closeFocusRequester,
                    handledFocusRequester = handledFocusRequester,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                HelpRequestDetails(
                    request = request,
                    modifier = Modifier
                        .weight(1.04f)
                        .fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HelpActionButton(
                    text = "✓  已联系处置",
                    focusedColor = Color(0xFFC91628),
                    normalColor = Color(0xFF8D0E1B),
                    onClick = onHandled,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(handledFocusRequester)
                        .focusProperties {
                            up = leftContentFocusRequester
                            right = laterFocusRequester
                        },
                )
                HelpActionButton(
                    text = "◷  稍后联系",
                    focusedColor = Color(0xFF263953),
                    normalColor = Color(0xFF111F34),
                    onClick = onContactLater,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(laterFocusRequester)
                        .focusProperties {
                            up = leftContentFocusRequester
                            left = handledFocusRequester
                        },
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "温馨提示：请尽快与群众取得联系并提供帮助，感谢您的付出！",
                color = Color(0xFF8490A4),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    LaunchedEffect(request.id, request.submittedAt, requestInitialFocus) {
        if (requestInitialFocus) closeFocusRequester.requestFocus()
    }
}

@Composable
private fun HelpDialogHeader(
    request: PublicHelpRequest,
    onDismiss: () -> Unit,
    closeFocusRequester: FocusRequester,
    leftContentFocusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFC95C)),
            contentAlignment = Alignment.Center,
        ) {
            Text("!", color = Color(0xFF9A1822), fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
        Text(
            text = request.title,
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
        )
        HelpBadge(request.urgency)
        Spacer(Modifier.weight(1f))
//        if (request.submittedAt.isNotBlank()) {
//            Text(
//                text = request.submittedAt,
//                color = Color(0xFFB7C0D1),
//                fontSize = 14.sp,
//                maxLines = 1,
//            )
//        }
        HelpActionButton(
            text = "×",
            focusedColor = Color(0xFF3A465A),
            normalColor = Color.Transparent,
            onClick = onDismiss,
            compact = true,
            modifier = Modifier
                .focusRequester(closeFocusRequester)
                .focusProperties { down = leftContentFocusRequester },
        )
    }
}

@Composable
private fun HelpBadge(text: String) {
    Text(
        text = text,
        color = Color(0xFFFFD7DA),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF9F1422))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun HelpRequestLeftColumn(
    request: PublicHelpRequest,
    focusRequester: FocusRequester,
    closeFocusRequester: FocusRequester,
    handledFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val scrollStepPx = with(LocalDensity.current) { 120.dp.toPx() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = Color(0xFFFFD27A),
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusRequester(focusRequester)
            .focusProperties {
                up = closeFocusRequester
                down = handledFocusRequester
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when {
                        event.key == Key.DirectionDown && scrollState.canScrollForward -> {
                            coroutineScope.launch {
                                scrollState.scrollTo(
                                    (scrollState.value + scrollStepPx.toInt())
                                        .coerceAtMost(scrollState.maxValue),
                                )
                            }
                            true
                        }

                        event.key == Key.DirectionUp && scrollState.canScrollBackward -> {
                            coroutineScope.launch {
                                scrollState.scrollTo(
                                    (scrollState.value - scrollStepPx.toInt())
                                        .coerceAtLeast(0),
                                )
                            }
                            true
                        }

                        else -> false
                    }
                }
            }
            .focusable()
            .clipToBounds()
            .verticalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HelpSectionTitle("群众位置")
        Text(
            text = request.location.ifBlank { "未提供位置信息" },
            color = Color(0xFFE7EBF4),
            fontSize = 16.sp,
            lineHeight = 23.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0x731A273A))
                .border(1.dp, Color(0xFF2A3B54), RoundedCornerShape(7.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
        )
        HelpSectionTitle("求助内容")
        Text(
            text = request.content.ifBlank { "未提供求助内容" },
            color = Color(0xFFD7DEEA),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0x731A273A))
                .padding(14.dp),
        )
        if (request.attachments.isNotEmpty()) {
            HelpSectionTitle("图片附件（${request.attachments.size}）")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                request.attachments.take(3).forEach { url ->
                    HelpRemoteImage(
                        url = url,
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(RoundedCornerShape(7.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpRequestDetails(request: PublicHelpRequest, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x73111E31))
            .border(1.dp, Color(0xFF263650), RoundedCornerShape(8.dp))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        HelpDetailRow("◆", "服务类型", request.serviceType)
        HelpDetailRow("♟", "对应党员服务队", request.serviceTeam)
        HelpDetailRow("●", "求助人姓名", request.requesterName)
        HelpDetailRow("☎", "联系电话", request.phone)
        HelpDetailRow("✣", "其他联系方式", request.otherContact)
        HelpDetailRow("◷", "提交时间", request.submittedAt)
    }
}

@Composable
private fun HelpDetailRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(icon, color = Color(0xFFD7DDE9), fontSize = 19.sp, modifier = Modifier.width(22.dp))
        Text(label, color = Color(0xFFD7DDE9), fontSize = 16.sp, modifier = Modifier.width(126.dp))
        Text(
            text = value.ifBlank { "未提供" },
            color = Color(0xFFF1F4FA),
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HelpSectionTitle(title: String) {
    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun HelpActionButton(
    text: String,
    focusedColor: Color,
    normalColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val clickFocusRequester = remember { FocusRequester() }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        label = "helpActionScale",
    )
    val shape = RoundedCornerShape(if (compact) 22.dp else 7.dp)
    Box(
        modifier = modifier
            .height(if (compact) 44.dp else 52.dp)
            .then(if (compact) Modifier.width(44.dp) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (focused) focusedColor else normalColor)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color(0xFFFFD27A) else Color(0xFF33415A),
                shape = shape,
            )
            .focusRequester(clickFocusRequester)
            .clickable(role = Role.Button) {
                clickFocusRequester.requestFocus()
                onClick()
            }
            .padding(horizontal = if (compact) 0.dp else 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = if (compact) 27.sp else 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun HelpRemoteImage(url: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = if (url.startsWith("http://") || url.startsWith("https://")) {
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(url).openStream().use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        } else {
            null
        }
    }
    if (bitmap == null) {
        Box(
            modifier = modifier.background(Color(0xFF243147)),
            contentAlignment = Alignment.Center,
        ) {
            Text("图片", color = Color(0xFF9DA8B9), fontSize = 14.sp)
        }
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "求助图片附件",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Preview(name = "群众求助弹窗", widthDp = 960, heightDp = 540, showBackground = true)
@Composable
private fun PublicHelpRequestDialogPreview() {
    MaterialTheme {
        PublicHelpRequestDialogContent(
            request = PublicHelpRequest(
                id = "preview-help",
                title = "群众求助申请",
                urgency = "紧急求助",
                location = "四川省甘孜州康定市炉城街道榆林社区",
                content = "我母亲突发心脏病，现昏倒在家中，急需送医救治，希望能尽快安排车辆或医护人员帮助！",
                attachments = listOf("one", "two", "three"),
                serviceType = "紧急救治",
                serviceTeam = "炉城街道党员服务队",
                requesterName = "张某某",
                phone = "138 1234 5678",
                otherContact = "zhangmoumou88",
                submittedAt = "2024-04-24 09:53:22",
            ),
            onDismiss = {},
            onHandled = {},
            onContactLater = {},
            requestInitialFocus = false,
        )
    }
}
