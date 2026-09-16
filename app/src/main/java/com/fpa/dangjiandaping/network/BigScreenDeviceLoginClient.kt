package com.fpa.dangjiandaping.network

import android.content.Context
import com.fpa.dangjiandaping.BuildConfig
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginException
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginRequest
import com.fpa.dangjiandaping.network.model.BigScreenDeviceLoginResponse
import com.fpa.dangjiandaping.network.model.BigScreenDeviceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * 大屏设备登录 Retrofit 客户端。
 *
 * 本 App 的业务接口固定使用 [BuildConfig.BIG_SCREEN_API_BASE_URL]；安牧开放平台接口
 * 则独立使用 https://open.qly.cmviot.cn/，详见 [AndmuApiClient]。
 * 登录成功的 JWT 仅写入本地会话；请求与日志都不会输出 token 内容。
 */
@OptIn(ExperimentalSerializationApi::class)
object BigScreenDeviceLoginClient {
    suspend fun login(context: Context): BigScreenDeviceSession = withContext(Dispatchers.IO) {
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
        val session = response.toLoginResponse().toSession()
        saveSession(context, session)
        session
    }

    /** 供同一应用内的后续业务接口读取 JWT；调用方自行按服务端要求设置 Authorization 格式。 */
    fun accessToken(context: Context): String? = context
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getString(KEY_ACCESS_TOKEN, null)
        ?.takeIf { it.isNotBlank() }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
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

    private fun Response<BigScreenDeviceLoginResponse>.toLoginResponse(): BigScreenDeviceLoginResponse {
        body()?.let { return it }
        errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { errorBody ->
            return json.decodeFromString(BigScreenDeviceLoginResponse.serializer(), errorBody)
        }
        throw BigScreenDeviceLoginException(
            "大屏设备登录接口返回 HTTP ${code()}，且没有响应内容。",
        )
    }

    private fun saveSession(context: Context, session: BigScreenDeviceSession) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_USER_NAME, session.userName)
            .putString(KEY_NICK_NAME, session.nickName)
            .putString(KEY_ORGAN_ID, session.organId)
            .putString(KEY_ORGAN_NAME, session.organName)
            .apply()
    }

    private interface BigScreenDeviceLoginApi {
        @Headers("Content-Type: application/json;charset=UTF-8")
        @POST("api/TokenAuth/siginByDeviceCode")
        suspend fun login(
            @Body request: BigScreenDeviceLoginRequest,
        ): Response<BigScreenDeviceLoginResponse>
    }

    private val api: BigScreenDeviceLoginApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BIG_SCREEN_API_BASE_URL)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(BigScreenDeviceLoginApi::class.java)
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()

    private const val PREFERENCES_NAME = "big_screen_device_session"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_NICK_NAME = "nick_name"
    private const val KEY_ORGAN_ID = "organ_id"
    private const val KEY_ORGAN_NAME = "organ_name"
}
