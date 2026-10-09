package com.fpa.dangjiandaping.network.model

import kotlinx.serialization.Serializable

@Serializable
data class BigScreenDeviceLoginRequest(
    val deviceCode: String,
    val timestamp: Long,
    val token: String,
)

@Serializable
data class BigScreenDeviceLoginResponse(
    val success: Boolean = false,
    val message: String? = null,
    val code: Int? = null,
    val result: BigScreenDeviceLoginPayload? = null,
    val error: BigScreenDeviceLoginError? = null,
)

@Serializable
data class BigScreenDeviceLoginPayload(
    val accessToken: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val nickName: String? = null,
    val organId: String? = null,
    val organName: String? = null,
)

@Serializable
data class BigScreenDeviceLoginError(
    val code: Int? = null,
    val details: String? = null,
    val message: String? = null,
)

/** 大屏设备登录响应的 result，同时供原生接口和 H5 登录回调使用。 */
@Serializable
data class BigScreenDeviceSession(
    val accessToken: String,
    val userId: String?,
    val userName: String?,
    val nickName: String?,
    val organId: String?,
    val organName: String?,
)

class BigScreenDeviceLoginException(message: String) : IllegalStateException(message)
