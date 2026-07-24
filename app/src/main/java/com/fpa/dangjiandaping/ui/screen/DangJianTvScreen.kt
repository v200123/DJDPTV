package com.fpa.dangjiandaping.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.SoundEffectConstants
import android.view.View
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
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
import androidx.navigation3.runtime.rememberNavBackStack
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
    var pendingContentFocusRoute by remember { mutableStateOf<TvRoute?>(null) }

    val tabFocusRequesters = rememberTvTabFocusRequesters()
    val contentFocusRequester = remember { FocusRequester() }
    val rootView = LocalView.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isInPreview = LocalInspectionMode.current

    fun replaceRoute(route: TvRoute) {
        backStack.clear()
        backStack.add(route)
    }

    fun requestSelectedTabFocus() {
        lastFocusedTab = selectedTab
        tabFocusRequesters[selectedTab].requestFocus(FocusDirection.Up)
    }

    fun activateTab(tabIndex: Int, moveFocusToContent: Boolean) {
        lastFocusedTab = tabIndex
        val targetRoute = TV_TABS[tabIndex].destination.toRoute(tabIndex)
        if (targetRoute != currentRoute) {
            webView = null
            replaceRoute(targetRoute)
        }
        pendingContentFocusRoute = if (moveFocusToContent) targetRoute else null
        rootView.playSoundEffect(SoundEffectConstants.CLICK)
    }

    fun handleBack() {
        when {
            currentRoute is WebRoute && !headerHasFocus && canWebViewGoBack -> {
                webView?.goBack()
            }

            !headerHasFocus -> {
                requestSelectedTabFocus()
            }

            selectedTab != HOME_TAB_INDEX -> {
                lastFocusedTab = HOME_TAB_INDEX
                replaceRoute(HomeRoute)
                tabFocusRequesters[HOME_TAB_INDEX].requestFocus()
            }

            else -> activity?.finish()
        }
    }

    BackHandler(onBack = ::handleBack)

    LaunchedEffect(isInPreview) {
        if (!isInPreview) {
            tabFocusRequesters[lastFocusedTab].requestFocus()
        }
    }

    LaunchedEffect(currentRoute, pendingContentFocusRoute, webView) {
        canWebViewGoBack = false
        if (pendingContentFocusRoute == currentRoute) {
            when (currentRoute) {
                HomeRoute -> {
                    withFrameNanos { }
                    contentFocusRequester.requestFocus(FocusDirection.Down)
                    pendingContentFocusRoute = null
                }

                is WebRoute -> {
                    webView?.let { currentWebView ->
                        currentWebView.requestFocus(View.FOCUS_DOWN)
                        pendingContentFocusRoute = null
                    }
                }
            }
        }
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
                onTabFocused = { tabIndex ->
                    if (tabIndex == selectedTab) {
                        lastFocusedTab = tabIndex
                    } else {
                        activateTab(tabIndex, moveFocusToContent = false)
                    }
                },
                onTabSelected = { tabIndex -> activateTab(tabIndex, moveFocusToContent = false) },
                onTabDown = { tabIndex -> activateTab(tabIndex, moveFocusToContent = true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { headerHasFocus = it.hasFocus },
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
            ) {
                val homeIsActive = currentRoute == HomeRoute
                if (homeIsActive) {
                    HomeScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f),
                        contentFocusRequester = contentFocusRequester,
                        onRequestTabFocus = ::requestSelectedTabFocus,
                    )
                }

                TV_TABS.forEachIndexed { tabIndex, tab ->
                    val route = tab.destination.toRoute(tabIndex) as? WebRoute ?: return@forEachIndexed
                    val webIsActive = currentRoute == route
                    WebContent(
                        url = route.url,
                        active = webIsActive,
                        onCreated = { createdView ->
                            if (currentRoute == route) webView = createdView
                        },
                        onReleased = { releasedView ->
                            if (webView === releasedView) webView = null
                        },
                        onCanGoBackChanged = { canGoBack ->
                            if (currentRoute == route) canWebViewGoBack = canGoBack
                        },
                        onRequestNativeFocus = ::requestSelectedTabFocus,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(50.dp, 0.dp)
                            .zIndex(if (webIsActive) 1f else 0f),
                    )
                }
            }
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
