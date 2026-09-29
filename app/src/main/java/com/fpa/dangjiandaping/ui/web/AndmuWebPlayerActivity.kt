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
    val handleWebViewPermissionRequest = rememberWebViewPermissionHandler()
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
        webViewFocusRequester.requestFocus()
    }

    BackHandler {
        if (virtualPointer.pointerVisible) {
            virtualPointer.requestExitFocus()
        } else {
            onBack()
        }
    }

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
                    settings.userAgentString = MOBILE_BROWSER_USER_AGENT
                    // WebSDK 页面按当前电视窗口尺寸布局，避免被概览模式缩放到顶部一小块。
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = true
                    setInitialScale(100)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
                            handleWebViewPermissionRequest(request)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, pageUrl: String?) {
                            super.onPageFinished(view, pageUrl)
                            // Disable Chromium's temporary tap highlight; it can look like
                            // a dark overlay when the remote pointer dispatches a touch.
                            view.evaluateJavascript(
                                "(function(){var s=document.getElementById('__android_remote_tap_style');"
                                    + "if(!s){s=document.createElement('style');"
                                    + "s.id='__android_remote_tap_style';"
                                    + "(document.head||document.documentElement).appendChild(s);}"
                                    + "s.textContent='*{-webkit-tap-highlight-color:rgba(0,0,0,0)!important;}';"
                                    + "})();",
                                null,
                            )
                        }
                    }
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
                text = "方向键移动 · 确认点击 · 按住确认键拖动",
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
 * Emulates a mouse pointer for WebView: directional keys send hover/move events,
 * and holding OK while moving sends a primary-button drag.
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
    private var pointerHovering = false
    private var primaryButtonDown = false
    private var mouseDownTime = 0L
    private var onExitPointerMode: (() -> Unit)? = null

    fun attach(webView: WebView) {
        this.webView = webView
        webView.post {
            ensurePointerInBounds()
            if (pointerEnabled && !pointerHovering) {
                dispatchHover(MotionEvent.ACTION_HOVER_ENTER)
                pointerHovering = true
            }
        }
    }

    fun detach(releasedWebView: WebView? = null) {
        if (releasedWebView == null || releasedWebView === webView) {
            setPointerEnabled(false)
            webView = null
            pointerVisible = false
        }
    }

    fun setPointerEnabled(enabled: Boolean) {
        if (pointerEnabled == enabled) return
        if (!enabled) {
            releasePrimaryButton()
            dispatchHover(MotionEvent.ACTION_HOVER_EXIT)
            pointerHovering = false
        }
        pointerEnabled = enabled
        pointerVisible = enabled
        if (enabled) {
            webView?.post {
                ensurePointerInBounds()
                dispatchHover(MotionEvent.ACTION_HOVER_ENTER)
                pointerHovering = true
            }
        }
    }

    fun requestExitFocus() {
        onExitPointerMode?.invoke()
    }

    fun setOnExitPointerMode(listener: (() -> Unit)?) {
        onExitPointerMode = listener
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!pointerEnabled) return false
        val keyCode = event.keyCode
        if (keyCode !in PointerKeyCodes) return false
        if (event.action == KeyEvent.ACTION_UP) {
            if (keyCode in ConfirmKeyCodes) releasePrimaryButton()
            return true
        }
        if (event.action != KeyEvent.ACTION_DOWN) return true

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> moveBy(-pointerStep(event), 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveBy(pointerStep(event), 0f)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveBy(0f, pointerStep(event))
            KeyEvent.KEYCODE_DPAD_UP -> moveBy(0f, -pointerStep(event))
            else -> {
                if (event.repeatCount == 0) pressPrimaryButton()
                true
            }
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
            releasePrimaryButton()
            setPointerEnabled(false)
            onExitPointerMode?.invoke()
            return true
        }
        pointerX = nextX
        pointerY = nextY
        if (primaryButtonDown) {
            dispatchTouchGesture(MotionEvent.ACTION_MOVE)
        } else {
            if (!pointerHovering) {
                dispatchHover(MotionEvent.ACTION_HOVER_ENTER)
                pointerHovering = true
            } else {
                dispatchHover(MotionEvent.ACTION_HOVER_MOVE)
            }
        }
        return true
    }

    private fun pressPrimaryButton(): Boolean {
        if (primaryButtonDown) return true
        ensurePointerInBounds()
        dispatchHover(MotionEvent.ACTION_HOVER_EXIT)
        pointerHovering = false
        mouseDownTime = android.os.SystemClock.uptimeMillis()
        primaryButtonDown = true
        dispatchTouchGesture(MotionEvent.ACTION_DOWN)
        return true
    }

    private fun releasePrimaryButton() {
        if (!primaryButtonDown) return
        dispatchTouchGesture(MotionEvent.ACTION_UP)
        primaryButtonDown = false
        if (pointerEnabled) {
            dispatchHover(MotionEvent.ACTION_HOVER_ENTER)
            pointerHovering = true
        }
    }

    private fun dispatchHover(action: Int) {
        val view = webView ?: return
        val (x, y) = localPointerPosition() ?: return
        val now = android.os.SystemClock.uptimeMillis()
        createMouseEvent(action, x, y, 0, now).also { event ->
            view.dispatchGenericMotionEvent(event)
            event.recycle()
        }
    }

    private fun dispatchTouchGesture(action: Int) {
        val view = webView ?: return
        val (x, y) = localPointerPosition() ?: return
        val now = android.os.SystemClock.uptimeMillis()
        val downTime = if (primaryButtonDown) mouseDownTime else now
        MotionEvent.obtain(downTime, now, action, x, y, 0).also { event ->
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            view.dispatchTouchEvent(event)
            event.recycle()
        }
    }

    private fun createMouseEvent(
        action: Int,
        x: Float,
        y: Float,
        buttonState: Int,
        eventTime: Long,
        downTime: Long = eventTime,
    ): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
        val coordinates = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = if (buttonState == 0) 0f else 1f
            size = 1f
        }
        return MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            1,
            arrayOf(properties),
            arrayOf(coordinates),
            0,
            buttonState,
            1f,
            1f,
            0,
            0,
            android.view.InputDevice.SOURCE_MOUSE,
            0,
        )
    }

    private fun localPointerPosition(): Pair<Float, Float>? {
        val bounds = webViewBounds() ?: return null
        ensurePointerInBounds()
        return (pointerX - bounds.left) to (pointerY - bounds.top)
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
        val ConfirmKeyCodes = setOf(
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
        )
        val PointerKeyCodes = ConfirmKeyCodes + setOf(
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
        )
    }
}
