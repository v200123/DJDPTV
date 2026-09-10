package com.fpa.dangjiandaping.network.model

data class AndmuTokenResult(
    val resultCode: String,
    val resultMessage: String,
    val token: String?,
    val expiresInSeconds: Long?,
    val fromLocalCache: Boolean = false,
    val rawResponseJson: String = "",
) {
    val isSuccess: Boolean
        get() = resultCode == SUCCESS_CODE && !token.isNullOrBlank()

    private companion object {
        const val SUCCESS_CODE = "000000"
    }
}

data class AndmuDeviceListResult(
    val resultCode: String,
    val resultMessage: String,
    val total: Int,
    val devices: List<AndmuDevice>,
    val rawResponseJson: String,
)

data class AndmuDevice(
    val deviceId: String,
    val deviceName: String,
    val deviceAddress: String,
    val deviceStatus: Int,
)

data class AndmuRealtimeThumbnailResult(
    val resultCode: String,
    val resultMessage: String,
    val thumbnailUrl: String?,
    val rawResponseJson: String,
)

data class AndmuWebSdkPlayerResult(
    val resultCode: String,
    val resultMessage: String,
    val deviceId: String,
    val playerUrl: String?,
    val expiresInSeconds: Long?,
    val rawResponseJson: String,
)
