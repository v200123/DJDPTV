package com.fpa.dangjiandaping.network

import android.net.Uri
import android.util.Log
import com.fpa.dangjiandaping.BuildConfig
import com.fpa.dangjiandaping.network.model.XyxfArticle
import com.fpa.dangjiandaping.network.model.XyxfArticleFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

/** 雪域先锋首页资讯接口；两个栏目共享同一个 Retrofit 客户端。 */
object XyxfNewsApiClient {
    suspend fun getArticles(columnId: String, onlyImage: Boolean): XyxfArticleFeed =
        withContext(Dispatchers.IO) {
            val response = api.getByColumnId(
                columnId = columnId,
                pageIndex = 1,
                pageSize = PAGE_SIZE,
                keyword = "",
                onlyImage = onlyImage,
            )
            val rawResponse = response.readBodyText()
            val responseJson = JSONObject(rawResponse)
            val responseCode = responseJson.optInt("code", HTTP_SUCCESS_CODE)
            if (responseCode != HTTP_SUCCESS_CODE) {
                throw IllegalStateException(
                    responseJson.optString("msg").ifBlank { "雪域先锋资讯接口返回失败。" },
                )
            }
            val articles = responseJson.findArticleArray().let { articleArray ->
                List(articleArray.length()) { index ->
                    articleArray.optJSONObject(index)?.toXyxfArticle()
                }.filterNotNull()
            }
            XyxfArticleFeed(
                articles = articles,
                serverMessage = responseJson.optString("msg"),
            )
        }

    private fun Response<ResponseBody>.readBodyText(): String {
        val responseBody = (if (isSuccessful) body() else errorBody())
            ?: throw IllegalStateException("雪域先锋资讯接口返回 HTTP ${code()}，且没有响应内容。")
        return responseBody.string()
    }

    /** 兼容当前接口的 records/list/rows 三种分页容器，防止服务端分页字段升级导致首页空白。 */
    private fun JSONObject.findArticleArray(): JSONArray {
        optJSONArray("data")?.let { return it }
        optJSONArray("rows")?.let { return it }
        val data = optJSONObject("data") ?: optJSONObject("result") ?: return JSONArray()
        return data.optJSONArray("records")
            ?: data.optJSONArray("list")
            ?: data.optJSONArray("rows")
            ?: data.optJSONArray("items")
            ?: JSONArray()
    }

    private fun JSONObject.toXyxfArticle(): XyxfArticle? {
        val title = firstNonBlank("title", "articleTitle", "name") ?: return null
        return XyxfArticle(
            id = firstNonBlank("id", "articleId") ?: title,
            title = title,
            imageUrl = firstImageFileId()?.let { fileId -> IMAGE_BASE_URL + Uri.encode(fileId) },
            publishedAt = firstNonBlank(
                "pubTime",
                "publishTime",
                "publishedAt",
                "publishDateTime",
                "publicTime",
                "publicDate",
                "releaseTime",
                "releaseDate",
                "issueTime",
                "sendTime",
                "newsDate",
                "createTime",
                "createDate",
                "createdAt",
                "publishDate",
            ).orEmpty(),
        )
    }

    private fun JSONObject.firstImageFileId(): String? {
        val imageValue = opt("image")
        val images = when (imageValue) {
            is JSONArray -> imageValue
            is JSONObject -> JSONArray().put(imageValue)
            is String -> {
                val json = imageValue.trim()
                when {
                    json.startsWith("[") -> runCatching { JSONArray(json) }.getOrNull()
                    json.startsWith("{") -> runCatching { JSONArray().put(JSONObject(json)) }.getOrNull()
                    else -> null
                }
            }
            else -> null
        } ?: return null

        for (index in 0 until images.length()) {
            images.optJSONObject(index)?.firstNonBlank("fileId")?.let { return it }
        }
        return null
    }
    private fun JSONObject.firstNonBlank(vararg keys: String): String? = keys
        .asSequence()
        .map { key -> optString(key).trim() }
        .firstOrNull { value -> value.isNotEmpty() }

    private interface XyxfNewsApi {
        @GET("prod-api/unLogin/article/getByColumnId")
        suspend fun getByColumnId(
            @Query("columnId") columnId: String,
            @Query("pageIndex") pageIndex: Int,
            @Query("pageSize") pageSize: Int,
            @Query("keyword") keyword: String,
            @Query("onlyImage") onlyImage: Boolean,
        ): Response<ResponseBody>
    }

    private val api: XyxfNewsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .build()
            .create(XyxfNewsApi::class.java)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor { message -> Log.d(LOG_TAG, message) }.apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )
            .build()
    }

    private const val BASE_URL = "https://www.xyxf.gov.cn/"
    private const val IMAGE_BASE_URL = "https://www.scycjy.gov.cn/xyxfapi/image/"
    private const val PAGE_SIZE = 4
    private const val HTTP_SUCCESS_CODE = 200
    private const val LOG_TAG = "XyxfNewsApi"
}
