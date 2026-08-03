package com.fpa.dangjiandaping.ui.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.BuildConfig

private const val FOCUS_LOG_TAG = "FocusTrace"
private const val WEB_LOG_TAG = "WebContent"
internal const val MOBILE_BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
private const val CAPTURE_WEB_FOCUS_SCRIPT =
    "(function(){var old=document.querySelector('[data-android-focus-return]');" +
        "if(old){old.removeAttribute('data-android-focus-return');}" +
        "var el=document.activeElement;" +
        "if(el&&el!==document.body&&el!==document.documentElement){" +
        "el.setAttribute('data-android-focus-return','true');window.__androidFocusReturnElement=el;}})();"
private const val RESTORE_WEB_FOCUS_SCRIPT =
    "(function(){var el=window.__androidFocusReturnElement||" +
        "document.querySelector('[data-android-focus-return]');" +
        "if(el&&document.documentElement.contains(el)){" +
        "try{el.focus({preventScroll:true});}catch(e){el.focus();}" +
        "try{el.scrollIntoView({block:'nearest',inline:'nearest'});}catch(e){}" +
        "return true;}return false;})();"

private class WebFocusBridge(
    private val webView: WebView,
    private val onRequestNativeFocus: () -> Unit,
    private val onShowNewsDetail: (String) -> Unit,
    private val onShowServiceTeam: (String) -> Unit,
    private val onShowPublicHelpRequest: (String) -> Unit,
    private val onPlayVideo: (String,String) -> Unit,
    private val onShowWebViewUrl: (String) -> Unit,
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
        Log.d(FOCUS_LOG_TAG, "H5 called showNewsDetail,收到的数据为:$newsJson")
        webView.post {
            webView.evaluateJavascript(CAPTURE_WEB_FOCUS_SCRIPT) {
                webView.clearFocus()
                onShowNewsDetail(newsJson)
            }
        }
    }

//    @JavascriptInterface
//    fun openNewsDetail(newsJson: String) {
//        showNewsDetail(newsJson)
//    }

    @JavascriptInterface
    fun showServiceTeam(teamJson: String) {
        Log.d(WEB_LOG_TAG, "H5 called showServiceTeam${teamJson}")
        webView.post {
            webView.evaluateJavascript(CAPTURE_WEB_FOCUS_SCRIPT) {
                webView.clearFocus()
                onShowServiceTeam(teamJson)
            }
        }
    }

    @JavascriptInterface
    fun openServiceTeam(teamJson: String) = showServiceTeam(teamJson)

    @JavascriptInterface
    fun showPublicHelpRequest(requestJson: String) {
        Log.d(WEB_LOG_TAG, "H5 called showPublicHelpRequest")
        webView.post {
            webView.clearFocus()
            onShowPublicHelpRequest(requestJson)
        }
    }

    @JavascriptInterface
    fun openPublicHelpRequest(requestJson: String) = showPublicHelpRequest(requestJson)

    @JavascriptInterface
    fun showHelpRequest(requestJson: String) = showPublicHelpRequest(requestJson)

    @JavascriptInterface
    fun openHelpRequest(requestJson: String) = showPublicHelpRequest(requestJson)

    @JavascriptInterface
    fun playVideo(videoUrl: String?,title:String?) {
        Log.d(WEB_LOG_TAG, "H5 called playVideo: $videoUrl")
        webView.post {
            onPlayVideo(videoUrl?.trim()?:"",title?:"")
        }
    }

    @JavascriptInterface
    fun showWebViewUrl(url: String) {
        Log.d(WEB_LOG_TAG, "H5 called showWebViewUrl: $url")
        webView.post {
            onShowWebViewUrl(url.trim())
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebContent(
    url: String,
    active: Boolean,
    onCreated: (WebView) -> Unit,
    onReleased: (WebView) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onRequestNativeFocus: () -> Unit,
    onShowPublicHelpRequest: (PublicHelpRequest) -> Unit,
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
    var serviceTeam by remember(url) { mutableStateOf<ServiceTeam?>(null) }
    var webViewDialogUrl by remember(url) { mutableStateOf<String?>(null) }
    var restoreNewsDetailFocus by remember(url) { mutableStateOf(false) }
    var restoreServiceTeamFocus by remember(url) { mutableStateOf(false) }
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }

    LaunchedEffect(newsDetail, restoreNewsDetailFocus) {
        if (newsDetail == null && restoreNewsDetailFocus) {
            webViewHolder[0]?.let { webView ->
                webView.isFocusable = true
                webView.isFocusableInTouchMode = true
                webView.requestFocus()
                webView.evaluateJavascript(RESTORE_WEB_FOCUS_SCRIPT) { restored ->
                    Log.d(FOCUS_LOG_TAG, "News detail dialog focus restored=$restored")
                }
            }
            restoreNewsDetailFocus = false
        }
    }

    LaunchedEffect(serviceTeam, restoreServiceTeamFocus) {
        if (serviceTeam == null && restoreServiceTeamFocus) {
            webViewHolder[0]?.let { webView ->
                webView.isFocusable = true
                webView.isFocusableInTouchMode = true
                webView.requestFocus()
                webView.evaluateJavascript(RESTORE_WEB_FOCUS_SCRIPT) { restored ->
                    Log.d(FOCUS_LOG_TAG, "Service team dialog focus restored=$restored")
                }
            }
            restoreServiceTeamFocus = false
        }
    }

//    LaunchedEffect(Unit) {
//        // WebView 首次初始化较重，至少让顶部原生界面先完成一帧绘制。
//        withFrameNanos { }
//        withFrameNanos { }
//        createWebView = true
//    }
    Box(
        modifier = modifier
            .alpha(if (active) 1f else 0f)
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
//                    if (BuildConfig.DEBUG) {
//                        WebView.setWebContentsDebuggingEnabled(true)
//                        Log.i(WEB_LOG_TAG, "WebView remote debugging enabled")
//                    }
                    WebView(context).apply {
                        val scrollStepPx = (72 * resources.displayMetrics.density).toInt()

                        webViewHolder[0] = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnTouchListener { view, event ->
                            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                                view.requestFocus()
                            }
                            false
                        }
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
                                },
                                onShowServiceTeam = { teamJson ->
                                    runCatching { parseServiceTeam(teamJson) }
                                        .onSuccess { serviceTeam = it }
                                        .onFailure { error ->
                                            Log.e(WEB_LOG_TAG, "Unable to parse service team JSON", error)
                                        }
                                },
                                onShowPublicHelpRequest = { requestJson ->
                                    runCatching { parsePublicHelpRequest(requestJson) }
                                        .onSuccess(onShowPublicHelpRequest)
                                        .onFailure { error ->
                                            Log.e(
                                                WEB_LOG_TAG,
                                                "Unable to parse public help request JSON",
                                                error,
                                            )
                                        }
                                },
                                onPlayVideo = { requestedUrl,title ->
                                    if (requestedUrl.isNotEmpty()) {
                                        context.startActivity(
                                            FullscreenVideoActivity.newIntent(
                                                context = context,
                                                videoUrl = requestedUrl,
                                                videoTitle = title.orEmpty(),
                                            )
                                        )
                                    }
                                },
                                onShowWebViewUrl = { requestedUrl ->
                                    if (requestedUrl.isNotEmpty()) {
                                        webViewDialogUrl = requestedUrl
                                    }
                                },
                            ),
                            "AndroidFocusBridge"
                        )
                        setOnKeyListener { view, keyCode, event ->
                            if (event.action != KeyEvent.ACTION_DOWN) {
                                false
                            } else {
                                when (keyCode) {
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        if (view.canScrollVertically(-1)) {
                                            view.scrollBy(0, -scrollStepPx)
                                        } else if (event.repeatCount == 0) {
                                            Log.d(
                                                FOCUS_LOG_TAG,
                                                "WebView reached top -> request native tab focus",
                                            )
                                            view.post {
                                                view.clearFocus()
                                                onRequestNativeFocus()
                                            }
                                        }
                                        true
                                    }

                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        if (view.canScrollVertically(1)) {
                                            view.scrollBy(0, scrollStepPx)
                                        }
                                        true
                                    }

                                    else -> false
                                }
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
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView, title: String?) {
                                super.onReceivedTitle(view, title)
                                view.contentDescription = title
                                    ?.trim()
                                    ?.takeIf(String::isNotEmpty)
                                    ?: "网页内容"
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = false
                            builtInZoomControls = false
                            displayZoomControls = true
                            userAgentString = MOBILE_BROWSER_USER_AGENT
                            setSupportZoom(true)
                        }
                        isVerticalScrollBarEnabled = true
                        isScrollbarFadingEnabled = false
                        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                        tag = url
                        Log.i(WEB_LOG_TAG, "loadUrl(initial): $url")

                        loadUrl(url)
                        onCreated(this)
                    }
                },
                update = { view ->
                    view.visibility = if (active) View.VISIBLE else View.INVISIBLE
                    view.isActivated = active
                    val webViewInteractive = active
                    view.isFocusable = webViewInteractive
                    view.isFocusableInTouchMode = webViewInteractive
                    if (active) {
                        onCreated(view)
                        onCanGoBackChanged(view.canGoBack())
                    }
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
                    if (webViewHolder[0] === view) {
                        webViewHolder[0] = null
                    }
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
                onDismiss = {
                    newsDetail = null
                    restoreNewsDetailFocus = true
                }
            )
        }

        serviceTeam?.let { team ->
            ServiceTeamDialog(
                team = team,
                onDismiss = {
                    serviceTeam = null
                    restoreServiceTeamFocus = true
                },
            )
        }

        webViewDialogUrl?.let { requestedUrl ->
            WebViewDialog(
                url = requestedUrl,
                onDismiss = { webViewDialogUrl = null },
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
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = ComposeColor.White
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
            active = true,
            onCreated = {},
            onReleased = {},
            onCanGoBackChanged = {},
            onRequestNativeFocus = {},
            onShowPublicHelpRequest = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
