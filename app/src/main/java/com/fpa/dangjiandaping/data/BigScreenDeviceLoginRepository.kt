package com.fpa.dangjiandaping.data

import com.fpa.dangjiandaping.network.BigScreenClient
import com.fpa.dangjiandaping.network.model.BigScreenDeviceSession

/** 协调登录请求与本地会话，供 ViewModel 调用。 */
class BigScreenDeviceLoginRepository(
    private val sessionStore: BigScreenDeviceSessionStore,
) {
    suspend fun login(): BigScreenDeviceSession = BigScreenClient.login()
        .also(sessionStore::save)

    fun validSession(): BigScreenDeviceSession? = sessionStore.validSession()

    fun accessToken(): String? = sessionStore.accessToken()

    fun clearSession() {
        sessionStore.clear()
    }
}
