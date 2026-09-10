package com.fpa.dangjiandaping.ui.andmu

import android.content.Context
import com.fpa.dangjiandaping.config.GlobalVariables
import com.fpa.dangjiandaping.network.AndmuApiClient
import com.fpa.dangjiandaping.network.model.AndmuDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal data class AndmuCameraItem(
    val device: AndmuDevice,
    val thumbnailUrl: String? = null,
)

internal data class AndmuDeviceUiState(
    val loading: Boolean = false,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
    val cameras: List<AndmuCameraItem> = emptyList(),
)

/**
 * 千里眼设备的进程内缓存。
 *
 * 将加载任务置于独立作用域，页面或 Tab 被销毁后缩略图请求仍能继续，返回页面可直接复用数据。
 */
internal object AndmuDeviceStore {
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(AndmuDeviceUiState())

    val state: StateFlow<AndmuDeviceUiState> = mutableState

    fun loadIfNeeded(context: Context) {
        load(context, forceRefresh = false)
    }

    fun refresh(context: Context) {
        load(context, forceRefresh = true)
    }

    private fun load(context: Context, forceRefresh: Boolean) {
        synchronized(this) {
            if (mutableState.value.loading || (!forceRefresh && mutableState.value.hasLoaded)) return
            mutableState.value = mutableState.value.copy(loading = true, errorMessage = null)
        }
        val appContext = context.applicationContext
        storeScope.launch {
            val deviceListResult = runCatching {
                require(GlobalVariables.isAndmuConfigured) { "请先填写千里眼应用凭据。" }
                var tokenResult = AndmuApiClient.getCachedOrRequestApplicationToken(appContext)
                require(tokenResult.isSuccess) { tokenResult.resultMessage.ifBlank { "获取 token 失败。" } }
                var deviceResult = AndmuApiClient.requestDeviceList(tokenResult.token.orEmpty())
                if (deviceResult.resultMessage.trim() == TOKEN_EXPIRED_MESSAGE) {
                    // 服务端 token 的有效状态优先于本地过期时间；仅清一次并只重试一次。
                    AndmuApiClient.clearCachedApplicationToken(appContext)
                    tokenResult = AndmuApiClient.getCachedOrRequestApplicationToken(appContext)
                    require(tokenResult.isSuccess) {
                        tokenResult.resultMessage.ifBlank { "重新获取 token 失败。" }
                    }
                    deviceResult = AndmuApiClient.requestDeviceList(tokenResult.token.orEmpty())
                }
                require(deviceResult.resultCode == SUCCESS_CODE) {
                    deviceResult.resultMessage.ifBlank { "获取在线设备列表失败。" }
                }
                tokenResult.token.orEmpty() to deviceResult.devices
            }
            val (token, devices) = deviceListResult.getOrElse { error ->
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    errorMessage = error.message ?: "加载在线设备失败。",
                )
                return@launch
            }

            val existingThumbnailByDeviceId = mutableState.value.cameras.associate { item ->
                item.device.deviceId to item.thumbnailUrl
            }
            val cameras = devices
                .filter { it.deviceStatus == 1 && it.deviceId.isNotBlank() }
                .map { device ->
                    AndmuCameraItem(
                        device = device,
                        thumbnailUrl = existingThumbnailByDeviceId[device.deviceId],
                    )
                }
            mutableState.value = AndmuDeviceUiState(
                hasLoaded = true,
                cameras = cameras,
            )
            loadRealtimeThumbnails(
                token = token,
                devices = devices.filter { existingThumbnailByDeviceId[it.deviceId].isNullOrBlank() },
            )
        }
    }

    private suspend fun loadRealtimeThumbnails(token: String, devices: List<AndmuDevice>) {
        for (deviceChunk in devices.filter { it.deviceStatus == 1 && it.deviceId.isNotBlank() }.chunked(4)) {
            val updates = coroutineScope {
                deviceChunk.map { device ->
                    async {
                        val thumbnail = runCatching {
                            AndmuApiClient.requestRealtimeThumbnailUrl(token, device.deviceId)
                        }.getOrNull()
                        device.deviceId to thumbnail
                            ?.takeIf { it.resultCode == SUCCESS_CODE }
                            ?.thumbnailUrl
                    }
                }.awaitAll()
            }
            updates.forEach { (deviceId, thumbnailUrl) ->
                mutableState.value = mutableState.value.copy(
                    cameras = mutableState.value.cameras.map { item ->
                        if (item.device.deviceId == deviceId) {
                            item.copy(thumbnailUrl = thumbnailUrl)
                        } else {
                            item
                        }
                    },
                )
            }
        }
    }
}

private const val SUCCESS_CODE = "000000"
private const val TOKEN_EXPIRED_MESSAGE = "token已过期"
