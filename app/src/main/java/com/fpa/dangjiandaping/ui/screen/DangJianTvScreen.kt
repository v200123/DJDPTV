package com.fpa.dangjiandaping.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.SoundEffectConstants
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
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
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.tv.material3.MaterialTheme
import com.fpa.dangjiandaping.R
import com.fpa.dangjiandaping.ui.MainContentHorizontalPadding
import com.fpa.dangjiandaping.ui.header.NativeHeader
import com.fpa.dangjiandaping.ui.header.rememberTvTabFocusRequesters
import com.fpa.dangjiandaping.ui.home.HomeScreen
import com.fpa.dangjiandaping.ui.home.defaultPartyStats
import com.fpa.dangjiandaping.ui.home.fetchPartyStats
import com.fpa.dangjiandaping.ui.navigation.HomeRoute
import com.fpa.dangjiandaping.ui.navigation.TV_TABS
import com.fpa.dangjiandaping.ui.navigation.TvRoute
import com.fpa.dangjiandaping.ui.navigation.WebRoute
import com.fpa.dangjiandaping.ui.navigation.coursewareRoute
import com.fpa.dangjiandaping.ui.navigation.partyBuildingRoute
import com.fpa.dangjiandaping.ui.navigation.toRoute
import com.fpa.dangjiandaping.ui.web.PublicHelpRequest
import com.fpa.dangjiandaping.ui.web.PublicHelpRequestDialog
import com.fpa.dangjiandaping.ui.web.WebContent
import com.fpa.dangjiandaping.ui.web.mockPublicHelpRequests
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val HOME_TAB_INDEX = 0
private const val MOCK_HELP_MIN_DELAY_MILLIS = 5_000L
private const val MOCK_HELP_MAX_DELAY_MILLIS = 12_001L
private const val CLEAR_WEB_DOM_FOCUS_SCRIPT =
    "(function(){var el=document.activeElement;" +
        "if(el&&el!==document.body&&el!==document.documentElement&&" +
        "typeof el.blur==='function'){el.blur();}" +
        "return true;})();"

@Composable
fun DangJianTvScreen(
    manualHelpTrigger: Int = 0,
) {
    val backStack = rememberNavBackStack(HomeRoute)
    val currentRoute = (backStack.lastOrNull() as? TvRoute) ?: HomeRoute
    val selectedTab = when (currentRoute) {
        HomeRoute -> HOME_TAB_INDEX
        is WebRoute -> currentRoute.tabIndex
    }

    var lastFocusedTab by rememberSaveable { mutableStateOf(HOME_TAB_INDEX) }
    var headerHasFocus by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var readyWebView by remember { mutableStateOf<WebView?>(null) }
    var canWebViewGoBack by remember { mutableStateOf(false) }
    var pendingContentFocusRoute by remember { mutableStateOf<TvRoute?>(null) }
    var pendingTabFocusIndex by remember { mutableStateOf<Int?>(null) }
    var partyStats by remember { mutableStateOf(defaultPartyStats) }
    var publicHelpRequest by remember { mutableStateOf<PublicHelpRequest?>(null) }

    val tabFocusRequesters = rememberTvTabFocusRequesters()
    val contentFocusRequester = remember { FocusRequester() }
    val rootView = LocalView.current
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isInPreview = LocalInspectionMode.current

    fun replaceRoute(route: TvRoute) {
        if (backStack.isEmpty()) {
            backStack.add(route)
            return
        }
        backStack[backStack.lastIndex] = route
        while (backStack.size > 1) {
            backStack.removeAt(0)
        }
    }

    fun requestSelectedTabFocus() {
        lastFocusedTab = selectedTab
        tabFocusRequesters[selectedTab].requestFocus(FocusDirection.Up)
    }

    fun activateRoute(tabIndex: Int, targetRoute: TvRoute, moveFocusToContent: Boolean) {
        lastFocusedTab = tabIndex
        // NavDisplay disposes the previous entry. During disposal Compose may temporarily restore
        // focus to the Home tab; keep the intended tab pending so that transient focus event does
        // not get interpreted by onTabFocused as a navigation back to Home.
        pendingTabFocusIndex = if (moveFocusToContent) null else tabIndex
        if (targetRoute != currentRoute) {
            webView = null
            readyWebView = null
            replaceRoute(targetRoute)
        }
        pendingContentFocusRoute = if (moveFocusToContent) targetRoute else null
        rootView.playSoundEffect(SoundEffectConstants.CLICK)
    }

    fun activateTab(tabIndex: Int, moveFocusToContent: Boolean) {
        val targetRoute = (currentRoute as? WebRoute)
            ?.takeIf { it.tabIndex == tabIndex }
            ?: TV_TABS[tabIndex].destination.toRoute(tabIndex)
        activateRoute(
            tabIndex = tabIndex,
            targetRoute = targetRoute,
            moveFocusToContent = moveFocusToContent,
        )
    }

    fun openPartyBuilding(channelId: Int) {
        activateRoute(
            tabIndex = 6,
            targetRoute = partyBuildingRoute(channelId),
            moveFocusToContent = false,
        )
        pendingTabFocusIndex = 6
    }

    fun openCourseware(type: Int) {
        activateRoute(
            tabIndex = 5,
            targetRoute = coursewareRoute(type),
            moveFocusToContent = false,
        )
        pendingTabFocusIndex = 5
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

    LaunchedEffect(selectedTab, pendingTabFocusIndex) {
        if (pendingTabFocusIndex == selectedTab) {
            withFrameNanos { }
            tabFocusRequesters[selectedTab].requestFocus(FocusDirection.Up)
            pendingTabFocusIndex = null
        }
    }

    LaunchedEffect(Unit) {
        fetchPartyStats()?.let { fetchedCounts ->
            partyStats = defaultPartyStats.map { stat ->
                fetchedCounts[stat.title]?.let { update ->
                    stat.copy(count = update.newsCount, channelId = update.channelId)
                } ?: stat
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isInPreview || mockPublicHelpRequests.isEmpty()) return@LaunchedEffect

        delay(
            Random.nextLong(
                from = MOCK_HELP_MIN_DELAY_MILLIS,
                until = MOCK_HELP_MAX_DELAY_MILLIS,
            ),
        )
        while (publicHelpRequest != null) {
            delay(1_000L)
        }
        publicHelpRequest = mockPublicHelpRequests.random()
    }

    LaunchedEffect(manualHelpTrigger) {
        if (isInPreview || manualHelpTrigger <= 0 || mockPublicHelpRequests.isEmpty()) {
            return@LaunchedEffect
        }
        val otherRequests = mockPublicHelpRequests.filterNot {
            it.id == publicHelpRequest?.id
        }
        publicHelpRequest = (otherRequests.ifEmpty { mockPublicHelpRequests }).random()
    }

    LaunchedEffect(publicHelpRequest?.id) {
        if (!isInPreview && publicHelpRequest != null) {
            playPublicHelpAlertTone()
        }
    }

    LaunchedEffect(currentRoute) {
        canWebViewGoBack = false
    }

    LaunchedEffect(currentRoute, pendingContentFocusRoute, webView, readyWebView) {
        if (pendingContentFocusRoute == currentRoute) {
            when (currentRoute) {
                HomeRoute -> {
                    withFrameNanos { }
                    contentFocusRequester.requestFocus(FocusDirection.Down)
                    pendingContentFocusRoute = null
                }

                is WebRoute -> {
                    webView?.takeIf { it === readyWebView }?.let { currentWebView ->
                        withFrameNanos { }
                        if (pendingContentFocusRoute == currentRoute &&
                            webView === currentWebView &&
                            readyWebView === currentWebView
                        ) {
                            currentWebView.requestFocus()
                            currentWebView.evaluateJavascript(
                                CLEAR_WEB_DOM_FOCUS_SCRIPT,
                                null,
                            )
                            pendingContentFocusRoute = null
                        }
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
            Spacer(modifier = Modifier.height(14.dp))
            NativeHeader(
                selectedTab = selectedTab,
                focusedTab = lastFocusedTab,
                tabFocusRequesters = tabFocusRequesters,
                onTabFocused = { tabIndex ->
                    if (pendingContentFocusRoute != null ||
                        pendingTabFocusIndex != null ||
                        tabIndex == selectedTab
                    ) {
                        lastFocusedTab = tabIndex
                    } else {
                        activateTab(tabIndex, moveFocusToContent = false)
                    }
                },
                onTabSelected = { tabIndex -> activateTab(tabIndex, moveFocusToContent = false) },
                onTabDown = { tabIndex -> activateTab(tabIndex, moveFocusToContent = true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MainContentHorizontalPadding)
                    .onFocusChanged { headerHasFocus = it.hasFocus },
            )
            Spacer(modifier = Modifier.height(6.dp))
            NavDisplay(
                backStack = backStack,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = MainContentHorizontalPadding)
                    .clipToBounds(),
                onBack = ::handleBack,
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                entryProvider = entryProvider<NavKey> {
                    entry<HomeRoute> {
                        HomeScreen(
                            active = currentRoute == HomeRoute && publicHelpRequest == null,
                            modifier = Modifier.fillMaxSize(),
                            partyStats = partyStats,
                            contentFocusRequester = contentFocusRequester,
                            onRequestTabFocus = ::requestSelectedTabFocus,
                            onCoursewareClick = ::openCourseware,
                            onPartyBuildingClick = ::openPartyBuilding,
                        )
                    }
                    entry<WebRoute> { route ->
                        WebContent(
                            url = route.url,
                            active = currentRoute == route && publicHelpRequest == null,
                            onCreated = { createdView ->
                                if (currentRoute == route) webView = createdView
                            },
                            onPageReadyChanged = { changedView, ready ->
                                if (currentRoute == route) {
                                    if (ready) {
                                        readyWebView = changedView
                                    } else if (readyWebView === changedView) {
                                        readyWebView = null
                                    }
                                }
                            },
                            onReleased = { releasedView ->
                                if (webView === releasedView) webView = null
                                if (readyWebView === releasedView) readyWebView = null
                            },
                            onCanGoBackChanged = { canGoBack ->
                                if (currentRoute == route) canWebViewGoBack = canGoBack
                            },
                            onRequestNativeFocus = ::requestSelectedTabFocus,
                            onShowPublicHelpRequest = { request ->
                                publicHelpRequest = request
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = 8.dp,
                                    bottom = 10.dp,
                                ),
                        )
                    }
                },
            )
        }

        publicHelpRequest?.let { request ->
            PublicHelpRequestDialog(
                request = request,
                onDismiss = { publicHelpRequest = null },
                onHandled = { publicHelpRequest = null },
                onContactLater = { publicHelpRequest = null },
            )
        }
    }
}

private suspend fun playPublicHelpAlertTone() {
    val toneGenerator = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    }.getOrNull() ?: return

    try {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 380)
        delay(520L)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 520)
        delay(600L)
    } finally {
        toneGenerator.release()
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
