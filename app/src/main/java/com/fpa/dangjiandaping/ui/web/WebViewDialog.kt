package com.fpa.dangjiandaping.ui.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

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

/**
 * WebView 弹窗。
 *
 * DialogProperties 会把遥控器返回键交给 onDismissRequest，右上角按钮也使用同一个关闭入口。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebViewDialog(
    url: String,
    onDismiss: () -> Unit,
) {
    val closeFocusRequester = remember { FocusRequester() }
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }
    var pageTitle by remember { mutableStateOf("网页详情") }
    var closeFocusRequestTrigger by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HelpDialogScrim)
                .padding(horizontal = 42.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .tvDialogPanel(RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pageTitle.ifBlank { "网页详情" },
                        color = HelpDialogWarmWhite,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TvDialogCloseButton(
                        onClick = onDismiss,
                        focusRequester = closeFocusRequester,
                    )
                }
                Spacer(Modifier.height(14.dp))

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HelpDialogWarmWhite)
                        .border(1.dp, HelpDialogGoldBorder, RoundedCornerShape(8.dp)),
                    factory = { context ->
                        val scrollStepPx =
                            (72 * context.resources.displayMetrics.density).toInt()
                        object : WebView(context) {
                            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                                if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                    if (event.action == KeyEvent.ACTION_DOWN) {
                                        if (canScrollVertically(-1)) {
                                            scrollBy(0, -scrollStepPx)
                                        } else if (event.repeatCount == 0) {
                                            isFocusable = false
                                            isFocusableInTouchMode = false
                                            clearFocus()
                                            closeFocusRequestTrigger++
                                        }
                                    }
                                    // Consume DOWN and UP before WebView/HTML handles them.
                                    return true
                                }
                                if (event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                    if (event.action == KeyEvent.ACTION_DOWN &&
                                        canScrollVertically(1)
                                    ) {
                                        scrollBy(0, scrollStepPx)
                                    }
                                    return true
                                }
                                return super.dispatchKeyEvent(event)
                            }
                        }.apply {
                            webViewHolder[0] = this
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                defaultFocusHighlightEnabled = false
                            }

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
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Some pages finish laying out shortly after onPageFinished.
                                    view.postDelayed(
                                        { view.evaluateJavascript(SCALE_WIDE_PAGE_SCRIPT, null) },
                                        300L,
                                    )
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    pageTitle = title
                                        ?.trim()
                                        ?.takeIf(String::isNotEmpty)
                                        ?: "网页详情"
                                    view.contentDescription = pageTitle
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                userAgentString = MOBILE_BROWSER_USER_AGENT
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = false
                                displayZoomControls = false
                                setSupportZoom(true)
                            }
                            isVerticalScrollBarEnabled = true
                            isScrollbarFadingEnabled = false
                            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                            tag = url
                            loadUrl(url)
                        }
                    },
                    update = { webView ->
                        if (webView.tag != url) {
                            pageTitle = "网页详情"
                            webView.tag = url
                            webView.loadUrl(url)
                        }
                    },
                    onRelease = { webView ->
                        if (webViewHolder[0] === webView) {
                            webViewHolder[0] = null
                        }
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.removeAllViews()
                        webView.destroy()
                    },
                )
            }
        }
    }

    LaunchedEffect(url) {
        pageTitle = "网页详情"
        closeFocusRequester.requestFocus()
    }

    LaunchedEffect(closeFocusRequestTrigger) {
        if (closeFocusRequestTrigger > 0) {
            // Let AndroidView finish dispatching DPAD_UP before Compose takes focus back.
            withFrameNanos { }
            closeFocusRequester.requestFocus()
            withFrameNanos { }
            webViewHolder[0]?.let { webView ->
                webView.isFocusable = true
                webView.isFocusableInTouchMode = true
            }
        }
    }
}
