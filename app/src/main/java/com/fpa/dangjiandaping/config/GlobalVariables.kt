package com.fpa.dangjiandaping.config

/**
 * 应用运行期可修改的全局参数。
 *
 * 安牧开放平台凭据请仅填写在本地工作区，切勿提交真实 secret 或私钥。
 */
object GlobalVariables {
    const val ANDMU_TOKEN_URL = "https://open.qly.cmviot.cn/v3/open/api/token"

    /** 在安牧开发者控制台获取的 appid。 */
    var andmuAppId: String = "bdea95dff98b47978907f8c9c3a8a169"

    /** 在安牧开发者控制台获取的 secret。 */
    var andmuSecret: String = "b1soe8NOdTCIiTdP"

    /** 控制台分配的 PKCS#8 RSA 私钥 Base64 内容，可粘贴带 PEM 头尾的内容。 */
    var andmuPrivateKey: String = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAI/hVluB28WZFE59+lmEBJ5vVdkwZNGFJVh3O9MERcvvhpq88bi6byLCbXl/sh1c5XBbSVbusH5DT7a2ggj7lq8sQTc84Rpnj51QzN0R2uhNOYayGcz33+aCowiekw45Aw46d5uTWnmVPWaF+qXvjjlh+IrDnEpeeCYrswq2azb9AgMBAAECgYAmUz6+l0OkSddn5RS3nXvhfAShGsvwJ4hAHVqZJe01mnL/as70huytlJ62m3YlbiZLnHVXq7LlnyZvpAPVQrbrPf3DdF82snOogfctFxy1kxqYxMRYvA5GjIwXPOVoZBHUp4b98irJKlhh4IRxuKADTyFwftxOb4f5AhgepiBFHQJBAP7EQSNCnK+t0nF5IFwD9VbsMmJRmJ5MRkGmFSQHmo7QV3zEpWsAEEyI+LtUGRgEb1z72c4l5/Nmi6X0UurLB8sCQQCQk6fVRbImLxcW/6KD3ZjDS8qbDcQZ41z9aztopPqK+VIT8mEH60NrBs+1aVns6JFD7mYTtCl+HLJhekCvlpNXAkEA0lZ94PqyGmlMgdsbWFz8RdOklYAAnVEkADd65NBSTu68DIred8UJr+a2VRNN1IJ03zQf0w+AvXhAe7eIbclKmQJAFLyY/gYZH1DxxuKztKY8Gwbr8IFw9yWdWNvgkYnYRcas9x90u2YLLXa0pBiQRWK2M0Amc/0LVoNXMpQOYyD90QJAUSv5F311yZqfavUHpVLL0vaNIBWGwuz8WhmT6ZlbLJcDiJZ0DmJp/KKyf/vgfBDHq3+McN8VBV/nqf9mkZ4Drg=="

    /** 请求头 version，按安牧接口规范参与签名。 */
    var andmuClientVersion: String = "1.0.0"

    /** 最近一次成功获取或从本地缓存恢复的 token。 */
    var andmuToken: String = ""

    /** [andmuToken] 的本地安全过期时间（毫秒时间戳），0 表示尚未获得 token。 */
    var andmuTokenExpiresAtMillis: Long = 0L

    val isAndmuConfigured: Boolean
        get() = andmuAppId.isNotBlank() &&
            andmuSecret.isNotBlank() &&
            andmuPrivateKey.isNotBlank()
}
