package com.fpa.dangjiandaping.ui.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.BuildConfig

private const val FOCUS_LOG_TAG = "FocusTrace"
private const val WEB_LOG_TAG = "WebContent"

private class WebFocusBridge(
    private val webView: WebView,
    private val onRequestNativeFocus: () -> Unit,
    private val onShowNewsDetail: (String) -> Unit
) {
    @JavascriptInterface
    fun requestPreviousTabFocus() {
        Log.d(
            FOCUS_LOG_TAG,
            "H5 called requestPreviousTabFocus -> request last focused native tab"
        )
        webView.post {
            webView.clearFocus()
            onRequestNativeFocus()
        }
    }

    @JavascriptInterface
    fun showNewsDetail(newsJson: String) {
        Log.d(FOCUS_LOG_TAG, "H5 called showNewsDetail")
        webView.post {
            webView.clearFocus()
            onShowNewsDetail(newsJson)
        }
    }

    @JavascriptInterface
    fun openNewsDetail(newsJson: String) {
        showNewsDetail(newsJson)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebContent(
    url: String,
//    active: Boolean,
    onCreated: (WebView) -> Unit,
    onReleased: (WebView) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onRequestNativeFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
//    if (LocalInspectionMode.current) {
//        WebContentPlaceholder(
//            message = "网页内容预览区",
//            modifier = modifier
//        )
//        return
//    }

//    var createWebView by remember { mutableStateOf(false) }
    var loadingUrl by remember { mutableStateOf<String?>(url) }
    var newsDetail by remember(url) { mutableStateOf<NewsDetail?>(null) }

//    LaunchedEffect(Unit) {
//        // WebView 首次初始化较重，至少让顶部原生界面先完成一帧绘制。
//        withFrameNanos { }
//        withFrameNanos { }
//        createWebView = true
//    }
//    LaunchedEffect(active) {
//        if (!active) newsDetail = null
//    }

    Box(
        modifier = modifier
            .focusProperties {
                onExit = {
                    if (requestedFocusDirection == FocusDirection.Up) {
                        onRequestNativeFocus()
                    } else {
                        cancelFocusChange()
                    }
                }
            }
            .focusGroup(),
    ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    if (BuildConfig.DEBUG) {
                        WebView.setWebContentsDebuggingEnabled(true)
                        Log.i(WEB_LOG_TAG, "WebView remote debugging enabled")
                    }
                    WebView(context).apply {
//                        isActivated = active
//                        isFocusable = active
//                        isFocusableInTouchMode = active
//                        if (!active) {
//                            onPause()
//                        }
                        onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                            Log.d(
                                FOCUS_LOG_TAG,
                                "WebView ${if (hasFocus) "FOCUSED" else "LOST_FOCUS"}, url=$url"
                            )
                        }
                        addJavascriptInterface(
                            WebFocusBridge(
                                webView = this,
                                onRequestNativeFocus = onRequestNativeFocus,
                                onShowNewsDetail = { newsJson ->
                                    runCatching { parseNewsDetail(newsJson) }
                                        .onSuccess { newsDetail = it }
                                        .onFailure { error ->
                                            Log.e(
                                                FOCUS_LOG_TAG,
                                                "Unable to parse news detail JSON",
                                                error
                                            )
                                        }
                                }
                            ),
                            "AndroidFocusBridge"
                        )
                        setOnKeyListener { view, keyCode, event ->
                            val shouldReturnToTabs =
                                keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                                    event.action == KeyEvent.ACTION_DOWN &&
                                    event.repeatCount == 0
                            if (shouldReturnToTabs) {
                                Log.d(
                                    FOCUS_LOG_TAG,
                                    "WebView DPAD_UP -> clear WebView focus and request last focused native tab"
                                )
                                view.post {
                                    view.clearFocus()
                                    onRequestNativeFocus()
                                }
                                true
                            } else {
                                false
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                Log.i(WEB_LOG_TAG, "onPageStarted: $url")
                                loadingUrl = view.tag as? String ?: url
                            }

                            override fun onPageCommitVisible(view: WebView, url: String?) {
                                super.onPageCommitVisible(view, url)
                                Log.i(WEB_LOG_TAG, "onPageCommitVisible: $url")
                                loadingUrl = null
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                Log.i(WEB_LOG_TAG, "onPageFinished: $url")
                                loadingUrl = null
                            }

                            override fun doUpdateVisitedHistory(
                                view: WebView,
                                url: String?,
                                isReload: Boolean
                            ) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                onCanGoBackChanged(view.canGoBack())
                            }
                        }
                        webChromeClient = WebChromeClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = true
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0"
                            setSupportZoom(true)
                        }
                        tag = url
                        Log.i(WEB_LOG_TAG, "loadUrl(initial): $url")
                        loadUrl(url)
                        onCreated(this)
                    }
                },
                update = { view ->
//                    if (view.isActivated != active) {
//                        view.isActivated = active
//                        if (active) {
//                            view.onResume()
//                        } else {
//                            view.clearFocus()
//                            view.onPause()
//                        }
//                    }
                    val webViewInteractive = true
                    view.isFocusable = webViewInteractive
                    view.isFocusableInTouchMode = webViewInteractive
                    if (!webViewInteractive && view.hasFocus()) {
                        view.clearFocus()
                    }
                    if (view.tag != url) {
                        Log.i(WEB_LOG_TAG, "loadUrl(routeChanged): ${view.tag} -> $url")
                        view.stopLoading()
                        view.clearHistory()
                        view.tag = url
                        loadingUrl = url
                        onCanGoBackChanged(false)
                        view.loadUrl(url)
                    } else if (view.url.isNullOrEmpty()) {
                        Log.i(WEB_LOG_TAG, "loadUrl(emptyWebView): $url")
                        loadingUrl = url
                        view.loadUrl(url)
                    }
                },
                onRelease = { view ->
                    onCanGoBackChanged(false)
                    onReleased(view)
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.removeJavascriptInterface("AndroidFocusBridge")
                    view.removeAllViews()
                    view.destroy()
                }
            )


        if ( loadingUrl != null) {
            WebContentPlaceholder(
                message = "页面加载中…",
                modifier = Modifier.fillMaxSize()
            )
        }

        newsDetail?.let { detail ->
            NewsDetailDialog(
                news = detail,
                onDismiss = { newsDetail = null }
            )
        }
    }
}

@Composable
private fun WebContentPlaceholder(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(ComposeColor.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = ComposeColor.Black
        )
    }
}

@Preview(
    name = "网页内容区域",
    widthDp = 1280,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun WebContentPreview() {
    MaterialTheme {
        WebContent(
            url = "https://www.baidu.com",
            onCreated = {},
            onReleased = {},
            onCanGoBackChanged = {},
            onRequestNativeFocus = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
