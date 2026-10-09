package com.fpa.dangjiandaping.network

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** 大屏设备登录的 token = MD5(deviceCode + timestamp + secret)，输出为小写十六进制。 */
internal object BigScreenDeviceLoginSigner {
    fun createToken(deviceCode: String, timestamp: Long, secret: String): String {
        require(deviceCode.isNotBlank()) { "deviceCode 不能为空。" }
        require(secret.isNotBlank()) { "大屏登录密钥不能为空。" }
        val source = deviceCode + timestamp.toString() + secret
        return MessageDigest.getInstance("MD5")
            .digest(source.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
