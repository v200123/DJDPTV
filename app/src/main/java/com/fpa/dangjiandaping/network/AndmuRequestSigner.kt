package com.fpa.dangjiandaping.network

import com.fpa.dangjiandaping.config.GlobalVariables
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/** 构造安牧接口要求的 body md5 与 RSA/SHA1 公共请求头签名。 */
internal object AndmuRequestSigner {
    fun md5(value: String): String = MessageDigest.getInstance("MD5")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    fun createHeaders(body: String, token: String? = null): Map<String, String> {
        val appId = GlobalVariables.andmuAppId
        val timestamp = System.currentTimeMillis().toString()
        val bodyMd5 = md5(body)
        val signatureFields = buildString {
            append("{\"appid\":")
            append(JSONObject.quote(appId))
            append(",\"md5\":\"")
            append(bodyMd5)
            append("\",\"timestamp\":\"")
            append(timestamp)
            append('"')
            token?.takeIf { it.isNotBlank() }?.let {
                append(",\"token\":")
                append(JSONObject.quote(it))
            }
            append(",\"version\":")
            append(JSONObject.quote(GlobalVariables.andmuClientVersion))
            append('}')
        }
        return linkedMapOf(
            "appid" to appId,
            "md5" to bodyMd5,
            "timestamp" to timestamp,
            "version" to GlobalVariables.andmuClientVersion,
            "signature" to signSha1WithRsa(signatureFields, GlobalVariables.andmuPrivateKey),
        ).apply {
            token?.takeIf { it.isNotBlank() }?.let { put("token", it) }
        }
    }

    private fun signSha1WithRsa(content: String, privateKeyText: String): String {
        val signer = Signature.getInstance("SHA1withRSA")
        signer.initSign(parsePrivateKey(privateKeyText))
        signer.update(content.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(signer.sign())
    }

    private fun parsePrivateKey(privateKeyText: String): PrivateKey {
        val base64 = privateKeyText
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace(Regex("\\s"), "")
        return KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)))
    }
}
