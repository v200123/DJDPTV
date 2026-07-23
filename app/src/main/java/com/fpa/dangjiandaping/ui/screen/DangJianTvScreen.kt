package com.fpa.dangjiandaping.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.SoundEffectConstants
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.tv.material3.MaterialTheme
import com.fpa.dangjiandaping.R
import com.fpa.dangjiandaping.ui.header.NativeHeader
import com.fpa.dangjiandaping.ui.header.rememberTvTabFocusRequesters
import com.fpa.dangjiandaping.ui.home.HomeScreen
import com.fpa.dangjiandaping.ui.navigation.HomeRoute
import com.fpa.dangjiandaping.ui.navigation.TV_TABS
import com.fpa.dangjiandaping.ui.navigation.TvRoute
import com.fpa.dangjiandaping.ui.navigation.WebRoute
import com.fpa.dangjiandaping.ui.navigation.toRoute
import com.fpa.dangjiandaping.ui.web.WebContent

private const val HOME_TAB_INDEX = 0

@Composable
fun DangJianTvScreen() {
    val backStack = rememberNavBackStack(HomeRoute)
    val currentRoute = (backStack.lastOrNull() as? TvRoute) ?: HomeRoute
    val selectedTab = when (currentRoute) {
        HomeRoute -> HOME_TAB_INDEX
        is WebRoute -> currentRoute.tabIndex
    }

    var lastFocusedTab by rememberSaveable { mutableStateOf(HOME_TAB_INDEX) }
    var headerHasFocus by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canWebViewGoBack by remember { mutableStateOf(false) }

    val tabFocusRequesters = rememberTvTabFocusRequesters()
    val rootView = LocalView.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isInPreview = LocalInspectionMode.current

    fun replaceRoute(route: TvRoute) {
        backStack.clear()
        backStack.add(route)
    }

    fun handleBack() {
        when {
            currentRoute is WebRoute && !headerHasFocus && canWebViewGoBack -> {
                webView?.goBack()
            }

            !headerHasFocus -> {
                tabFocusRequesters[selectedTab].requestFocus()
            }

            selectedTab != HOME_TAB_INDEX -> {
                lastFocusedTab = HOME_TAB_INDEX
                replaceRoute(HomeRoute)
                tabFocusRequesters[HOME_TAB_INDEX].requestFocus()
            }

            else -> activity?.finish()
        }
    }

    LaunchedEffect(isInPreview) {
        if (!isInPreview) {
            tabFocusRequesters[lastFocusedTab].requestFocus()
        }
    }

    LaunchedEffect(currentRoute) {
        canWebViewGoBack = false
    }

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_app),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        Column(Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(22.dp))
            NativeHeader(
                selectedTab = selectedTab,
                focusedTab = lastFocusedTab,
                tabFocusRequesters = tabFocusRequesters,
                onTabFocused = { lastFocusedTab = it },
                onTabSelected = { tabIndex ->
                    lastFocusedTab = tabIndex
                    val targetRoute = TV_TABS[tabIndex].destination.toRoute(tabIndex)
                    if (targetRoute != currentRoute) {
                        replaceRoute(targetRoute)
                    }
                    rootView.playSoundEffect(SoundEffectConstants.CLICK)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { headerHasFocus = it.hasFocus },
            )
            Spacer(modifier = Modifier.height(12.dp))

            NavDisplay(
                backStack = backStack,
                onBack = ::handleBack,
                entryProvider = entryProvider {
                    entry<HomeRoute> {
                        HomeScreen(modifier = Modifier.fillMaxSize())
                    }
                    entry<WebRoute> { route ->
                        WebContent(
                            url = route.url,
                            active = currentRoute == route,
                            onCreated = { createdView ->
                                if (backStack.lastOrNull() == route) {
                                    webView = createdView
                                }
                            },
                            onReleased = { releasedView ->
                                if (webView === releasedView) {
                                    webView = null
                                }
                            },
                            onCanGoBackChanged = { canGoBack ->
                                if (backStack.lastOrNull() == route) {
                                    canWebViewGoBack = canGoBack
                                }
                            },
                            onRequestNativeFocus = {
                                tabFocusRequesters[selectedTab].requestFocus()
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(50.dp,0.dp),
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(
    name = "党建电视主界面",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun DangJianTvScreenPreview() {
    val navigationEventDispatcherOwner =
        rememberNavigationEventDispatcherOwner(parent = null)

    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
    ) {
        MaterialTheme {
            DangJianTvScreen()
        }
    }
}
