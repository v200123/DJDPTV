package com.fpa.dangjiandaping.config

/**
 * 应用运行期可修改的全局参数。
 *
 * 安牧开放平台凭据请仅填写在本地工作区，切勿提交真实 secret 或私钥。
 */
object GlobalVariables {
    const val ANDMU_TOKEN_URL = "https://open.qly.cmviot.cn/v3/open/api/token"

    /** 在安牧开发者控制台获取的 appid。 */
    var andmuAppId: String = "cf2972d3795c4358bf0144faadb3115b"

    /** 在安牧开发者控制台获取的 secret。 */
    var andmuSecret: String = "fJ0Wz96JkQiN4syH"

    /** 控制台分配的 PKCS#8 RSA 私钥 Base64 内容，可粘贴带 PEM 头尾的内容。 */
    var andmuPrivateKey: String = "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAKvFTlptzKyXhBXKJwv11xeeuL7Dx9l2yCqiCeYn7mJUa+wLjwetq9gZA/70COLg9cOJxjWMJq626/cH/hXVj0cOTusxBpDAVfgi9WOBfikHQz98dCRzkHIPQSoFodK4kMtFl5sr8Sss/9n8H1scBzMIj0I8S2EV+xzk6ifHCYqNAgMBAAECgYEAkG8ZAlrfRqUk8LmJ+bmfQoI5MFcpvcbua0LTdg9PmKkKEnJps3gqTkxCmugSbMvie6hm3XHauQChC5hR44Qus5uIG7uvUEVPnCZz7csEz6+ZNtrE2+ts7eJBJzPoL5lWIPSagds1YLFEua3m4sr3IYla9YgP5f9YRwXEHUg3ReECQQDne86/EqWe+7J1GRK44qzDAv0lHgYbSyhENJNLU+2IVuSpw2G0j2GjLpIprIlxmF1tgAnUAKSbl4HAJhWpiJipAkEAvfaCNxlBL3dbBmPCQ6swSGP/5FMB6ulVaSUZ2zFSsvwLZdF0IGE8IqhMUhX63hFwU6RKYqoLD3Pu66YUQotdRQJBAIXXkxntaS+8blnAbo/SGHwFDWNZscQ0N4sALy49z7imLT5vBt5EjPqyIbbQ2QOCSnrWrlgTKxn/hvkXzCyjG0kCQBGTrZjLjWyG7rU4pdD9FgqctiC6TYMe8/g2pp3RgoVtLODO8J/OX3IVgHpX7k597pbOrNNUSJG1eHX1eApwOsECQQDcCFfCfakE3OvDo3Tbj0jT/+yG9mJaAyKAsW7O9Bh32jLJczgNaef9UFeJ3toikgzdFJ4kQyxJ4qfSnD4GGyo1"

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
