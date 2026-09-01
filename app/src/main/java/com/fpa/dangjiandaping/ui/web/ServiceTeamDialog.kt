package com.fpa.dangjiandaping.ui.web

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import com.fpa.dangjiandaping.ui.focus.logFocusTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

private const val SERVICE_TEAM_IMAGE_BASE_URL = "https://www.xyxf.gov.cn/prod-api/image/"

internal data class ServiceTeam(
    val name: String,
    val services: List<String>,
    val introduction: String,
    val photos: List<String>,
    val members: List<ServiceTeamMember>,
    val navigationUrl: String,
    val helpUrl: String,
)

internal data class ServiceTeamMember(
    val name: String,
    val role: String,
    val avatarUrl: String,
    val phone: String,
    val services: List<String>,
)

/**
 * WebView 调用示例：
 * AndroidFocusBridge.showServiceTeam(JSON.stringify({
 *   name: "白角村党员服务队",
 *   services: ["道路抢险", "应急救援"],
 *   introduction: "服务队简介",
 *   photos: ["https://example.com/team-1.jpg"],
 *   members: [{ name: "李*", role: "队长", avatarUrl: "https://example.com/member.jpg", phone: "13800000000", services: ["道路抢险"] }],
 *   navigationUrl: "https://example.com/map",
 *   helpUrl: "https://example.com/help"
 * }))
 */
internal fun parseServiceTeam(json: String): ServiceTeam {
    val record = unwrapRecord(JSONObject(json))
    val services = record.stringListOf("services", "serviceItems", "tags", "serviceTypes")
        .ifEmpty { record.objectStringListOf("labelsList", valueKeys = arrayOf("name", "labelName")) }
    val members = record.objectListOf(
        "members",
        "membersList",
        "teamMembers",
        "teamMemberList",
        "memberList",
        "serviceMembers",
        "serviceTeamMembers",
        "partyMembers",
        "partyMemberList",
        "personList",
    ).map(::parseServiceTeamMember).ifEmpty {
        val contactName = record.stringOf("contactName", "linkman", "contact")
        val contactPhone = record.stringOf("contactPhone", "linkPhone", "mobile", "phone")
        if (contactName.isBlank() && contactPhone.isBlank()) {
            emptyList()
        } else {
            listOf(
                ServiceTeamMember(
                    name = contactName.ifBlank { "服务队联系人" },
                    role = "服务队联系人",
                    avatarUrl = "",
                    phone = contactPhone,
                    services = services,
                ),
            )
        }
    }
    return ServiceTeam(
        name = record.stringOf("name", "teamName", "title", "serviceTeamName")
            .ifBlank { "党员服务队" },
        services = services,
        introduction = record.stringOf("summary", "introduction", "intro", "description", "content"),
        photos = record.imageUrlListOf("image", "photos", "images", "teamPhotos", "photoUrls"),
        members = members,
        navigationUrl = record.stringOf("navigationUrl", "navigation", "mapUrl", "locationUrl"),
        helpUrl = record.stringOf("helpUrl", "help", "helpPageUrl"),
    )
}

private fun unwrapRecord(root: JSONObject): JSONObject =
    root.optJSONObject("data")
        ?: root.optJSONObject("result")
        ?: root.optJSONObject("team")
        ?: root

private fun parseServiceTeamMember(record: JSONObject): ServiceTeamMember = ServiceTeamMember(
    name = record.stringOf("name", "memberName", "realName", "contactName").ifBlank { "服务队成员" },
    role = record.stringOf("role", "position", "job", "duty", "identity"),
    avatarUrl = record.imageUrlListOf("avatarUrl", "avatar", "photo", "image", "headImage").firstOrNull().orEmpty(),
    phone = record.stringOf("phone", "mobile", "telephone", "tel", "contactPhone"),
    services = record.stringListOf("services", "serviceItems", "tags", "serviceTypes"),
)

private fun JSONObject.stringOf(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key -> optString(key).trim().takeIf { it.isNotBlank() } }.orEmpty()

private fun JSONObject.stringListOf(vararg keys: String): List<String> {
    for (key in keys) {
        val values = when (val value = opt(key)) {
            is JSONArray -> buildList {
                for (index in 0 until value.length()) {
                    value.optString(index).trim().takeIf { it.isNotBlank() }?.let { add(it) }
                }
            }
            is String -> value.split(',', '，').map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
        if (values.isNotEmpty()) return values
    }
    return emptyList()
}

private fun JSONObject.objectStringListOf(
    vararg keys: String,
    valueKeys: Array<String>,
): List<String> {
    for (key in keys) {
        val array = optJSONArray(key) ?: continue
        val values = buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)
                    ?.stringOf(*valueKeys)
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
        if (values.isNotEmpty()) return values
    }
    return emptyList()
}

/**
 * 接口的 image 字段是一个 JSON 数组字符串，例如：
 * [{"fileId":"abc","filename":"服务队.jpg"}]
 * fileId 需要拼接统一图片访问前缀。旧版直接传 URL 数组的格式仍然兼容。
 */
private fun JSONObject.imageUrlListOf(vararg keys: String): List<String> {
    for (key in keys) {
        val values = imageUrlsFromValue(opt(key))
        if (values.isNotEmpty()) return values
    }
    return emptyList()
}

private fun imageUrlsFromValue(value: Any?): List<String> = when (value) {
    is JSONArray -> buildList {
        for (index in 0 until value.length()) {
            addAll(imageUrlsFromValue(value.opt(index)))
        }
    }
    is JSONObject -> {
        val directUrl = value.stringOf("url", "imageUrl", "downloadUrl")
        val fileId = value.stringOf("fileId")
        when {
            directUrl.isNotBlank() -> listOf(directUrl)
            fileId.isNotBlank() -> listOf(SERVICE_TEAM_IMAGE_BASE_URL + Uri.encode(fileId))
            else -> emptyList()
        }
    }
    is String -> {
        val text = value.trim()
        when {
            text.isBlank() -> emptyList()
            text.startsWith("[") -> runCatching { imageUrlsFromValue(JSONArray(text)) }.getOrDefault(emptyList())
            text.startsWith("{") -> runCatching { imageUrlsFromValue(JSONObject(text)) }.getOrDefault(emptyList())
            text.startsWith("http://") || text.startsWith("https://") -> listOf(text)
            else -> text.split(',', '，')
                .map(String::trim)
                .filter { it.startsWith("http://") || it.startsWith("https://") }
        }
    }
    else -> emptyList()
}

private fun JSONObject.objectListOf(vararg keys: String): List<JSONObject> {
    for (key in keys) {
        val value = opt(key)
        val array = when (value) {
            is JSONArray -> value
            is String -> runCatching { JSONArray(value) }.getOrNull()
            else -> null
        } ?: continue
        val values = buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(it) }
            }
        }
        if (values.isNotEmpty()) return values
    }
    return emptyList()
}

@Composable
internal fun ServiceTeamDialog(team: ServiceTeam, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        ServiceTeamDialogContent(team = team, onDismiss = onDismiss)
    }
}

@Composable
private fun ServiceTeamDialogContent(team: ServiceTeam, onDismiss: () -> Unit) {
    val closeFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val contentListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HelpDialogScrim),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .tvDialogPanel(RoundedCornerShape(12.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        return@onPreviewKeyEvent false
                    }
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (!contentListState.canScrollForward) {
                                false
                            } else {
                                contentFocusRequester.requestFocus()
                                coroutineScope.launch { contentListState.animateScrollBy(190f) }
                                true
                            }
                        }
                        Key.DirectionUp -> {
                            if (!contentListState.canScrollBackward) {
                                false
                            } else {
                                contentFocusRequester.requestFocus()
                                coroutineScope.launch { contentListState.animateScrollBy(-190f) }
                                true
                            }
                        }
                        else -> false
                    }
                },
        ) {
            LazyColumn(
                state = contentListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .focusRequester(contentFocusRequester)
                    .logFocusTarget("ServiceTeam.Content")
                    .focusable(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = team.name,
                            color = Color.White,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        TvDialogCloseButton(
                            onClick = onDismiss,
                            focusRequester = closeFocusRequester,
                        )
                    }
                }
                if (team.services.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ServiceLabels(team.services)
                        }
                    }
                }
                if (team.introduction.isNotBlank()) {
                    item {
                        ServiceSection(title = "服务队简介") {
                            Text(
                                text = team.introduction,
                                color = HelpDialogWarmWhite,
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HelpDialogCardRed)
                                    .border(
                                        1.dp,
                                        HelpDialogGoldBorder,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(16.dp),
                            )
                        }
                    }
                }
                if (team.photos.isNotEmpty()) {
                    item {
                        ServiceSection(title = "服务队照片") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(team.photos) { photo ->
                                    RemoteImage(
                                        url = photo,
                                        modifier = Modifier
                                            .size(width = 210.dp, height = 126.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                1.dp,
                                                HelpDialogGoldBorder,
                                                RoundedCornerShape(12.dp),
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
                if (team.members.isNotEmpty()) {
                    item { SectionHeading("服务队成员") }
                    items(team.members) { member ->
                        ServiceTeamMemberCard(member = member)
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }
    }
    LaunchedEffect(team.name) { closeFocusRequester.requestFocus() }
}

@Composable
private fun ServiceSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading(title)
        content()
    }
}

@Composable
private fun SectionHeading(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(HelpDialogGold),
        )
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun ServiceTeamMemberCard(member: ServiceTeamMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HelpDialogCardRed)
            .border(1.dp, HelpDialogGoldBorder, RoundedCornerShape(10.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(
            url = member.avatarUrl,
            fallbackText = member.name.take(1),
            modifier = Modifier.size(width = 128.dp, height = 112.dp).clip(RoundedCornerShape(8.dp)),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("姓名：${member.name}", color = HelpDialogWarmWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (member.role.isNotBlank()) Text("职务：${member.role}", color = HelpDialogMutedText, fontSize = 16.sp)
            if (member.phone.isNotBlank()) Text("联系电话：${member.phone}", color = HelpDialogLightGold, fontSize = 16.sp)
            if (member.services.isNotEmpty()) ServiceLabels(member.services)
        }
    }
}

@Composable
private fun ServiceLabels(services: List<String>, modifier: Modifier = Modifier) {
    Text(
        text = services.take(4).mapIndexed { index, service -> "${serviceIcon(index)} $service" }.joinToString("   "),
        color = HelpDialogLightGold,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(HelpDialogCardRed)
            .border(1.dp, HelpDialogGoldBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private fun serviceIcon(index: Int): String = when (index % 4) {
    0 -> "♟"
    1 -> "▲"
    2 -> "◆"
    else -> "♻"
}

@Composable
private fun RemoteImage(url: String, modifier: Modifier, fallbackText: String = "照片") {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, url) {
        value = if (url.startsWith("http://") || url.startsWith("https://")) {
            withContext(Dispatchers.IO) {
                runCatching { URL(url).openStream().use(BitmapFactory::decodeStream) }.getOrNull()
            }
        } else {
            null
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(HelpDialogCardRed), contentAlignment = Alignment.Center) {
            Text(fallbackText, color = HelpDialogLightGold, fontSize = 14.sp)
        }
    }
}

@Preview(name = "党员服务队弹窗", widthDp = 960, heightDp = 540, showBackground = true)
@Composable
private fun ServiceTeamDialogPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color(0xB8000000)),
            contentAlignment = Alignment.Center,
        ) {
            ServiceTeamDialogContent(
                team = ServiceTeam(
                    name = "白角村党员服务队",
                    services = listOf("道路抢险", "应急救援", "纠纷调解", "环境治理"),
                    introduction = "立足本村山地多、坡度大的地理特点，重点防范森林防火、山体滑坡、泥石流等灾害，组建巡山护林小分队，常态化开展山林巡查和隐患点监测。",
                    photos = listOf(
                        "https://example.com/service-team-1.jpg",
                        "https://example.com/service-team-2.jpg",
                    ),
                    members = listOf(
                        ServiceTeamMember(
                            name = "李*",
                            role = "队长",
                            avatarUrl = "https://example.com/member-1.jpg",
                            phone = "13800000000",
                            services = listOf("道路抢险", "应急救援"),
                        ),
                        ServiceTeamMember(
                            name = "王*",
                            role = "队员",
                            avatarUrl = "https://example.com/member-2.jpg",
                            phone = "13900000000",
                            services = listOf("纠纷调解", "环境治理"),
                        ),
                    ),
                    navigationUrl = "https://example.com/map",
                    helpUrl = "https://example.com/help",
                ),
                onDismiss = {},
            )
        }
    }
}
