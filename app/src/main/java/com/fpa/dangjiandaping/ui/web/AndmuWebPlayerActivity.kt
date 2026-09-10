package com.fpa.dangjiandaping.ui.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.ui.focus.focusOnClick
import com.fpa.dangjiandaping.ui.focus.logFocusTarget

/** 全屏承载安牧 WebSDK；进入网页区域后用遥控器驱动原生虚拟指针。 */
class AndmuWebPlayerActivity : ComponentActivity() {
    private val virtualPointer = WebViewVirtualPointer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        val playerUrl = intent.getStringExtra(EXTRA_PLAYER_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        if (playerUrl.isBlank()) {
            finish()
            return
        }
        setContent {
            MaterialTheme {
                AndmuWebPlayer(
                    url = playerUrl,
                    title = title,
                    virtualPointer = virtualPointer,
                    onBack = ::finish,
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (virtualPointer.handleKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        virtualPointer.detach()
        super.onDestroy()
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val EXTRA_PLAYER_URL = "andmu_player_url"
        private const val EXTRA_TITLE = "andmu_player_title"

        fun newIntent(context: Context, url: String, title: String): Intent =
            Intent(context, AndmuWebPlayerActivity::class.java)
                .putExtra(EXTRA_PLAYER_URL, url)
                .putExtra(EXTRA_TITLE, title)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AndmuWebPlayer(
    url: String,
    title: String,
    virtualPointer: WebViewVirtualPointer,
    onBack: () -> Unit,
) {
    val backFocusRequester = remember { FocusRequester() }
    val webViewFocusRequester = remember { FocusRequester() }

    DisposableEffect(virtualPointer) {
        virtualPointer.setOnExitPointerMode {
            backFocusRequester.requestFocus()
        }
        onDispose {
            virtualPointer.setOnExitPointerMode(null)
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        backFocusRequester.requestFocus()
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black),
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    // WebSDK 页面按当前电视窗口尺寸布局，避免被概览模式缩放到顶部一小块。
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = false
                    setInitialScale(100)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    virtualPointer.attach(this)
                    loadUrl(url)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(webViewFocusRequester)
                .logFocusTarget("Andmu.WebSdk")
                .focusProperties { up = backFocusRequester }
                .onFocusChanged {
                    if (it.isFocused) virtualPointer.setPointerEnabled(true)
                }
                .focusable(),
            onRelease = { webView ->
                virtualPointer.detach(webView)
                webView.destroy()
            },
        )

        if (virtualPointer.pointerVisible) {
            val halfPointerSize = 13.dp
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            virtualPointer.pointerX.toInt() - halfPointerSize.roundToPx(),
                            virtualPointer.pointerY.toInt() - halfPointerSize.roundToPx(),
                        )
                    }
                    .size(26.dp)
                    .background(androidx.compose.ui.graphics.Color(0xCCFFFFFF), CircleShape)
                    .border(2.dp, androidx.compose.ui.graphics.Color(0xFFD71920), CircleShape),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xD9000000))
                .padding(horizontal = 30.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = title.ifBlank { "实时画面" },
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "方向键移动 · 确认点击",
                color = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
                fontSize = 14.sp,
            )
            TvBackButton(
                focusRequester = backFocusRequester,
                downFocusRequester = webViewFocusRequester,
                onFocusReceived = { virtualPointer.setPointerEnabled(false) },
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun TvBackButton(
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onFocusReceived: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var confirmPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 44.dp)
            .focusRequester(focusRequester)
            .logFocusTarget("Andmu.WebSdk.Back")
            .focusOnClick(focusRequester)
            .focusProperties { down = downFocusRequester }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocusReceived()
                if (!it.isFocused) confirmPressed = false
            }
            .background(
                color = if (focused) androidx.compose.ui.graphics.Color(0xFFD71920) else
                    androidx.compose.ui.graphics.Color(0x99000000),
                shape = shape,
            )
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) androidx.compose.ui.graphics.Color(0xFFFFD889) else
                    androidx.compose.ui.graphics.Color(0x88FFFFFF),
                shape = shape,
            )
            .onPreviewKeyEvent { event ->
                val confirm = event.key in ConfirmKeys
                when {
                    !confirm -> false
                    event.type == KeyEventType.KeyDown -> {
                        confirmPressed = true
                        true
                    }
                    event.type == KeyEventType.KeyUp && confirmPressed -> {
                        confirmPressed = false
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .clickable {
                focusRequester.requestFocus()
                onClick()
            }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Text("返回", color = androidx.compose.ui.graphics.Color.White, fontSize = 17.sp)
    }
}

private val ConfirmKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * 使用原生触摸事件点击 WebView，而非向网页派发不受信任的 JavaScript click 事件。
 * 因此对于跨域 iframe、Canvas 及自绘播放器控件同样有效。
 */
private class WebViewVirtualPointer {
    var pointerX by mutableStateOf(-1f)
        private set
    var pointerY by mutableStateOf(-1f)
        private set
    var pointerVisible by mutableStateOf(false)
        private set

    private var webView: WebView? = null
    private var pointerEnabled = false
    private var onExitPointerMode: (() -> Unit)? = null

    fun attach(webView: WebView) {
        this.webView = webView
        webView.post(::ensurePointerInBounds)
    }

    fun detach(releasedWebView: WebView? = null) {
        if (releasedWebView == null || releasedWebView === webView) {
            webView = null
            pointerEnabled = false
            pointerVisible = false
        }
    }

    fun setPointerEnabled(enabled: Boolean) {
        pointerEnabled = enabled
        pointerVisible = enabled
        if (enabled) webView?.post(::ensurePointerInBounds)
    }

    fun setOnExitPointerMode(listener: (() -> Unit)?) {
        onExitPointerMode = listener
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!pointerEnabled) return false
        val keyCode = event.keyCode
        if (keyCode !in PointerKeyCodes) return false
        if (event.action == KeyEvent.ACTION_UP) return true
        if (event.action != KeyEvent.ACTION_DOWN) return true

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> moveBy(-pointerStep(event), 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveBy(pointerStep(event), 0f)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveBy(0f, pointerStep(event))
            KeyEvent.KEYCODE_DPAD_UP -> moveBy(0f, -pointerStep(event))
            else -> tap()
        }
    }

    private fun pointerStep(event: KeyEvent): Float =
        if (event.repeatCount > 0) 52f else 32f

    private fun moveBy(deltaX: Float, deltaY: Float): Boolean {
        val bounds = webViewBounds() ?: return false
        ensurePointerInBounds()
        val nextX = (pointerX + deltaX).coerceIn(bounds.left, bounds.right)
        val nextY = (pointerY + deltaY).coerceIn(bounds.top, bounds.bottom)

        // 指针已在顶边时，主动回到原生返回按钮，不能交给 WebView 消费。
        if (deltaY < 0f && nextY == bounds.top) {
            setPointerEnabled(false)
            onExitPointerMode?.invoke()
            return true
        }
        pointerX = nextX
        pointerY = nextY
        return true
    }

    private fun tap(): Boolean {
        val view = webView ?: return false
        val bounds = webViewBounds() ?: return false
        ensurePointerInBounds()
        val x = (pointerX - bounds.left).coerceIn(0f, bounds.width - 1f)
        val y = (pointerY - bounds.top).coerceIn(0f, bounds.height - 1f)
        val downTime = android.os.SystemClock.uptimeMillis()
        MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0).also {
            view.dispatchTouchEvent(it)
            it.recycle()
        }
        val upTime = android.os.SystemClock.uptimeMillis()
        MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0).also {
            view.dispatchTouchEvent(it)
            it.recycle()
        }
        return true
    }

    private fun ensurePointerInBounds() {
        val bounds = webViewBounds() ?: return
        pointerX = if (pointerX < bounds.left || pointerX > bounds.right) {
            (bounds.left + bounds.right) / 2f
        } else {
            pointerX
        }
        pointerY = if (pointerY < bounds.top || pointerY > bounds.bottom) {
            (bounds.top + bounds.bottom) / 2f
        } else {
            pointerY
        }
    }

    private fun webViewBounds(): PointerBounds? {
        val view = webView ?: return null
        if (view.width <= 0 || view.height <= 0) return null
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return PointerBounds(
            left = location[0].toFloat(),
            top = location[1].toFloat(),
            width = view.width.toFloat(),
            height = view.height.toFloat(),
        )
    }

    private data class PointerBounds(
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float,
    ) {
        val right: Float get() = left + width - 1f
        val bottom: Float get() = top + height - 1f
    }

    private companion object {
        val PointerKeyCodes = setOf(
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
        )
    }
}
