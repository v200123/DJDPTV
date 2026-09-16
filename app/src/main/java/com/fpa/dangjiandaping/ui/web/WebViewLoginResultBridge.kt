package com.fpa.dangjiandaping.ui.web

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import com.fpa.dangjiandaping.network.model.BigScreenDeviceSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 单个 WebView 当前可提供给 H5 的登录 result JSON。 */
internal class WebViewLoginResultState(
    var requestUrl: String,
) {
    @Volatile
    var resultJson: String? = null
}

internal fun BigScreenDeviceSession?.toH5LoginResultJson(): String? =
    this?.let { Json.encodeToString(it) }

/**
 * 业务 WebView 的通用基础实现。
 *
 * 统一维护登录 result 和共同 WebSettings；调用方只处理各自的焦点、布局、页面回调及
 * AndroidFocusBridge 的业务能力。
 */
internal open class CommonWebView(
    context: Context,
    initialUrl: String,
    loginResultJson: String?,
) : WebView(context) {
    private val loginResultState = WebViewLoginResultState(initialUrl).apply {
        resultJson = loginResultJson
    }

    val requestUrl: String
        get() = loginResultState.requestUrl

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false
        }
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = MOBILE_BROWSER_USER_AGENT
            useWideViewPort = true
            builtInZoomControls = false
            setSupportZoom(true)
        }
        isVerticalScrollBarEnabled = true
        isScrollbarFadingEnabled = false
        scrollBarStyle = SCROLLBARS_INSIDE_OVERLAY
    }

    fun updateLoginResult(loginResultJson: String?) {
        loginResultState.resultJson = loginResultJson
    }

    fun updateRequestUrl(url: String, loginResultJson: String?) {
        loginResultState.requestUrl = url
        loginResultState.resultJson = loginResultJson
    }

    fun getUserJson(): String? = loginResultState.resultJson
}
