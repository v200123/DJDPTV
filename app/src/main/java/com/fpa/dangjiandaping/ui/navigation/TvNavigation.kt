package com.fpa.dangjiandaping.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.fpa.dangjiandaping.BuildConfig
import kotlinx.serialization.Serializable

private val BASE_URL = BuildConfig.BASE_URL

private val HOME_WEB_URL = BASE_URL
private val PARTY_MEMBER_URL = "${BASE_URL}find-party-member"
private val kangba_URL = "${BASE_URL}kangba"
private val teacher_URL = "${BASE_URL}teacher"
private val NAV_BASE_URL = "${BASE_URL}base"
private val courseware_URL = "${BASE_URL}courseware"
private val jicengdangjian_URL = "${BASE_URL}basic-party-building"
private val peixunban_URL = "${BASE_URL}training-class"
private val party_URL = "${BASE_URL}party"

internal sealed interface TvTabDestination {
    data object NativeHome : TvTabDestination

    data class Web(val url: String) : TvTabDestination
}

@Serializable
internal sealed interface TvRoute : NavKey

@Serializable
internal data object HomeRoute : TvRoute

@Serializable
internal data class WebRoute(
    val tabIndex: Int,
    val url: String,
) : TvRoute

internal fun TvTabDestination.toRoute(tabIndex: Int): TvRoute = when (this) {
    TvTabDestination.NativeHome -> HomeRoute
    is TvTabDestination.Web -> WebRoute(tabIndex = tabIndex, url = url)
}

internal fun partyBuildingRoute(channelId: Int): WebRoute = WebRoute(
    tabIndex = 6,
    url = "$jicengdangjian_URL?id=$channelId",
)

internal fun coursewareRoute(type: Int): WebRoute = WebRoute(
    tabIndex = 5,
    url = "$courseware_URL?type=$type",
)

internal data class TvTabSpec(
    val title: String,
    val widthWeight: Float,
    val destination: TvTabDestination,
    val recommended: Boolean = false
)

internal val TV_TABS = listOf(
    TvTabSpec("首页", 0.75f, TvTabDestination.NativeHome),
    TvTabSpec(
        "有事找党员",
        1.10f,
        TvTabDestination.Web(PARTY_MEMBER_URL),
        recommended = true
    ),
//    TvTabSpec("咔哒时间·康巴党旗红", 1.65f, TvTabDestination.Web(kangba_URL)),
    TvTabSpec("师资库", 0.80f, TvTabDestination.Web(teacher_URL)),
    TvTabSpec("阵地库", 0.80f, TvTabDestination.Web(NAV_BASE_URL)),
    TvTabSpec("课件库", 0.80f, TvTabDestination.Web(courseware_URL)),
    TvTabSpec("培训班", 0.80f, TvTabDestination.Web(peixunban_URL)),
    TvTabSpec("基层党建", 0.80f, TvTabDestination.Web(jicengdangjian_URL)),
    TvTabSpec("我的党支部", 1.10f, TvTabDestination.Web(party_URL))
)
