package com.fpa.dangjiandaping.ui.login

import androidx.compose.runtime.staticCompositionLocalOf
import com.fpa.dangjiandaping.network.model.BigScreenDeviceSession

/** 将登录接口的 result 向需要调用 H5 onLogin 的界面下发。 */
val LocalBigScreenDeviceLoginResult = staticCompositionLocalOf<BigScreenDeviceSession?> { null }
