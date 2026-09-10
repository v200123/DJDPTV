package com.fpa.dangjiandaping.network

import android.content.Context
import android.util.Log
import com.fpa.dangjiandaping.config.GlobalVariables
import com.fpa.dangjiandaping.network.model.AndmuDevice
import com.fpa.dangjiandaping.network.model.AndmuDeviceListResult
import com.fpa.dangjiandaping.network.model.AndmuRealtimeThumbnailResult
import com.fpa.dangjiandaping.network.model.AndmuTokenResult
import com.fpa.dangjiandaping.network.model.AndmuWebSdkPlayerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

/** 安牧开放平台 Retrofit 客户端：负责 token 缓存、token 请求和设备列表请求。 */
object AndmuApiClient {
    /** 优先复用仍在 6 天安全窗口内的本地 token；没有可用缓存时才调用远端接口。 */
    suspend fun getCachedOrRequestApplicationToken(context: Context): AndmuTokenResult {
        readCachedToken(context)?.let { cached ->
            GlobalVariables.andmuToken = cached.token
            GlobalVariables.andmuTokenExpiresAtMillis = cached.expiresAtMillis
            return AndmuTokenResult(
                resultCode = SUCCESS_CODE,
                resultMessage = "使用本地缓存 token",
                token = cached.token,
                expiresInSeconds = (cached.expiresAtMillis - System.currentTimeMillis()) / 1_000L,
                fromLocalCache = true,
                rawResponseJson = cached.rawResponseJson,
            )
        }
        return requestApplicationToken(context)
    }

    /**
     * 服务端已明确拒绝当前 token 时删除本地缓存，确保下一次获取走申请新 token 的接口。
     * token 缓存使用独立的 SharedPreferences，不影响应用的其它本地数据。
     */
    fun clearCachedApplicationToken(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        GlobalVariables.andmuToken = ""
        GlobalVariables.andmuTokenExpiresAtMillis = 0L
    }

    /** 使用有效 token 获取第 1 页最多 100 条在线设备。 */
    suspend fun requestDeviceList(token: String): AndmuDeviceListResult = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "获取设备列表需要有效的安牧 token。" }
        val requestBody = "{\"page\":1,\"pageSize\":100,\"onlineStatus\":1}"
        val response = api.requestDeviceList(
            headers = AndmuRequestSigner.createHeaders(requestBody, token),
            requestBody = requestBody.toRequestBody(JSON_MEDIA_TYPE),
        )
        val rawResponseJson = response.readBodyText("安牧设备列表接口")
        val responseJson = JSONObject(rawResponseJson)
        val devices = responseJson.optJSONArray("data")?.let { deviceArray ->
            List(deviceArray.length()) { index ->
                deviceArray.optJSONObject(index)?.let { deviceJson ->
                    AndmuDevice(
                        deviceId = deviceJson.optString("deviceId"),
                        deviceName = deviceJson.optString("deviceName"),
                        deviceAddress = deviceJson.optString("deviceAddress"),
                        deviceStatus = deviceJson.optInt("deviceStatus"),
                    )
                }
            }.filterNotNull()
        }.orEmpty()
        return@withContext AndmuDeviceListResult(
            resultCode = responseJson.optString("resultCode"),
            resultMessage = responseJson.optString("resultMsg"),
            total = responseJson.optInt("total"),
            devices = devices,
            rawResponseJson = rawResponseJson,
        )
    }

    /** 获取摄像机实时缩略图下载地址。 */
    suspend fun requestRealtimeThumbnailUrl(
        token: String,
        deviceId: String,
    ): AndmuRealtimeThumbnailResult = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "获取实时缩略图需要有效的安牧 token。" }
        require(deviceId.isNotBlank()) { "获取实时缩略图需要有效的 deviceId。" }
        val requestBody = "{\"deviceId\":${JSONObject.quote(deviceId)}}"
        val response = api.requestRealtimeThumbnailUrl(
            headers = AndmuRequestSigner.createHeaders(requestBody, token),
            requestBody = requestBody.toRequestBody(JSON_MEDIA_TYPE),
        )
        val rawResponseJson = response.readBodyText("安牧实时缩略图接口")
        val responseJson = JSONObject(rawResponseJson)
        return@withContext AndmuRealtimeThumbnailResult(
            resultCode = responseJson.optString("resultCode"),
            resultMessage = responseJson.optString("resultMsg"),
            thumbnailUrl = responseJson.optJSONObject("data")
                ?.optString("url")
                ?.takeIf { it.isNotBlank() },
            rawResponseJson = rawResponseJson,
        )
    }

    /** 获取指定在线摄像机的 WebSDK 播放地址；链接到期或使用一次后失效。 */
    suspend fun requestWebSdkPlayerUrl(token: String, deviceId: String): AndmuWebSdkPlayerResult =
        withContext(Dispatchers.IO) {
            require(token.isNotBlank()) { "获取 WebSDK 播放链接需要有效的安牧 token。" }
            require(deviceId.isNotBlank()) { "获取 WebSDK 播放链接需要有效的 deviceId。" }
            val requestBody = "{\"deviceId\":${JSONObject.quote(deviceId)}}"
            val response = api.requestWebSdkPlayerUrl(
                headers = AndmuRequestSigner.createHeaders(requestBody, token),
                requestBody = requestBody.toRequestBody(JSON_MEDIA_TYPE),
            )
            val rawResponseJson = response.readBodyText("安牧 WebSDK 播放接口")
            val responseJson = JSONObject(rawResponseJson)
            val data = responseJson.optJSONObject("data")
            return@withContext AndmuWebSdkPlayerResult(
                resultCode = responseJson.optString("resultCode"),
                resultMessage = responseJson.optString("resultMsg"),
                deviceId = deviceId,
                playerUrl = data?.optString("url")?.takeIf { it.isNotBlank() },
                expiresInSeconds = data?.takeIf { it.has("expiresIn") }?.optLong("expiresIn"),
                rawResponseJson = rawResponseJson,
            )
        }

    private suspend fun requestApplicationToken(context: Context): AndmuTokenResult = withContext(Dispatchers.IO) {
        require(GlobalVariables.isAndmuConfigured) {
            "请先在 GlobalVariables 中填写 andmuAppId、andmuSecret 和 andmuPrivateKey。"
        }
        val applicationSignature = AndmuRequestSigner.md5(
            "${GlobalVariables.andmuAppId}${GlobalVariables.andmuSecret}",
        )
        // 此字符串既是 HTTP body，也是计算 md5 的唯一来源；请勿重新格式化。
        val requestBody = "{\"operatorType\":1,\"sig\":\"$applicationSignature\"}"
        val response = api.requestApplicationToken(
            headers = AndmuRequestSigner.createHeaders(requestBody),
            requestBody = requestBody.toRequestBody(JSON_MEDIA_TYPE),
        )
        val responseText = response.readBodyText("安牧 token 接口")
        parseTokenResponse(responseText).also { result ->
            if (result.isSuccess) {
                saveToken(
                    context = context,
                    token = result.token.orEmpty(),
                    serverExpiresInSeconds = result.expiresInSeconds,
                    rawResponseJson = responseText,
                )
            }
        }
    }

    private fun parseTokenResponse(responseText: String): AndmuTokenResult {
        val response = JSONObject(responseText)
        val data = response.optJSONObject("data")
        return AndmuTokenResult(
            resultCode = response.optString("resultCode"),
            resultMessage = response.optString("resultMsg"),
            token = data?.optString("token")?.takeIf { it.isNotBlank() },
            expiresInSeconds = data?.takeIf { it.has("expires_in") }?.optLong("expires_in"),
            rawResponseJson = responseText,
        )
    }

    private fun readCachedToken(context: Context): CachedToken? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val token = preferences.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val expiresAtMillis = preferences.getLong(KEY_EXPIRES_AT_MILLIS, 0L)
        if (expiresAtMillis > System.currentTimeMillis()) {
            return CachedToken(
                token = token,
                expiresAtMillis = expiresAtMillis,
                rawResponseJson = preferences.getString(KEY_RAW_RESPONSE_JSON, "").orEmpty(),
            )
        }
        clearCachedApplicationToken(context)
        return null
    }

    private fun saveToken(
        context: Context,
        token: String,
        serverExpiresInSeconds: Long?,
        rawResponseJson: String,
    ) {
        val serverValidityMillis = serverExpiresInSeconds
            ?.coerceAtLeast(0L)
            ?.times(1_000L)
            ?: LOCAL_TOKEN_VALIDITY_MILLIS
        // 官方有效期为 7 天；提前一天刷新，避免设备时间漂移或临界请求失败。
        val expiresAtMillis = System.currentTimeMillis() +
            minOf(serverValidityMillis, LOCAL_TOKEN_VALIDITY_MILLIS)
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRES_AT_MILLIS, expiresAtMillis)
            .putString(KEY_RAW_RESPONSE_JSON, rawResponseJson)
            .apply()
        GlobalVariables.andmuToken = token
        GlobalVariables.andmuTokenExpiresAtMillis = expiresAtMillis
    }

    private fun Response<ResponseBody>.readBodyText(apiName: String): String {
        val responseBody = if (isSuccessful) body() else errorBody()
            ?: throw IllegalStateException("$apiName 返回 HTTP ${code()}，且没有响应内容。")
        return responseBody?.string()?:""
    }

    private data class CachedToken(
        val token: String,
        val expiresAtMillis: Long,
        val rawResponseJson: String,
    )

    private interface AndmuApi {
        @POST("v3/open/api/token")
        suspend fun requestApplicationToken(
            @HeaderMap headers: Map<String, String>,
            @Body requestBody: okhttp3.RequestBody,
        ): Response<ResponseBody>

        @POST("v3/open/api/device/list")
        suspend fun requestDeviceList(
            @HeaderMap headers: Map<String, String>,
            @Body requestBody: okhttp3.RequestBody,
        ): Response<ResponseBody>

        @POST("v3/open/api/websdk/player")
        suspend fun requestWebSdkPlayerUrl(
            @HeaderMap headers: Map<String, String>,
            @Body requestBody: okhttp3.RequestBody,
        ): Response<ResponseBody>

        @POST("v3/open/api/camera/thumbnail/realtime")
        suspend fun requestRealtimeThumbnailUrl(
            @HeaderMap headers: Map<String, String>,
            @Body requestBody: okhttp3.RequestBody,
        ): Response<ResponseBody>
    }

    private val api: AndmuApi by lazy {
        Retrofit.Builder()
            .baseUrl(ANDMU_BASE_URL)
            .client(httpClient)
            .build()
            .create(AndmuApi::class.java)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                try {
                    chain.proceed(request).also { response ->
                        Log.i(
                            LOG_TAG,
                            "Retrofit 响应：${request.method} ${request.url}，" +
                                "HTTP ${response.code}，responseJson=" +
                                response.peekBody(LOG_RESPONSE_BODY_MAX_BYTES).string(),
                        )
                    }
                } catch (error: Exception) {
                    Log.e(LOG_TAG, "Retrofit 请求失败：${request.method} ${request.url}", error)
                    throw error
                }
            }
            .build()
    }

    private const val SUCCESS_CODE = "000000"
    private const val LOG_TAG = "AndmuApiClient"
    private const val ANDMU_BASE_URL = "https://open.qly.cmviot.cn/"
    private const val PREFERENCES_NAME = "andmu_token_cache"
    private const val KEY_TOKEN = "token"
    private const val KEY_EXPIRES_AT_MILLIS = "expires_at_millis"
    private const val KEY_RAW_RESPONSE_JSON = "raw_response_json"
    private const val LOCAL_TOKEN_VALIDITY_MILLIS = 6L * 24 * 60 * 60 * 1_000L
    private const val LOG_RESPONSE_BODY_MAX_BYTES = 1_048_576L
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
}
