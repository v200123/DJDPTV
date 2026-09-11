package com.fpa.dangjiandaping.ui.andmu

import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.config.GlobalVariables
import com.fpa.dangjiandaping.network.AndmuApiClient
import com.fpa.dangjiandaping.network.model.AndmuDevice
import com.fpa.dangjiandaping.ui.focus.focusOnClick
import com.fpa.dangjiandaping.ui.focus.logFocusTarget
import com.fpa.dangjiandaping.ui.web.AndmuWebPlayerActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.net.URL

private const val SUCCESS_CODE = "000000"

/** “千里眼”原生设备页：只展示在线设备，按确认键后才申请会计费的播放链接。 */
@Composable
internal fun AndmuDeviceScreen(
    active: Boolean,
    contentFocusRequester: FocusRequester,
    onRequestTabFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firstItemFocusRequester = remember { FocusRequester() }
    val deviceGridState = rememberLazyGridState()
    val deviceUiState by AndmuDeviceStore.state.collectAsState()
    var contentHasFocus by remember { mutableStateOf(false) }
    var firstRowHasFocus by remember { mutableStateOf(false) }
    var focusedColumn by remember { mutableStateOf(0) }
    val firstRowFocusRequesters = remember(deviceUiState.cameras) {
        List(minOf(DEVICE_GRID_COLUMNS, deviceUiState.cameras.size)) { index ->
            if (index == 0) firstItemFocusRequester else FocusRequester()
        }
    }
    var playerErrorMessage by remember { mutableStateOf<String?>(null) }
    var openingDeviceId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        AndmuDeviceStore.loadIfNeeded(context)
        while (true) {
            delay(DEVICE_LIST_REFRESH_INTERVAL_MILLIS)
            AndmuDeviceStore.refresh(context)
        }
    }

    LaunchedEffect(active, contentHasFocus, deviceUiState.loading, deviceUiState.cameras) {
        if (active && contentHasFocus && !deviceUiState.loading && deviceUiState.cameras.isNotEmpty()) {
            withFrameNanos { }
            firstItemFocusRequester.requestFocus(FocusDirection.Down)
        }
    }

    BackHandler(
        enabled = active && contentHasFocus && deviceUiState.cameras.isNotEmpty(),
    ) {
        if (firstRowHasFocus) {
            onRequestTabFocus()
        } else {
            scope.launch {
                deviceGridState.animateScrollToItem(index = focusedColumn)
                withFrameNanos { }
                (firstRowFocusRequesters.getOrNull(focusedColumn) ?: firstItemFocusRequester)
                    .requestFocus(FocusDirection.Up)
            }
        }
    }

    Box(
        modifier = modifier
            .focusRequester(contentFocusRequester)
            .logFocusTarget("Andmu.DeviceList")
            .focusProperties {
                up = FocusRequester.Cancel
                if (deviceUiState.cameras.isNotEmpty()) down = firstItemFocusRequester
            }
            .onFocusChanged { contentHasFocus = it.isFocused }
            .focusable(),
    ) {
        AndmuDeviceContent(
            deviceUiState = deviceUiState,
            gridState = deviceGridState,
            firstRowFocusRequesters = firstRowFocusRequesters,
            openingDeviceId = openingDeviceId,
            onFirstRowUp = onRequestTabFocus,
            onFirstRowFocusChanged = { firstRow, column ->
                firstRowHasFocus = firstRow
                focusedColumn = column
            },
            onOpenDevice = { item ->
                if (openingDeviceId == null) {
                    openingDeviceId = item.device.deviceId
                    scope.launch {
                        runCatching {
                            val token = GlobalVariables.andmuToken
                            require(token.isNotBlank()) { "没有可用的千里眼 token。" }
                            AndmuApiClient.requestWebSdkPlayerUrl(token, item.device.deviceId)
                        }.onSuccess { playerResult ->
                            if (playerResult.resultCode == SUCCESS_CODE &&
                                !playerResult.playerUrl.isNullOrBlank()
                            ) {
                                context.startActivity(
                                    AndmuWebPlayerActivity.newIntent(
                                        context = context,
                                        url = playerResult.playerUrl,
                                        title = "${item.device.deviceName}实时画面",
                                    ),
                                )
                            } else {
                                playerErrorMessage = playerResult.resultMessage.ifBlank {
                                    "获取实时画面播放链接失败。"
                                }
                            }
                        }.onFailure { error ->
                            playerErrorMessage = error.message ?: "获取实时画面播放链接失败。"
                        }
                        openingDeviceId = null
                    }
                }
            },
        )

        playerErrorMessage?.let { message ->
            CenterMessage(message)
        }
    }
}

@Composable
private fun AndmuDeviceContent(
    deviceUiState: AndmuDeviceUiState,
    gridState: LazyGridState,
    firstRowFocusRequesters: List<FocusRequester>,
    openingDeviceId: String?,
    onFirstRowUp: () -> Unit,
    onFirstRowFocusChanged: (Boolean, Int) -> Unit,
    onOpenDevice: (AndmuCameraItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        deviceUiState.cameras.isNotEmpty() -> LazyVerticalGrid(
            columns = GridCells.Fixed(DEVICE_GRID_COLUMNS),
            state = gridState,
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = deviceUiState.cameras,
                key = { _, item -> item.device.deviceId },
            ) { index, item ->
                val itemFocusRequester = firstRowFocusRequesters.getOrNull(index)
                    ?: remember { FocusRequester() }
                AndmuCameraCard(
                    item = item,
                    focusRequester = itemFocusRequester,
                    firstRow = index < DEVICE_GRID_COLUMNS,
                    column = index % DEVICE_GRID_COLUMNS,
                    last = index == deviceUiState.cameras.lastIndex,
                    opening = openingDeviceId == item.device.deviceId,
                    onFirstRowUp = onFirstRowUp,
                    onFirstRowFocusChanged = onFirstRowFocusChanged,
                    onOpen = { onOpenDevice(item) },
                )
            }
        }

        deviceUiState.loading -> CenterMessage("正在加载在线设备", modifier)
        deviceUiState.errorMessage != null -> CenterMessage(deviceUiState.errorMessage.orEmpty(), modifier)
        else -> CenterMessage("暂无在线设备", modifier)
    }
}

@Composable
private fun AndmuCameraCard(
    item: AndmuCameraItem,
    focusRequester: FocusRequester,
    firstRow: Boolean,
    column: Int,
    last: Boolean,
    opening: Boolean,
    onFirstRowUp: () -> Unit,
    onFirstRowFocusChanged: (Boolean, Int) -> Unit,
    onOpen: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var confirmPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(DEVICE_CARD_HEIGHT)
            .focusRequester(focusRequester)
            .logFocusTarget("Andmu.Device[${item.device.deviceId}]")
            .focusOnClick(focusRequester)
            .focusProperties {
                if (firstRow) up = FocusRequester.Cancel
                if (last) down = FocusRequester.Cancel
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) {
                    onFirstRowFocusChanged(firstRow, column)
                }
                if (!it.isFocused) confirmPressed = false
            }
            .clip(shape)
            .background(if (focused) FocusedCardColor else CardColor)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) FocusBorderColor else CardBorderColor,
                shape = shape,
            )
            .onPreviewKeyEvent { event ->
                when {
                    firstRow && event.key == Key.DirectionUp -> {
                        if (event.type == KeyEventType.KeyDown) onFirstRowUp()
                        true
                    }

                    event.key !in ConfirmKeys -> false
                    event.type == KeyEventType.KeyDown -> {
                        confirmPressed = true
                        true
                    }

                    event.type == KeyEventType.KeyUp && confirmPressed -> {
                        confirmPressed = false
                        onOpen()
                        true
                    }

                    else -> false
                }
            }
            .clickable {
                focusRequester.requestFocus()
                onOpen()
            }
            .focusable(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DEVICE_THUMBNAIL_HEIGHT),
        ) {
            RealtimeThumbnail(
                url = item.thumbnailUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = ThumbnailBorderColor,
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PlayButtonColor)
                    .border(1.dp, ThumbnailBorderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶", color = TitleColor, fontSize = 11.sp)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(CardFooterColor)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.device.deviceName.ifBlank { "未命名摄像机" },
                color = TitleColor,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Text(
                text = if (opening) {
                    "正在获取实时画面…"
                } else {
                    "地址：${item.device.deviceAddress.ifBlank { "" }}"
                },
                color = SubtitleColor,
                fontSize = 9.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RealtimeThumbnail(url: String?, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, url) {
        value = url?.takeIf { it.isNotBlank() }?.let { imageUrl ->
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(imageUrl).openStream().use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        }
    }
    if (bitmap == null) {
        Box(
            modifier = modifier.background(ThumbnailPlaceholderColor),
            contentAlignment = Alignment.Center,
        ) {
            Text("暂无缩略图", color = SubtitleColor, fontSize = 12.sp)
        }
    } else {
        AndroidView(
            factory = { context ->
                ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            },
            update = { it.setImageBitmap(bitmap) },
            modifier = modifier,
        )
    }
}

@Composable
private fun CenterMessage(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = TitleColor, fontSize = 18.sp)
    }
}

private val PreviewCameras = listOf(
    AndmuCameraItem(
        device = AndmuDevice(
            deviceId = "preview-camera-1",
            deviceName = "党员教育示范点摄像机",
            deviceAddress = "成都市高新区",
            deviceStatus = 1,
        ),
    ),
    AndmuCameraItem(
        device = AndmuDevice(
            deviceId = "preview-camera-2",
            deviceName = "基层党群服务中心摄像机",
            deviceAddress = "成都市武侯区",
            deviceStatus = 1,
        ),
    ),
    AndmuCameraItem(
        device = AndmuDevice(
            deviceId = "preview-camera-3",
            deviceName = "乡村振兴实训基地摄像机",
            deviceAddress = "成都市郫都区",
            deviceStatus = 1,
        ),
    ),
    AndmuCameraItem(
        device = AndmuDevice(
            deviceId = "preview-camera-4",
            deviceName = "红色文化教育基地摄像机",
            deviceAddress = "成都市锦江区",
            deviceStatus = 1,
        ),
    ),
    AndmuCameraItem(
        device = AndmuDevice(
            deviceId = "preview-camera-5",
            deviceName = "党员活动室摄像机",
            deviceAddress = "成都市青羊区",
            deviceStatus = 1,
        ),
    ),
    AndmuCameraItem(
        device = AndmuDevice(
            deviceId = "preview-camera-6",
            deviceName = "社区服务中心摄像机",
            deviceAddress = "成都市金牛区",
            deviceStatus = 1,
        ),
    ),
    AndmuCameraItem(
        device = AndmuDevice(
            deviceId = "preview-camera-7",
            deviceName = "乡村振兴直播间摄像机",
            deviceAddress = "成都市双流区",
            deviceStatus = 1,
        ),
    ),
    AndmuCameraItem(
        device = AndmuDevice(
            deviceId = "preview-camera-8",
            deviceName = "党群驿站摄像机",
            deviceAddress = "成都市新都区",
            deviceStatus = 1,
        ),
    ),
)

@Preview(
    name = "千里眼 - 在线设备",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun AndmuDeviceContentPreview() {
    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PreviewBackgroundColor)
            .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        AndmuDeviceContent(
            deviceUiState = AndmuDeviceUiState(cameras = PreviewCameras),
            gridState = rememberLazyGridState(),
            firstRowFocusRequesters = List(DEVICE_GRID_COLUMNS) { index ->
                if (index == 0) firstItemFocusRequester else FocusRequester()
            },
            openingDeviceId = PreviewCameras[1].device.deviceId,
            onFirstRowUp = {},
            onFirstRowFocusChanged = { _, _ -> },
            onOpenDevice = {},
        )
    }
}

@Preview(
    name = "千里眼 - 空设备",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun AndmuDeviceEmptyPreview() {
    val firstItemFocusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PreviewBackgroundColor),
    ) {
        AndmuDeviceContent(
            deviceUiState = AndmuDeviceUiState(hasLoaded = true),
            gridState = rememberLazyGridState(),
            firstRowFocusRequesters = listOf(firstItemFocusRequester),
            openingDeviceId = null,
            onFirstRowUp = {},
            onFirstRowFocusChanged = { _, _ -> },
            onOpenDevice = {},
        )
    }
}

private val ConfirmKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)
private const val DEVICE_LIST_REFRESH_INTERVAL_MILLIS = 60_000L
private const val DEVICE_GRID_COLUMNS = 4
private val DEVICE_CARD_HEIGHT = 164.dp
private val DEVICE_THUMBNAIL_HEIGHT = 110.dp
// 与党建红主界面的文章列表一致：卡片透出页面背景，焦点使用金色描边。
private val CardColor = androidx.compose.ui.graphics.Color(0xB8B8171C)
private val FocusedCardColor = androidx.compose.ui.graphics.Color(0xD6A90E18)
private val CardFooterColor = androidx.compose.ui.graphics.Color(0xD06E0000)
private val CardBorderColor = androidx.compose.ui.graphics.Color(0x45FFD896)
private val FocusBorderColor = androidx.compose.ui.graphics.Color(0xFFFFD889)
private val ThumbnailPlaceholderColor = androidx.compose.ui.graphics.Color(0x993F080C)
private val ThumbnailBorderColor = androidx.compose.ui.graphics.Color(0x66FFD896)
private val PlayButtonColor = androidx.compose.ui.graphics.Color(0xB8000000)
private val TitleColor = androidx.compose.ui.graphics.Color(0xFFFFF0D4)
private val SubtitleColor = androidx.compose.ui.graphics.Color(0xD6FFD39B)
private val PreviewBackgroundColor = androidx.compose.ui.graphics.Color(0xFFB80912)
