package com.fpa.dangjiandaping.ui.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.R
import com.fpa.dangjiandaping.ui.login.LocalBigScreenDeviceLoginResult

private const val SCALE_WIDE_PAGE_SCRIPT =
    "(function(){" +
        "var root=document.documentElement;var body=document.body;" +
        "if(!root){return false;}" +
        "root.style.zoom='';" +
        "var viewport=Math.max(root.clientWidth||0,window.innerWidth||0);" +
        "var content=Math.max(root.scrollWidth||0,body?body.scrollWidth:0);" +
        "var overflow=content>viewport+2;" +
        "root.style.zoom=overflow?'0.7':'';" +
        "return overflow;" +
        "})();"

private fun isXyxfHostUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = Uri.parse(url)
    return uri.scheme == "https" &&
        uri.host.equals("www.xyxf.gov.cn", ignoreCase = true)
}

private fun isXyxfDetailsUrl(url: String?): Boolean {
    if (!isXyxfHostUrl(url)) return false
    val fragment = Uri.parse(url).fragment ?: return false
    return fragment == "/index/details" || fragment.startsWith("/index/details?")
}

private const val INSTALL_XYXF_TV_SCROLL_SCRIPT = """
    (function() {
        if (window.__xyxfTvKeyHandler) {
            window.removeEventListener('keydown', window.__xyxfTvKeyHandler, true);
        }
        window.__xyxfTvKeyHandler = function(event) {
            if (location.hostname.toLowerCase() !== 'www.xyxf.gov.cn' ||
                !/^#\/index\/details(?:\?|$)/.test(location.hash) ||
                (event.key !== 'ArrowUp' && event.key !== 'ArrowDown')) return;
            if (event.target && event.target.closest &&
                event.target.closest('input, textarea, [contenteditable="true"]')) return;

            event.preventDefault();
            event.stopImmediatePropagation();
            var direction = event.key === 'ArrowDown' ? 1 : -1;
            var root = document.scrollingElement || document.documentElement;
            function canScroll(element) {
                if (!element || element.clientHeight <= 0 ||
                    element.scrollHeight <= element.clientHeight + 2) return false;
                if (element === root) return true;
                var style = window.getComputedStyle(element);
                return (/auto|scroll|overlay/.test(style.overflowY) || element.scrollTop > 0) &&
                    style.visibility !== 'hidden' && style.display !== 'none';
            }
            function move(element) {
                if (!canScroll(element)) return false;
                var before = element.scrollTop;
                element.scrollTop = before + direction * 72;
                return Math.abs(element.scrollTop - before) > 0.5;
            }
            var article = document.getElementById('app');
            if (canScroll(article)) {
                if (move(article) || move(root)) return;
            } else {
                var hit = document.elementFromPoint(window.innerWidth / 2, window.innerHeight / 2);
                while (hit && hit !== root) {
                    if (move(hit)) return;
                    hit = hit.parentElement;
                }
                if (move(root)) return;
            }
            if (direction < 0 && window.AndroidFocusBridge &&
                window.AndroidFocusBridge.requestPreviousTabFocus) {
                window.AndroidFocusBridge.requestPreviousTabFocus();
            }
        };
        window.addEventListener('keydown', window.__xyxfTvKeyHandler, true);
    })();
"""

/**
 * WebView 弹窗。
 *
 * DialogProperties 会把遥控器返回键交给 onDismissRequest，右上角按钮也使用同一个关闭入口。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebViewDialog(
    url: String,
    title: String? = null,
    onDismiss: () -> Unit,
) {
    val closeFocusRequester = remember { FocusRequester() }
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }
    val loginResultJson = LocalBigScreenDeviceLoginResult.current.toH5LoginResultJson()
    val requestedTitle = title?.trim()?.takeIf(String::isNotEmpty)
    var isPageLoading by remember(url) { mutableStateOf(true) }
    var pageTitle by remember(url, requestedTitle) {
        mutableStateOf(requestedTitle ?: "网页详情")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        // Compose Dialog adds a platform dim-behind scrim independently of its
        // content. Clear it as well as leaving the full-screen host transparent.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialogWindow?.setDimAmount(0f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.95f)
                    .tvDialogPanel(RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = requestedTitle ?: pageTitle.ifBlank { "网页详情" },
                        color = HelpDialogWarmWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    TvDialogCloseButton(
                        onClick = onDismiss,
                        focusRequester = closeFocusRequester,
                        modifier = Modifier.onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown ||
                                event.key != Key.DirectionDown
                            ) {
                                false
                            } else {
                                webViewHolder[0]
                                    ?.takeIf { isXyxfDetailsUrl(it.url ?: url) }
                                    ?.requestFocus() == true
                            }
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HelpDialogWarmWhite)
                        .border(1.dp, HelpDialogGoldBorder, RoundedCornerShape(8.dp)),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                        CommonWebView(context, url, loginResultJson).apply {
                            webViewHolder[0] = this
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setBackgroundColor(Color.WHITE)
                            isFocusable = true
                            isFocusableInTouchMode = true
                            setOnTouchListener { view, event ->
                                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                                    view.requestFocus()
                                }
                                false
                            }
                            setOnKeyListener { view, keyCode, event ->
                                if (event.action != KeyEvent.ACTION_DOWN) {
                                    false
                                } else {
                                    val direction = when (keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> -1
                                        KeyEvent.KEYCODE_DPAD_DOWN -> 1
                                        else -> 0
                                    }
                                    if (direction == 0 || isXyxfDetailsUrl((view as WebView).url ?: url)) {
                                        // This page receives DPAD in the DOM; its #app container owns scrolling.
                                        false
                                    } else if (view.canScrollVertically(direction)) {
                                        val scrollStepPx = (72 * resources.displayMetrics.density).toInt()
                                            .coerceAtLeast(1)
                                        view.scrollBy(0, direction * scrollStepPx)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isPageLoading = true
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: android.webkit.WebResourceRequest,
                                    error: android.webkit.WebResourceError,
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request.isForMainFrame) {
                                        isPageLoading = false
                                    }
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    super.onPageFinished(view, url)
                                    isPageLoading = false
                                    if (isXyxfHostUrl(view.url ?: url)) {
                                        view.evaluateJavascript(INSTALL_XYXF_TV_SCROLL_SCRIPT, null)
                                    }
                                    // Some pages finish laying out shortly after onPageFinished.
                                    view.postDelayed(
                                        {
                                            view.evaluateJavascript(SCALE_WIDE_PAGE_SCRIPT, null)
                                        },
                                        300L,
                                    )
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (requestedTitle == null) {
                                        pageTitle = title
                                            ?.trim()
                                            ?.takeIf(String::isNotEmpty)
                                            ?: "网页详情"
                                    }
                                    view.contentDescription = requestedTitle ?: pageTitle
                                }
                            }
                            settings.apply {
                                loadWithOverviewMode = true
                                displayZoomControls = false
                            }
                            addJavascriptInterface(
                                WebFocusBridge(
                                    webView = this,
                                    onGetUserJson = { getUserJson() },
                                    onRequestNativeFocus = {
                                        closeFocusRequester.requestFocus()
                                    },
                                ),
                                "AndroidFocusBridge",
                            )
                            loadUrl(url)
                        }
                    },
                    update = { webView ->
                        val commonWebView = webView as? CommonWebView
                        if (commonWebView?.requestUrl != url) {
                            pageTitle = requestedTitle ?: "网页详情"
                            isPageLoading = true
                            commonWebView?.updateRequestUrl(url, loginResultJson)
                            webView.loadUrl(url)
                        } else {
                            commonWebView?.updateLoginResult(loginResultJson)
                        }
                    },
                    onRelease = { webView ->
                        if (webViewHolder[0] === webView) {
                            webViewHolder[0] = null
                        }
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.removeJavascriptInterface("AndroidFocusBridge")
                        webView.removeAllViews()
                        webView.destroy()
                    },
                    )
                    if (isPageLoading) {
                        WebPageLoadingOverlay(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    LaunchedEffect(url, requestedTitle) {
        pageTitle = requestedTitle ?: "网页详情"
        closeFocusRequester.requestFocus()
    }

}

@Composable
private fun WebPageLoadingOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "webPageLoading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "webPageLoadingRotation",
    )
    Box(
        modifier = modifier.background(HelpDialogWarmWhite),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_web_loading),
                contentDescription = "网页加载中",
                modifier = Modifier.size(58.dp).graphicsLayer { rotationZ = rotation },
            )
            Text(
                text = "网页加载中…",
                color = androidx.compose.ui.graphics.Color(0xFF8C251D),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
