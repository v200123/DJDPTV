package com.fpa.dangjiandaping.data

import android.content.Context
import com.fpa.dangjiandaping.network.model.BigScreenDeviceSession

/** 大屏设备登录会话的本地存储；网络层不直接依赖 Android 存储。 */
class BigScreenDeviceSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun save(session: BigScreenDeviceSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_USER_NAME, session.userName)
            .putString(KEY_NICK_NAME, session.nickName)
            .putString(KEY_ORGAN_ID, session.organId)
            .putString(KEY_ORGAN_NAME, session.organName)
            .putLong(KEY_EXPIRES_AT_MILLIS, System.currentTimeMillis() + TOKEN_VALIDITY_MILLIS)
            .apply()
    }

    fun validSession(nowMillis: Long = System.currentTimeMillis()): BigScreenDeviceSession? {
        if (preferences.getLong(KEY_EXPIRES_AT_MILLIS, 0L) <= nowMillis) {
            clear()
            return null
        }
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return BigScreenDeviceSession(
            accessToken = accessToken,
            userId = preferences.getString(KEY_USER_ID, null),
            userName = preferences.getString(KEY_USER_NAME, null),
            nickName = preferences.getString(KEY_NICK_NAME, null),
            organId = preferences.getString(KEY_ORGAN_ID, null),
            organName = preferences.getString(KEY_ORGAN_NAME, null),
        )
    }

    fun accessToken(): String? = validSession()?.accessToken

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "big_screen_device_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_NICK_NAME = "nick_name"
        const val KEY_ORGAN_ID = "organ_id"
        const val KEY_ORGAN_NAME = "organ_name"
        const val KEY_EXPIRES_AT_MILLIS = "expires_at_millis"
        const val TOKEN_VALIDITY_MILLIS = 2 * 24 * 60 * 60 * 1000L
    }
}
