package com.fpa.dangjiandaping.ui.web

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

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
    return ServiceTeam(
        name = record.stringOf("name", "teamName", "title", "serviceTeamName")
            .ifBlank { "党员服务队" },
        services = record.stringListOf("services", "serviceItems", "tags", "serviceTypes"),
        introduction = record.stringOf("introduction", "intro", "description", "content"),
        photos = record.stringListOf("photos", "images", "teamPhotos", "photoUrls"),
        members = record.objectListOf("members", "teamMembers", "memberList", "serviceMembers")
            .map(::parseServiceTeamMember),
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
    name = record.stringOf("name", "memberName", "realName").ifBlank { "服务队成员" },
    role = record.stringOf("role", "position", "job", "duty"),
    avatarUrl = record.stringOf("avatarUrl", "avatar", "photo", "image", "headImage"),
    phone = record.stringOf("phone", "mobile", "telephone", "tel"),
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

private fun JSONObject.objectListOf(vararg keys: String): List<JSONObject> {
    for (key in keys) {
        val array = optJSONArray(key) ?: continue
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val closeFocusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .fillMaxHeight(0.88f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = team.name,
                    color = Color(0xFF202020),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                DialogActionButton(
                    text = "×",
                    color = Color(0xFFF1F1F1),
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(closeFocusRequester),
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (team.services.isNotEmpty()) {
                    ServiceLabels(team.services)
                }
            }

            Spacer(Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (team.introduction.isNotBlank()) {
                    item {
                        ServiceSection(title = "简介") {
                            Text(
                                text = team.introduction,
                                color = Color(0xFF5E5E5E),
                                fontSize = 17.sp,
                                lineHeight = 25.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .padding(14.dp),
                            )
                        }
                    }
                }
                if (team.photos.isNotEmpty()) {
                    item {
                        ServiceSection(title = "服务队照片") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(team.photos) { photo ->
                                    RemoteImage(
                                        url = photo,
                                        modifier = Modifier
                                            .size(width = 180.dp, height = 110.dp)
                                            .clip(RoundedCornerShape(10.dp)),
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
                .background(Color(0xFFD83E3E)),
        )
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF262626))
    }
}

@Composable
private fun ServiceTeamMemberCard(member: ServiceTeamMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFAFAFA))
            .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(
            url = member.avatarUrl,
            fallbackText = member.name.take(1),
            modifier = Modifier.size(width = 128.dp, height = 112.dp).clip(RoundedCornerShape(8.dp)),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("姓名：${member.name}", color = Color(0xFF1E1E1E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (member.role.isNotBlank()) Text("职务：${member.role}", color = Color(0xFF333333), fontSize = 16.sp)
            if (member.services.isNotEmpty()) ServiceLabels(member.services)
        }
    }
}

@Composable
private fun ServiceLabels(services: List<String>, modifier: Modifier = Modifier) {
    Text(
        text = services.take(4).mapIndexed { index, service -> "${serviceIcon(index)} $service" }.joinToString("   "),
        color = Color(0xFF3C4C5B),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun serviceIcon(index: Int): String = when (index % 4) {
    0 -> "♟"
    1 -> "▲"
    2 -> "◆"
    else -> "♻"
}

@Composable
private fun DialogActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val clickFocusRequester = remember { FocusRequester() }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "serviceTeamActionScale")
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(color)
            .border(if (focused) 2.dp else 0.dp, Color(0xFFFFD889), shape)
            .focusRequester(clickFocusRequester)
            .clickable {
                clickFocusRequester.requestFocus()
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (text == "×") Color(0xFF777777) else Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
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
        Box(modifier.background(Color(0xFFE8E8E8)), contentAlignment = Alignment.Center) {
            Text(fallbackText, color = Color(0xFF777777), fontSize = 14.sp)
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
