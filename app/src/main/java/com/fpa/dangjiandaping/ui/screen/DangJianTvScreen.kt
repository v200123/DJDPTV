package com.fpa.dangjiandaping.ui.screen

import android.view.SoundEffectConstants
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.fpa.dangjiandaping.R
import com.fpa.dangjiandaping.ui.header.NativeHeader
import com.fpa.dangjiandaping.ui.header.rememberTvTabFocusRequesters
import com.fpa.dangjiandaping.ui.home.HomeScreen
import com.fpa.dangjiandaping.ui.navigation.TV_TABS
import com.fpa.dangjiandaping.ui.navigation.TvTabDestination
import com.fpa.dangjiandaping.ui.web.WebContent

@Composable
fun DangJianTvScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var lastFocusedTab by rememberSaveable { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canWebViewGoBack by remember { mutableStateOf(false) }
    val tabFocusRequesters = rememberTvTabFocusRequesters()
    val rootView = LocalView.current
    val isInPreview = LocalInspectionMode.current
    val destination = TV_TABS.getOrElse(selectedTab) { TV_TABS.first() }.destination
    val webDestination = destination as? TvTabDestination.Web
    val webUrl = webDestination?.url ?: TV_TABS.firstNotNullOf {
        (it.destination as? TvTabDestination.Web)?.url
    }

    LaunchedEffect(isInPreview) {
        if (!isInPreview) {
            tabFocusRequesters[lastFocusedTab].requestFocus()
        }
    }

    BackHandler(enabled = destination is TvTabDestination.Web && canWebViewGoBack) {
        webView?.goBack()
    }

    LaunchedEffect(destination) {
        canWebViewGoBack = false
    }

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()

    ) {

        Image(
            painter = painterResource(R.drawable.bg_app),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        Column(Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(88.dp))
            NativeHeader(
                selectedTab = selectedTab,
                focusedTab = lastFocusedTab,
                tabFocusRequesters = tabFocusRequesters,
                onTabFocused = { lastFocusedTab = it },
                onTabSelected = {
                    selectedTab = it
                    lastFocusedTab = it
                    rootView.playSoundEffect(SoundEffectConstants.CLICK)
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (destination is TvTabDestination.NativeHome) {
                    HomeScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }else {
                    WebContent(
                        url = webUrl,
                        active = webDestination != null,
                        onCreated = { webView = it },
                        onReleased = { releasedView ->
                            if (webView === releasedView) {
                                webView = null
                            }
                        },
                        onCanGoBackChanged = { canWebViewGoBack = it },
                        onRequestNativeFocus = {
                            tabFocusRequesters[lastFocusedTab].requestFocus()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }


            }
        }
    }
}

@Preview(
    name = "党建电视主界面",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true
)
@Composable
private fun DangJianTvScreenPreview() {
    MaterialTheme {
        DangJianTvScreen()
    }
}
