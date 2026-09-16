package com.fpa.dangjiandaping.network

import android.util.Log
import com.fpa.dangjiandaping.BuildConfig
import com.fpa.dangjiandaping.network.api.BigScreenDeviceLoginApi
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginException
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginRequest
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginResponse
import com.fpa.dangjiandaping.network.model.BigScreenDeviceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * 大屏设备登录 Retrofit 客户端，只负责网络请求与响应映射。
 *
 * 本 App 的业务接口固定使用 [BuildConfig.BIG_SCREEN_API_BASE_URL]；安牧开放平台接口
 * 则独立使用 https://open.qly.cmviot.cn/，详见 [AndmuApiClient]。
 */
@OptIn(ExperimentalSerializationApi::class)
object BigScreenDeviceLoginClient {
    suspend fun login(): BigScreenDeviceSession = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val deviceCode = BuildConfig.BIG_SCREEN_DEVICE_CODE
        val requestToken = BigScreenDeviceLoginSigner.createToken(
            deviceCode = deviceCode,
            timestamp = timestamp,
            secret = BuildConfig.BIG_SCREEN_LOGIN_SECRET,
        )
        val response = api.login(
            BigScreenDeviceLoginRequest(
                deviceCode = deviceCode,
                timestamp = timestamp,
                token = requestToken,
            ),
        )
        val loginResponse = response.toLoginResponse()
        Log.d(
            LOG_TAG,
            "登录响应：HTTP ${response.code()}，success=${loginResponse.success}，code=${loginResponse.code}" +
                "，message=${loginResponse.logMessage()}",
        )
        loginResponse.toSession()
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

    private fun Response<BigScreenDeviceLoginResponse>.toLoginResponse(): BigScreenDeviceLoginResponse {
        body()?.let { return it }
        errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { errorBody ->
            return json.decodeFromString(BigScreenDeviceLoginResponse.serializer(), errorBody)
        }
        throw BigScreenDeviceLoginException(
            "大屏设备登录接口返回 HTTP ${code()}，且没有响应内容。",
        )
    }

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
}
