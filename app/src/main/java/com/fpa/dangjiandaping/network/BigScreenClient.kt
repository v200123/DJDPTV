package com.fpa.dangjiandaping.network

import android.util.Log
import com.fpa.dangjiandaping.BuildConfig
import com.fpa.dangjiandaping.network.api.BigScreenDeviceLoginApi
import com.fpa.dangjiandaping.network.api.LearnTimesRequest
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginException
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginRequest
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginResponse
import com.fpa.dangjiandaping.network.model.BigScreenDeviceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * 大屏设备登录与学时记录 Retrofit 客户端。
 *
 * 本 App 的业务接口固定使用 [BuildConfig.BIG_SCREEN_API_BASE_URL]；安牧开放平台接口
 * 则独立使用 https://open.qly.cmviot.cn/，详见 [AndmuApiClient]。
 */
@OptIn(ExperimentalSerializationApi::class)
object BigScreenClient {
    suspend fun addLearnTime(request: LearnTimesRequest, accessToken: String) {
        require(accessToken.isNotBlank()) { "学习记录请求缺少 accessToken。" }
        val authorization = if (accessToken.startsWith("Bearer ", ignoreCase = true)) {
            accessToken
        } else {
            "Bearer $accessToken"
        }
        if (BuildConfig.DEBUG) {
            val requestUrl = BuildConfig.BIG_SCREEN_API_BASE_URL.trimEnd('/') +
                "/swapi/services/app/learnTimes/add"
            Log.d(LEARN_TIMES_LOG_TAG, "request method=POST url=$requestUrl")
            Log.d(LEARN_TIMES_LOG_TAG, "request headers: Content-Type=application/json;charset=UTF-8, Authorization=$authorization")
            Log.d(LEARN_TIMES_LOG_TAG, "request body=${json.encodeToString(request)}")
        }
        val response = api.addLearnTime(authorization, request)
        val responseBody = response.body()?.string()
        val errorBody = response.errorBody()?.string()
        if (BuildConfig.DEBUG) {
            Log.d(LEARN_TIMES_LOG_TAG, "response status=${response.code()} ${response.message()}")
            val safeHeaders = response.headers().toMultimap()
                .filterKeys { !it.equals("Set-Cookie", ignoreCase = true) }
            Log.d(LEARN_TIMES_LOG_TAG, "response headers=$safeHeaders")
            Log.d(LEARN_TIMES_LOG_TAG, "response body=${responseBody ?: errorBody ?: "<empty>"}")
        }
        check(response.isSuccessful) { "学习记录接口返回 HTTP ${response.code()}。" }
    }
    suspend fun login(): BigScreenDeviceSession = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val deviceCode = BuildConfig.BIG_SCREEN_DEVICE_CODE
        val requestToken = BigScreenDeviceLoginSigner.createToken(
            deviceCode = deviceCode,
            timestamp = timestamp,
            secret = BuildConfig.BIG_SCREEN_LOGIN_SECRET,
        )
        val response = api.loginRaw(
            BigScreenDeviceLoginRequest(
                deviceCode = deviceCode,
                timestamp = timestamp,
                token = requestToken,
            ),
        )
        val rawResponse = response.body()?.string() ?: response.errorBody()?.string()
        if (rawResponse.isNullOrBlank()) {
            throw BigScreenDeviceLoginException(
                "大屏设备登录接口返回 HTTP ${response.code()}，且没有响应内容。",
            )
        }
        if (BuildConfig.DEBUG) {
            logRawLoginResponse(response.code(), rawResponse)
        }
        val loginResponse = json.decodeFromString(
            BigScreenDeviceLoginResponse.serializer(),
            rawResponse,
        )
        Log.i(
            LOG_TAG,
            "siginByDeviceCode解析结果：HTTP ${response.code()}, success=${loginResponse.success}, " +
                "code=${loginResponse.code}, message=${loginResponse.logMessage()}, " +
                "result=${loginResponse.result}, error=${loginResponse.error}",
        )
        loginResponse.toSession()
    }

    private fun logRawLoginResponse(httpCode: Int, rawResponse: String) {
        val chunks = rawResponse.chunked(LOG_CHUNK_SIZE)
        chunks.forEachIndexed { index, chunk ->
            Log.i(
                LOG_TAG,
                "siginByDeviceCode原始响应 HTTP $httpCode (${index + 1}/${chunks.size}): $chunk",
            )
        }
    }
    private fun BigScreenDeviceLoginResponse.toSession(): BigScreenDeviceSession {
        if (!success) {
            throw BigScreenDeviceLoginException(errorMessage())
        }
        val loginResult = result
            ?: throw BigScreenDeviceLoginException("大屏设备登录未返回登录信息。")
        val accessToken = loginResult.accessToken?.trim().orEmpty()
        if (accessToken.isEmpty()) {
            throw BigScreenDeviceLoginException("大屏设备登录未返回 accessToken。")
        }
        return BigScreenDeviceSession(
            accessToken = accessToken,
            userId = loginResult.userId?.trim()?.takeIf { it.isNotEmpty() },
            userName = loginResult.userName?.trim()?.takeIf { it.isNotEmpty() },
            nickName = loginResult.nickName?.trim()?.takeIf { it.isNotEmpty() },
            organId = loginResult.organId?.trim()?.takeIf { it.isNotEmpty() },
            organName = loginResult.organName?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun BigScreenDeviceLoginResponse.errorMessage(): String = sequenceOf(
        message,
        error?.message,
        error?.details,
    ).mapNotNull { value -> value?.trim()?.takeIf { it.isNotEmpty() } }.firstOrNull()
        ?: "大屏设备登录失败。"

    private fun BigScreenDeviceLoginResponse.logMessage(): String = sequenceOf(
        message,
        error?.message,
    ).mapNotNull { value -> value?.trim()?.takeIf { it.isNotEmpty() } }.firstOrNull()
        ?: ""

    private val api: BigScreenDeviceLoginApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BIG_SCREEN_API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(BigScreenDeviceLoginApi::class.java)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 登录体含签名、响应体含 accessToken；Debug 仅记录请求行、状态和请求头，Release 关闭。
     */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor { message -> Log.d(LOG_TAG, message) }.apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
//                    redactHeader("Authorization")
//                    redactHeader("Cookie")
//                    redactHeader("Set-Cookie")
                },
            )
            .build()
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()

    private const val LOG_TAG = "BigScreenLogin"
    private const val LOG_CHUNK_SIZE = 2_500
    private const val LEARN_TIMES_LOG_TAG = "LearnTimes"
}
