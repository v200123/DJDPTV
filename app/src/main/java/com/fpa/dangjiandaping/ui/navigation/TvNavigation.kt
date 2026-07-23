package com.fpa.dangjiandaping.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

private const val BASE_URL = "http://192.168.20.233:5173/xiaoyuTv/#/"

private const val HOME_WEB_URL = "$BASE_URL"
private const val PARTY_MEMBER_URL = "${BASE_URL}find-party-member"
private const val kangba_URL = "${BASE_URL}kangba"
private const val teacher_URL = "${BASE_URL}teach"
private const val NAV_BASE_URL = "${BASE_URL}base"
private const val courseware_URL = "${BASE_URL}courseware"
private const val jicengdangjian_URL = "${BASE_URL}basic-party-building"
private const val party_URL = "${BASE_URL}party"

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
    TvTabSpec("咔哒时间·康巴党旗红", 1.65f, TvTabDestination.Web(kangba_URL)),
    TvTabSpec("师资库", 0.80f, TvTabDestination.Web(teacher_URL)),
    TvTabSpec("阵地库", 0.80f, TvTabDestination.Web(NAV_BASE_URL)),
    TvTabSpec("课件库", 0.80f, TvTabDestination.Web(courseware_URL)),
    TvTabSpec("基层党建", 0.80f, TvTabDestination.Web(jicengdangjian_URL)),
    TvTabSpec("我的党支部", 1.10f, TvTabDestination.Web(party_URL))
)
