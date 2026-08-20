package com.fpa.dangjiandaping.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.fpa.dangjiandaping.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

internal data class AppUpdate(
    val version: String,
    val build: String,
    val changelog: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
)

internal data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
)

/** Client for BetaQR/fir's GET /apps/latest/{id} version detection endpoint. */
internal object BetaQrUpdateManager {
    private const val API_BASE_URL = "https://api.appmeta.cn/apps/latest"

    suspend fun findUpdate(context: Context): Result<AppUpdate?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = BuildConfig.BETAQR_API_TOKEN.trim()
            require(token.isNotEmpty()) { "未配置 BetaQR API Token" }

            val appId = BuildConfig.BETAQR_APP_ID.trim().ifEmpty { context.packageName }
            val url = URL(
                "$API_BASE_URL/${URLEncoder.encode(appId, "UTF-8")}?api_token=" +
                    URLEncoder.encode(token, "UTF-8") +
                    "&type=android",
            )
            val response = url.openConnection() as HttpURLConnection
            try {
                response.connectTimeout = 10_000
                response.readTimeout = 15_000
                response.requestMethod = "GET"
                require(response.responseCode in 200..299) {
                    "版本检测失败（HTTP ${response.responseCode}）"
                }
                val json = response.inputStream.bufferedReader().use { it.readText() }
                val item = JSONObject(json)
                val downloadUrl = item.optString("install_url")
                    .ifBlank { item.optString("installUrl") }
                require(downloadUrl.isNotBlank()) { "服务端未返回 APK 下载地址" }

                AppUpdate(
                    version = item.optString("versionShort").ifBlank { item.optString("version") },
                    build = item.optString("build"),
                    changelog = item.optString("changelog"),
                    downloadUrl = downloadUrl,
                    fileSizeBytes = item.optJSONObject("binary")?.optLong("fsize", -1L) ?: -1L,
                ).takeIf { isNewerThanCurrent(it) }
            } finally {
                response.disconnect()
            }
        }
    }

    suspend fun downloadApk(
        update: AppUpdate,
        context: Context,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(updatesDir, "update-${update.build.ifBlank { "latest" }}.apk")
                val temporary = File(updatesDir, "${target.name}.download")
                URL(update.downloadUrl).openConnection().run {
                    connectTimeout = 15_000
                    readTimeout = 60_000
                    val totalBytes = contentLengthLong.takeIf { it > 0L } ?: update.fileSizeBytes
                    getInputStream().use { input ->
                        temporary.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloadedBytes = 0L
                            var lastReportedBytes = -1L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                downloadedBytes += count
                                if (downloadedBytes - lastReportedBytes >= 64 * 1024 ||
                                    (totalBytes > 0L && downloadedBytes == totalBytes)
                                ) {
                                    withContext(Dispatchers.Main.immediate) {
                                        onProgress(downloadedBytes, totalBytes)
                                    }
                                    lastReportedBytes = downloadedBytes
                                }
                            }
                            if (downloadedBytes != lastReportedBytes) {
                                withContext(Dispatchers.Main.immediate) {
                                    onProgress(downloadedBytes, totalBytes)
                                }
                            }
                        }
                    }
                }
                check(temporary.length() > 0L) { "下载的安装包为空" }
                if (target.exists()) target.delete()
                check(temporary.renameTo(target)) { "无法保存安装包" }
                target
            }
        }

    fun installApk(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun isNewerThanCurrent(update: AppUpdate): Boolean {
        update.build.toLongOrNull()?.let { return it > BuildConfig.VERSION_CODE.toLong() }
        return compareVersion(update.version, BuildConfig.VERSION_NAME) > 0
    }

    private fun compareVersion(left: String, right: String): Int {
        val leftParts = left.split('.', '-', '_')
        val rightParts = right.split('.', '-', '_')
        val size = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until size) {
            val result = (leftParts.getOrNull(index)?.toIntOrNull() ?: 0)
                .compareTo(rightParts.getOrNull(index)?.toIntOrNull() ?: 0)
            if (result != 0) return result
        }
        return 0
    }
}
