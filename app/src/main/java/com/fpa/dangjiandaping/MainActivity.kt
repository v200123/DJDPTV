package com.fpa.dangjiandaping

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CONTENT_URL = "http://192.168.20.233:5173/xiaoyuTv/#/teacher"
private const val FOCUS_LOG_TAG = "FocusTrace"

private class WebFocusBridge(
    private val webView: WebView,
    private val onRequestNativeFocus: () -> Unit
) {
    @JavascriptInterface
    fun requestPreviousTabFocus() {
        Log.d(
            FOCUS_LOG_TAG,
            "H5 called requestPreviousTabFocus -> request selected native tab"
        )
        webView.post {
            webView.clearFocus()
            onRequestNativeFocus()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { MaterialTheme { DangJianTvApp() } }
    }
}

@Composable
private fun DangJianTvApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canWebViewGoBack by remember { mutableStateOf(false) }
    val nativeTabFocusRequester = remember { FocusRequester() }
    val rootView = LocalView.current

    LaunchedEffect(Unit) {
        nativeTabFocusRequester.requestFocus()
    }

    BackHandler(enabled = canWebViewGoBack) { webView?.goBack() }
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFFB50000))
    ) {
        val nativeHeaderHeight = (maxHeight * 0.14f).coerceIn(72.dp, 144.dp)
        Column(Modifier.fillMaxSize()) {
            NativeHeader(
                selectedTab = selectedTab,
                nativeTabFocusRequester = nativeTabFocusRequester,
                onTabSelected = {
                    selectedTab = it
                    rootView.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(nativeHeaderHeight)
            )
            WebContent(
                onCreated = { webView = it },
                onCanGoBackChanged = { canWebViewGoBack = it },
                onRequestNativeFocus = { nativeTabFocusRequester.requestFocus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun NativeHeader(
    selectedTab: Int,
    nativeTabFocusRequester: FocusRequester,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            "首页" to 0.75f,
            "有事找党员" to 1.10f,
            "咔哒时间·康巴党旗红" to 1.65f,
            "师资库" to 0.80f,
            "阵地库" to 0.80f,
            "课件库" to 0.80f,
            "我的党支部" to 1.10f
        )
    }

    Column(
        modifier = modifier.background(
            Brush.horizontalGradient(
                listOf(
                    ComposeColor(0xFFB00000),
                    ComposeColor(0xFFDF130B),
                    ComposeColor(0xFFA60000)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.54f)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "☭",
                color = ComposeColor(0xFFFFD36A),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "康巴党旗红数字党建平台",
                color = ComposeColor(0xFFFFD36A),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("中共甘孜州委组织部", color = ComposeColor.White, fontSize = 12.sp)
                Text(currentDateText(), color = ComposeColor(0xFFFFE5B6), fontSize = 10.sp)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ComposeColor.Transparent,
                            ComposeColor(0xFFFFD36A),
                            ComposeColor.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.46f)
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, (label, weight) ->
                TvTab(
                    index = index,
                    text = label,
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .weight(weight)
                        .then(
                            if (selectedTab == index) {
                                Modifier.focusRequester(nativeTabFocusRequester)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun TvTab(
    index: Int,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val tabScale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        label = "tabScale"
    )
    val shape = RoundedCornerShape(6.dp)

    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .scale(tabScale)
            .onFocusChanged { focusState ->
                val isFocusedNow = focusState.isFocused
                if (focused != isFocusedNow) {
                    focused = isFocusedNow
                    Log.d(
                        FOCUS_LOG_TAG,
                        "NativeTab[$index][$text] ${if (isFocusedNow) "FOCUSED" else "LOST_FOCUS"}"
                    )
                }
            }
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) ComposeColor(0xFFFFD36A) else ComposeColor.Transparent,
                shape = shape
            )
            .background(
                color = if (focused) ComposeColor(0x33FFFFFF) else ComposeColor.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = if (selected || focused) ComposeColor.White else ComposeColor(0xFFEBCACA),
            fontSize = if (selected) 18.sp else 16.sp,
            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .width(if (selected) 44.dp else 0.dp)
                .height(2.dp)
                .graphicsLayer { rotationZ = -5f }
                .background(ComposeColor(0xFFFFD36A), RoundedCornerShape(2.dp))
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebContent(
    onCreated: (WebView) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onRequestNativeFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.background(ComposeColor.Black),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.BLACK)
                isFocusable = true
                isFocusableInTouchMode = true
                onFocusChangeListener = android.view.View.OnFocusChangeListener { _, hasFocus ->
                    Log.d(
                        FOCUS_LOG_TAG,
                        "WebView ${if (hasFocus) "FOCUSED" else "LOST_FOCUS"}, url=$url"
                    )
                }
                addJavascriptInterface(
                    WebFocusBridge(this, onRequestNativeFocus),
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
                            "WebView DPAD_UP -> clear WebView focus and request selected native tab"
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
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(false)
                    userAgentString = "$userAgentString DangJianDaPingTV/1.0"
                }
                loadUrl(CONTENT_URL)
                onCreated(this)
            }
        },
        update = { view ->
            if (view.url.isNullOrEmpty()) view.loadUrl(CONTENT_URL)
        }
    )
}

private fun currentDateText(): String =
    SimpleDateFormat("yyyy年M月d日  EEEE", Locale.CHINA).format(Date())
