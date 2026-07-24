package com.fpa.dangjiandaping

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/**
 * Isolates WebView from Compose and Navigation 3 for white-screen diagnostics.
 * Launch with:
 * adb shell am start -n com.fpa.dangjiandaping/.NativeWebViewTestActivity
 */
class NativeWebViewTestActivity : ComponentActivity() {

    private lateinit var diagnosticsText: TextView
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Native WebView Test"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(20, 20, 20))
        }

        diagnosticsText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            text = "Creating native WebView…"
        }
        val reloadButton = Button(this).apply {
            text = "Reload"
            setOnClickListener { webView.reload() }
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                diagnosticsText,
                LinearLayout.LayoutParams(0, dp(56), 1f)
            )
            addView(reloadButton, LinearLayout.LayoutParams(dp(120), dp(56)))
        }
        root.addView(toolbar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56)
        ))

        webView = createWebView()
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            webView.loadDataWithBaseURL(
                TEST_PAGE_BASE_URL,
                TEST_PAGE_HTML,
                "text/html",
                "UTF-8",
                null
            )
        } else {
            webView.loadUrl(url)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView = WebView(this).apply {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                updateDiagnostics("Loaded: $url")
                reportViewport()
            }
        }
        webChromeClient = WebChromeClient()
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_NO_CACHE
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0"
            setSupportZoom(true)
        }
        addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            updateDiagnostics("Native WebView: ${right - left} × ${bottom - top}")
        }
    }

    private fun reportViewport() {
        val script = """
            (function () {
              var app = document.querySelector('.app-container');
              var probe = document.querySelector('#probe');
              var content = probe || app;
              return JSON.stringify({
                innerHeight: window.innerHeight,
                outerHeight: window.outerHeight,
                html: getComputedStyle(document.documentElement).height,
                body: getComputedStyle(document.body).height,
                content: content ? getComputedStyle(content).height : 'not-found'
              });
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            Log.i(LOG_TAG, "Viewport diagnostics: $result")
            updateDiagnostics("Native WebView ${webView.width} × ${webView.height}; CSS: $result")
        }
    }

    private fun updateDiagnostics(message: String) {
        diagnosticsText.text = message
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val LOG_TAG = "NativeWebViewTest"
        const val EXTRA_URL = "url"
        const val TEST_PAGE_BASE_URL = "https://webview-test.local/"
        const val TEST_PAGE_HTML = """
            <!doctype html>
            <html>
              <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                  html, body { margin: 0; width: 100%; height: 100%; font-family: sans-serif; }
                  #probe { width: 100vw; height: 100vh; box-sizing: border-box; padding: 32px;
                    color: white; background: linear-gradient(135deg, #1261a0, #553c9a); }
                  h1 { margin-top: 0; }
                  pre { padding: 18px; border-radius: 12px; background: rgba(0, 0, 0, .25); }
                </style>
              </head>
              <body>
                <main id="probe">
                  <h1>Native WebView viewport probe</h1>
                  <p>这个区域应填满 WebView。若 CSS 100vh 被算为 0，本区域不会显示。</p>
                  <pre id="metrics">Reading viewport…</pre>
                </main>
                <script>
                  function updateMetrics() {
                    var probe = document.getElementById('probe');
                    document.getElementById('metrics').textContent = JSON.stringify({
                      innerWidth: window.innerWidth,
                      innerHeight: window.innerHeight,
                      htmlHeight: getComputedStyle(document.documentElement).height,
                      bodyHeight: getComputedStyle(document.body).height,
                      css100vh: getComputedStyle(probe).height,
                      probeRectHeight: probe.getBoundingClientRect().height
                    }, null, 2);
                  }
                  addEventListener('resize', updateMetrics);
                  updateMetrics();
                </script>
              </body>
            </html>
        """
    }
}
